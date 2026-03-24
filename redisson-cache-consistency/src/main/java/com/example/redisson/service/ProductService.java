package com.example.redisson.service;

import com.example.redisson.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务类
 * 负责商品信息的数据库与缓存访问，并使用 Redisson 读写锁（RReadWriteLock）保证缓存与数据库的数据一致性。
 */
@Slf4j
@Service
public class ProductService {

    private final RedissonClient redissonClient;

    /**
     * 模拟数据库，使用 ConcurrentHashMap 存储数据
     */
    private final Map<Long, Product> productDatabase = new ConcurrentHashMap<>();

    /**
     * 商品缓存键前缀
     */
    private static final String PRODUCT_CACHE_PREFIX = "product:cache:";

    /**
     * 商品分布式锁前缀
     */
    private static final String PRODUCT_LOCK_PREFIX = "product:lock:";
    /**
     * 缓存空值占位（用于防止缓存穿透）
     * 约定 ID=-1 为保留值，实际业务不应使用
     */
    private static final long NULL_PRODUCT_ID = -1L;
    private static final Product NULL_PRODUCT = new Product(NULL_PRODUCT_ID, "__NULL__", 0);

    /**
     * 正常缓存与空值缓存的 TTL
     */
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(1);
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(2);
    private static final int NULL_CACHE_JITTER_SECONDS = 30;
    /**
     * 锁等待与缓存重试策略（折中方案）
     * - 读锁等待时间稍长，尽量保证读路径可用
     * - 写锁等待时间较短，避免用户等待太久
     * - 写锁获取失败后，短暂重试读取缓存，避免误判为“数据不存在”
     */
    private static final long READ_LOCK_WAIT_SECONDS = 3;
    private static final long WRITE_LOCK_WAIT_MILLIS = 300;
    private static final int CACHE_RETRY_TIMES = 3;
    private static final long CACHE_RETRY_SLEEP_MILLIS = 50;
    /**
     * 缓存删除重试策略
     */
    private static final int CACHE_DELETE_RETRY_TIMES = 3;
    private static final long CACHE_DELETE_RETRY_SLEEP_MILLIS = 50;

