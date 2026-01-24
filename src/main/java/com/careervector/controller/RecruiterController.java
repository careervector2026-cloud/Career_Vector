package com.careervector.controller;

import com.careervector.dto.LoginData;
import com.careervector.model.Recruiter;
import com.careervector.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/recruiter")
@CrossOrigin(origins = "${app.frontend.url}")
public class RecruiterController {

    @Autowired
    private RecruiterService recruiterService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // STEP 1: Generate & Send OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isEmpty()) {
            return new ResponseEntity<>("Email is required", HttpStatus.BAD_REQUEST);
        }
        try {
            recruiterService.generateAndSendOtp(email);
            return ResponseEntity.ok("OTP sent successfully to " + email);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // STEP 2: Verify OTP and Save User
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestParam("email") String email,
            @RequestParam("fullName") String fullName,
            @RequestParam("username") String userName,
            @RequestParam("mobile") String mobile,
            @RequestParam("companyName") String companyName,
            @RequestParam("role") String role,
            @RequestParam("password") String password,
            @RequestParam("otp") String otp, // ✅ Accept OTP
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            // ✅ 1. Verify OTP first
            boolean isOtpValid = recruiterService.verifyOtp(email, otp);

            if (!isOtpValid) {
                return new ResponseEntity<>("Invalid or Expired verification code.", HttpStatus.BAD_REQUEST);
            }

            // ✅ 2. Save User (OTP is valid)
            Recruiter recruiter = recruiterService.save(email, fullName, userName, mobile, companyName, role, password, image);
            return new ResponseEntity<>(recruiter, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginData loginData){
        String identifier = loginData.getEmailOrUsername();
        if(identifier == null || identifier.isEmpty()) identifier = loginData.getEmail();

        if(identifier == null || identifier.isEmpty()){
            return new ResponseEntity<>("Email or Username is Required", HttpStatus.BAD_REQUEST);
        }

        Recruiter recruiter = recruiterService.findUser(identifier);

        if(recruiter != null && passwordEncoder.matches(loginData.getPassword(), recruiter.getPassword())) {
            // Check verification status
            if(!recruiter.isVerified()) {
                return new ResponseEntity<>("Account not verified", HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(recruiter);
        }

        return new ResponseEntity<>("Invalid Credentials", HttpStatus.UNAUTHORIZED);
    }
}