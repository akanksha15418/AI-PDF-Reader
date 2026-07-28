package com.aipdf.chat.dto;

/**
 * Data Transfer Object returned after PDF upload and processing.
 */
public class UploadResponse {

    private boolean success;
    private String message;
    private String fileName;
    private int chunksCreated;
    private StatsResponse stats;

    public UploadResponse() {
    }

    public UploadResponse(boolean success, String message, String fileName, int chunksCreated, StatsResponse stats) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.chunksCreated = chunksCreated;
        this.stats = stats;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getChunksCreated() {
        return chunksCreated;
    }

    public void setChunksCreated(int chunksCreated) {
        this.chunksCreated = chunksCreated;
    }

    public StatsResponse getStats() {
        return stats;
    }

    public void setStats(StatsResponse stats) {
        this.stats = stats;
    }
}
