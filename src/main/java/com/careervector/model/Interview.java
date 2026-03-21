package com.careervector.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

 // Inside com.careervector.model.Interview.java

    @OneToOne
    @JoinColumn(name = "application_id")
    @JsonBackReference // This prevents Jackson from re-serializing the application
    private JobApplication application;

    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private String meetingLink;
    
    private boolean isNotified; // Track if email was sent successfully
}