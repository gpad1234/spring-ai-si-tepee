package com.example.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central AI configuration.
 *
 * <p>Production swap points:
 * <ul>
 *   <li>Replace {@link HashEmbeddingModel} with an API-backed model (e.g. OpenAI
 *       text-embedding-3-small) or the transformers starter for semantic accuracy.</li>
 *   <li>Replace {@link SimpleVectorStore} with PgVectorStore, ChromaVectorStore, etc.</li>
 *   <li>Add {@code ChatMemory} bean here for shared conversation state.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    /**
     * Default {@link ChatClient} with a system prompt applied to all requests.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a helpful assistant. Be concise and accurate.")
                .build();
    }

    /**
     * Zero-native-dependency embedding model — deterministic SHA-256 hash vectors.
     * Swap for a real API-backed model in production for meaningful semantic search.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new HashEmbeddingModel();
    }

    /**
     * In-memory vector store for development / demos.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
