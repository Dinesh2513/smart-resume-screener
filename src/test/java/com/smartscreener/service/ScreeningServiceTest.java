package com.smartscreener.service;

import com.smartscreener.dto.MatchResultDto;
import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import com.smartscreener.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreeningServiceTest {

    private CandidateRepository repository;
    private OllamaMatchingService matchingService;
    private ScreeningService screeningService;

    @BeforeEach
    void setUp() {
        repository = mock(CandidateRepository.class);
        matchingService = mock(OllamaMatchingService.class);
        screeningService = new ScreeningService(
                repository,
                matchingService,
                7
        );
    }

    @Test
    void shortlistsStrongCandidateAndPersistsEvidence() {
        Candidate candidate = candidate(1L, "Java Spring Boot SQL");
        MatchResultDto result = new MatchResultDto(
                8,
                "Strong Java and Spring Boot alignment.",
                List.of("Java", "Spring Boot"),
                List.of("Docker"),
                List.of(
                        "Add a Docker project with measurable results.",
                        "Quantify backend performance improvements.",
                        "Use relevant job-description keywords."
                ),
                "RECOMMENDED",
                "OLLAMA_LLM"
        );

        when(repository.findById(1L)).thenReturn(Optional.of(candidate));
        when(matchingService.analyzeMatch(
                candidate.getResumeText(),
                "We need a Java Spring Boot developer with SQL."
        )).thenReturn(result);
        when(repository.saveAndFlush(candidate)).thenReturn(candidate);

        Candidate screened = screeningService.screenCandidate(
                1L,
                "Backend Developer",
                "We need a Java Spring Boot developer with SQL."
        );

        assertThat(screened.getMatchScore()).isEqualTo(8);
        assertThat(screened.getStatus())
                .isEqualTo(CandidateStatus.SHORTLISTED);
        assertThat(screened.getMatchedSkills())
                .isEqualTo("Java, Spring Boot");
        assertThat(screened.getMissingSkills()).isEqualTo("Docker");
        assertThat(screened.getImprovementRecommendations())
                .contains("Docker project")
                .contains("measurable results");
        assertThat(screened.getAnalysisSource()).isEqualTo("OLLAMA_LLM");
        assertThat(screened.getScreenedAt()).isNotNull();
    }

    @Test
    void marksBorderlineCandidateForReview() {
        Candidate candidate = candidate(2L, "Java");
        when(repository.findById(2L)).thenReturn(Optional.of(candidate));
        when(matchingService.analyzeMatch(
                candidate.getResumeText(),
                "We need Java, Spring Boot, SQL and Docker skills."
        )).thenReturn(new MatchResultDto(
                6,
                "Some requirements are satisfied.",
                List.of("Java"),
                List.of("Spring Boot", "SQL", "Docker"),
                List.of(
                        "Add relevant project evidence.",
                        "Quantify achievements.",
                        "Use standard ATS headings."
                ),
                "REVIEW",
                "RULE_BASED_FALLBACK"
        ));
        when(repository.saveAndFlush(candidate)).thenReturn(candidate);

        Candidate screened = screeningService.screenCandidate(
                2L,
                "Java Developer",
                "We need Java, Spring Boot, SQL and Docker skills."
        );

        assertThat(screened.getStatus())
                .isEqualTo(CandidateStatus.REVIEW_REQUIRED);
    }

    private Candidate candidate(Long id, String resumeText) {
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setFullName("Test Candidate");
        candidate.setOriginalFileName("resume.txt");
        candidate.setResumeText(resumeText);
        candidate.setStatus(CandidateStatus.PENDING);
        return candidate;
    }
}
