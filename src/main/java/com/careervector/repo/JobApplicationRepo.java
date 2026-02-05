package com.careervector.repo;

import com.careervector.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobApplicationRepo extends JpaRepository<JobApplication, Long> {

    // For Recruiter: See all students who applied for a specific job
    List<JobApplication> findByJobId(Long jobId);

    // For Student: See all jobs they have applied to
    List<JobApplication> findByStudentRollNumber(String rollNumber);

    // Prevent duplicate applications
    boolean existsByJobIdAndStudentRollNumber(Long jobId, String rollNumber);
}