package com.aipdf.chat.service;

import com.aipdf.chat.dto.AskResponse;
import com.aipdf.chat.dto.StatsResponse;
import com.aipdf.chat.dto.UploadResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Service performing RAG (Retrieval-Augmented Generation) pipeline:
 * 1. PDF Text Extraction (Apache PDFBox)
 * 2. Text Chunking (LangChain4j DocumentSplitter)
 * 3. Embeddings Generation & Storage (InMemoryEmbeddingStore)
 * 4. Semantic Search Retrieval
 * 5. Dynamic Google AI Studio Model Discovery (ListModels API) & Gemini Q&A
 */
@Service
public class PdfRagService {

    private static final Logger log = LoggerFactory.getLogger(PdfRagService.class);

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${langchain4j.googleai.api-key:demo-key}")
    private String defaultApiKey;

    // In-memory session stats
    private boolean pdfUploaded = false;
    private String currentFileName = null;
    private int questionsAskedCount = 0;
    private int chunksCount = 0;
    private String activeModelUsed = "Google Gemini (Auto-Discovered)";
    private final LinkedList<String> recentQuestions = new LinkedList<>();

    public PdfRagService(InMemoryEmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Handles single PDF upload, text parsing, chunk splitting, embedding generation, and storage.
     */
    public synchronized UploadResponse uploadAndIngestPdf(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Please upload a valid non-empty PDF file.");
        }

        // 1. Extract raw text using Apache PDFBox
        String extractedText;
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            extractedText = textStripper.getText(document);
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("The uploaded PDF file contains no readable text or is image-only.");
        }

