package com.smartscreener.service;

import com.smartscreener.dto.ParsedResumeDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeParserServiceTest {

    private final ResumeParserService parserService =
            new ResumeParserService();

    @Test
    void extractsStructuredCandidateInformation() {
        String resumeText = """
                ANANYA SHARMA
                Email: ananya.sharma@example.com
                Phone: 987-654-3210

                PROFESSIONAL SUMMARY
                Backend developer with enterprise application experience.

                TECHNICAL SKILLS
                Java, Spring Boot, Hibernate, JPA, MySQL, Git, Docker

                WORK EXPERIENCE
                Software Engineer at Example Technologies
                Developed REST APIs and microservices using Spring Boot.
                Worked with Docker and MySQL databases.

                EDUCATION
                Bachelor of Technology in Computer Science
                Example Institute of Technology, 2025

                PROJECTS
                Smart recruitment management application
                """;

        ParsedResumeDto result = parserService.parse(resumeText);

        assertThat(result.fullName()).isEqualTo("ANANYA SHARMA");
        assertThat(result.email())
                .isEqualTo("ananya.sharma@example.com");
        assertThat(result.phone()).isEqualTo("987-654-3210");

        assertThat(result.skills())
                .contains(
                        "Java",
                        "Spring Boot",
                        "Hibernate",
                        "JPA",
                        "MySQL",
                        "Git",
                        "Docker",
                        "REST API",
                        "Microservices"
                );

        assertThat(result.experience())
                .contains("Software Engineer")
                .contains("Developed REST APIs")
                .doesNotContain("Bachelor of Technology");

        assertThat(result.education())
                .contains("Bachelor of Technology")
                .contains("Example Institute of Technology")
                .doesNotContain("Smart recruitment");
    }

    @Test
    void handlesCaseInsensitiveSkillNames() {
        String resumeText = """
                RAHUL KUMAR
                PYTHON developer experienced with machine learning,
                TENSORFLOW, POSTGRESQL, AWS and KUBERNETES.
                """;

        ParsedResumeDto result = parserService.parse(resumeText);

        assertThat(result.skills())
                .contains(
                        "Python",
                        "Machine Learning",
                        "TensorFlow",
                        "PostgreSQL",
                        "AWS",
                        "Kubernetes"
                );
    }

    @Test
    void returnsFallbackValuesForMissingFields() {
        String resumeText = """
                RESUME
                Candidate information is unavailable.
                """;

        ParsedResumeDto result = parserService.parse(resumeText);

        assertThat(result.email()).isEqualTo("Not identified");
        assertThat(result.phone()).isEqualTo("Not identified");
        assertThat(result.skills()).isEmpty();
        assertThat(result.experience())
                .isEqualTo("Not clearly specified");
        assertThat(result.education())
                .isEqualTo("Not clearly specified");
    }

    @Test
    void rejectsEmptyResumeText() {
        assertThatThrownBy(() -> parserService.parse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resume text must not be empty.");
    }
}