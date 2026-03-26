//JobService.java
package com.careervector.service;

import com.careervector.dto.JobRequest;
import com.careervector.dto.fastapi;
import com.careervector.dto.fastapi.RankingRequest;
import com.careervector.dto.fastapi.CandidateInfo;
import com.careervector.dto.fastapi.RankingResponse;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobService {

    @Autowired private JobRepo jobRepo;
    @Autowired private StudentRepo studentRepo;
    @Autowired private RecruiterRepo recruiterRepo;
    @Autowired private JobApplicationRepo applicationRepo;
    @Autowired private EmailService emailService;
    @Autowired private MatchingService matchingService;
    @Autowired @Qualifier("fastApiRestTemplate") private RestTemplate fastApiRestTemplate;
    // --- 1. POST A NEW JOB ---
    public Job postJob(JobRequest req) {
        Recruiter recruiter = recruiterRepo.findByEmail(req.getRecruiterEmail());
        if(recruiter==null) throw new EntityNotFoundException("Recruiter not found with email: " + req.getRecruiterEmail());

        Job job = Job.builder()
                .jobTitle(req.getJobTitle())
                .jobType(req.getJobType().toUpperCase().replace("-", "_"))
                .location(req.getLocation())
                .salaryRange(req.getSalaryRange())
                .description(req.getDescription())
                .isActive(true)
                .recruiter(recruiter)
                .numberOfPostings(req.getNumberOfPostings())
                .build();

        return jobRepo.save(job);
    }

    // --- 2. EDIT JOB ---
    public Job updateJob(Long jobId, JobRequest req) {
        Job job = getOwnedJob(jobId, req.getRecruiterEmail());
        job.setJobTitle(req.getJobTitle());
        job.setJobType(req.getJobType());
        job.setLocation(req.getLocation());
        job.setSalaryRange(req.getSalaryRange());
        job.setDescription(req.getDescription());
        job.setNumberOfPostings(req.getNumberOfPostings());
        return jobRepo.save(job);
    }

    // --- 3. TOGGLE STATUS (Close/Open) ---
    public Job toggleJobStatus(Long jobId, String email) {
        Job job = getOwnedJob(jobId, email);

        // Check if any notifications have already been dispatched
        boolean mailsAlreadySent = applicationRepo.findByJobId(jobId).stream()
                .anyMatch(JobApplication::isMailSent);

        if (mailsAlreadySent) {
            throw new RuntimeException("Action Blocked: This job is finalized because notifications have already been sent.");
        }

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
    private Job getOwnedJob(Long jobId, String email) {
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (!job.getRecruiter().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You do not own this job");
        }
        return job;
    }

    // --- APPLY FOR JOB ---
    public void applyForJob(Long jobId, String rollNumber) {
        if (applicationRepo.existsByJobIdAndStudentRollNumber(jobId, rollNumber)) {
            throw new RuntimeException("You have already applied for this job.");
        }
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        Student student = studentRepo.findById(rollNumber).orElseThrow(() -> new EntityNotFoundException("Student not found"));

        JobApplication application = JobApplication.builder()
                .job(job)
                .student(student)
                .status("PENDING")
                .isMailSent(false)
                .build();
        applicationRepo.save(application);
    }

    // --- STUDENT VIEW: SCORED JOBS ---
    // Updated method in JobService.java
    public List<Map<String, Object>> getJobsWithScores(String rollNumber) {
        Student student = studentRepo.findById(rollNumber)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        return jobRepo.findByIsActive(true).stream().map(job -> {
            Map<String, Object> jobMap = new HashMap<>();

            double score = matchingService.calculateMatchScore(student.getSkills(), job.getDescription());

            // Find existing application to get status and mail flag
            Optional<JobApplication> appOpt = applicationRepo.findByJobIdAndStudentRollNumber(job.getId(), rollNumber);

            jobMap.put("job", job);
            jobMap.put("matchScore", score >= 0 ? Math.round(score) : null);
            jobMap.put("hasApplied", appOpt.isPresent());
            jobMap.put("applicationStatus", appOpt.map(JobApplication::getStatus).orElse(null));
            jobMap.put("mailSent", appOpt.map(JobApplication::isMailSent).orElse(false)); // Added for UI locking

            return jobMap;
        }).collect(Collectors.toList());
    }

    public List<JobApplication> getStudentApplications(String rollNumber) {
        return applicationRepo.findByStudentRollNumber(rollNumber);
    }

    // --- RECRUITER VIEW: APPLICANTS ---
    public List<JobApplication> getApplicantsForJob(Long jobId, String email) {
        getOwnedJob(jobId, email);
        return applicationRepo.findByJobId(jobId);
    }

    public JobApplication updateStatus(Long applicationId, String status) {
        JobApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        String newStatus = status.toUpperCase();

        // --- UPDATED LOCK LOGIC ---
        // If a mail was already sent (e.g., the Shortlist/Interview invite), 
        // we ONLY allow updates if the recruiter is now selecting (Hiring) or rejecting (Passing) the candidate.
        if (app.isMailSent()) {
            boolean isFinalDecision = newStatus.equals("SELECTED") || newStatus.equals("REJECTED");
            
            if (!isFinalDecision) {
                throw new RuntimeException("Status is locked because the notification email has already been sent.");
            }

            // IMPORTANT: We temporarily reset the mail flag to 'false'.
            // This allows the Controller/Service to trigger the FINAL email (Hire/Reject) 
            // through the notify endpoint without getting blocked by the 'already sent' check.
            app.setMailSent(false);
        }

        app.setStatus(newStatus);
        JobApplication savedApp = applicationRepo.save(app);
        syncDecisionToAi(
                savedApp.getStudent().getRollNumber(), 
                savedApp.getJob().getDescription(), 
                newStatus
            );
        
        return savedApp;
    }

    public List<Job> getJobs() {
        return jobRepo.findByIsActive(true);
    }
 // Add this to JobService.java
    public List<Job> getAllJobs() {
        // This ignores the isActive flag and returns everything in the job table
        return jobRepo.findAll();
    }

    // --- MAIL LOGIC ---
    public void sendApplicationUpdateNotification(Long applicationId, String recruiterEmail) {
        // 1. Fetch Application
        JobApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        // 2. Security Check: Verify recruiter ownership
        if (!app.getJob().getRecruiter().getEmail().equals(recruiterEmail)) {
            throw new RuntimeException("Unauthorized: You do not own the job associated with this application.");
        }

        // 3. Prevent duplicate notifications
        if (app.isMailSent()) {
            throw new RuntimeException("Notification has already been sent to this student.");
        }

        String status = app.getStatus();
        String studentEmail = app.getStudent().getEmail();
        String studentName = app.getStudent().getFullName();
        String jobTitle = app.getJob().getJobTitle();
        String companyName = app.getJob().getRecruiter().getCompanyName();

        // 4. Dispatch Email based on status
        if ("SELECTED".equals(status)) {
            // You'll need to add this method in your EmailService.java
            emailService.sendSelectionNotification(studentEmail, studentName, jobTitle, companyName);
        }
        else if ("SHORTLISTED".equals(status)) {
            emailService.sendShortlistNotification(studentEmail, studentName, jobTitle, companyName);
        } else if ("REJECTED".equals(status)) {
            emailService.sendRejectionNotification(studentEmail, studentName, jobTitle, companyName);
        } else {
            throw new RuntimeException("Notification can only be sent for SHORTLISTED or REJECTED statuses.");
        }

        // 5. Update flag and lock the application
        app.setMailSent(true);
        applicationRepo.save(app);
    }

    // Add to com.careervector.service.JobService

    public void withdrawJobApplication(Long jobId, String rollNumber) {
        // 1. Find the application
        JobApplication application = applicationRepo.findByJobIdAndStudentRollNumber(jobId, rollNumber)
                .orElseThrow(() -> new EntityNotFoundException("Application not found for this job and student."));

        // 2. Logic Check: Block withdrawal if mail is sent or status is not PENDING
        if (application.isMailSent()) {
            throw new RuntimeException("Cannot withdraw: Your application has already been processed and a notification has been sent.");
        }

        if (!"PENDING".equals(application.getStatus())) {
            throw new RuntimeException("Cannot withdraw: Your application is already " + application.getStatus());
        }

        // 3. Delete the application
        applicationRepo.delete(application);
    }

    //used for ai shortlisting
    @Value("${fastapi.url}")
    private String fastApiUrl;
    private void syncDecisionToAi(String studentId, String jdText, String status) {
        try {
            String url = fastApiUrl + "/recruiter/decision-by-jd-text";
            fastapi.RecruiterDecisionRequest syncReq = new fastapi.RecruiterDecisionRequest(
                studentId, 
                jdText, 
                status.toLowerCase()
            );
            fastApiRestTemplate.postForObject(url, syncReq, Void.class);
        } catch (Exception e) {
            // We log the error but don't crash the main transaction
            System.err.println("Failed to sync decision to AI: " + e.getMessage());
        }
    }
    @Transactional
    public void autoShortlistCandidates(Long jobId, String recruiterEmail) {
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (!job.getRecruiter().getEmail().equals(recruiterEmail)) throw new RuntimeException("Unauthorized");

        // 1. Safety Check: Prevent AI ranking if candidates have already been notified
        boolean alreadyNotified = applicationRepo.findByJobId(jobId).stream()
                .anyMatch(JobApplication::isMailSent);

        if (alreadyNotified) {
            throw new RuntimeException("AI Ranking Locked: Notifications have already been dispatched.");
        }

        if (job.isActive()) throw new RuntimeException("Job must be closed first before AI shortlisting.");

        // 2. Filter candidates who haven't been notified yet
        List<JobApplication> appsToRank = applicationRepo.findByJobId(jobId).stream()
                .filter(app -> !app.isMailSent())
                .toList();

        if (appsToRank.isEmpty()) return;

        // 3. Map to Updated DTO (Using Roll Number as student_id)
        List<CandidateInfo> candidateInfos = appsToRank.stream().map(app -> {
            Student s = app.getStudent();
            return new CandidateInfo(
                s.getRollNumber(),          // student_id
                s.getResumeUrl(),           // resume_url
                s.getGithubUrl(),           // github_url
                extractLeetCodeUsername(s.getLeetcodeUrl()), // leetcode_username
                s.getClgName()              // college_name
            );
        }).toList();

        RankingRequest request = new RankingRequest(job.getDescription(), candidateInfos);

        try {
            String url = fastApiUrl + "/rank-candidates-summary";
            RankingResponse[] response = fastApiRestTemplate.postForObject(url, request, RankingResponse[].class);

            if (response != null) {
                for (RankingResponse res : response) {
                    applicationRepo.findByJobIdAndStudentRollNumber(jobId, res.student_id())
                            .ifPresent(app -> {
                                app.setMatchScore(res.final_score());
                                
                                if ("PENDING".equals(app.getStatus())) {
                                    String aiStatus = res.status().toLowerCase();
                                    String dbStatus = "PENDING";
                                    
                                    if ("shortlist".equals(aiStatus)) dbStatus = "SHORTLISTED";
                                    else if ("reject".equals(aiStatus)) dbStatus = "REJECTED";
                                    else if ("review".equals(aiStatus)) dbStatus = "UNDER_REVIEW";
                                    
                                    app.setStatus(dbStatus);
                                    applicationRepo.save(app);

                                    // --- NEW: Sync the AI's "Decision" to the learning endpoint ---
                                    syncDecisionToAi(
                                        res.student_id(), 
                                        app.getJob().getDescription(), 
                                        dbStatus
                                    );
                                }
                            });
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("AI Ranking Error: " + e.getMessage());
        }
    }

    // NEW: Bulk Email Method
    @Transactional
    public void sendBulkNotifications(Long jobId, String recruiterEmail) {
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));

        // Force close job upon finalization
        if (job.isActive()) {
            job.setActive(false);
            jobRepo.save(job);
        }

        List<JobApplication> apps = applicationRepo.findByJobId(jobId);
        for (JobApplication app : apps) {
            if (!app.isMailSent() && ("SHORTLISTED".equals(app.getStatus()) || "REJECTED".equals(app.getStatus()))) {
                try {
                    sendApplicationUpdateNotification(app.getId(), recruiterEmail);
                } catch (Exception e) {
                    System.err.println("Failed to send bulk mail to: " + app.getStudent().getEmail());
                }
            }
        }
    }

    /**
     * Extracts username from LeetCode URL.
     * Handles: https://leetcode.com/u/username/ or https://leetcode.com/username
     */
    private String extractLeetCodeUsername(String url) {
        if (url == null || url.isBlank()) return "unknown";
        String cleanUrl = url.trim().replaceAll("/$", "");
        return cleanUrl.substring(cleanUrl.lastIndexOf("/") + 1);
    }

    // --- MASTER FINALIZE: CLOSE -> AI RANK -> SEND INITIAL MAILS ---
    @Transactional
    public void finalizeJobProcess(Long jobId, String recruiterEmail) {
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (!job.getRecruiter().getEmail().equals(recruiterEmail)) throw new RuntimeException("Unauthorized");

        // 1. Close Job
        if (job.isActive()) {
            job.setActive(false);
            jobRepo.save(job);
        }

        // 2. AI Shortlist (Checks for isMailSent internally)
        autoShortlistCandidates(jobId, recruiterEmail);

        // 3. Initial Bulk Notify (Sends to AI-decided Shortlisted/Rejected)
        sendBulkNotifications(jobId, recruiterEmail);
    }

    // --- SECONDARY NOTIFY: SEND MAILS TO UPDATED REVIEW CANDIDATES ---
    @Transactional
    public void notifyReviewedCandidates(Long jobId, String recruiterEmail) {
        List<JobApplication> apps = applicationRepo.findByJobId(jobId);

        for (JobApplication app : apps) {
            // Only send if the recruiter manually updated from UNDER_REVIEW to a final status
            if (!app.isMailSent() && ("SHORTLISTED".equals(app.getStatus()) || "REJECTED".equals(app.getStatus()))) {
                try {
                    sendApplicationUpdateNotification(app.getId(), recruiterEmail);
                } catch (Exception e) {
                    System.err.println("Failed to notify reviewed candidate: " + app.getStudent().getEmail());
                }
            }
        }
    }

    // No changes strictly required here if you've already implemented the
// shortlistForJobProcess that returns List<fastapi.RankingResponse>.
// Just ensuring it remains stateless as per your requirement.
    public List<fastapi.RankingResponse> shortlistForJobProcess(Long jobId, String email) {
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Job not found"));
        if (!job.getRecruiter().getEmail().equals(email)) throw new RuntimeException("Unauthorized");

        List<JobApplication> appsToRank = applicationRepo.findByJobId(jobId).stream()
                .filter(app -> !app.isMailSent())
                .toList();

        if (appsToRank.isEmpty()) return Collections.emptyList();

        List<fastapi.CandidateInfo> candidateInfos = appsToRank.stream().map(app -> {
            Student s = app.getStudent();
            return new fastapi.CandidateInfo(
                s.getRollNumber(), 
                s.getResumeUrl(), 
                s.getGithubUrl(), 
                extractLeetCodeUsername(s.getLeetcodeUrl()), 
                s.getClgName()
            );
        }).toList();

        fastapi.RankingRequest request = new fastapi.RankingRequest(job.getDescription(), candidateInfos);

        try {
            String url = fastApiUrl + "/rank-candidates-summary";
            fastapi.RankingResponse[] response = fastApiRestTemplate.postForObject(url, request, fastapi.RankingResponse[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException("AI Ranking Error: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getJobsWithAiScoring(String rollNumber) {
        Student student = studentRepo.findById(rollNumber)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        List<Job> activeJobs = jobRepo.findByIsActive(true);
        if (activeJobs.isEmpty()) return List.of();

        // Updated StudentProfile to match your fastapi.StudentProfile record
        fastapi.StudentProfile profile = new fastapi.StudentProfile(
                student.getRollNumber(),
                student.getResumeUrl(),
                student.getGithubUrl(),
                extractLeetCodeUsername(student.getLeetcodeUrl()),
                student.getClgName()
        );

        List<fastapi.JobDescriptionInfo> jdList = activeJobs.stream()
                .map(job -> new fastapi.JobDescriptionInfo(
                        String.valueOf(job.getId()),
                        job.getDescription()
                ))
                .toList();

        fastapi.StudentMatchRequest aiRequest = new fastapi.StudentMatchRequest(profile, jdList);

        try {
            String url = fastApiUrl + "/match-student-jds?mode=lite";
            fastapi.StudentMatchResponse[] aiResults = fastApiRestTemplate.postForObject(url, aiRequest, fastapi.StudentMatchResponse[].class);

            Map<String, fastapi.StudentMatchResponse> aiMap = new HashMap<>();
            if (aiResults != null) {
                for (fastapi.StudentMatchResponse res : aiResults) {
                    aiMap.put(res.jd_id(), res);
                }
            }

            return activeJobs.stream().map(job -> {
                Map<String, Object> responseMap = new HashMap<>();
                fastapi.StudentMatchResponse aiData = aiMap.get(String.valueOf(job.getId()));
                Optional<JobApplication> appOpt = applicationRepo.findByJobIdAndStudentRollNumber(job.getId(), rollNumber);

                responseMap.put("job", job);
                responseMap.put("aiStats", aiData);
                responseMap.put("hasApplied", appOpt.isPresent());
                responseMap.put("applicationStatus", appOpt.map(JobApplication::getStatus).orElse(null));
                responseMap.put("mailSent", appOpt.map(JobApplication::isMailSent).orElse(false));

                return responseMap;
            })
            .sorted((a, b) -> {
                fastapi.StudentMatchResponse resA = (fastapi.StudentMatchResponse) a.get("aiStats");
                fastapi.StudentMatchResponse resB = (fastapi.StudentMatchResponse) b.get("aiStats");
                if (resA == null || resB == null) return 0;
                return Integer.compare(resA.rank(), resB.rank());
            })
            .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("AI Scoring Error: " + e.getMessage());
            throw new RuntimeException("Could not fetch AI job recommendations.");
        }
    }
    

    public fastapi.SkillGapReportResponse getSkillGapReport(fastapi.SkillGapReportRequest request) {
        try {
            // Updated endpoint to match your FastAPI route for detailed reports
            String url = fastApiUrl + "/skill-gap-report";

            return fastApiRestTemplate.postForObject(
                    url,
                    request,
                    fastapi.SkillGapReportResponse.class
            );
        } catch (Exception e) {
            throw new RuntimeException("AI Skill Gap Report Error: " + e.getMessage());
        }
    }
    public fastapi.LearningPathResponse generateLearningPath(fastapi.LearningPathRequest request) {
        String url = fastApiUrl + "/learning-path"; // Match your FastAPI endpoint
        return fastApiRestTemplate.postForObject(url, request, fastapi.LearningPathResponse.class);
    }
    
 // Inside JobService.java

    public Object fetchJobReadiness(Map<String, Object> payload) {
        // 1. Use the configured fastApiUrl from application.properties
        // 2. Use the @Qualifier("fastApiRestTemplate") bean already defined in your class
        String url = fastApiUrl + "/job-readiness"; 
        
        try {
            // We use Object.class or a specific DTO to capture the FastAPI response
            return fastApiRestTemplate.postForObject(url, payload, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("AI Analysis Service (FastAPI) is currently unreachable: " + e.getMessage());
        }
    }
 // Logic for Failure Diagnosis
    public Object fetchFailureDiagnosis(Map<String, Object> payload) {
        // Construct the FastAPI endpoint URL
        String url = fastApiUrl + "/failure-diagnosis"; 
        
        try {
            // Proxy the request to FastAPI using the specialized RestTemplate
            return fastApiRestTemplate.postForObject(url, payload, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("AI Diagnosis Service (FastAPI) is currently unreachable: " + e.getMessage());
        }
    }
    public Object fetchAtsScore(Map<String, Object> payload) {
        // Construct the FastAPI endpoint URL for ATS Check
        String url = fastApiUrl + "/ats-check"; 
        
        try {
            // Proxy the request to FastAPI using the specialized RestTemplate
            return fastApiRestTemplate.postForObject(url, payload, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("AI ATS Service (Scanner) is currently unreachable: " + e.getMessage());
        }
    }
    @Autowired
    private JobApplicationRepo jobApplicationRepo;
    
    public Map<String, Object> getRecruiterStats(String email) {
        Map<String, Object> stats = new HashMap<>();
        
        // Fetch jobs once for counts
        List<Job> allJobs = jobRepo.findByRecruiterEmail(email);

        long activeJobsCount = allJobs.stream().filter(Job::isActive).count();
        long totalCandidates = allJobs.stream()
                .mapToLong(job -> applicationRepo.findByJobId(job.getId()).size())
                .sum();

        // 1. Pending: Only from ACTIVE jobs
        stats.put("pending", applicationRepo.countPendingForActiveJobs(email));

        // 2. Shortlisted: From ALL jobs (Active + Closed) - Updated logic
        stats.put("shortlisted", applicationRepo.countByStatusForAllJobs(email, "SHORTLISTED"));

        // 3. Rejected: Only from CLOSED jobs
        stats.put("rejected", applicationRepo.countByStatusForClosedJobs(email, "REJECTED"));

        // 4. Hired: Only from CLOSED jobs
        stats.put("hired", applicationRepo.countHiredForClosedJobs(email));

        // General Stats
        stats.put("activeJobs", activeJobsCount);
        stats.put("totalCandidates", totalCandidates);

        return stats;
    }
}