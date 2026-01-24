package com.careervector.repo;

import com.careervector.model.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruiterRepo extends JpaRepository<Recruiter,String> {
    Recruiter findByEmail(String email);
    Recruiter findByUserName(String userName);
}
