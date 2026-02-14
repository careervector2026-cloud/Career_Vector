package com.careervector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean(name = "fastApiRestTemplate")
    public RestTemplate fastApiRestTemplate() {
        return new RestTemplate();
    }
}