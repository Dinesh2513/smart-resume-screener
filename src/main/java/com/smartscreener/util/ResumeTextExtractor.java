package com.smartscreener.util;

import com.smartscreener.exception.ResumeProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Component
public class ResumeTextExtractor {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "txt");

    private final long maximumResumeSize;

    public ResumeTextExtractor(
            @Value("${app.maximum-resume-size-bytes:10485760}")
            long maximumResumeSize
    ) {
        this.maximumResumeSize = maximumResumeSize;
    }

    public String extractText(MultipartFile file) {
        validateFile(file);

        String safeFileName = getSafeFileName(file);
        String extension = getExtension(safeFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResumeProcessingException(
                    "Only PDF and TXT resume files are supported."
            );
        }

        try {
            byte[] fileBytes = file.getBytes();

            String extractedText = switch (extension) {
                case "pdf" -> extractPdfText(fileBytes);
                case "txt" -> extractPlainText(fileBytes);
                default -> throw new ResumeProcessingException(
                        "Unsupported resume file type."
                );
            };

            String cleanedText = cleanText(extractedText);

            if (cleanedText.isBlank()) {
                throw new ResumeProcessingException(
                        "No readable text was found in the uploaded resume."
                );
            }

            return cleanedText;
        } catch (ResumeProcessingException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResumeProcessingException(
                    "The uploaded resume could not be read.",
                    exception
            );
        }
    }

    public String getSafeFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            return "resume";
        }

        return Paths.get(originalFileName)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeProcessingException(
                    "Please select a non-empty resume file."
            );
        }

        if (file.getSize() > maximumResumeSize) {
            throw new ResumeProcessingException(
                    "The resume file exceeds the maximum allowed size of 10 MB."
            );
        }
    }

    private String getExtension(String fileName) {
        int finalDotPosition = fileName.lastIndexOf('.');

        if (finalDotPosition < 0 ||
                finalDotPosition == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(finalDotPosition + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String extractPdfText(byte[] fileBytes) throws IOException {
        validatePdfSignature(fileBytes);

        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            return textStripper.getText(document);
        }
    }

    private String extractPlainText(byte[] fileBytes) {
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    private void validatePdfSignature(byte[] fileBytes) {
        if (fileBytes.length < 5 ||
                fileBytes[0] != '%' ||
                fileBytes[1] != 'P' ||
                fileBytes[2] != 'D' ||
                fileBytes[3] != 'F' ||
                fileBytes[4] != '-') {

            throw new ResumeProcessingException(
                    "The uploaded file is not a valid PDF document."
            );
        }
    }

    private String cleanText(String text) {
        return text
                .replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}