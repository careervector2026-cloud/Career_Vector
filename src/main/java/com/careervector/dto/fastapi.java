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

    // DTO for matching student with jd's
    // DTO for sending data to FastAPI
    public record StudentMatchRequest(
            StudentProfile student_profile,
            List<JobDescriptionInfo> jds
    ) {}

    public record StudentProfile(
            String student_id,
            String resume_url,
            String github_url,
            String leetcode_username
    ) {}

    public record JobDescriptionInfo(
            String jd_id,
            String job_description
    ) {}

    // DTO for receiving data from FastAPI
    public record StudentMatchResponse(
            String jd_id,
            int rank,
            double final_score,
            String status,
            String reason,
            String role_level,
            double job_readiness_score,
            String readiness_level
    ) {}

    //skill gap report
    public record SkillGapReportRequest(
            String resume_url,
            String job_description,
            String github_url,
            String leetcode_username
    ) {}

    // Detailed Report Response
    public record SkillGapReportResponse(
            List<String> matched_skills,
            List<MissingSkill> missing_skills,
            double overall_match_score,
            ExternalValidation external_validation
    ) {}

    public record MissingSkill(
            String skill,
            String priority,
            double weight
    ) {}

    public record ExternalValidation(
            List<String> github_confirmed,
            List<String> leetcode_indicators,
            List<String> confidence_notes
    ) {}
    //Learning Path Generator
    public record LearningPathRequest(
            String resume_url,
            String job_description,
            String github_url,
            String leetcode_username
    ) {}

    public record LearningPathResponse(
            String target_role,
            int estimated_readiness_weeks,
            List<LearningStep> learning_path
    ) {}

    public record LearningStep(
            int step,
            String skill,
            String skill_type,
            String priority,
            int estimated_time_weeks,
            List<String> resources,
            DetailedRoadmap detailed_roadmap,
            List<String> prerequisites,
            SkillGraph graph,
            String outcome
    ) {}

    public record DetailedRoadmap(
            int estimated_total_weeks,
            List<RoadmapLevel> levels
    ) {}

    public record RoadmapLevel(
            int level,
            String name,
            int duration_weeks,
            List<String> topics
    ) {}

    public record SkillGraph(
            List<GraphNode> nodes,
            List<GraphEdge> edges
    ) {}

    public record GraphNode(String id, String label, String type) {}
    public record GraphEdge(String from, String to) {}
}
