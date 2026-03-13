package com.careervector.repo;

import com.careervector.model.MockInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface MockInterviewRepo extends JpaRepository<MockInterview, Long> {
    List<MockInterview> findByStudentEmailOrderByCreatedAtDesc(String email);
}