        // 2. Split text into chunks (e.g. 500 max characters per chunk, 50 overlap)
        var splitter = DocumentSplitters.recursive(500, 50);
        List<TextSegment> segments = splitter.split(Document.from(extractedText));

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Could not create text chunks from the uploaded PDF.");
        }

        // 3. Embed each segment and add to InMemoryEmbeddingStore
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        for (int i = 0; i < segments.size(); i++) {
            embeddingStore.add(embeddings.get(i), segments.get(i));
        }

        // Update session stats
        this.pdfUploaded = true;
        this.currentFileName = file.getOriginalFilename();
        this.chunksCount = segments.size();

        StatsResponse stats = getStats();
        return new UploadResponse(true, "PDF processed and embedded successfully!", this.currentFileName, this.chunksCount, stats);
    }

    /**
     * Processes user questions using RAG (Semantic search + Dynamic Gemini API model discovery).
     */
    public synchronized AskResponse askQuestion(String question, String providedApiKey) {
        if (!pdfUploaded || chunksCount == 0) {
            throw new IllegalStateException("Please upload a PDF document before asking questions.");
        }

        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be empty.");
        }

        // 1. Generate embedding for the question
        Embedding questionEmbedding = embeddingModel.embed(question.trim()).content();

        // 2. Perform semantic search retrieval in InMemoryEmbeddingStore
        List<EmbeddingMatch<TextSegment>> relevantMatches = embeddingStore.findRelevant(questionEmbedding, 4, 0.3);

        List<String> retrievedSnippets = relevantMatches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());

        // 3. Construct Context Prompt
        String contextBlock = retrievedSnippets.isEmpty() 
                ? "No specific matching context found in PDF." 
                : String.join("\n\n---\n\n", retrievedSnippets);

        String systemPrompt = String.format("""
                You are a helpful AI PDF Chat Assistant. Answer the user's question based strictly on the provided context retrieved from their uploaded PDF file.
                
                If the question cannot be answered using the context provided below, politely respond: "I could not find relevant information in the uploaded PDF to answer this question."
                
                Context from PDF:
                %s
                
                User Question:
                %s
                
                Answer:
                """, contextBlock, question.trim());

        // 4. Determine API Key to use (priority: header/provided key > environment variable > application.properties)
        String apiKeyToUse = (providedApiKey != null && !providedApiKey.trim().isEmpty()) 
                ? providedApiKey.trim() 
                : System.getenv("GEMINI_API_KEY");

        if (apiKeyToUse == null || apiKeyToUse.trim().isEmpty()) {
            apiKeyToUse = defaultApiKey;
        }

        if (apiKeyToUse == null || apiKeyToUse.trim().isEmpty() || "demo-key".equalsIgnoreCase(apiKeyToUse)) {
            throw new IllegalStateException("Google Gemini API Key is missing. Please configure GEMINI_API_KEY or provide your API key in the app.");
        }

        // 5. Query Google AI Studio ListModels API to dynamically discover supported models for this key
        List<String> discoveredModels = queryListModels(apiKeyToUse);
        
        if (discoveredModels.isEmpty()) {
            throw new RuntimeException("Error communicating with Gemini API: No models supporting generateContent found for your API key. Verify key permissions at https://aistudio.google.com/app/apikey");
        }

        log.info("ListModels returned available generateContent models: {}", discoveredModels);
        System.out.println("[ListModels] Discovered supported models for API key: " + discoveredModels);

        // 6. Invoke Google Gemini Chat Model using the first dynamically discovered model
        String aiAnswer = null;
        Exception lastException = null;
        String modelNameUsed = null;

        for (String candidateModel : discoveredModels) {
            try {
                log.info("Attempting generation using model: {}", candidateModel);
                GoogleAiGeminiChatModel geminiModel = GoogleAiGeminiChatModel.builder()
                        .apiKey(apiKeyToUse)
                        .modelName(candidateModel)
                        .temperature(0.2)
                        .build();

                aiAnswer = geminiModel.generate(systemPrompt);
                if (aiAnswer != null && !aiAnswer.trim().isEmpty()) {
                    modelNameUsed = candidateModel;
                    this.activeModelUsed = "Google Gemini (" + candidateModel + ")";
                    break;
                }
            } catch (Exception e) {
                log.warn("Model '{}' failed during generateContent: {}", candidateModel, e.getMessage());
                if (lastException == null) {
                    lastException = e;
                }
            }
        }

        if (aiAnswer == null) {
            throw new RuntimeException("Error communicating with Gemini API: " + 
                    (lastException != null ? lastException.getMessage() : "Unable to generate response"), lastException);
        }

        // 7. Update Session Stats
        this.questionsAskedCount++;
        
        recentQuestions.remove(question.trim());
        recentQuestions.addFirst(question.trim());
        while (recentQuestions.size() > 5) {
            recentQuestions.removeLast();
        }

        StatsResponse stats = getStats();
        return new AskResponse(question, aiAnswer, retrievedSnippets, stats);
    }

    /**
     * Dynamically queries the Google AI Studio ListModels API (v1beta) and returns
     * all model names supporting content generation for the provided API Key.
     */
    private List<String> queryListModels(String apiKey) {
        List<String> supportedModels = new ArrayList<>();
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            if (jsonResponse != null) {
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                JsonNode modelsNode = rootNode.get("models");

                if (modelsNode != null && modelsNode.isArray()) {
                    for (JsonNode modelNode : modelsNode) {
                        String rawName = modelNode.has("name") ? modelNode.get("name").asText() : "";
                        JsonNode methodsNode = modelNode.get("supportedGenerationMethods");

                        boolean supportsGenerate = false;
                        if (methodsNode != null && methodsNode.isArray()) {
                            for (JsonNode method : methodsNode) {
                                if ("generateContent".equalsIgnoreCase(method.asText())) {
                                    supportsGenerate = true;
                                    break;
                                }
                            }
                        }

                        if (supportsGenerate && rawName.startsWith("models/")) {
                            String modelIdentifier = rawName.substring("models/".length());
                            // Exclude specialized preview/embedding/imagen models if standard chat is desired
                            if (modelIdentifier.contains("gemini") && !modelIdentifier.contains("embedding") && !modelIdentifier.contains("robotics")) {
                                supportedModels.add(modelIdentifier);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query ListModels API: {}", e.getMessage());
        }

        return supportedModels;
    }

    /**
     * Returns current project dashboard statistics.
     */
    public synchronized StatsResponse getStats() {
        return new StatsResponse(
                this.pdfUploaded,
                this.currentFileName,
                this.questionsAskedCount,
                this.chunksCount,
                this.activeModelUsed,
                new ArrayList<>(this.recentQuestions)
        );
    }

    /**
     * Resets session state and stats.
     */
    public synchronized void resetSession() {
        this.pdfUploaded = false;
        this.currentFileName = null;
        this.chunksCount = 0;
        this.questionsAskedCount = 0;
        this.activeModelUsed = "Google Gemini (Auto-Discovered)";
        this.recentQuestions.clear();
    }
}
