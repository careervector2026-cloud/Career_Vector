//AdminService.java
package com.careervector.service;

import com.careervector.service.StudentService;
import com.careervector.model.Admin;
import com.careervector.model.Student;
import com.careervector.repo.AdminRepo; // Ensure you create this Repository

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AdminService {

    @Autowired
    private AdminRepo adminRepo;

    @Autowired
    private SupabaseService supabaseService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StudentService studentService;
    
    public void generateAndSendOtp(String email) {
        if (adminRepo.findByEmail(email) != null) {
            throw new RuntimeException("Admin email is already registered.");
        }
        otpService.generateAndSendOtp(email, "Admin Verification", "Welcome Admin! Your verification code is: ");
    }

    public boolean verifyOtp(String email, String otpInput) {
        return otpService.verifyOtp(email, otpInput);
    }

    public Admin findAdmin(String identifier) {
        Admin admin = adminRepo.findByUserName(identifier);
        if (admin == null) {
            admin = adminRepo.findByEmail(identifier);
        }
        return admin;
    }

    public Admin save(String name, String username, String email, String institute, String password, MultipartFile photo) {
        Admin admin = new Admin();
        admin.setName(name);
        admin.setUserName(username);
        admin.setEmail(email);
        admin.setInstituteName(institute);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setVerified(true);

        if (photo != null && !photo.isEmpty()) {
            String fileName = "admin_" + username + "_" + System.currentTimeMillis() + ".png";
            // Using "admin-images" bucket (ensure it exists in Supabase)
            String url = supabaseService.uploadFile(photo, "admin-images", fileName);
            admin.setImageUrl(url);
        }

        return adminRepo.save(admin);
    }
    public List<Student> getStudents(String college_name) {
    	return studentService.getStudentsWithCollegeName(college_name);
    }
    @Autowired 
    @Qualifier("fastApiRestTemplate") 
    private RestTemplate fastApiRestTemplate;

    @Value("${fastapi.url}")
    private String fastApiUrl;

    // --- Helper Method for GET Requests ---
    private Object executeGetRequest(String path, String paramName, String paramValue) {
        String url = UriComponentsBuilder.fromHttpUrl(fastApiUrl + path)
                .queryParam(paramName, paramValue)
                .build()
                .toUriString();
        try {
            Object response = fastApiRestTemplate.getForObject(url, Object.class);
            return response;
        } catch (Exception e) {
            System.err.println("DEBUG: Error -> " + e.getMessage());
            throw new RuntimeException("FastAPI Error: " + e.getMessage());
        }
    }

    public Object fetchPlacementFunnel(String collegeName) {
        return executeGetRequest("/admin/placement-funnel", "college_name", collegeName);
    }

    public Object fetchTopStudents(String collegeName) {
        return executeGetRequest("/admin/top-students", "college_name", collegeName);
    }

    public Object fetchAtRiskStudents(String collegeName) {
        return executeGetRequest("/admin/at-risk-students", "college_name", collegeName);
    }

    public Object fetchSkillGapTrends(String collegeName) {
        return executeGetRequest("/admin/skill-gap-trends", "college_name", collegeName);
    }

    public Object fetchStudentProgression(String studentId) {
        return executeGetRequest("/admin/student-progression", "student_id", studentId);
    }
}