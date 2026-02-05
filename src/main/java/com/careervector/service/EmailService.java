package com.careervector.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends an automated shortlist notification using the professional template.
     * Triggered by the recruiter when they click the 'Send' button on a shortlisted candidate.
     */
    public void sendShortlistNotification(String toEmail, String studentName, String jobTitle, String companyName) {
        String subject = "Application Update: Shortlisted for " + jobTitle;
        String bodyContent = "Hi " + studentName + ",\n\n" +
                "Congratulations! You have been shortlisted for the role of " + jobTitle + " at " + companyName + ". " +
                "Further updates regarding the next steps and interview scheduling will be shared with you soon.\n\n" +
                "Best regards,\n" +
                "Recruitment Team\n" +
                companyName;

        // Calls the core sendEmail method to execute the HTTP request
        this.sendEmail(toEmail, subject, bodyContent);
    }
    // Add this method to your EmailService.java

    public void sendRejectionNotification(String toEmail, String studentName, String jobTitle, String companyName) {
        String subject = "Update regarding your application for " + jobTitle;
        String bodyContent = "Hi " + studentName + ",\n\n" +
                "Thank you for your interest in the " + jobTitle + " position at " + companyName + ". " +
                "After careful review of your application, we regret to inform you that we will not be moving forward with your candidacy at this time.\n\n" +
                "We appreciate the time you took to apply and wish you the very best in your job search.\n\n" +
                "Best regards,\n" +
                "Recruitment Team\n" +
                companyName;

        this.sendEmail(toEmail, subject, bodyContent);
    }
    /**
     * Core method to send an email using the Brevo HTTP API (Port 443).
     * This bypasses SMTP ports (587/465) which are often blocked on cloud hosting.
     */
    public void sendEmail(String toEmail, String subject, String bodyContent) {
        try {
            // 1. Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            // 2. Body Payload
            Map<String, Object> body = new HashMap<>();

            // Sender (MUST be the email you verified in Brevo)
            Map<String, String> sender = new HashMap<>();
            sender.put("name", "CareerVector");
            sender.put("email", "careervector2026@gmail.com");
            body.put("sender", sender);

            // Recipient
            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail);
            body.put("to", List.of(to));

            // Content
            body.put("subject", subject);
            body.put("textContent", bodyContent);

            // 3. Send Request
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("DEBUG: Email sent successfully via Brevo API to " + toEmail);
            } else {
                System.err.println("DEBUG: Brevo API Error: " + response.getBody());
                throw new RuntimeException("Failed to send email. Provider rejected the request.");
            }

        } catch (Exception e) {
            System.err.println("DEBUG: Email Exception: " + e.getMessage());
            throw new RuntimeException("Email Service Failed: " + e.getMessage());
        }
    }
}