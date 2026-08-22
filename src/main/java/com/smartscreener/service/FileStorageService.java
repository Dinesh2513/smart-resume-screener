package com.smartscreener.service;

import com.smartscreener.exception.ResumeProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDirectory;

    public FileStorageService(
            @Value("${app.upload.directory:uploads}")
            String uploadDirectory
    ) {
        this.uploadDirectory = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();

        createUploadDirectory();
    }

    public String storeFile(
            MultipartFile file,
            String safeOriginalFileName
    ) {
        String extension = extractExtension(safeOriginalFileName);
        String storedFileName = UUID.randomUUID() + extension;

        Path targetPath = uploadDirectory
                .resolve(storedFileName)
                .normalize();

        validateTargetPath(targetPath);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return storedFileName;
        } catch (IOException exception) {
            throw new ResumeProcessingException(
                    "The resume file could not be stored.",
                    exception
            );
        }
    }

    public void deleteFile(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }

        Path targetPath = uploadDirectory
                .resolve(storedFileName)
                .normalize();

        validateTargetPath(targetPath);

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            throw new ResumeProcessingException(
                    "The stored resume file could not be deleted.",
                    exception
            );
        }
    }

    public Path getFilePath(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            throw new ResumeProcessingException(
                    "Stored resume filename is missing."
            );
        }

        Path filePath = uploadDirectory
                .resolve(storedFileName)
                .normalize();

        validateTargetPath(filePath);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResumeProcessingException(
                    "The requested resume file was not found."
            );
        }

        return filePath;
    }

    private void createUploadDirectory() {
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new ResumeProcessingException(
                    "The resume upload directory could not be created.",
                    exception
            );
        }
    }

    private String extractExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');

        if (dotPosition < 0) {
            return "";
        }

        return fileName.substring(dotPosition).toLowerCase();
    }

    private void validateTargetPath(Path targetPath) {
        if (!targetPath.startsWith(uploadDirectory)) {
            throw new ResumeProcessingException(
                    "Invalid resume storage path."
            );
        }
    }
}