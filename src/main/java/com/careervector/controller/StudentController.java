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

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestParam("roll") String rollNumber,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            // ✅ NEW PARAMETER: OTP
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
            // ✅ STEP 1: Verify OTP before saving anything
            boolean isOtpValid = studentService.verifyOtp(email, otp);

            if (!isOtpValid) {
                return new ResponseEntity<>("Invalid or expired OTP. Please try again.", HttpStatus.BAD_REQUEST);
            }

            // ✅ STEP 2: Proceed with Saving (Student will be marked verified inside service)
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

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody LoginData loginData){
        String identifier = loginData.getEmailOrUsername();
        if (identifier == null || identifier.isEmpty()) {
            identifier = loginData.getEmail();
        }
        if (identifier == null || identifier.isEmpty()) {
            return new ResponseEntity<>("Email or Username is required", HttpStatus.BAD_REQUEST);
        }
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