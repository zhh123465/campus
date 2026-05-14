package com.campusforum.infra;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.campusforum.common.BusinessException;
import com.campusforum.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class OssStorageService implements StorageService {

    private final OSS client;
    private final String bucket;

    public OssStorageService(@Value("${storage.oss.endpoint}") String endpoint,
                              @Value("${storage.oss.access-key}") String accessKey,
                              @Value("${storage.oss.secret-key}") String secretKey,
                              @Value("${storage.oss.bucket:campusforum}") String bucket) {
        this.bucket = bucket;
        this.client = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        ensureBucket();
        log.info("OssStorageService initialized, bucket={}", bucket);
    }

    private void ensureBucket() {
        try {
            if (!client.doesBucketExist(bucket)) {
                client.createBucket(bucket);
                log.info("Created bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Failed to ensure bucket: {}", e.getMessage());
        }
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
            client.putObject(bucket, storageKey, inputStream);
            log.info("File uploaded to OSS: {}", storageKey);
            return storageKey;
        } catch (Exception e) {
            log.error("OSS upload failed", e);
            throw new BusinessException(ErrorCode.STORAGE_ERROR);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            OSSObject object = client.getObject(bucket, storageKey);
            return object.getObjectContent();
        } catch (Exception e) {
            log.warn("OSS download failed: {}", storageKey);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            client.deleteObject(bucket, storageKey);
            log.info("File deleted from OSS: {}", storageKey);
        } catch (Exception e) {
            log.warn("OSS delete failed: {}", storageKey, e);
        }
    }
}
