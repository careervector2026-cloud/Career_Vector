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
    public void sendSelectionNotification(String to, String name, String jobTitle, String companyName) {
        String subject = "Congratulations! Selection for " + jobTitle;
        String content = "<h1>Hired!</h1>"
                + "<p>Dear <b>" + name + "</b>,</p>"
                + "<p>We are absolutely thrilled to inform you that <b>" + companyName + "</b> has selected you for the <b>" + jobTitle + "</b> position!</p>"
                + "<p>The team was highly impressed with your performance throughout the interview process and your technical skills.</p>"
                + "<p>Our HR department will reach out to you shortly with the official offer letter and onboarding details.</p>"
                + "<br><p>Welcome to the team!</p>"
                + "<p>Best Regards,<br>The " + companyName + " Recruitment Team</p>";
        
        sendEmail(to, subject, content);
    }
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
 // EmailService.java

    public void sendEmail(String toEmail, String subject, String bodyContent) {
        try {
            // 1. Ensure the key is trimmed of any hidden newlines/spaces
            String cleanKey = brevoApiKey.trim();

            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", cleanKey); // Brevo v3 Requirement
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            // 2. Build the payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of("name", "CareerVector", "email", "careervector2026@gmail.com"));
            payload.put("to", List.of(Map.of("email", toEmail)));
            payload.put("subject", subject);
            payload.put("textContent", bodyContent);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            System.out.println("DEBUG: Attempting to send email to " + toEmail);
            
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("DEBUG: Success! Brevo accepted the email.");
            }

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            // If this hits, the key is 100% being rejected by their server
            System.err.println("DEBUG: 401 Unauthorized. Key Used: " + brevoApiKey.substring(0, 10) + "...");
            System.err.println("Full Error from Brevo: " + e.getResponseBodyAsString());
            throw new RuntimeException("Invalid Brevo API Key or IP restricted.");
        } catch (Exception e) {
            System.err.println("DEBUG: General Error: " + e.getMessage());
            throw new RuntimeException("Email failed: " + e.getMessage());
        }
    }
    public void sendInterviewInvitation(String toEmail, String studentName, String jobTitle, String date, String time, String link) {
        String subject = "Interview Invitation: " + jobTitle;
        String bodyContent = "Hi " + studentName + ",\n\n" +
                "We are pleased to invite you for an interview for the " + jobTitle + " position.\n\n" +
                "Details are as follows:\n" +
                "Date: " + date + "\n" +
                "Time: " + time + "\n" +
                "Meeting Link: " + link + "\n\n" +
                "Please ensure you join on time. We look forward to speaking with you.\n\n" +
                "Best regards,\n" +
                "CareerVector Recruitment Team";

        this.sendEmail(toEmail, subject, bodyContent);
    }
}