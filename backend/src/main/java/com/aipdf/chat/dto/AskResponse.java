package com.aipdf.chat.dto;

import java.util.List;

/**
 * Data Transfer Object for AI response to user questions.
 */
public class AskResponse {

    private String question;
    private String answer;
    private List<String> retrievedContext;
    private StatsResponse stats;

    public AskResponse() {
    }

    public AskResponse(String question, String answer, List<String> retrievedContext, StatsResponse stats) {
        this.question = question;
        this.answer = answer;
        this.retrievedContext = retrievedContext;
        this.stats = stats;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getRetrievedContext() {
        return retrievedContext;
    }

    public void setRetrievedContext(List<String> retrievedContext) {
        this.retrievedContext = retrievedContext;
    }

    public StatsResponse getStats() {
        return stats;
    }

    public void setStats(StatsResponse stats) {
        this.stats = stats;
    }
}
