package com.aipdf.chat.dto;

import java.util.List;

/**
 * Data Transfer Object representing current dashboard project statistics.
 */
public class StatsResponse {

    private boolean pdfUploaded;
    private String fileName;
    private int questionsAsked;
    private int chunksCreated;
    private String aiModel;
    private List<String> recentQuestions;

    public StatsResponse() {
    }

    public StatsResponse(boolean pdfUploaded, String fileName, int questionsAsked, int chunksCreated, String aiModel, List<String> recentQuestions) {
        this.pdfUploaded = pdfUploaded;
        this.fileName = fileName;
        this.questionsAsked = questionsAsked;
        this.chunksCreated = chunksCreated;
        this.aiModel = aiModel;
        this.recentQuestions = recentQuestions;
    }

    public boolean isPdfUploaded() {
        return pdfUploaded;
    }

    public void setPdfUploaded(boolean pdfUploaded) {
        this.pdfUploaded = pdfUploaded;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getQuestionsAsked() {
        return questionsAsked;
    }

    public void setQuestionsAsked(int questionsAsked) {
        this.questionsAsked = questionsAsked;
    }

    public int getChunksCreated() {
        return chunksCreated;
    }

    public void setChunksCreated(int chunksCreated) {
        this.chunksCreated = chunksCreated;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public List<String> getRecentQuestions() {
        return recentQuestions;
    }

    public void setRecentQuestions(List<String> recentQuestions) {
        this.recentQuestions = recentQuestions;
    }
}
