package com.invoiceprocessor.adapter.out.storage;

import com.invoiceprocessor.application.port.out.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(
            @Value("${s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${s3.access-key:minioadmin}") String accessKey,
            @Value("${s3.secret-key:minioadmin}") String secretKey,
            @Value("${s3.bucket:invoices}") String bucketName) {
        this.bucketName = bucketName;
        
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .endpointOverride(java.net.URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .forcePathStyle(true) // Required for MinIO
                .build();
    }

    @PostConstruct
    public void initializeBucket() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            // Bucket doesn't exist, create it
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.createBucket(createBucketRequest);
        }
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            
            return key; // Return the key as the content reference
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
}

