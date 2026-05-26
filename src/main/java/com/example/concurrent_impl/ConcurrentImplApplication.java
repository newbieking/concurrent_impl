package com.example.concurrent_impl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ConcurrentImplApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcurrentImplApplication.class, args);
    }

}
