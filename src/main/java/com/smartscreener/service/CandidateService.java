package com.smartscreener.service;

import com.smartscreener.dto.ParsedResumeDto;
import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import com.smartscreener.exception.ResumeProcessingException;
import com.smartscreener.repository.CandidateRepository;
import com.smartscreener.util.ResumeTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ResumeTextExtractor resumeTextExtractor;
    private final ResumeParserService resumeParserService;
    private final FileStorageService fileStorageService;

    public CandidateService(
            CandidateRepository candidateRepository,
            ResumeTextExtractor resumeTextExtractor,
            ResumeParserService resumeParserService,
            FileStorageService fileStorageService
    ) {
        this.candidateRepository = candidateRepository;
        this.resumeTextExtractor = resumeTextExtractor;
        this.resumeParserService = resumeParserService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public Candidate processAndSaveResume(MultipartFile resumeFile) {
        String extractedText =
                resumeTextExtractor.extractText(resumeFile);

        ParsedResumeDto parsedResume =
                resumeParserService.parse(extractedText);

        String safeOriginalFileName =
                resumeTextExtractor.getSafeFileName(resumeFile);

        String storedFileName = null;

        try {
            storedFileName = fileStorageService.storeFile(
                    resumeFile,
                    safeOriginalFileName
            );

            Candidate candidate = createCandidate(
                    parsedResume,
                    extractedText,
                    safeOriginalFileName,
                    storedFileName
            );

            return candidateRepository.saveAndFlush(candidate);
        } catch (RuntimeException exception) {
            safelyDeleteStoredFile(storedFileName);

            if (exception instanceof ResumeProcessingException) {
                throw exception;
            }

            throw new ResumeProcessingException(
                    "The candidate resume could not be processed.",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Candidate getCandidateById(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResumeProcessingException(
                        "Candidate not found."
                ));
    }

    @Transactional(readOnly = true)
    public Path getResumeFile(Long candidateId) {
        Candidate candidate = getCandidateById(candidateId);

        return fileStorageService.getFilePath(
                candidate.getStoredFileName()
        );
    }

    @Transactional
    public void deleteCandidate(Long candidateId) {
        Candidate candidate = getCandidateById(candidateId);

        candidateRepository.delete(candidate);
        candidateRepository.flush();

        fileStorageService.deleteFile(
                candidate.getStoredFileName()
        );
    }

    private Candidate createCandidate(
            ParsedResumeDto parsedResume,
            String extractedText,
            String originalFileName,
            String storedFileName
    ) {
        Candidate candidate = new Candidate();

        candidate.setFullName(parsedResume.fullName());
        candidate.setEmail(parsedResume.email());
        candidate.setPhone(parsedResume.phone());
        candidate.setSkills(parsedResume.skillsAsText());
        candidate.setExperience(parsedResume.experience());
        candidate.setEducation(parsedResume.education());
        candidate.setOriginalFileName(originalFileName);
        candidate.setStoredFileName(storedFileName);
        candidate.setResumeText(extractedText);
        candidate.setStatus(CandidateStatus.PENDING);

        return candidate;
    }

    private void safelyDeleteStoredFile(String storedFileName) {
        if (storedFileName == null) {
            return;
        }

        try {
            fileStorageService.deleteFile(storedFileName);
        } catch (RuntimeException ignored) {
            // Preserve the original processing error.
        }
    }
}