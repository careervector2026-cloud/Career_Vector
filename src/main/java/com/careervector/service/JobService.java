package com.careervector.service;

import com.careervector.dto.JobRequest;
import com.careervector.model.Job;
import com.careervector.model.JobApplication;
import com.careervector.model.Recruiter;
import com.careervector.model.Student;
import com.careervector.repo.JobApplicationRepo;
import com.careervector.repo.JobRepo;
import com.careervector.repo.RecruiterRepo;
import com.careervector.repo.StudentRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobService {

    @Autowired
    private JobRepo jobRepo;
    @Autowired private StudentRepo studentRepo;
    @Autowired
    private RecruiterRepo recruiterRepo;
    @Autowired private JobApplicationRepo applicationRepo;
    @Autowired
    private EmailService emailService;
    // --- 1. POST A NEW JOB ---
    public Job postJob(JobRequest req) {
        // Find Recruiter by Email
        Recruiter recruiter = recruiterRepo.findByEmail(req.getRecruiterEmail());
        if(recruiter==null)throw new EntityNotFoundException("Recruiter not found with email: " + req.getRecruiterEmail());

        Job job = Job.builder()
                .jobTitle(req.getJobTitle())
                .jobType(req.getJobType().toUpperCase().replace("-", "_"))
                .location(req.getLocation())
                .salaryRange(req.getSalaryRange())
                .description(req.getDescription())
                .isActive(true)
                .recruiter(recruiter) // Link the job
                .build();

        return jobRepo.save(job);
    }

    // --- 2. EDIT JOB ---
    public Job updateJob(Long jobId, JobRequest req) {
        // Verify ownership using the email from request
        Job job = getOwnedJob(jobId, req.getRecruiterEmail());

        job.setJobTitle(req.getJobTitle());
        job.setJobType(req.getJobType());
        job.setLocation(req.getLocation());
        job.setSalaryRange(req.getSalaryRange());
        job.setDescription(req.getDescription());

        return jobRepo.save(job);
    }

    // --- 3. TOGGLE STATUS (Close/Open) ---
    public Job toggleJobStatus(Long jobId, String email) {
        Job job = getOwnedJob(jobId, email);
        job.setActive(!job.isActive());
        return jobRepo.save(job);
    }

    // --- 4. DELETE JOB ---
    public void deleteJob(Long jobId, String email) {
        Job job = getOwnedJob(jobId, email);
        jobRepo.delete(job);
    }

    // --- 5. GET RECRUITER'S JOBS ---
    public List<Job> getJobsByRecruiterEmail(String email) {
        return jobRepo.findByRecruiterEmail(email);
    }

    // --- SECURITY HELPER ---
    // Fetches job and ensures the requester owns it
    private Job getOwnedJob(Long jobId, String email) {
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));

        if (!job.getRecruiter().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You do not own this job");
        }
        return job;
    }


    // --- APPLY FOR JOB ---
    public void applyForJob(Long jobId, String rollNumber) {
        // 1. Check if application already exists
        if (applicationRepo.existsByJobIdAndStudentRollNumber(jobId, rollNumber)) {
            throw new RuntimeException("You have already applied for this job.");
        }

        // 2. Verify Job and Student exist
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        Student student = studentRepo.findById(rollNumber)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        // 3. Save application
        JobApplication application = JobApplication.builder()
                .job(job)
                .student(student)
                .status("PENDING")
                .build();

        applicationRepo.save(application);
    }

    @Autowired private MatchingService matchingService;

    // JobService.java

    public List<Map<String, Object>> getJobsWithScores(String rollNumber) {
        Student student = studentRepo.findById(rollNumber)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        return jobRepo.findByIsActive(true).stream().map(job -> {
            Map<String, Object> jobMap = new HashMap<>();

            // 1. Calculate Match Score
            double score = matchingService.calculateMatchScore(student.getSkills(), job.getDescription());

            // 2. Check if student has already applied for this job
            boolean hasApplied = applicationRepo.existsByJobIdAndStudentRollNumber(job.getId(), rollNumber);

            jobMap.put("job", job);
            jobMap.put("matchScore", score >= 0 ? Math.round(score) : null);
            jobMap.put("hasApplied", hasApplied); // Added this field for frontend
            return jobMap;
        }).collect(Collectors.toList());
    }
    // JobService.java

    public List<JobApplication> getStudentApplications(String rollNumber) {
        return applicationRepo.findByStudentRollNumber(rollNumber);
    }
    // JobService.java

    public List<JobApplication> getApplicantsForJob(Long jobId, String email) {
        // Ensure the recruiter owns the job before showing candidates
        getOwnedJob(jobId, email);
        return applicationRepo.findByJobId(jobId);
    }

    public JobApplication updateStatus(Long applicationId, String status) {
        JobApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
        app.setStatus(status.toUpperCase());
        return applicationRepo.save(app);
    }

    public List<Job> getJobs() {
        return jobRepo.findByIsActive(true);
    }


    public void sendApplicationUpdateNotification(Long applicationId) {
        JobApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Application not found"));

        String status = app.getStatus();
        String studentEmail = app.getStudent().getEmail();
        String studentName = app.getStudent().getFullName();
        String jobTitle = app.getJob().getJobTitle();
        String companyName = app.getJob().getRecruiter().getCompanyName();

        if ("SHORTLISTED".equals(status)) {
            emailService.sendShortlistNotification(studentEmail, studentName, jobTitle, companyName);
        } else if ("REJECTED".equals(status)) {
            emailService.sendRejectionNotification(studentEmail, studentName, jobTitle, companyName);
        } else {
            throw new RuntimeException("Notification can only be sent for SHORTLISTED or REJECTED statuses.");
        }
    }
}