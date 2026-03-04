package com.careervector.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careervector.dto.InterviewBulkRequest;
import com.careervector.dto.InterviewResponseDTO;
import com.careervector.model.Interview;
import com.careervector.model.JobApplication;
import com.careervector.repo.InterviewRepo;
import com.careervector.repo.JobApplicationRepo;


@Service
public class InterviewService {

    @Autowired private InterviewRepo interviewRepo;
    @Autowired private JobApplicationRepo applicationRepo;
    @Autowired private EmailService emailService;

    @Transactional
    public void processBulkInterviews(InterviewBulkRequest request) {
        for (Long appId : request.getApplicationIds()) {
            JobApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

            // Save record
            Interview interview = Interview.builder()
                .application(app)
                .interviewDate(LocalDate.parse(request.getInterviewDate()))
                .interviewTime(LocalTime.parse(request.getInterviewTime()))
                .meetingLink(request.getMeetingLink())
                .isNotified(true)
                .build();
            interviewRepo.save(interview);

            // Send Email
            emailService.sendInterviewInvitation(
                app.getStudent().getEmail(),
                app.getStudent().getFullName(),
                request.getJobTitle(),
                request.getInterviewDate(),
                request.getInterviewTime(),
                request.getMeetingLink()
            );
        }
    }
    @Transactional(readOnly = true)
    public List<InterviewResponseDTO> getInterviewsForStudent(String email) {
        // This assumes your JobApplication entity has a 'student' field with an 'email' property
        return interviewRepo.findAllByApplicationStudentEmail(email)
            .stream()
            .map(interview -> InterviewResponseDTO.builder()
                .jobTitle(interview.getApplication().getJob().getJobTitle())
                .interviewDate(interview.getInterviewDate().toString())
                .interviewTime(interview.getInterviewTime().toString())
                .meetingLink(interview.getMeetingLink())
                .build())
            .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<InterviewResponseDTO> getInterviewsForRecruiter(String email) {
        return interviewRepo.findAllByApplicationJobRecruiterEmail(email)
            .stream()
            .map(interview -> InterviewResponseDTO.builder()
                .jobTitle(interview.getApplication().getJob().getJobTitle() + " - " + interview.getApplication().getStudent().getFullName())
                .interviewDate(interview.getInterviewDate().toString())
                .interviewTime(interview.getInterviewTime().toString())
                .meetingLink(interview.getMeetingLink())
                .build())
            .collect(Collectors.toList());
    }
}