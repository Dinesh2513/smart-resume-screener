package com.smartscreener.controller;

import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import com.smartscreener.exception.GlobalExceptionHandler;
import com.smartscreener.exception.ResumeProcessingException;
import com.smartscreener.service.CandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateApiControllerTest {

    private CandidateService candidateService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        candidateService = mock(CandidateService.class);

        CandidateApiController controller =
                new CandidateApiController(candidateService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadsResumeAndReturnsCandidate() throws Exception {
        Candidate candidate = createCandidate();

        when(candidateService.processAndSaveResume(
                any(MultipartFile.class)
        )).thenReturn(candidate);

        MockMultipartFile resume = new MockMultipartFile(
                "resume",
                "candidate.txt",
                "text/plain",
                "Java Spring Boot developer".getBytes()
        );

        mockMvc.perform(multipart("/api/candidates/upload")
                        .file(resume))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.fullName")
                        .value("Ananya Sharma"))
                .andExpect(jsonPath("$.email")
                        .value("ananya@example.com"))
                .andExpect(jsonPath("$.skills[0]")
                        .value("Java"))
                .andExpect(jsonPath("$.skills[1]")
                        .value("Spring Boot"))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"))
                .andExpect(jsonPath("$.originalFileName")
                        .value("candidate.txt"));
    }

    @Test
    void returnsAllCandidates() throws Exception {
        when(candidateService.getAllCandidates())
                .thenReturn(List.of(createCandidate()));

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].fullName")
                        .value("Ananya Sharma"));
    }

    @Test
    void returnsCandidateById() throws Exception {
        when(candidateService.getCandidateById(7L))
                .thenReturn(createCandidate());

        mockMvc.perform(get("/api/candidates/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email")
                        .value("ananya@example.com"));
    }

    @Test
    void deletesCandidate() throws Exception {
        mockMvc.perform(delete("/api/candidates/7"))
                .andExpect(status().isNoContent());

        verify(candidateService).deleteCandidate(7L);
    }

    @Test
    void returnsBadRequestForInvalidResume() throws Exception {
        when(candidateService.processAndSaveResume(
                any(MultipartFile.class)
        )).thenThrow(new ResumeProcessingException(
                "Only PDF and TXT resume files are supported."
        ));

        MockMultipartFile resume = new MockMultipartFile(
                "resume",
                "candidate.docx",
                "application/vnd.openxmlformats-officedocument"
                        + ".wordprocessingml.document",
                "Invalid file".getBytes()
        );

        mockMvc.perform(multipart("/api/candidates/upload")
                        .file(resume))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Only PDF and TXT resume files "
                                        + "are supported."
                        ))
                .andExpect(jsonPath("$.path")
                        .value("/api/candidates/upload"));
    }

    private Candidate createCandidate() {
        Candidate candidate = new Candidate();

        candidate.setId(7L);
        candidate.setFullName("Ananya Sharma");
        candidate.setEmail("ananya@example.com");
        candidate.setPhone("987-654-3210");
        candidate.setSkills("Java, Spring Boot");
        candidate.setExperience("Java Developer");
        candidate.setEducation("Bachelor of Technology");
        candidate.setOriginalFileName("candidate.txt");
        candidate.setStoredFileName("stored-candidate.txt");
        candidate.setResumeText("Java Spring Boot developer");
        candidate.setStatus(CandidateStatus.PENDING);
        candidate.beforeSave();

        return candidate;
    }
}