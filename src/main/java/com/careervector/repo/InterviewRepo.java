package com.careervector.repo;

import com.careervector.model.Interview;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewRepo extends JpaRepository<Interview, Long> {
	List<Interview> findAllByApplicationStudentEmail(String email);
	List<Interview> findAllByApplicationJobRecruiterEmail(String email);
}