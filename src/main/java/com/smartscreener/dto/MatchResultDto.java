package com.smartscreener.dto;

import java.util.List;

public record MatchResultDto(
        int score,
        String justification,
        List<String> matchedSkills,
        List<String> missingSkills,
        String recommendation,
        String analysisSource
) {

    public MatchResultDto {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException(
                    "Match score must be between 1 and 10."
            );
        }

        justification = normalizeText(
                justification,
                "No justification was provided."
        );

        matchedSkills = matchedSkills == null
                ? List.of()
                : List.copyOf(matchedSkills);

        missingSkills = missingSkills == null
                ? List.of()
                : List.copyOf(missingSkills);

        recommendation = normalizeText(
                recommendation,
                "REVIEW"
        );

        analysisSource = normalizeText(
                analysisSource,
                "UNKNOWN"
        );
    }

    private static String normalizeText(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}