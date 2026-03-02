package com.careervector.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careervector.model.Admin;

@Repository
public interface AdminRepo extends JpaRepository<Admin, Long> {
    Admin findByEmail(String email);
    Admin findByUserName(String userName);
}