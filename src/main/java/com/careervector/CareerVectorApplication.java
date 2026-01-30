package com.careervector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CareerVectorApplication {

    public static void main(String[] args) {
        // --- DEBUG BLOCK: RUNS BEFORE APP STARTS ---
        System.out.println("========================================");
        System.out.println("DEBUG: CHECKING RENDER VARIABLES...");

        // Read the raw environment variable from Render
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USERNAME");

        System.out.println("RAW DB_URL: [" + dbUrl + "]");
        System.out.println("RAW DB_USER: [" + dbUser + "]");
        System.out.println("========================================");
        // -------------------------------------------

        SpringApplication.run(CareerVectorApplication.class, args);
    }
}