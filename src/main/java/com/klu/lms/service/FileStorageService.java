package com.klu.lms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadRoot);
    }

    public StoredFile store(MultipartFile file, String folderName) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        Path targetFolder = uploadRoot.resolve(folderName).normalize();
        Files.createDirectories(targetFolder);

        String originalFileName = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedFileName = UUID.randomUUID() + "_" + originalFileName;
        Path targetPath = targetFolder.resolve(storedFileName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(
            originalFileName,
            storedFileName,
            file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
            targetPath.toString()
        );
    }

    public Resource loadAsResource(String filePath) throws IOException {
        Path path = Paths.get(filePath).normalize();
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("File not found or unreadable");
        }
        return resource;
    }

    public void delete(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        Files.deleteIfExists(Paths.get(filePath).normalize());
    }

    public record StoredFile(String originalFileName, String storedFileName, String contentType, String filePath) {
    }
}
