//StudentController.java
package com.careervector.controller;

import com.careervector.dto.InterviewResponseDTO;
import com.careervector.dto.LoginData;
import com.careervector.dto.StudentUpdateDto;
import com.careervector.model.JobApplication;
import com.careervector.model.MockInterview;
import com.careervector.model.Student;
import com.careervector.repo.MockInterviewRepo;
import com.careervector.service.InterviewService;
import com.careervector.service.JobService;
import com.careervector.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "${app.frontend.url}")
public class StudentController {

    @Autowired
    private StudentService studentService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JobService jobService;
    @Autowired
    private InterviewService interviewService;
    
    // --- 1. SEND OTP (For SIGNUP) ---
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
        if(email != null) email = email.trim();

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

    // --- 2. FORGOT PASSWORD ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
        System.out.println("Forgot Password Request Received for: '" + email + "'");

        if(email == null || email.isEmpty()){
            return new ResponseEntity<>("Email is Required", HttpStatus.BAD_REQUEST);
        }

        email = email.trim();

        try {
            studentService.generateAndSendOtpForReset(email);
            return ResponseEntity.ok("OTP sent for password reset.");
        } catch (Exception e) {
            System.err.println("Forgot Password Error: " + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // --- 3. RESET PASSWORD ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String,String> payload){
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");

        if(email != null) email = email.trim();
        if(otp != null) otp = otp.trim();

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

    // --- 4. SIGNUP ---
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
            @RequestParam(value = "github", required = false) String githubUrl,
            @RequestParam(value = "college",required=true) String clgName
    ) {
        try {
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
                    leetCodeUrl, githubUrl,clgName
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

    // --- 6. GET SCORED JOBS (NEW ENDPOINT) ---
    @GetMapping("/{rollNumber}/get-scored-jobs")
    public ResponseEntity<List<Map<String, Object>>> getScoredJobs(@PathVariable String rollNumber) {
        // Calls the service method that handles null scores if skills are missing
        return ResponseEntity.ok(jobService.getJobsWithAiScoring(rollNumber));
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody StudentUpdateDto studentUpdateDto){
        try{
            Student student = studentService.updateStudentProfile(studentUpdateDto);
            return ResponseEntity.ok(Map.of(
                    "sucess",true,
                    "message","Profile update sucess",
                    "student",student
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Internal Server Error"));
        }
    }

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        try {
            studentService.changePassword(email, password);
            return ResponseEntity.ok("Password updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
            if (email == null || email.isEmpty()) return ResponseEntity.badRequest().body("Email is required");

            String fileUrl = studentService.uploadProfilePic(email, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile picture updated",
                    "url", fileUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @PatchMapping("/upload-resume")
    public ResponseEntity<?> uploadResume(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
            if (email == null || email.isEmpty()) return ResponseEntity.badRequest().body("Email is required");

            String fileUrl = studentService.uploadResume(email, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Resume uploaded successfully",
                    "url", fileUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/apply/{jobId}")
    public ResponseEntity<String> applyToJob(
            @PathVariable Long jobId,
            @RequestParam String rollNumber) {
        jobService.applyForJob(jobId, rollNumber);
        return ResponseEntity.ok("Application submitted successfully!");
    }
    // StudentController.java

    @GetMapping("/{rollNumber}/applications")
    public ResponseEntity<List<JobApplication>> getMyApplications(@PathVariable String rollNumber) {
        // This uses the JobApplicationRepo to find records by student roll number
        return ResponseEntity.ok(jobService.getStudentApplications(rollNumber));
    }
    @DeleteMapping("/withdraw/{jobId}")
    public ResponseEntity<Map<String, String>> withdrawApplication(
            @PathVariable Long jobId,
            @RequestParam String rollNumber) {

        jobService.withdrawJobApplication(jobId, rollNumber);

        return ResponseEntity.ok(Map.of("message", "Application withdrawn successfully"));
    }
    @GetMapping("/my-interviews")
    public ResponseEntity<List<InterviewResponseDTO>> getMyInterviews(@RequestParam String email) {
        return ResponseEntity.ok(interviewService.getInterviewsForStudent(email));
    }
    @Value("${fastapi.url}")
    private String fastApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/simulation/generate-questions")
    public ResponseEntity<?> generateSimulationQuestions(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            Integer nQuestions = (Integer) payload.get("n_questions");
            String jdText = (String) payload.get("jd_text");

            // Fetch student details from DB to get the latest Resume/GitHub URLs
            Student student = studentService.findStudent(email);
            if (student == null) return ResponseEntity.badRequest().body("Student not found");

            // Prepare payload for FastAPI
            Map<String, Object> aiPayload = Map.of(
                "jd_text", jdText != null ? jdText : "General Software Engineering Role",
                "resume_url", student.getResumeUrl() != null ? student.getResumeUrl() : "",
                "github_url", student.getGithubUrl() != null ? student.getGithubUrl() : "",
                "n_questions", nQuestions != null ? nQuestions : 4
            );

            return restTemplate.postForEntity(fastApiUrl + "/generate-interview-questions", aiPayload, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("AI Generation Error: " + e.getMessage());
        }
    }

    @PostMapping("/simulation/evaluate")
    public ResponseEntity<?> evaluateSimulation(@RequestBody Map<String, Object> payload) {
        try {
            // payload should contain "answers" list as per FastAPI spec
            return restTemplate.postForEntity(fastApiUrl + "/evaluate-interview", payload, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("AI Evaluation Error: " + e.getMessage());
        }
    }
    @PostMapping("/simulation/start-adaptive")
    public ResponseEntity<?> startAdaptive(@RequestBody Map<String, Object> payload) {
        try {
            Student student = studentService.findStudent((String) payload.get("email"));
            Map<String, Object> aiPayload = Map.of(
                "jd_text", payload.get("jd_text"),
                "candidate_id", student.getEmail(),
                "resume_url", student.getResumeUrl(),
                "github_url", student.getGithubUrl(),
                "n_questions", payload.get("n_questions")
            );
            return restTemplate.postForEntity(fastApiUrl + "/start-adaptive-interview", aiPayload, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error starting adaptive: " + e.getMessage());
        }
    }

    @PostMapping("/simulation/adaptive-answer")
    public ResponseEntity<?> adaptiveAnswer(@RequestBody Map<String, Object> payload) {
        try {
            // payload contains candidate_id and answer
            return restTemplate.postForEntity(fastApiUrl + "/adaptive-interview-answer", payload, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Evaluation error: " + e.getMessage());
        }
    }
    
    @Autowired
    private MockInterviewRepo mockInterviewRepo;

    // Save a completed simulation session
    @PostMapping("/simulation/save-history")
    public ResponseEntity<?> saveMockHistory(@RequestBody Map<String, Object> payload) {
        try {
            MockInterview mock = new MockInterview();
            mock.setStudentEmail((String) payload.get("email"));
            mock.setOverallScore(Double.parseDouble(payload.get("score").toString()));
            mock.setJdSummary((String) payload.get("jd"));
            
            // Convert the details list to a JSON string
            ObjectMapper mapper = new ObjectMapper();
            String jsonDetails = mapper.writeValueAsString(payload.get("details"));
            mock.setInterviewDetailsJson(jsonDetails);

            return ResponseEntity.ok(mockInterviewRepo.save(mock));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to save history: " + e.getMessage());
        }
    }

    // Get all mock history for a student
    @GetMapping("/simulation/history")
    public ResponseEntity<List<MockInterview>> getMockHistory(@RequestParam String email) {
        return ResponseEntity.ok(mockInterviewRepo.findByStudentEmailOrderByCreatedAtDesc(email));
    }
    
}