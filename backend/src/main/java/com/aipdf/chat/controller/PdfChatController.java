package com.aipdf.chat.controller;

import com.aipdf.chat.dto.AskRequest;
import com.aipdf.chat.dto.AskResponse;
import com.aipdf.chat.dto.StatsResponse;
import com.aipdf.chat.dto.UploadResponse;
import com.aipdf.chat.service.PdfRagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller exposing backend endpoints for AI PDF Chat Assistant:
 * - POST /upload (or /api/pdf/upload): Single PDF document upload & RAG ingestion
 * - POST /ask (or /api/pdf/ask): Semantic search + Gemini question answering
 * - GET /stats (or /api/pdf/stats): Dashboard session statistics
 */
@RestController
public class PdfChatController {

    private final PdfRagService pdfRagService;

    public PdfChatController(PdfRagService pdfRagService) {
        this.pdfRagService = pdfRagService;
    }

    /**
     * Upload single PDF file and create embeddings.
     */
    @PostMapping(value = {"/upload", "/api/pdf/upload"})
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            UploadResponse response = pdfRagService.uploadAndIngestPdf(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to process PDF: " + e.getMessage()));
        }
    }

    /**
     * Ask question based on the uploaded PDF.
     */
    @PostMapping(value = {"/ask", "/api/pdf/ask"})
    public ResponseEntity<?> askQuestion(
            @RequestBody AskRequest request,
            @RequestHeader(value = "X-Gemini-Api-Key", required = false) String headerApiKey) {
        try {
            String apiKey = (request.getApiKey() != null && !request.getApiKey().trim().isEmpty())
                    ? request.getApiKey()
                    : headerApiKey;

            AskResponse response = pdfRagService.askQuestion(request.getQuestion(), apiKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generating answer: " + e.getMessage()));
        }
    }

    /**
     * Fetch project statistics for dashboard.
     */
    @GetMapping(value = {"/stats", "/api/pdf/stats"})
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(pdfRagService.getStats());
    }

    /**
     * Optional endpoint to clear session and reset PDF store.
     */
    @PostMapping(value = {"/reset", "/api/pdf/reset"})
    public ResponseEntity<StatsResponse> resetSession() {
        pdfRagService.resetSession();
        return ResponseEntity.ok(pdfRagService.getStats());
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
