package com.campusforum.infra;

import java.io.InputStream;

public interface StorageService {
    String upload(InputStream inputStream, String originalName, String contentType);
    InputStream download(String storageKey);
    void delete(String storageKey);
}
