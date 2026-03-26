package com.careervector.repo;

import com.careervector.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepo extends JpaRepository<JobApplication, Long> {

    // For Recruiter: See all students who applied for a specific job
    List<JobApplication> findByJobId(Long jobId);

    // For Student: See all jobs they have applied to
    List<JobApplication> findByStudentRollNumber(String rollNumber);

    // Prevent duplicate applications
    boolean existsByJobIdAndStudentRollNumber(Long jobId, String rollNumber);
    Optional<JobApplication> findByJobIdAndStudentRollNumber(Long jobId, String rollNumber);
    Optional<JobApplication> findByJobIdAndStudentEmailIgnoreCase(Long jobId, String email);
    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.job.recruiter.email = :email AND a.job.isActive = true")
    long countPendingForActiveJobs(@Param("email") String email);

    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.job.recruiter.email = :email AND a.job.isActive = false AND a.status = :status")
    long countByStatusForClosedJobs(@Param("email") String email, @Param("status") String status);

    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.job.recruiter.email = :email AND a.job.isActive = false AND (a.status = 'SELECTED' OR a.status = 'HIRED')")
    long countHiredForClosedJobs(@Param("email") String email);  
    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.job.recruiter.email = :email AND a.status = :status")
    long countByStatusForAllJobs(@Param("email") String email, @Param("status") String status);
}