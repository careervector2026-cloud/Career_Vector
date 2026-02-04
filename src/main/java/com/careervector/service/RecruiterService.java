package com.careervector.service;

import com.careervector.dto.RecruiterUpdateDto;
import com.careervector.model.Recruiter;
import com.careervector.repo.RecruiterRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class RecruiterService {

    @Autowired
    private RecruiterRepo recruiterRepo;

    @Autowired
    private SupabaseService supabaseService; // Injected Service

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // --- OTP Logic ---
    public void generateAndSendOtp(String email) {
        if (recruiterRepo.findByEmail(email) != null) {
            throw new RuntimeException("Email is already registered. Please login.");
        }
        sendEmailOtp(email, "Welcome Recruiter! Your verification code is: ");
    }

    public void generateAndSendOtpForReset(String email) {
        if (recruiterRepo.findByEmail(email) == null) {
            throw new RuntimeException("Email not found. Please register first.");
        }
        sendEmailOtp(email, "Password Reset Request. Your verification code is: ");
    }

    public void resetPassword(String email, String otp, String newPassword) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if (recruiter == null) throw new RuntimeException("User not found.");
        if (!verifyOtp(email, otp)) throw new RuntimeException("Invalid or Expired verification code.");

        recruiter.setPassword(passwordEncoder.encode(newPassword));
        recruiterRepo.save(recruiter);
    }

    private void sendEmailOtp(String email, String messagePrefix) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        redisTemplate.opsForValue().set(email, otp, 5, TimeUnit.MINUTES);
        try {
            String body = messagePrefix + otp + "\n\nThis code expires in 5 minutes.";
            emailService.sendEmail(email, "CareerVector Verification", body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otpInput) {
        String storedOtp = redisTemplate.opsForValue().get(email);
        if (storedOtp != null && storedOtp.equals(otpInput)) {
            redisTemplate.delete(email);
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

        // --- Use Supabase Service for Upload ---
        if(image != null && !image.isEmpty()){
            String safeUserName = (userName != null && !userName.isEmpty()) ? userName : "recruiter";
            String fileName = safeUserName + "_avatar_" + System.currentTimeMillis() + ".png";
            String url = supabaseService.uploadFile(image, "recruiter-images", fileName);
            r.setImageUrl(url);
        }

        return recruiterRepo.save(r);
    }

    // Overloaded for JWT Auth if needed, or stick to this if email is in DTO
    public Recruiter updateREcruiterProfile(RecruiterUpdateDto recruiterUpdateDto) {
        if(recruiterUpdateDto.getEmail() == null || recruiterUpdateDto.getEmail().isEmpty())
            throw new RuntimeException("Email is Required");

        Recruiter recruiter = recruiterRepo.findByEmail(recruiterUpdateDto.getEmail());
        if(recruiter == null) throw new RuntimeException("Recruiter Not Found");

        if(recruiterUpdateDto.getMobile() != null && !recruiterUpdateDto.getMobile().isEmpty())
            recruiter.setMobile(recruiterUpdateDto.getMobile());

        if(recruiterUpdateDto.getCompanyName() != null && !recruiterUpdateDto.getCompanyName().isEmpty())
            recruiter.setCompanyName(recruiterUpdateDto.getCompanyName());

        if(recruiterUpdateDto.getRole() != null && !recruiterUpdateDto.getRole().isEmpty())
            recruiter.setRole(recruiterUpdateDto.getRole());

        return recruiterRepo.save(recruiter);
    }

    // Overloaded method for JWT (optional, based on previous prompt)
    public Recruiter updateREcruiterProfile(String email, RecruiterUpdateDto recruiterUpdateDto) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if(recruiter == null) throw new RuntimeException("Recruiter Not Found (Auth Error)");

        if(recruiterUpdateDto.getMobile() != null && !recruiterUpdateDto.getMobile().isEmpty())
            recruiter.setMobile(recruiterUpdateDto.getMobile());

        if(recruiterUpdateDto.getCompanyName() != null && !recruiterUpdateDto.getCompanyName().isEmpty())
            recruiter.setCompanyName(recruiterUpdateDto.getCompanyName());

        if(recruiterUpdateDto.getRole() != null && !recruiterUpdateDto.getRole().isEmpty())
            recruiter.setRole(recruiterUpdateDto.getRole());

        return recruiterRepo.save(recruiter);
    }

    public void changePassword(String email, String password) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if(recruiter == null) throw new RuntimeException("Recruiter not Found");
        recruiter.setPassword(passwordEncoder.encode(password));
        recruiterRepo.save(recruiter);
    }

    // --- Upload Profile Pic (Update) ---
    public String uploadProfilePic(String email, MultipartFile file) {
        Recruiter recruiter = recruiterRepo.findByEmail(email);
        if(recruiter == null) throw new RuntimeException("Recruiter not Found");

        // Delete old image if exists
        String oldUrl = recruiter.getImageUrl();
        if(oldUrl != null && !oldUrl.isEmpty()){
            String oldFileName = supabaseService.extractFileNameFromUrl(oldUrl);
            if(oldFileName != null) {
                supabaseService.deleteFile("recruiter-images", oldFileName);
            }
        }

        // Upload new image
        String fileName = recruiter.getUserName() + "_avatar_" + System.currentTimeMillis() + ".png";
        String newUrl = supabaseService.uploadFile(file, "recruiter-images", fileName);

        recruiter.setImageUrl(newUrl);
        recruiterRepo.save(recruiter);
        return newUrl;
    }
}