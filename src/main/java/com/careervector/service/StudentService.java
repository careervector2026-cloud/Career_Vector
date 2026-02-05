package com.careervector.service;

import com.careervector.dto.StudentUpdateDto;
import com.careervector.model.Student;
import com.careervector.repo.StudentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;

@Service
public class StudentService {

    @Autowired
    private StudentRepo studentRepo;

    @Autowired
    private SupabaseService supabaseService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- OTP Logic ---
    public void generateAndSendOtp(String email) {
        if (studentRepo.findByEmail(email) != null) {
            throw new RuntimeException("Email is already registered. Please login.");
        }
        otpService.generateAndSendOtp(email, "CareerVector Verification", "Welcome to CareerVector! Your Verification Code is: ");
    }

    public void generateAndSendOtpForReset(String email) {
        if (studentRepo.findByEmail(email) == null) {
            throw new RuntimeException("Email not found in our records.");
        }
        otpService.generateAndSendOtp(email, "Password Reset", "CareerVector Password Reset. Your Verification Code is: ");
    }

    public boolean verifyOtp(String email, String otpInput) {
        return otpService.verifyOtp(email, otpInput);
    }

    // --- Password Reset ---
    public void resetPassword(String email, String otp, String newPassword) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) throw new RuntimeException("User not found.");

        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or Expired OTP.");
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        studentRepo.save(student);
    }

    // --- Signup / Save ---
    public Student save(String rollNumber, String fullName, String email, String password, String userName,
                        String dept, String branch, String mobileNumber, String semester, String year,
                        MultipartFile image, MultipartFile resume, String semesterGPAsJson,
                        String leetCodeUrl, String githubUrl) {
        Student s = new Student();
        s.setRollNumber(rollNumber);
        s.setFullName(fullName);
        s.setEmail(email);
        s.setPassword(passwordEncoder.encode(password));
        s.setUserName(userName);
        s.setDept(dept);
        s.setBranch(branch);
        s.setMobileNumber(mobileNumber);
        s.setLeetcodeurl(leetCodeUrl);
        s.setGithubUrl(githubUrl);
        s.setVerified(true);

        try {
            if (semester == null || semester.trim().isEmpty()) throw new NumberFormatException("Semester is missing");
            if (year == null || year.trim().isEmpty()) throw new NumberFormatException("Year is missing");
            s.setSemester(Integer.parseInt(semester.trim()));
            s.setYear(Integer.parseInt(year.trim()));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Semester or Year format.");
        }

        if (semesterGPAsJson != null && !semesterGPAsJson.isEmpty()) {
            try {
                Map<String, String> gpaMap = objectMapper.readValue(semesterGPAsJson, new TypeReference<HashMap<String,String>>() {});
                s.setGpaSem1(parseGpa(gpaMap.get("sem1")));
                s.setGpaSem2(parseGpa(gpaMap.get("sem2")));
                s.setGpaSem3(parseGpa(gpaMap.get("sem3")));
                s.setGpaSem4(parseGpa(gpaMap.get("sem4")));
                s.setGpaSem5(parseGpa(gpaMap.get("sem5")));
                s.setGpaSem6(parseGpa(gpaMap.get("sem6")));
                s.setGpaSem7(parseGpa(gpaMap.get("sem7")));
                s.setGpaSem8(parseGpa(gpaMap.get("sem8")));
            } catch (Exception e) {
                System.err.println("Error parsing GPA JSON: " + e.getMessage());
            }
        }

        if (image != null && !image.isEmpty()) {
            String fileName = rollNumber + "_avatar_" + System.currentTimeMillis() + ".png";
            String url = supabaseService.uploadFile(image, "images", fileName);
            s.setProfileImageUrl(url);
        }

        if (resume != null && !resume.isEmpty()) {
            String fileName = rollNumber + "_resume_" + System.currentTimeMillis() + ".pdf";
            String url = supabaseService.uploadFile(resume, "resumes", fileName);
            s.setResumeUrl(url);
        }

        return studentRepo.save(s);
    }

    // --- Uploads ---
    @Transactional
    public String uploadProfilePic(String email, MultipartFile file) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) throw new RuntimeException("Student not found");

        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
            String oldFileName = supabaseService.extractFileNameFromUrl(student.getProfileImageUrl());
            if (oldFileName != null) supabaseService.deleteFile("images", oldFileName);
        }

        String fileName = student.getRollNumber() + "_avatar_" + System.currentTimeMillis() + ".png";
        String newUrl = supabaseService.uploadFile(file, "images", fileName);

        student.setProfileImageUrl(newUrl);
        studentRepo.save(student);
        return newUrl;
    }

    @Transactional
    public String uploadResume(String email, MultipartFile file) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) throw new RuntimeException("Student not found");

        if (student.getResumeUrl() != null && !student.getResumeUrl().isEmpty()) {
            String oldFileName = supabaseService.extractFileNameFromUrl(student.getResumeUrl());
            if (oldFileName != null) supabaseService.deleteFile("resumes", oldFileName);
        }

        String fileName = student.getRollNumber() + "_resume_" + System.currentTimeMillis() + ".pdf";
        String newUrl = supabaseService.uploadFile(file, "resumes", fileName);

        student.setResumeUrl(newUrl);
        studentRepo.save(student);
        return newUrl;
    }

    // --- Other Methods ---
    private Double parseGpa(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public Student findStudent(String userNameEmail) {
        Student student;
        if((student = studentRepo.findByUserName(userNameEmail))!=null) return student;
        else if((student = studentRepo.findByEmail(userNameEmail))!=null) return student;
        return null;
    }

    @Transactional
    public Student updateStudentProfile(StudentUpdateDto dto) {
        if(dto.getEmail() == null || dto.getEmail().isEmpty()) throw new RuntimeException("Email is required.");
        Student student = studentRepo.findByEmail(dto.getEmail());
        if(student==null) throw new RuntimeException("Student not found.");

        if(dto.getMobileNumber()!=null) student.setMobileNumber(dto.getMobileNumber());
        if(dto.getGithubUrl()!=null) student.setGithubUrl(dto.getGithubUrl());
        if(dto.getLeetcodeUrl()!=null) student.setLeetcodeurl(dto.getLeetcodeUrl());

        if(dto.getGpa_sem_1()!=null) student.setGpaSem1(dto.getGpa_sem_1());
        if(dto.getGpa_sem_2()!=null) student.setGpaSem2(dto.getGpa_sem_2());
        if(dto.getGpa_sem_3()!=null) student.setGpaSem3(dto.getGpa_sem_3());
        if(dto.getGpa_sem_4()!=null) student.setGpaSem4(dto.getGpa_sem_4());
        if(dto.getGpa_sem_5()!=null) student.setGpaSem5(dto.getGpa_sem_5());
        if(dto.getGpa_sem_6()!=null) student.setGpaSem6(dto.getGpa_sem_6());
        if(dto.getGpa_sem_7()!=null) student.setGpaSem7(dto.getGpa_sem_7());
        if(dto.getGpa_sem_8()!=null) student.setGpaSem8(dto.getGpa_sem_8());

        return studentRepo.save(student);
    }

    public void changePassword(String email, String password) {
        Student student = studentRepo.findByEmail(email);
        if(student == null) throw new RuntimeException("Student not found.");
        student.setPassword(passwordEncoder.encode(password));
        studentRepo.save(student);
    }
}