    /**
     * 构造方法，初始化服务并加载部分模拟数据
     *
     * @param redissonClient Redisson 客户端实例
     */
    public ProductService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        // 初始化一些测试数据到模拟数据库（1..500）
        for (long i = 1; i <= 500; i++) {
            productDatabase.put(i, new Product(i, "Product-" + i, (int) (100 + i)));
        }
    }

    /**
     * 获取商品信息
     * 采用“短写锁 + 缓存重试”的折中方案：
     * 1) 先走读锁读缓存（并发友好）
     * 2) 缓存未命中后释放读锁，尝试短时写锁重建缓存
     * 3) 写锁未获取到，则短暂重试读取缓存，避免误判不存在
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    public Product getProduct(Long id) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(PRODUCT_LOCK_PREFIX + id);
        RBucket<Product> bucket = redissonClient.getBucket(PRODUCT_CACHE_PREFIX + id);

        // 1) 获取读锁：允许并发读取，尽量快速返回
        boolean isLocked = false;
        try {
            isLocked = rwLock.readLock().tryLock(READ_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Acquire read lock timeout for product ID: {}", id);
                throw new RuntimeException("获取读锁超时，请稍后再试");
            }
            log.info("Acquired read lock for product ID: {}", id);

            // 2) 读缓存：命中直接返回
            Product cachedProduct = bucket.get();
            if (cachedProduct != null) {
                if (isNullProduct(cachedProduct)) {
                    log.info("Cache hit (null placeholder) for product ID: {}", id);
                    return null;
                }
                log.info("Cache hit for product ID: {}", id);
                return cachedProduct;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Acquiring read lock was interrupted for product ID: {}", id, e);
            throw new RuntimeException("系统繁忙，获取信息失败", e);
        } finally {
            // 释放读锁
            if (isLocked) {
                rwLock.readLock().unlock();
                log.info("Released read lock for product ID: {}", id);
            }
        }

        // 3) 缓存未命中：尝试短写锁，做单线程回填
        boolean writeLocked = false;
        try {
            writeLocked = rwLock.writeLock().tryLock(WRITE_LOCK_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            if (writeLocked) {
                log.info("Acquired write lock for product ID: {}", id);

                // 3.1) 写锁到手后二次检查缓存，避免重复回填
                Product cachedProduct = bucket.get();
                if (cachedProduct != null) {
                    if (isNullProduct(cachedProduct)) {
                        log.info("Cache hit (null placeholder) after write lock for product ID: {}", id);
                        return null;
                    }
                    log.info("Cache hit after write lock for product ID: {}", id);
                    return cachedProduct;
                }

                // 3.2) 仍未命中则查询数据库并回写缓存
                log.info("Cache miss for product ID: {}, querying DB...", id);
                Product dbProduct = productDatabase.get(id);
                if (dbProduct != null) {
                    bucket.set(dbProduct, PRODUCT_CACHE_TTL);
                    log.info("Updated cache for product ID: {}", id);
                } else {
                    // 缓存空值，短 TTL + 抖动，防止缓存穿透与雪崩
                    Duration nullTtl = nullCacheTtlWithJitter();
                    bucket.set(NULL_PRODUCT, nullTtl);
                    log.info("Cached null placeholder for product ID: {} with TTL {}s", id, nullTtl.toSeconds());
                }
                return dbProduct;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Acquiring write lock was interrupted for product ID: {}", id, e);
            throw new RuntimeException("系统繁忙，获取信息失败", e);
        } finally {
            if (writeLocked && rwLock.writeLock().isHeldByCurrentThread()) {
                rwLock.writeLock().unlock();
                log.info("Released write lock for product ID: {}", id);
            }
        }

        // 4) 写锁未获取到：短暂重试读取缓存，避免误判不存在
        for (int i = 0; i < CACHE_RETRY_TIMES; i++) {
            Product cachedProduct = bucket.get();
            if (cachedProduct != null) {
                if (isNullProduct(cachedProduct)) {
                    log.info("Cache hit (null placeholder) after retry for product ID: {}", id);
                    return null;
                }
                log.info("Cache hit after retry for product ID: {}", id);
                return cachedProduct;
            }
            // 短暂等待，让持有写锁的线程完成回填
            try {
                Thread.sleep(CACHE_RETRY_SLEEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Cache retry sleep interrupted for product ID: {}", id, e);
                throw new RuntimeException("系统繁忙，获取信息失败", e);
            }
        }

        // 5) 仍未命中：明确告知繁忙，避免把“未回填”当作“无数据”
        throw new RuntimeException("系统繁忙，请稍后再试");
    }

    /**
     * 更新商品信息
     * 演示写锁（Write Lock）的使用，以确保更新数据库和清除缓存时的排他性和数据一致性
     *
     * @param product 包含更新内容的商品对象
     * @return 更新后的商品信息
     */
    public Product updateProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product or Product ID must not be null");
        }

        Long id = product.getId();
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(PRODUCT_LOCK_PREFIX + id);

        // 1. 获取写锁，独占锁，直到写锁释放前，其他读锁和写锁都会被阻塞。最大等待 3 秒。
        boolean isLocked = false;
        try {
            isLocked = rwLock.writeLock().tryLock(3, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Acquire write lock timeout for product ID: {}", id);
                throw new RuntimeException("获取写锁超时，请稍后再试");
            }
            log.info("Acquired write lock for product ID: {}", id);

            // 2. 更新数据库
            productDatabase.put(id, product);
            log.info("Updated DB for product ID: {}", id);

            // 3. 删除缓存（对于缓存一致性，更新数据库后删除缓存通常比直接更新缓存更安全）
            RBucket<Product> bucket = redissonClient.getBucket(PRODUCT_CACHE_PREFIX + id);
            deleteCacheWithRetry(bucket, id);
            log.info("Evicted cache for product ID: {}", id);

            return product;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Acquiring write lock was interrupted for product ID: {}", id, e);
            throw new RuntimeException("系统繁忙，更新信息失败", e);
        } finally {
            // 释放写锁前确认当前线程持有该锁
            if (isLocked && rwLock.writeLock().isHeldByCurrentThread()) {
                rwLock.writeLock().unlock();
                log.info("Released write lock for product ID: {}", id);
            }
        }
    }

    private boolean isNullProduct(Product product) {
        return product != null && Objects.equals(product.getId(), NULL_PRODUCT_ID);
    }

    private Duration nullCacheTtlWithJitter() {
        int jitter = ThreadLocalRandom.current().nextInt(NULL_CACHE_JITTER_SECONDS + 1);
        return NULL_CACHE_TTL.plusSeconds(jitter);
    }

    /**
     * 删除缓存时带重试机制
     * 如果删除失败，会重试多次，避免缓存未删除导致数据不一致
     *
     * @param bucket 缓存桶
     * @param id 商品 ID
     */
    private void deleteCacheWithRetry(RBucket<Product> bucket, Long id) {
        for (int i = 0; i < CACHE_DELETE_RETRY_TIMES; i++) {
            try {
                boolean deleted = bucket.delete();
                if (deleted) {
                    log.info("Cache deleted successfully for product ID: {}", id);
                    return;
                }
                log.warn("Cache delete returned false for product ID: {}, retry {}/{}", id, i + 1, CACHE_DELETE_RETRY_TIMES);
            } catch (Exception e) {
                log.warn("Cache delete failed for product ID: {}, retry {}/{}", id, i + 1, CACHE_DELETE_RETRY_TIMES, e);
            }
            try {
                Thread.sleep(CACHE_DELETE_RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("缓存删除被中断", ie);
            }
        }
        log.error("Cache delete failed after {} retries for product ID: {}", CACHE_DELETE_RETRY_TIMES, id);
    }
}
