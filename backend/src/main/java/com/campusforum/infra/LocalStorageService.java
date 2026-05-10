package com.campusforum.infra;

import com.campusforum.common.BusinessException;
import com.campusforum.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${storage.local.path:./uploads}") String path) {
        this.basePath = Paths.get(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + basePath, e);
        }
        log.info("LocalStorageService initialized at {}", basePath);
    }

    @Override
    public String upload(InputStream inputStream, String originalName, String contentType) {
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) ext = originalName.substring(dot);

        String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String filename = UUID.randomUUID() + ext;
        String storageKey = dateDir + "/" + filename;

        try {
            Path targetDir = basePath.resolve(dateDir);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {}", storageKey);
            return storageKey;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            Path file = basePath.resolve(storageKey).normalize();
            if (!file.startsWith(basePath)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path file = basePath.resolve(storageKey).normalize();
            if (!file.startsWith(basePath)) {
                return;
            }
            Files.deleteIfExists(file);
            log.info("File deleted: {}", storageKey);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storageKey, e);
        }
    }
}
