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
    private String storageUrl;

    @Value("${secret_key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file, String bucket, String fileName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            String baseUploadUrl = storageUrl.endsWith("/") ? storageUrl : storageUrl + "/";
            String finalUrl = baseUploadUrl + bucket + "/" + fileName;

            restTemplate.postForEntity(finalUrl, entity, String.class);
            return baseUploadUrl + "public/" + bucket + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Supabase Storage Upload failed: " + e.getMessage());
        }
    }

    public void deleteFile(String bucket, String fileName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String baseUploadUrl = storageUrl.endsWith("/") ? storageUrl : storageUrl + "/";
            String deleteUrl = baseUploadUrl + bucket + "/" + fileName;

            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                System.err.println("Warning: Failed to delete file: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Warning: Error deleting file: " + e.getMessage());
        }
    }

    public String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        return url.substring(url.lastIndexOf("/") + 1);
    }
}