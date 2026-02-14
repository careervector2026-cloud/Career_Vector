package com.careervector.dto;

import java.util.List;
public class fastapi {
    // Main Request Wrapper
    public record RankingRequest(
            String job_description,
            List<CandidateInfo> candidates
    ) {}

    // Candidate Details
    public record CandidateInfo(
            String candidate_id,
            String resume_url,
            String github_url,
            String leetcode_username
    ) {}

    // Response Object
    public record RankingResponse(
            String candidate_id,  // Maps to "candidate_id" in JSON
            int rank,             // Maps to "rank"
            double final_score,   // Maps to "final_score"
            String status         // Maps to "status" ("shortlist", "review", or "reject")
    ) {}
}
