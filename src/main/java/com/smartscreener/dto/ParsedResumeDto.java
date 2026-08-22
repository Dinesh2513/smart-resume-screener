package com.smartscreener.dto;

import java.util.List;

public record ParsedResumeDto(
        String fullName,
        String email,
        String phone,
        List<String> skills,
        String experience,
        String education
) {

    public ParsedResumeDto {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public String skillsAsText() {
        return String.join(", ", skills);
    }
}