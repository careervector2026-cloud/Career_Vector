package com.careervector.controller;

import com.careervector.dto.JobRequest;
import com.careervector.dto.fastapi;
import com.careervector.model.Job;
import com.careervector.model.JobApplication;
import com.careervector.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "${app.frontend.url}")
public class JobController {

    @Autowired
    private JobService jobService;

    // --- JOB MANAGEMENT ---

    @PostMapping("/post-job")
    public ResponseEntity<Job> createJob(@RequestBody JobRequest jobRequest) {
        return ResponseEntity.ok(jobService.postJob(jobRequest));
    }

    @PutMapping("/{jobId}")
    public ResponseEntity<Job> updateJob(@PathVariable Long jobId, @RequestBody JobRequest jobRequest) {
        return ResponseEntity.ok(jobService.updateJob(jobId, jobRequest));
    }

    @PatchMapping("/{jobId}/toggle")
    public ResponseEntity<Job> toggleJobStatus(
            @PathVariable Long jobId,
            @RequestParam String email) {
        return ResponseEntity.ok(jobService.toggleJobStatus(jobId, email));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable Long jobId,
            @RequestParam String email) {
        jobService.deleteJob(jobId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-jobs")
    public ResponseEntity<List<Job>> getAllActiveJobs(){
        return ResponseEntity.ok(jobService.getJobs());
    }

    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getRecruiterJobs(@RequestParam String email) {
        return ResponseEntity.ok(jobService.getJobsByRecruiterEmail(email));
    }

    // --- APPLICATION MANAGEMENT ---

    @GetMapping("/{jobId}/candidates")
    public ResponseEntity<List<JobApplication>> getJobCandidates(
            @PathVariable Long jobId,
            @RequestParam String email) {
        return ResponseEntity.ok(jobService.getApplicantsForJob(jobId, email));
    }

    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<JobApplication> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam String status) {
        return ResponseEntity.ok(jobService.updateStatus(applicationId, status));
    }

    @PostMapping("/applications/{applicationId}/notify")
    public ResponseEntity<Map<String, String>> notifyStudent(
            @PathVariable Long applicationId,
            @RequestParam String email) { // Recruiter email for security/ownership check

        // Corrected call to match Service parameters
        jobService.sendApplicationUpdateNotification(applicationId, email);

        return ResponseEntity.ok(Map.of("message", "Professional notification sent successfully"));
    }

    // --- DASHBOARD STATS ---

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecruiterStats(@RequestParam String email) {
        List<Job> myJobs = jobService.getJobsByRecruiterEmail(email);

        long activeJobs = myJobs.stream().filter(Job::isActive).count();

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
    @PostMapping("/{jobId}/auto-shortlist")
    public ResponseEntity<Map<String, String>> triggerAiShortlisting(
            @PathVariable Long jobId,
            @RequestParam String email) { // recruiterEmail for security check

        jobService.autoShortlistCandidates(jobId, email);

        return ResponseEntity.ok(Map.of(
                "message", "AI processing complete. Candidates have been ranked and updated."
        ));
    }
    @PostMapping("/{jobId}/bulk-notify")
    public ResponseEntity<Map<String, String>> sendBulkEmails(
            @PathVariable Long jobId,
            @RequestParam String email) {
        jobService.sendBulkNotifications(jobId, email);
        return ResponseEntity.ok(Map.of("message", "Bulk emails dispatched successfully."));
    }

    @PostMapping("/{jobId}/finalize")
    public ResponseEntity<?> finalizeJob(@PathVariable Long jobId, @RequestParam String email) {
        jobService.finalizeJobProcess(jobId, email);
        return ResponseEntity.ok("Job finalized successfully.");
    }
    @PostMapping("/{jobId}/shortlist")
    public ResponseEntity<List<fastapi.RankingResponse>> shortlistForJob(@PathVariable Long jobId, @RequestParam String email) {
        // Capture the list returned by the service
        List<fastapi.RankingResponse> results = jobService.shortlistForJobProcess(jobId, email);
        // Return the list so the frontend can use it to update the UI state
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{jobId}/notify-reviewed")
    public ResponseEntity<?> notifyReviewed(@PathVariable Long jobId, @RequestParam String email) {
        jobService.notifyReviewedCandidates(jobId, email);
        return ResponseEntity.ok("Review notifications sent.");
    }
    @PostMapping("/skill-gap-report")
    public ResponseEntity<fastapi.SkillGapReportResponse> getSkillGapReport(
            @RequestBody fastapi.SkillGapReportRequest request) {
        return ResponseEntity.ok(jobService.getSkillGapReport(request));
    }
    // JobController.java

    @PostMapping("/learning-path") // This combines with @RequestMapping("/api/jobs")
    public ResponseEntity<fastapi.LearningPathResponse> getLearningPath(
            @RequestBody fastapi.LearningPathRequest request) {

        return ResponseEntity.ok(jobService.generateLearningPath(request));
    }
}