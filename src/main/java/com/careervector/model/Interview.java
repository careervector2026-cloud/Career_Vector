package com.careervector.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "application_id")
    private JobApplication application;

    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private String meetingLink;
    
    private boolean isNotified; // Track if email was sent successfully
}