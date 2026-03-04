package com.careervector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDTO {
    private String jobTitle;
    private String interviewDate;
    private String interviewTime;
    private String meetingLink;
}