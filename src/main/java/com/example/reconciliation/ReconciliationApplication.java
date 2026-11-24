package com.example.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// No @EnableBatchProcessing: Spring Boot auto-configures the JobRepository,
// JobLauncher, transaction manager and metadata tables for us.
@SpringBootApplication
@EnableScheduling
public class ReconciliationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationApplication.class, args);
    }
}
