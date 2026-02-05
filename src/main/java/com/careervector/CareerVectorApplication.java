package com.careervector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@EnableJpaRepositories(basePackages = "com.careervector.repo")
@SpringBootApplication(scanBasePackages = "com.careervector")
public class CareerVectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerVectorApplication.class, args);
    }

}
