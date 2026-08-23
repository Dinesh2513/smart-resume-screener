package com.smartscreener.dto;

import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record CandidateResponseDto(
        Long id,
        String fullName,
        String email,
        String phone,
        List<String> skills,
        String experience,
        String education,
        Integer matchScore,
        String justification,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> improvementRecommendations,
        String recommendation,
        String analysisSource,
        String jobTitle,
        CandidateStatus status,
        String originalFileName,
        LocalDateTime createdAt,
        LocalDateTime screenedAt
) {

    public static CandidateResponseDto from(Candidate candidate) {
        return new CandidateResponseDto(
                candidate.getId(),
                candidate.getFullName(),
                candidate.getEmail(),
                candidate.getPhone(),
                splitSkills(candidate.getSkills()),
                candidate.getExperience(),
                candidate.getEducation(),
                candidate.getMatchScore(),
                candidate.getJustification(),
                splitSkills(candidate.getMatchedSkills()),
                splitSkills(candidate.getMissingSkills()),
                splitRecommendations(
                        candidate.getImprovementRecommendations()
                ),
                candidate.getRecommendation(),
                candidate.getAnalysisSource(),
                candidate.getJobTitle(),
                candidate.getStatus(),
                candidate.getOriginalFileName(),
                candidate.getCreatedAt(),
                candidate.getScreenedAt()
        );
    }

    private static List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }

        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .toList();
    }

    private static List<String> splitRecommendations(String recommendations) {
        if (recommendations == null || recommendations.isBlank()) {
            return List.of();
        }

        return Arrays.stream(recommendations.split("\\|\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
