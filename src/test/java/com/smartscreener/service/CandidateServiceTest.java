package com.smartscreener.service;

import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import com.smartscreener.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = "app.upload.directory=target/test-uploads"
)
@Transactional
class CandidateServiceTest {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Test
    void processesStoresAndSavesTxtResume() throws Exception {
        String resumeContent = """
                ARJUN REDDY
                Email: arjun.reddy@example.com
                Phone: 987-654-3210

                TECHNICAL SKILLS
                Java, Spring Boot, Hibernate, JPA, MySQL, Git

                WORK EXPERIENCE
                Java Developer at Example Technologies
                Developed REST APIs using Spring Boot and MySQL.

                EDUCATION
                Bachelor of Technology in Computer Science
                Example Engineering College, 2025
                """;

        MockMultipartFile resumeFile = new MockMultipartFile(
                "resume",
                "arjun-resume.txt",
                "text/plain",
                resumeContent.getBytes()
        );

        Path storedResumePath = null;

        try {
            Candidate candidate =
                    candidateService.processAndSaveResume(resumeFile);

            assertThat(candidate.getId()).isNotNull();
            assertThat(candidate.getFullName())
                    .isEqualTo("ARJUN REDDY");
            assertThat(candidate.getEmail())
                    .isEqualTo("arjun.reddy@example.com");
            assertThat(candidate.getPhone())
                    .isEqualTo("987-654-3210");
            assertThat(candidate.getSkills())
                    .contains("Java")
                    .contains("Spring Boot")
                    .contains("Hibernate")
                    .contains("JPA")
                    .contains("MySQL")
                    .contains("Git");
            assertThat(candidate.getExperience())
                    .contains("Java Developer");
            assertThat(candidate.getEducation())
                    .contains("Bachelor of Technology");
            assertThat(candidate.getStatus())
                    .isEqualTo(CandidateStatus.PENDING);
            assertThat(candidate.getMatchScore()).isNull();

            Candidate databaseCandidate = candidateRepository
                    .findById(candidate.getId())
                    .orElseThrow();

            assertThat(databaseCandidate.getResumeText())
                    .contains("ARJUN REDDY");
            assertThat(databaseCandidate.getOriginalFileName())
                    .isEqualTo("arjun-resume.txt");
            assertThat(databaseCandidate.getStoredFileName())
                    .isNotBlank()
                    .endsWith(".txt");

            storedResumePath =
                    candidateService.getResumeFile(candidate.getId());

            assertThat(storedResumePath).exists();
            assertThat(Files.readString(storedResumePath))
                    .contains("ARJUN REDDY")
                    .contains("Spring Boot");
        } finally {
            if (storedResumePath != null) {
                Files.deleteIfExists(storedResumePath);
            }
        }
    }
}