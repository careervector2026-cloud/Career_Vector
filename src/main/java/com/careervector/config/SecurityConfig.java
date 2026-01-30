package com.careervector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${FRONTEND_URL}")
    private String frontend_Url;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for REST APIs
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    // UPDATE: Allow both your deployed frontend AND localhost
                    config.setAllowedOrigins(Arrays.asList(
                            frontend_Url,               // Your Deployed Frontend (from env var)
                            "http://localhost:5173",    // Vite/React Localhost
                            "http://localhost:3000"     // Alternative Localhost port
                    ));

                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true); // Important for cookies/sessions if you use them
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/student/**").permitAll()
                        .requestMatchers("/api/recruiter/**").permitAll()
                        .requestMatchers("/api/admins/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}