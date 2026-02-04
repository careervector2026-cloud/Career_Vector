package com.careervector.controller;

import com.careervector.dto.LoginData;
import com.careervector.dto.RecruiterUpdateDto;
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

    // STEP 1: Generate & Send OTP (For SIGNUP - Checks if email is NOT taken)
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

    // --- NEW: Generate & Send OTP (For FORGOT PASSWORD - Checks if email EXISTS) ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isEmpty()) {
            return new ResponseEntity<>("Email is required", HttpStatus.BAD_REQUEST);
        }
        try {
            recruiterService.generateAndSendOtpForReset(email);
            return ResponseEntity.ok("OTP sent successfully for password reset.");
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // --- NEW: Reset Password (Verify OTP & Update Password) ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return new ResponseEntity<>("Email, OTP and New Password are required", HttpStatus.BAD_REQUEST);
        }

        try {
            recruiterService.resetPassword(email, otp, newPassword);
            return ResponseEntity.ok("Password updated successfully. Please login.");
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // STEP 2: Verify OTP and Save User (Signup)
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestParam("email") String email,
            @RequestParam("fullName") String fullName,
            @RequestParam("username") String userName,
            @RequestParam("mobile") String mobile,
            @RequestParam("companyName") String companyName,
            @RequestParam("role") String role,
            @RequestParam("password") String password,
            @RequestParam("otp") String otp,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            boolean isOtpValid = recruiterService.verifyOtp(email, otp);

            if (!isOtpValid) {
                return new ResponseEntity<>("Invalid or Expired verification code.", HttpStatus.BAD_REQUEST);
            }

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
            if(!recruiter.isVerified()) {
                return new ResponseEntity<>("Account not verified", HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(recruiter);
        }

        return new ResponseEntity<>("Invalid Credentials", HttpStatus.UNAUTHORIZED);
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody RecruiterUpdateDto recruiterUpdateDto){
        try{
            Recruiter recruiter = recruiterService.updateREcruiterProfile(recruiterUpdateDto);
            return ResponseEntity.ok(Map.of(
               "sucess",true,
               "message","profile update sucessfull",
               "recruiter",recruiter
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("sucess",false,"message",e.getMessage()));
        }
        catch (Exception e){
            return ResponseEntity.status(500).body(Map.of("sucess",false,"message","Internal Server Error"));
        }
    }

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
        String password = payload.get("password");
        try{
            recruiterService.changePassword(email,password);
            return ResponseEntity.ok("Password updated sucessfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file
    ){
        try{
            if(file.isEmpty())return ResponseEntity.badRequest().body("File is Empty");
            if(email==null || email.isEmpty())return ResponseEntity.badRequest().body("Email is Required");
            String fileUrl = recruiterService.uploadProfilePic(email,file);
            return ResponseEntity.ok(Map.of(
                    "sucess",true,
                    "message","Profile Pic Updated",
                    "url",fileUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("UPload failed" + e.getMessage());
        }
    }
}