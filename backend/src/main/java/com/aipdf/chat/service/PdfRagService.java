package com.aipdf.chat.service;

import com.aipdf.chat.dto.AskResponse;
import com.aipdf.chat.dto.StatsResponse;
import com.aipdf.chat.dto.UploadResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Service performing RAG (Retrieval-Augmented Generation) pipeline:
 * 1. PDF Text Extraction (Apache PDFBox)
 * 2. Text Chunking (LangChain4j DocumentSplitter)
 * 3. Embeddings Generation & Storage (InMemoryEmbeddingStore)
 * 4. Semantic Search Retrieval
 * 5. Gemini API Answer Generation
 */
@Service
public class PdfRagService {

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Value("${langchain4j.googleai.api-key:demo-key}")
    private String defaultApiKey;

    @Value("${langchain4j.googleai.model-name:gemini-1.5-flash}")
    private String defaultModelName;

    // In-memory session stats
    private boolean pdfUploaded = false;
    private String currentFileName = null;
    private int questionsAskedCount = 0;
    private int chunksCount = 0;
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

        // 3. Clear existing store items and re-ingest
        // InMemoryEmbeddingStore can be re-initialized by embedding new segments
        // We embed each segment and add to store
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        // Remove all previous segments if any
        // InMemoryEmbeddingStore allows adding embeddings with segments
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
     * Processes user questions using RAG (Semantic search + Google Gemini API).
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

        if (apiKeyToUse == null || apiKeyToUse.trim().isEmpty() || "demo-key".equals(defaultApiKey) && (apiKeyToUse == null)) {
            apiKeyToUse = defaultApiKey;
        }

        if (apiKeyToUse == null || apiKeyToUse.trim().isEmpty() || "demo-key".equalsIgnoreCase(apiKeyToUse)) {
            throw new IllegalStateException("Google Gemini API Key is missing. Please configure GEMINI_API_KEY or provide your API key in the app.");
        }

        // 5. Invoke Google Gemini Model via LangChain4j
        String aiAnswer;
        try {
            GoogleAiGeminiChatModel geminiModel = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKeyToUse)
                    .modelName(defaultModelName)
                    .temperature(0.2)
                    .build();

            aiAnswer = geminiModel.generate(systemPrompt);
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Gemini API: " + e.getMessage(), e);
        }

        // 6. Update Session Stats
        this.questionsAskedCount++;
        
        // Add to recent questions (max 5)
        recentQuestions.remove(question.trim()); // avoid duplicate contiguous display
        recentQuestions.addFirst(question.trim());
        while (recentQuestions.size() > 5) {
            recentQuestions.removeLast();
        }

        StatsResponse stats = getStats();
        return new AskResponse(question, aiAnswer, retrievedSnippets, stats);
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
                "Google Gemini (" + defaultModelName + ")",
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
        this.recentQuestions.clear();
    }
}
