package com.careervector.service;

import com.careervector.model.Recruiter;
import com.careervector.repo.RecruiterRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class RecruiterService {

    @Autowired
    private RecruiterRepo recruiterRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // CHANGED: Use EmailService instead of JavaMailSender
    @Autowired
    private EmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${Storage_url}")
    private String Storage_url;

    @Value("${secret_key}")
    private String secret_key;

    // --- OTP Logic for SIGNUP ---
    public void generateAndSendOtp(String email) {
        if (recruiterRepo.findByEmail(email) != null) {
            throw new RuntimeException("Email is already registered. Please login.");
        }
        sendEmailOtp(email, "Welcome Recruiter! Your verification code is: ");
    }

    // --- OTP Logic for FORGOT PASSWORD ---
    public void generateAndSendOtpForReset(String email) {
        if (recruiterRepo.findByEmail(email) == null) {
            throw new RuntimeException("Email not found. Please register first.");
        }
        sendEmailOtp(email, "Password Reset Request. Your verification code is: ");
    }

    // --- Reset Password Logic ---
    public void resetPassword(String email, String otp, String newPassword) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if (recruiter == null) {
            throw new RuntimeException("User not found.");
        }

        if (!verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or Expired verification code.");
        }

        recruiter.setPassword(passwordEncoder.encode(newPassword));
        recruiterRepo.save(recruiter);
    }

    // --- Helper to send email via Brevo ---
    private void sendEmailOtp(String email, String messagePrefix) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // Store in Redis for 5 minutes
        redisTemplate.opsForValue().set(email, otp, 5, TimeUnit.MINUTES);

        try {
            // UPDATED: Call EmailService
            String body = messagePrefix + otp + "\n\nThis code expires in 5 minutes.";
            emailService.sendEmail(email, "CareerVector Verification", body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otpInput) {
        String storedOtp = redisTemplate.opsForValue().get(email);
        if (storedOtp != null && storedOtp.equals(otpInput)) {
            redisTemplate.delete(email); // Remove OTP after use
            return true;
        }
        return false;
    }

    public Recruiter findUser(String userNameEmail) {
        Recruiter recruiter;
        if((recruiter = recruiterRepo.findByUserName(userNameEmail)) != null) return recruiter;
        else if ((recruiter = recruiterRepo.findByEmail(userNameEmail)) != null) return recruiter;
        else return null;
    }

    public Recruiter save(String email, String fullName, String userName, String mobile, String companyName, String role, String password, MultipartFile image) {
        Recruiter r = new Recruiter();
        r.setEmail(email);
        r.setFullName(fullName);
        r.setUserName(userName);
        r.setPassword(passwordEncoder.encode(password));
        r.setMobile(mobile);
        r.setCompanyName(companyName);
        r.setRole(role);
        r.setVerified(true);

        if(image != null && !image.isEmpty()){
            String safeUserName = (userName != null && !userName.isEmpty()) ? userName : "recruiter";
            String fileName = safeUserName + "_avatar_" + System.currentTimeMillis() + ".png";
            uploadFile(image, "recruiter-images", fileName);
            r.setImageUrl(Storage_url + "public/recruiter-images/" + fileName);
        }

        return recruiterRepo.save(r);
    }

    private void uploadFile(MultipartFile image, String bucket, String fileName) {
        try{
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization","Bearer "+secret_key);
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(image.getBytes(), headers);
            String baseUploadUrl = Storage_url.endsWith("/") ? Storage_url : Storage_url + "/";
            String finalUrl = baseUploadUrl + bucket + "/" + fileName;

            rest.postForEntity(finalUrl, entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Storage Upload failed: " + e.getMessage());
        }
    }
}