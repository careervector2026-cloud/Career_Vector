package com.careervector.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class JobRequest {
    // Used to identify the logged-in recruiter
    @Getter
    private String recruiterEmail;

    // Job Data
    @Getter
    private String jobTitle;
    @Getter
    @Setter
    private String jobType;
    @Getter
    @Setter
    private String location;
    @Getter
    @Setter
    private String salaryRange;
    @Getter
    @Setter
    private String description;
}