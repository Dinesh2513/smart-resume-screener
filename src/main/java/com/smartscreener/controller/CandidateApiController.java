package com.smartscreener.controller;

import com.smartscreener.dto.CandidateResponseDto;
import com.smartscreener.entity.Candidate;
import com.smartscreener.exception.ResumeProcessingException;
import com.smartscreener.service.CandidateService;
import com.smartscreener.service.ScreeningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateApiController {

    private final CandidateService candidateService;
    private final ScreeningService screeningService;

    public CandidateApiController(
            CandidateService candidateService,
            ScreeningService screeningService
    ) {
        this.candidateService = candidateService;
        this.screeningService = screeningService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateResponseDto uploadResume(
            @RequestParam("resume") MultipartFile resume
    ) {
        return CandidateResponseDto.from(
                candidateService.processAndSaveResume(resume)
        );
    }

    @PostMapping(
            value = "/screen",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public List<CandidateResponseDto> screenResumes(
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("resumes") MultipartFile[] resumes
    ) {
        if (resumes == null || resumes.length == 0) {
            throw new ResumeProcessingException(
                    "Please upload at least one resume."
            );
        }
        if (resumes.length > 20) {
            throw new ResumeProcessingException(
                    "A maximum of 20 resumes can be screened at once."
            );
        }

        List<CandidateResponseDto> results = new ArrayList<>();
        for (MultipartFile resume : resumes) {
            Candidate uploaded = candidateService.processAndSaveResume(resume);
            Candidate screened = screeningService.screenCandidate(
                    uploaded.getId(),
                    jobTitle,
                    jobDescription
            );
            results.add(CandidateResponseDto.from(screened));
        }
        return results;
    }

    @PostMapping("/{candidateId}/screen")
    public CandidateResponseDto screenExistingCandidate(
            @PathVariable Long candidateId,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("jobDescription") String jobDescription
    ) {
        return CandidateResponseDto.from(
                screeningService.screenCandidate(
                        candidateId,
                        jobTitle,
                        jobDescription
                )
        );
    }

    @GetMapping
    public List<CandidateResponseDto> getAllCandidates() {
        return candidateService.getAllCandidates()
                .stream()
                .map(CandidateResponseDto::from)
                .toList();
    }

    @GetMapping("/ranked")
    public List<CandidateResponseDto> getRankedCandidates() {
        return screeningService.getRankedCandidates()
                .stream()
                .map(CandidateResponseDto::from)
                .toList();
    }

    @GetMapping("/shortlisted")
    public List<CandidateResponseDto> getShortlistedCandidates() {
        return screeningService.getShortlistedCandidates()
                .stream()
                .map(CandidateResponseDto::from)
                .toList();
    }

    @GetMapping("/{candidateId}")
    public CandidateResponseDto getCandidate(
            @PathVariable Long candidateId
    ) {
        return CandidateResponseDto.from(
                candidateService.getCandidateById(candidateId)
        );
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> deleteCandidate(
            @PathVariable Long candidateId
    ) {
        candidateService.deleteCandidate(candidateId);
        return ResponseEntity.noContent().build();
    }
}
