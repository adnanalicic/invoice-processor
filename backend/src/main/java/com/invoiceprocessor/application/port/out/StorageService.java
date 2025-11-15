package com.invoiceprocessor.application.port.out;

import java.io.InputStream;

/**
 * Port for storing and retrieving document content from object storage (S3).
 */
public interface StorageService {
    
    /**
     * Uploads a file to storage and returns the storage location/path.
     * 
     * @param key The unique key/path for the file in storage
     * @param inputStream The file content as input stream
     * @param contentType The MIME type of the file
     * @return The storage location/path that can be used as content reference
     */
    String uploadFile(String key, InputStream inputStream, String contentType);
    
    /**
     * Retrieves a file from storage.
     * 
     * @param key The storage key/path
     * @return The file content as input stream
     */
    InputStream downloadFile(String key);
    
    /**
     * Deletes a file from storage.
     * 
     * @param key The storage key/path
     */
    void deleteFile(String key);
}

