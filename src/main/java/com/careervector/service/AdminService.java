//AdminService.java
package com.careervector.service;

import com.careervector.model.Admin;
import com.careervector.repo.AdminRepo; // Ensure you create this Repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}