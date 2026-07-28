package com.aipdf.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration for LangChain4j RAG components.
 * Configures the in-memory vector store and local ONNX embedding model.
 */
@Configuration
public class LangChainConfig {

    /**
     * In-memory vector store required by the prompt specifications.
     * Keeps vectors and text segments in RAM for high performance and local testing.
     */
    @Bean
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * Local embedding model running in-process (AllMiniLmL6V2).
     * Converts text chunks into 384-dimensional dense vector embeddings.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
