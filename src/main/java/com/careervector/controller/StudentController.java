package com.careervector.controller;

import com.careervector.dto.LoginData;
import com.careervector.model.Student;
import com.careervector.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "${app.frontend.url}")
public class StudentController {

    @Autowired
    private StudentService studentService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- 1. SEND OTP (For SIGNUP) ---
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
        if(email != null) email = email.trim(); // FIX: Remove spaces

        if(email == null || email.isEmpty()){
            return new ResponseEntity<>("Email is Required",HttpStatus.BAD_REQUEST);
        }
        try{
            studentService.generateAndSendOtp(email);
            return ResponseEntity.ok("OTP sent successfully to " + email);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }
    }

    // --- 2. FORGOT PASSWORD (SEND OTP for Existing User) ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String,String> payload){
        String email = payload.get("email");

        // --- DEBUGGING LOG ---
        System.out.println("Forgot Password Request Received for: '" + email + "'");

        if(email == null || email.isEmpty()){
            return new ResponseEntity<>("Email is Required", HttpStatus.BAD_REQUEST);
        }

        // FIX: Remove accidental spaces from start/end
        email = email.trim();

        try {
            studentService.generateAndSendOtpForReset(email);
            return ResponseEntity.ok("OTP sent for password reset.");
        } catch (Exception e) {
            System.err.println("Forgot Password Error: " + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // --- 3. RESET PASSWORD (Verify OTP & Update) ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");

        if(email != null) email = email.trim(); // FIX: Trim email
        if(otp != null) otp = otp.trim();       // FIX: Trim OTP

        if(email == null || otp == null || newPassword == null){
            return new ResponseEntity<>("Email, OTP and New Password are required", HttpStatus.BAD_REQUEST);
        }

        try {
            studentService.resetPassword(email, otp, newPassword);
            return ResponseEntity.ok("Password updated successfully.");
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // --- 4. SIGNUP (Keep existing code, just ensure email is trimmed) ---
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestParam("roll") String rollNumber,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("otp") String otp,
            @RequestParam(value = "username", required = false) String userName,
            @RequestParam("dept") String dept,
            @RequestParam("branch") String branch,
            @RequestParam("mobile") String mobileNumber,
            @RequestParam("sem") String semester,
            @RequestParam("year") String year,
            @RequestParam(value = "profilePic", required = false) MultipartFile image,
            @RequestParam(value = "resume", required = false) MultipartFile resume,
            @RequestParam(value = "semesterGPAs", required = false) String semesterGPAsJson,
            @RequestParam(value = "leetcode", required = false) String leetCodeUrl,
            @RequestParam(value = "github", required = false) String githubUrl
    ) {
        try {
            // Trim inputs
            email = email.trim();
            otp = otp.trim();

            boolean isOtpValid = studentService.verifyOtp(email, otp);
            if (!isOtpValid) {
                return new ResponseEntity<>("Invalid or expired OTP. Please try again.", HttpStatus.BAD_REQUEST);
            }

            Student saved = studentService.save(
                    rollNumber, fullName, email, password, userName, dept, branch,
                    mobileNumber, semester, year, image, resume,
                    semesterGPAsJson,
                    leetCodeUrl, githubUrl
            );
            return new ResponseEntity<>(saved, HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // --- 5. LOGIN ---
    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody LoginData loginData){
        String identifier = loginData.getEmailOrUsername();
        if (identifier == null || identifier.isEmpty()) {
            identifier = loginData.getEmail();
        }
        if (identifier == null || identifier.isEmpty()) {
            return new ResponseEntity<>("Email or Username is required", HttpStatus.BAD_REQUEST);
        }

        // Fix: Trim identifier
        identifier = identifier.trim();

        Student student = studentService.findStudent(identifier);

        if(student != null && passwordEncoder.matches(loginData.getPassword(), student.getPassword())){
            if(!student.isVerified()){
                return new ResponseEntity<>("Account not verified. Please Signup Again.",HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(student);
        }

        return new ResponseEntity<>("Invalid Credentials", HttpStatus.UNAUTHORIZED);
    }
}