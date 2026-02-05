package com.careervector.repo;

import com.careervector.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobRepo extends JpaRepository<Job, Long> {

    // Find all jobs created by a specific recruiter's email
    // This JOINs the tables automatically
    List<Job> findByRecruiterEmail(String email);
    List<Job> findByIsActive(boolean isActive);
}