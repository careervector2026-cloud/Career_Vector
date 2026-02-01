package com.careervector.service;

import com.careervector.model.Student;
import com.careervector.repo.StudentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {
    @Autowired
    private StudentRepo studentRepo;

    @Value("${Storage_url}")
    private String Storage_url;

    @Value("${secret_key}")
    private String secret_key;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasswordEncoder passwordEncoder;

    // CHANGED: Use EmailService instead of JavaMailSender
    @Autowired
    private EmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean verifyOtp(String email, String otpInput) {
        String storedOtp = redisTemplate.opsForValue().get(email);
        if (storedOtp != null && storedOtp.equals(otpInput)) {
            redisTemplate.delete(email); // Delete OTP after successful use
            return true;
        }
        return false;
    }

    public void resetPassword(String email, String otp, String newPassword) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) {
            throw new RuntimeException("User not found.");
        }

        if (!verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or Expired OTP.");
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        studentRepo.save(student);
    }

    public Student save(
            String rollNumber, String fullName, String email, String password, String userName,
            String dept, String branch, String mobileNumber, String semester, String year,
            MultipartFile image, MultipartFile resume,
            String semesterGPAsJson,
            String leetCodeUrl, String githubUrl
    ) {
        Student s = new Student();
        s.setRollNumber(rollNumber);
        s.setFullName(fullName);
        s.setEmail(email);
        s.setPassword(passwordEncoder.encode(password));
        s.setUserName(userName);
        s.setDept(dept);
        s.setBranch(branch);
        s.setMobileNumber(mobileNumber);
        s.setLeetcodeurl(leetCodeUrl);
        s.setGithubUrl(githubUrl);
        s.setVerified(true);

        try {
            s.setSemester(Integer.parseInt(semester));
            s.setYear(Integer.parseInt(year));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Semester or Year format");
        }

        if (semesterGPAsJson != null && !semesterGPAsJson.isEmpty()) {
            try {
                Map<String, String> gpaMap = objectMapper.readValue(semesterGPAsJson, new TypeReference<HashMap<String,String>>() {});
                s.setGpaSem1(parseGpa(gpaMap.get("sem1")));
                s.setGpaSem2(parseGpa(gpaMap.get("sem2")));
                s.setGpaSem3(parseGpa(gpaMap.get("sem3")));
                s.setGpaSem4(parseGpa(gpaMap.get("sem4")));
                s.setGpaSem5(parseGpa(gpaMap.get("sem5")));
                s.setGpaSem6(parseGpa(gpaMap.get("sem6")));
                s.setGpaSem7(parseGpa(gpaMap.get("sem7")));
                s.setGpaSem8(parseGpa(gpaMap.get("sem8")));
            } catch (Exception e) {
                System.err.println("Error parsing GPA JSON: " + e.getMessage());
            }
        }

        if (image != null && !image.isEmpty()) {
            String fileName = rollNumber + "_avatar_" + System.currentTimeMillis() + ".png";
            uploadFile(image, "images", fileName);
            s.setProfileImageUrl(Storage_url + "public/images/" + fileName);
        }

        if (resume != null && !resume.isEmpty()) {
            String fileName = rollNumber + "_resume_" + System.currentTimeMillis() + ".pdf";
            uploadFile(resume, "resumes", fileName);
            s.setResumeUrl(Storage_url + "public/resumes/" + fileName);
        }

        return studentRepo.save(s);
    }

    private Double parseGpa(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void uploadFile(MultipartFile file, String bucket, String fileName) {
        try {
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secret_key);
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            String baseUploadUrl = Storage_url.endsWith("/") ? Storage_url : Storage_url + "/";
            String finalUrl = baseUploadUrl + bucket + "/" + fileName;

            rest.postForEntity(finalUrl, entity, String.class);
        } catch (IOException e) {
            throw new RuntimeException("Supabase Storage Upload failed: " + e.getMessage());
        }
    }

    public Student findStudent(String userNameEmail) {
        Student student = null;
        if((student = studentRepo.findByUserName(userNameEmail))!=null)return student;
        else if((student = studentRepo.findByEmail(userNameEmail))!=null)return student;
        else return null;
    }

    // --- Send OTP for SIGNUP ---
    public void generateAndSendOtp(String email) {
        if(studentRepo.findByEmail(email) != null) {
            throw new RuntimeException("Email is already registered. Please login");
        }
        sendEmailOtp(email, "Welcome to CareerVector! Your Verification Code is: ");
    }

    // --- Send OTP for RESET PASSWORD ---
    public void generateAndSendOtpForReset(String email) {
        if(studentRepo.findByEmail(email) == null) {
            throw new RuntimeException("Email not found in our records.");
        }
        sendEmailOtp(email, "CareerVector Password Reset. Your Verification Code is: ");
    }

    // UPDATED Helper: Uses EmailService (Brevo API)
    private void sendEmailOtp(String email, String messagePrefix) {
        String otp = String.valueOf(new Random().nextInt(900000)+100000);

        // Store in Redis
        redisTemplate.opsForValue().set(email, otp, 5, TimeUnit.MINUTES);

        try{
            // Call EmailService
            String body = messagePrefix + otp + "\n\nThis code expires in 5 minutes";
            emailService.sendEmail(email, "CareerVector Verification Code", body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}