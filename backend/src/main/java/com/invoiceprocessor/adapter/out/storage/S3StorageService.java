package com.invoiceprocessor.adapter.out.storage;

import com.invoiceprocessor.application.port.out.StorageService;
import com.invoiceprocessor.application.port.out.IntegrationEndpointRepository;
import com.invoiceprocessor.domain.entity.EndpointType;
import com.invoiceprocessor.domain.entity.IntegrationEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

@Component
public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final IntegrationEndpointRepository integrationEndpointRepository;
    private final Object clientLock = new Object();
    private S3Client cachedClient;
    private S3Config cachedConfig;

    public S3StorageService(
            IntegrationEndpointRepository integrationEndpointRepository) {
        this.integrationEndpointRepository = integrationEndpointRepository;
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, String contentType) {
        ClientContext context = getClientContext();
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(context.config().bucket())
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

            context.client().putObject(putObjectRequest, RequestBody.fromBytes(bytes));

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
        ClientContext context = getClientContext();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(context.config().bucket())
                    .key(key)
                    .build();

            return context.client().getObjectAsBytes(getObjectRequest).asInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String key) {
        ClientContext context = getClientContext();
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(context.config().bucket())
                    .key(key)
                    .build();

            context.client().deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    private ClientContext getClientContext() {
        S3Config desiredConfig = resolveConfig();
        synchronized (clientLock) {
            if (cachedClient == null || cachedConfig == null || !cachedConfig.equals(desiredConfig)) {
                rebuildClient(desiredConfig);
            }
            return new ClientContext(cachedClient, cachedConfig);
        }
    }

    private void rebuildClient(S3Config config) {
        if (cachedClient != null) {
            try {
                cachedClient.close();
            } catch (Exception e) {
                logger.warn("Failed to close existing S3 client: {}", e.getMessage());
            }
        }
        cachedClient = buildClient(config);
        cachedConfig = config;
        ensureBucketExists(cachedClient, config.bucket());
        logger.info("Initialized S3 client for endpoint {}", config.endpoint());
    }

    private S3Config resolveConfig() {
        return integrationEndpointRepository.findByType(EndpointType.STORAGE_TARGET)
                .map(this::fromEndpoint)
                .orElseThrow(() -> new IllegalStateException(
                        "No STORAGE_TARGET integration endpoint configured. Please configure S3/MinIO settings via the admin API."));
    }

    private S3Config fromEndpoint(IntegrationEndpoint endpoint) {
        Map<String, String> settings = endpoint.getSettings();
        String endpointUrl = requiredSetting(settings, "endpoint", "url");
        String accessKey = requiredSetting(settings, "accessKey", "access_key");
        String secretKey = requiredSetting(settings, "secretKey", "secret_key");
        String bucket = requiredSetting(settings, "bucket", "bucketName");
        String region = software.amazon.awssdk.regions.Region.US_EAST_1.toString();
        boolean forcePathStyle = parseBoolean(settings.getOrDefault("forcePathStyle", "true"));

        return new S3Config(endpointUrl, accessKey, secretKey, bucket, region, forcePathStyle);
    }

    private S3Client buildClient(S3Config config) {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(config.accessKey(), config.secretKey());
        return S3Client.builder()
                .endpointOverride(URI.create(config.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(config.region()))
                .forcePathStyle(config.forcePathStyle())
                .build();
    }

    private void ensureBucketExists(S3Client client, String bucketName) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            client.createBucket(createBucketRequest);
        }
    }

    private String requiredSetting(Map<String, String> settings, String... keys) {
        if (settings != null) {
            for (String key : keys) {
                String value = settings.get(key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        throw new IllegalStateException("Missing required S3 setting: one of " + String.join(", ", keys));
    }

    private boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    private record S3Config(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket,
            String region,
            boolean forcePathStyle
    ) {}

    private record ClientContext(
            S3Client client,
            S3Config config
    ) {}
}

