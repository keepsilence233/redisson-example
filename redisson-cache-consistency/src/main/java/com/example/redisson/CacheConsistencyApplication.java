package com.example.redisson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 缓存一致性示例应用主启动类
 */
@SpringBootApplication
public class CacheConsistencyApplication {

    /**
     * 程序入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CacheConsistencyApplication.class, args);
    }
}
