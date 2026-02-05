package com.careervector.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Builder
public class Job {

    // --- FIX START: REQUIRED CONSTRUCTORS ---

    // 1. No-Args Constructor (REQUIRED for Hibernate/JPA)
    public Job() {
    }

    // 2. All-Args Constructor (REQUIRED for Lombok @Builder to work with the manual No-Args one)
    public Job(Long id, String jobTitle, String jobType, String location, String salaryRange, String description, boolean isActive, Recruiter recruiter, LocalDateTime postedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.jobTitle = jobTitle;
        this.jobType = jobType;
        this.location = location;
        this.salaryRange = salaryRange;
        this.description = description;
        this.isActive = isActive;
        this.recruiter = recruiter;
        this.postedAt = postedAt;
        this.updatedAt = updatedAt;
    }
    // --- FIX END ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String location;

    private String salaryRange;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.EAGER) // Change LAZY to EAGER to ensure it's loaded for the frontend
    @JoinColumn(name = "recruiter_id", nullable = false)
    @JsonIgnoreProperties({"jobs", "password", "verified","email","fullName","id","imageUrl","mobile","role","userName"}) // Don't send sensitive data or the back-reference
    private Recruiter recruiter;

    @CreationTimestamp
    private LocalDateTime postedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // --- Getters and Setters (Keep your existing ones) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Recruiter getRecruiter() { return recruiter; }
    public void setRecruiter(Recruiter recruiter) { this.recruiter = recruiter; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}