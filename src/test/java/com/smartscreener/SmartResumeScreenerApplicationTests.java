package com.smartscreener;

import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import com.smartscreener.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SmartResumeScreenerApplicationTests {

    @Autowired
    private CandidateRepository candidateRepository;

    @Test
    void contextLoads() {
        assertThat(candidateRepository).isNotNull();
    }

    @Test
    void candidateCanBeStoredInDatabase() {
        Candidate candidate = new Candidate();
        candidate.setFullName("Test Candidate");
        candidate.setEmail("candidate@example.com");
        candidate.setOriginalFileName("test-resume.txt");
        candidate.setResumeText("Java Spring Boot developer");
        candidate.setStatus(CandidateStatus.PENDING);

        Candidate savedCandidate = candidateRepository.save(candidate);

        assertThat(savedCandidate.getId()).isNotNull();
        assertThat(savedCandidate.getFullName()).isEqualTo("Test Candidate");
        assertThat(savedCandidate.getStatus())
                .isEqualTo(CandidateStatus.PENDING);
        assertThat(savedCandidate.getCreatedAt()).isNotNull();
    }
}