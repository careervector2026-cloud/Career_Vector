package com.careervector.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_roll_number")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    private String status = "PENDING";

    @CreationTimestamp
    private LocalDateTime appliedAt;
    // In com.careervector.model.JobApplication.java

    // Getters and Setters
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isMailSent = false;
    // Add to JobApplication.java
    private Double matchScore; // Stores the final_score from AI
}