package com.careervector.service;

import com.careervector.model.Recruiter;
import com.careervector.repo.RecruiterRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate; // ✅ Added Redis
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate; // ✅ Using Redis for consistency

    @Value("${Storage_url}")
    private String Storage_url;

    @Value("${secret_key}")
    private String secret_key;

    // --- OTP Logic ---

    public void generateAndSendOtp(String email) {
        if (recruiterRepo.findByEmail(email) != null) {
            throw new RuntimeException("Email is already registered. Please login.");
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // Store in Redis for 5 minutes
        redisTemplate.opsForValue().set(email, otp, 5, TimeUnit.MINUTES);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("careervector2026@gmail.com");
            message.setTo(email);
            message.setSubject("CareerVector Recruiter Verification");
            message.setText("Welcome Recruiter!\n\nYour verification code is: " + otp + "\n\nThis code expires in 5 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email.");
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

    // --- User Management Logic ---

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

        // ✅ Mark verified since OTP check passed in Controller
        r.setVerified(true);

        // Image Upload Logic
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