package com.example.redisson.visit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HyperLogLog 访问统计示例应用主启动类
 */
@SpringBootApplication
public class VisitApplication {

    /**
     * 程序入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(VisitApplication.class, args);
    }
}
