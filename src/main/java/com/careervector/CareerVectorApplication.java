package com.careervector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class CareerVectorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CareerVectorApplication.class, args);

        // --- ADD THIS DEBUGGING BLOCK ---
        Environment env = context.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        String user = env.getProperty("spring.datasource.username");

        System.out.println("==========================================");
        System.out.println("DEBUG: RENDER DB CONNECTION INFO");
        System.out.println("URL: " + url);
        System.out.println("User: " + user);
        System.out.println("==========================================");
        // --------------------------------
    }
}