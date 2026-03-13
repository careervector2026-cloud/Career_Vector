package com.careervector.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mock_interviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MockInterview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentEmail;
    
    @Column(columnDefinition = "TEXT")
    private String jdSummary; // Shortened version of the JD used

    private Double overallScore;
    
    @Column(columnDefinition = "TEXT")
    private String interviewDetailsJson; // Stores the Q&A array as JSON

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}