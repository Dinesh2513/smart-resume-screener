package com.smartscreener.service;

import com.smartscreener.dto.MatchResultDto;
import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import com.smartscreener.exception.ResumeProcessingException;
import com.smartscreener.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ScreeningService {

    private final CandidateRepository candidateRepository;
    private final OllamaMatchingService matchingService;
    private final int shortlistThreshold;

    public ScreeningService(
            CandidateRepository candidateRepository,
            OllamaMatchingService matchingService,
            @Value("${app.screening.shortlist-threshold:7}")
            int shortlistThreshold
    ) {
        this.candidateRepository = candidateRepository;
        this.matchingService = matchingService;
        this.shortlistThreshold = Math.max(1, Math.min(10, shortlistThreshold));
    }

    @Transactional
    public Candidate screenCandidate(
            Long candidateId,
            String jobTitle,
            String jobDescription
    ) {
        validateJob(jobTitle, jobDescription);

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResumeProcessingException(
                        "Candidate not found."
                ));

        MatchResultDto result = matchingService.analyzeMatch(
                candidate.getResumeText(),
                jobDescription
        );

        candidate.setJobTitle(jobTitle.trim());
        candidate.setJobDescription(jobDescription.trim());
        candidate.setMatchScore(result.score());
        candidate.setJustification(result.justification());
        candidate.setMatchedSkills(String.join(", ", result.matchedSkills()));
        candidate.setMissingSkills(String.join(", ", result.missingSkills()));
        candidate.setImprovementRecommendations(
                String.join(" || ", result.improvementRecommendations())
        );
        candidate.setRecommendation(result.recommendation());
        candidate.setAnalysisSource(result.analysisSource());
        candidate.setScreenedAt(LocalDateTime.now());
        candidate.setStatus(statusForScore(result.score()));

        return candidateRepository.saveAndFlush(candidate);
    }

    @Transactional(readOnly = true)
    public List<Candidate> getRankedCandidates() {
        Comparator<Candidate> ranking = Comparator.comparing(
                Candidate::getMatchScore,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        return candidateRepository.findAll()
                .stream()
                .sorted(ranking)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Candidate> getShortlistedCandidates() {
        return getRankedCandidates()
                .stream()
                .filter(candidate -> candidate.getStatus()
                        == CandidateStatus.SHORTLISTED)
                .toList();
    }

    private CandidateStatus statusForScore(int score) {
        if (score >= shortlistThreshold) {
            return CandidateStatus.SHORTLISTED;
        }
        if (score >= 5) {
            return CandidateStatus.REVIEW_REQUIRED;
        }
        return CandidateStatus.REJECTED;
    }

    private void validateJob(String jobTitle, String jobDescription) {
        if (jobTitle == null || jobTitle.isBlank()) {
            throw new ResumeProcessingException("Job title is required.");
        }
        if (jobDescription == null || jobDescription.trim().length() < 30) {
            throw new ResumeProcessingException(
                    "Job description must contain at least 30 characters."
            );
        }
        if (jobDescription.length() > 15_000) {
            throw new ResumeProcessingException(
                    "Job description must not exceed 15,000 characters."
            );
        }
    }
}
