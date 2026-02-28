package com.example.redisson.service;

import com.example.redisson.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
     * 构造方法，初始化服务并加载部分模拟数据
     *
     * @param redissonClient Redisson 客户端实例
     */
    public ProductService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        // 初始化一些测试数据到模拟数据库
        productDatabase.put(1L, new Product(1L, "Laptop", 100));
        productDatabase.put(2L, new Product(2L, "Smartphone", 200));
    }

    /**
     * 获取商品信息
     * 演示读锁（Read Lock）的使用，以确保从缓存和数据库中读取数据时的一致性
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    public Product getProduct(Long id) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(PRODUCT_LOCK_PREFIX + id);

        // 1. 获取读锁，允许多个线程并发读取，但会阻止写锁的获取。最大等待 3 秒。
        boolean isLocked = false;
        try {
            isLocked = rwLock.readLock().tryLock(3, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Acquire read lock timeout for product ID: {}", id);
                throw new RuntimeException("获取读锁超时，请稍后再试");
            }
            log.info("Acquired read lock for product ID: {}", id);

            // 2. 查询缓存
            RBucket<Product> bucket = redissonClient.getBucket(PRODUCT_CACHE_PREFIX + id);
            Product cachedProduct = bucket.get();
            if (cachedProduct != null) {
                log.info("Cache hit for product ID: {}", id);
                return cachedProduct;
            }

            // 3. 缓存未命中：查询数据库并回写缓存
            log.info("Cache miss for product ID: {}, querying DB...", id);
            Product dbProduct = productDatabase.get(id);
            if (dbProduct != null) {
                // 设置缓存，并指定过期时间以避免产生死数据
                bucket.set(dbProduct, Duration.ofHours(1));
                log.info("Updated cache for product ID: {}", id);
            }
            return dbProduct;

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
            bucket.delete();
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
}
