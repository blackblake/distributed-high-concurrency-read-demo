package com.example.hw1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableTransactionManagement
public class Hw1Application {

    public static void main(String[] args) {
        SpringApplication.run(Hw1Application.class, args);
    }
}
