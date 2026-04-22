package com.example.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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
 *   <li>Replace {@link SimpleVectorStore} with PgVectorStore, ChromaVectorStore, etc.</li>
 *   <li>Add {@code ChatMemory} bean here for shared conversation state.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    /**
     * Default {@link ChatClient} with a system prompt applied to all requests.
     * Controllers that need different system prompts should inject
     * {@link ChatClient.Builder} and build their own.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a helpful assistant. Be concise and accurate.")
                .build();
    }

    /**
     * In-memory vector store for development / demos.
     * Swap the implementation without changing any service code.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }
}
