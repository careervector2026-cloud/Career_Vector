package com.careervector.repo;

import com.careervector.model.Student;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface StudentRepo extends JpaRepository<Student, String> {
    
    Student findByEmail(String email);
    
    Student findByUserName(String userName);

    List<Student> findByClgName(String clgName);
}