package com.careervector.dto;

import lombok.Data;
import java.util.List;

@Data
public class InterviewBulkRequest {
    private List<Long> applicationIds; // IDs of the job applications
    private String interviewDate;      // e.g., "2026-03-04"
    private String interviewTime;      // e.g., "10:00"
    private String meetingLink;        // Jitsi Link generated on frontend
    private String jobTitle;           // Title for the email subject
}