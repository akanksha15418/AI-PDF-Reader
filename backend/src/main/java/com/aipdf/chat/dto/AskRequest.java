package com.aipdf.chat.dto;

/**
 * Data Transfer Object for user question requests.
 */
public class AskRequest {

    private String question;
    private String apiKey; // Optional API key passed from frontend UI if environment variable is not set

    public AskRequest() {
    }

    public AskRequest(String question, String apiKey) {
        this.question = question;
        this.apiKey = apiKey;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
