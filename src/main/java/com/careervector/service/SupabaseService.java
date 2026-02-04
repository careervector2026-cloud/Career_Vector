package com.careervector.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class SupabaseService {

    @Value("${Storage_url}")
    private String storageUrl; // e.g. https://xyz.supabase.co/storage/v1/object/

    @Value("${secret_key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // --- UPLOAD FILE ---
    public String uploadFile(MultipartFile file, String bucket, String fileName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            // Construct API URL: .../storage/v1/object/{bucket}/{fileName}
            String baseUploadUrl = storageUrl.endsWith("/") ? storageUrl : storageUrl + "/";
            String finalUrl = baseUploadUrl + bucket + "/" + fileName;

            restTemplate.postForEntity(finalUrl, entity, String.class);

            // Return the public URL for easy access
            // Public URL: .../storage/v1/object/public/{bucket}/{fileName}
            // Note: Ensure your Supabase bucket is set to Public or use signed URLs if private.
            return baseUploadUrl + "public/" + bucket + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Supabase Storage Upload failed: " + e.getMessage());
        }
    }

    // --- DELETE FILE ---
    public void deleteFile(String bucket, String fileName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String baseUploadUrl = storageUrl.endsWith("/") ? storageUrl : storageUrl + "/";
            String deleteUrl = baseUploadUrl + bucket + "/" + fileName;

            System.out.println("Attempting to delete file at: " + deleteUrl);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
            System.out.println("Successfully deleted file: " + fileName);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("Info: File not found during delete (already removed).");
            } else {
                System.err.println("Warning: Failed to delete file: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Warning: Error deleting file: " + e.getMessage());
        }
    }

    // --- HELPER: Extract Filename ---
    public String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        // Extracts "filename.png" from ".../public/bucket/filename.png"
        return url.substring(url.lastIndexOf("/") + 1);
    }
}