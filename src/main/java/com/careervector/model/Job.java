package com.careervector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Builder
public class Job {

    // --- FIX: UPDATED CONSTRUCTORS ---

    // 1. No-Args Constructor (REQUIRED for Hibernate/JPA)
    public Job() {
    }

    // 2. All-Args Constructor (REQUIRED for Lombok @Builder to include the new variable)
    public Job(Long id, String jobTitle, String jobType, String location, String salaryRange, String description, boolean isActive, Recruiter recruiter, LocalDateTime postedAt, LocalDateTime updatedAt, int numberOfPostings) {
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
        this.numberOfPostings = numberOfPostings; // Logic added here
    }

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Column(nullable = false)
    private String jobTitle;

    @Setter
    @Column(nullable = false)
    private String jobType;

    @Setter
    @Column(nullable = false)
    private String location;

    @Setter
    private String salaryRange;

    @Setter
    @Getter
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Setter
    @Getter
    @Column(nullable = false)
    private boolean isActive = true;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recruiter_id", nullable = false)
    @JsonIgnoreProperties({"jobs", "password", "verified","email","fullName","id","imageUrl","mobile","role","userName"})
    private Recruiter recruiter;

    @Getter
    @CreationTimestamp
    private LocalDateTime postedAt;

    @Getter // Added Getter for consistency
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // FIX: New variable with Default Value for Database compatibility
    @Column(nullable = false, columnDefinition = "int default 1")
    @Getter
    @Setter
    private int numberOfPostings = 1;

    public void setId(Long id) { this.id = id; }
    public String getJobType() { return jobType; }
    public String getLocation() { return location; }
    public String getSalaryRange() { return salaryRange; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}