package com.careervector.service;

import com.careervector.dto.RecruiterUpdateDto;
import com.careervector.model.Recruiter;
import com.careervector.repo.RecruiterRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RecruiterService {

    @Autowired
    private RecruiterRepo recruiterRepo;

    @Autowired
    private SupabaseService supabaseService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- OTP Logic ---
    public void generateAndSendOtp(String email) {
        if (recruiterRepo.findByEmail(email) != null) {
            throw new RuntimeException("Email is already registered. Please login.");
        }
        otpService.generateAndSendOtp(email, "CareerVector Verification", "Welcome Recruiter! Your verification code is: ");
    }

    public void generateAndSendOtpForReset(String email) {
        if (recruiterRepo.findByEmail(email) == null) {
            throw new RuntimeException("Email not found. Please register first.");
        }
        otpService.generateAndSendOtp(email, "Password Reset", "Password Reset Request. Your verification code is: ");
    }

    public void resetPassword(String email, String otp, String newPassword) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if (recruiter == null) throw new RuntimeException("User not found.");

        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or Expired verification code.");
        }

        recruiter.setPassword(passwordEncoder.encode(newPassword));
        recruiterRepo.save(recruiter);
    }

    public boolean verifyOtp(String email, String otpInput) {
        return otpService.verifyOtp(email, otpInput);
    }

    // --- User Logic ---
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
            String url = supabaseService.uploadFile(image, "recruiter-images", fileName);
            r.setImageUrl(url);
        }

        return recruiterRepo.save(r);
    }

    public Recruiter updateREcruiterProfile(RecruiterUpdateDto dto) {
        if(dto.getEmail() == null || dto.getEmail().isEmpty()) throw new RuntimeException("Email is Required");

        Recruiter recruiter = recruiterRepo.findByEmail(dto.getEmail());
        if(recruiter == null) throw new RuntimeException("Recruiter Not Found");

        if(dto.getMobile() != null && !dto.getMobile().isEmpty()) recruiter.setMobile(dto.getMobile());
        if(dto.getCompanyName() != null && !dto.getCompanyName().isEmpty()) recruiter.setCompanyName(dto.getCompanyName());
        if(dto.getRole() != null && !dto.getRole().isEmpty()) recruiter.setRole(dto.getRole());

        return recruiterRepo.save(recruiter);
    }

    public void changePassword(String email, String password) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if(recruiter == null) throw new RuntimeException("Recruiter not Found");
        recruiter.setPassword(passwordEncoder.encode(password));
        recruiterRepo.save(recruiter);
    }

    // --- Uploads ---
    public String uploadProfilePic(String email, MultipartFile file) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if(recruiter == null) throw new RuntimeException("Recruiter not Found");

        if(recruiter.getImageUrl() != null && !recruiter.getImageUrl().isEmpty()){
            String oldFileName = supabaseService.extractFileNameFromUrl(recruiter.getImageUrl());
            if(oldFileName != null) supabaseService.deleteFile("recruiter-images", oldFileName);
        }

        String fileName = recruiter.getUserName() + "_avatar_" + System.currentTimeMillis() + ".png";
        String newUrl = supabaseService.uploadFile(file, "recruiter-images", fileName);

        recruiter.setImageUrl(newUrl);
        recruiterRepo.save(recruiter);
        return newUrl;
    }
}