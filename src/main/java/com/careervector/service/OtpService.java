package com.careervector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmailService emailService;

    // Generate, Store in Redis, and Send Email
    public void generateAndSendOtp(String email, String subject, String messagePrefix) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000); // 6-digit OTP

        // Store in Redis for 5 minutes
        redisTemplate.opsForValue().set(email, otp, 5, TimeUnit.MINUTES);

        try {
            String body = messagePrefix + otp + "\n\nThis code expires in 5 minutes.";
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            // If email fails, remove OTP so user can try again immediately
            redisTemplate.delete(email);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    // Verify and Delete OTP
    public boolean verifyOtp(String email, String otpInput) {
        String storedOtp = redisTemplate.opsForValue().get(email);
        if (storedOtp != null && storedOtp.equals(otpInput)) {
            redisTemplate.delete(email); // Prevent reuse
            return true;
        }
        return false;
    }
}