package com.careervector.service;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    public double calculateMatchScore(String studentSkills, String jobDescription) {
        // Return -1 if critical data is missing to signal "no score"
        if (studentSkills == null || studentSkills.trim().isEmpty() || jobDescription == null || jobDescription.trim().isEmpty()) {
            return -1.0;
        }

        Set<String> jobKeywords = extractKeywords(jobDescription);
        Set<String> studentKeywords = extractKeywords(studentSkills);

        if (studentKeywords.isEmpty()) return -1.0;

        long matches = studentKeywords.stream()
                .filter(jobKeywords::contains)
                .count();

        // Standard calculation
        return ((double) matches / studentKeywords.size()) * 100;
    }

    private Set<String> extractKeywords(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\W_]+"))
                .filter(word -> word.length() > 2)
                .collect(Collectors.toSet());
    }
}