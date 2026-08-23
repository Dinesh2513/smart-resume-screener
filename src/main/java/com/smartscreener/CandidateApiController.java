package com.smartscreener.controller;

import com.smartscreener.dto.CandidateResponseDto;
import com.smartscreener.entity.Candidate;
import com.smartscreener.service.CandidateService;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateApiController {

    private final CandidateService candidateService;

    public CandidateApiController(
            CandidateService candidateService
    ) {
        this.candidateService = candidateService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateResponseDto uploadResume(
            @RequestParam("resume") MultipartFile resume
    ) {
        Candidate candidate =
                candidateService.processAndSaveResume(resume);

        return CandidateResponseDto.from(candidate);
    }

    @GetMapping
    public List<CandidateResponseDto> getAllCandidates() {
        return candidateService.getAllCandidates()
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