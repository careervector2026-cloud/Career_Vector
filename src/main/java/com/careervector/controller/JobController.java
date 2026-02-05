package com.careervector.controller;

import com.careervector.dto.JobRequest;
import com.careervector.model.Job;
import com.careervector.model.JobApplication;
import com.careervector.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "${app.frontend.url}") // Allow React Frontend
public class JobController {

    @Autowired
    private JobService jobService;

    // POST: Create Job
    // Payload: { "recruiterEmail": "x@y.com", "jobTitle": "..." }
    @PostMapping("/post-job")
    public ResponseEntity<Job> createJob(@RequestBody JobRequest jobRequest) {
        return ResponseEntity.ok(jobService.postJob(jobRequest));
    }

    // PUT: Update Job
    // Payload: { "recruiterEmail": "x@y.com", ... }
    @PutMapping("/{jobId}")
    public ResponseEntity<Job> updateJob(@PathVariable Long jobId, @RequestBody JobRequest jobRequest) {
        return ResponseEntity.ok(jobService.updateJob(jobId, jobRequest));
    }

    // PATCH: Toggle Status (Close/Open)
    // URL: /api/jobs/5/toggle?email=recruiter@example.com
    @PatchMapping("/{jobId}/toggle")
    public ResponseEntity<Job> toggleJobStatus(
            @PathVariable Long jobId,
            @RequestParam String email) {
        return ResponseEntity.ok(jobService.toggleJobStatus(jobId, email));
    }

    // DELETE: Delete Job
    // URL: /api/jobs/5?email=recruiter@example.com
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable Long jobId,
            @RequestParam String email) {
        jobService.deleteJob(jobId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-jobs")
    public ResponseEntity<List<Job>> getMyJobs(){
        return ResponseEntity.ok(jobService.getJobs());
    }

    // PATCH: Update candidate application status
// URL: /api/jobs/applications/{applicationId}/status?status=SHORTLISTED
    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<JobApplication> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam String status) {
        return ResponseEntity.ok(jobService.updateStatus(applicationId, status));
    }
    // 1. Fetch all jobs for the recruiter dashboard
    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs(@RequestParam String email) {
        return ResponseEntity.ok(jobService.getJobsByRecruiterEmail(email));
    }

    // JobController.java

    @PostMapping("/applications/{applicationId}/notify")
    public ResponseEntity<Map<String, String>> notifyStudent(@PathVariable Long applicationId) {
        jobService.sendApplicationUpdateNotification(applicationId);
        return ResponseEntity.ok(Map.of("message", "Status notification sent successfully via Brevo"));
    }

    // JobController.java

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecruiterStats(@RequestParam String email) {
        List<Job> myJobs = jobService.getJobsByRecruiterEmail(email);

        long activeJobs = myJobs.stream().filter(Job::isActive).count();

        // Fetch all applications for all jobs posted by this recruiter
        List<JobApplication> allApps = myJobs.stream()
                .flatMap(job -> jobService.getApplicantsForJob(job.getId(), email).stream())
                .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeJobs", activeJobs);
        stats.put("totalCandidates", allApps.size());
        stats.put("applied", allApps.stream().filter(a -> "PENDING".equals(a.getStatus())).count());
        stats.put("shortlisted", allApps.stream().filter(a -> "SHORTLISTED".equals(a.getStatus())).count());
        stats.put("rejected", allApps.stream().filter(a -> "REJECTED".equals(a.getStatus())).count());

        return ResponseEntity.ok(stats);
    }

    // 2. Fetch candidates for a specific job ID
    @GetMapping("/{jobId}/candidates")
    public ResponseEntity<List<JobApplication>> getJobCandidates(
            @PathVariable Long jobId,
            @RequestParam String email) {
        return ResponseEntity.ok(jobService.getApplicantsForJob(jobId, email));
    }
}
