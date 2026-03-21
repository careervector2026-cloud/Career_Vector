//AdminController.java
package com.careervector.controller;

import com.careervector.dto.LoginData;
import com.careervector.model.Admin;
import com.careervector.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "${app.frontend.url}")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }
        try {
            adminService.generateAndSendOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully to " + email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestParam("name") String name,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("institute") String institute,
            @RequestParam("password") String password,
            @RequestParam("otp") String otp,
            @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto
    ) {
        try {
            boolean isOtpValid = adminService.verifyOtp(email, otp);
            if (!isOtpValid) {
                return new ResponseEntity<>("Invalid or Expired verification code.", HttpStatus.BAD_REQUEST);
            }

            Admin admin = adminService.save(name, username, email, institute, password, profilePhoto);
            return new ResponseEntity<>(admin, HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginData loginData) {
        String identifier = loginData.getEmailOrUsername();
        if (identifier == null || identifier.isEmpty()) identifier = loginData.getEmail();

        if (identifier == null || identifier.isEmpty()) {
            return new ResponseEntity<>("Email or Username is Required", HttpStatus.BAD_REQUEST);
        }

        Admin admin = adminService.findAdmin(identifier);

        if (admin != null && passwordEncoder.matches(loginData.getPassword(), admin.getPassword())) {
            if (!admin.isVerified()) {
                return new ResponseEntity<>("Account not verified", HttpStatus.FORBIDDEN);
            }
            return ResponseEntity.ok(admin);
        }

        return new ResponseEntity<>("Invalid Credentials", HttpStatus.UNAUTHORIZED);
    }
    @GetMapping("/get-students/{college_name}")
    public ResponseEntity<?> getStudents(@PathVariable("college_name") String college_name){
    	return ResponseEntity.ok(adminService.getStudents(college_name));
    }
    
    @GetMapping("/placement-funnel")
    public ResponseEntity<Object> getPlacementFunnel(@RequestParam String collegeName) {
        return ResponseEntity.ok(adminService.fetchPlacementFunnel(collegeName));
    }

    @GetMapping("/top-students")
    public ResponseEntity<Object> getTopStudents(@RequestParam String collegeName) {
        return ResponseEntity.ok(adminService.fetchTopStudents(collegeName));
    }

    @GetMapping("/at-risk-students")
    public ResponseEntity<Object> getAtRiskStudents(@RequestParam String collegeName) {
        return ResponseEntity.ok(adminService.fetchAtRiskStudents(collegeName));
    }

    @GetMapping("/skill-gap-trends")
    public ResponseEntity<Object> getSkillGapTrends(@RequestParam String collegeName) {
        return ResponseEntity.ok(adminService.fetchSkillGapTrends(collegeName));
    }

    @GetMapping("/student-progression/{studentId}")
    public ResponseEntity<Object> getStudentProgression(@PathVariable String studentId) {
        return ResponseEntity.ok(adminService.fetchStudentProgression(studentId));
    }
}