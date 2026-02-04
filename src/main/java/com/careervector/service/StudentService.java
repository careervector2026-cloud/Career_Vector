package com.careervector.service;

import com.careervector.dto.StudentUpdateDto;
import com.careervector.model.Student;
import com.careervector.repo.StudentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {

    @Autowired
    private StudentRepo studentRepo;

    @Autowired
    private SupabaseService supabaseService; // Injected Service

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- 1. Upload Profile Picture ---
    @Transactional
    public String uploadProfilePic(String email, MultipartFile file) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) throw new RuntimeException("Student not found");

        // Delete old image if exists
        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
            String oldFileName = supabaseService.extractFileNameFromUrl(student.getProfileImageUrl());
            if (oldFileName != null) {
                supabaseService.deleteFile("images", oldFileName);
            }
        }

        // Upload new image
        String fileName = student.getRollNumber() + "_avatar_" + System.currentTimeMillis() + ".png";
        String newUrl = supabaseService.uploadFile(file, "images", fileName);

        student.setProfileImageUrl(newUrl);
        studentRepo.save(student);

        return newUrl;
    }

    // --- 2. Upload Resume ---
    @Transactional
    public String uploadResume(String email, MultipartFile file) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) throw new RuntimeException("Student not found");

        // Delete old resume if exists
        if (student.getResumeUrl() != null && !student.getResumeUrl().isEmpty()) {
            String oldFileName = supabaseService.extractFileNameFromUrl(student.getResumeUrl());
            if (oldFileName != null) {
                supabaseService.deleteFile("resumes", oldFileName);
            }
        }

        // Upload new resume
        String fileName = student.getRollNumber() + "_resume_" + System.currentTimeMillis() + ".pdf";
        String newUrl = supabaseService.uploadFile(file, "resumes", fileName);

        student.setResumeUrl(newUrl);
        studentRepo.save(student);

        return newUrl;
    }

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
            throw new RuntimeException("Invalid Semester or Year format. Please enter numbers only.");
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

        // --- Use Supabase Service for Signup Uploads ---
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

    private Double parseGpa(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
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

    public void resetPassword(String email, String otp, String newPassword) {
        Student student = studentRepo.findByEmail(email);
        if (student == null) throw new RuntimeException("User not found.");
        if (!verifyOtp(email, otp)) throw new RuntimeException("Invalid or Expired OTP.");
        student.setPassword(passwordEncoder.encode(newPassword));
        studentRepo.save(student);
    }

    public Student findStudent(String userNameEmail) {
        Student student = null;
        if((student = studentRepo.findByUserName(userNameEmail))!=null)return student;
        else if((student = studentRepo.findByEmail(userNameEmail))!=null)return student;
        else return null;
    }

    public void generateAndSendOtp(String email) {
        if(studentRepo.findByEmail(email) != null) throw new RuntimeException("Email is already registered. Please login");
        sendEmailOtp(email, "Welcome to CareerVector! Your Verification Code is: ");
    }

    public void generateAndSendOtpForReset(String email) {
        if(studentRepo.findByEmail(email) == null) throw new RuntimeException("Email not found in our records.");
        sendEmailOtp(email, "CareerVector Password Reset. Your Verification Code is: ");
    }

    private void sendEmailOtp(String email, String messagePrefix) {
        String otp = String.valueOf(new Random().nextInt(900000)+100000);
        redisTemplate.opsForValue().set(email, otp, 5, TimeUnit.MINUTES);
        try{
            String body = messagePrefix + otp + "\n\nThis code expires in 5 minutes";
            emailService.sendEmail(email, "CareerVector Verification Code", body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Transactional
    public Student updateStudentProfile(StudentUpdateDto studentUpdateDto) {
        if(studentUpdateDto.getEmail() ==null || studentUpdateDto.getEmail().isEmpty()) throw new RuntimeException("Email is required.");
        Student student = studentRepo.findByEmail(studentUpdateDto.getEmail());
        if(student==null) throw new RuntimeException("Student not found.");

        if(studentUpdateDto.getMobileNumber()!=null)student.setMobileNumber(studentUpdateDto.getMobileNumber());
        if(studentUpdateDto.getGithubUrl()!=null)student.setGithubUrl(studentUpdateDto.getGithubUrl());
        if(studentUpdateDto.getLeetcodeUrl()!=null)student.setLeetcodeurl(studentUpdateDto.getLeetcodeUrl());

        if(studentUpdateDto.getGpa_sem_1()!=null)student.setGpaSem1(studentUpdateDto.getGpa_sem_1());
        if(studentUpdateDto.getGpa_sem_2()!=null)student.setGpaSem2(studentUpdateDto.getGpa_sem_2());
        if(studentUpdateDto.getGpa_sem_3()!=null)student.setGpaSem3(studentUpdateDto.getGpa_sem_3());
        if(studentUpdateDto.getGpa_sem_4()!=null)student.setGpaSem4(studentUpdateDto.getGpa_sem_4());
        if(studentUpdateDto.getGpa_sem_5()!=null)student.setGpaSem5(studentUpdateDto.getGpa_sem_5());
        if(studentUpdateDto.getGpa_sem_6()!=null)student.setGpaSem6(studentUpdateDto.getGpa_sem_6());
        if(studentUpdateDto.getGpa_sem_7()!=null)student.setGpaSem7(studentUpdateDto.getGpa_sem_7());
        if(studentUpdateDto.getGpa_sem_8()!=null)student.setGpaSem8(studentUpdateDto.getGpa_sem_8());

        return studentRepo.save(student);
    }

    public void changePassword(String email, String password) {
        Student student = studentRepo.findByEmail(email);
        if(student == null) throw new RuntimeException("Student not found.");
        student.setPassword(passwordEncoder.encode(password));
        studentRepo.save(student);
    }
}