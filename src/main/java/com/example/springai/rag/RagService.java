package com.example.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Pattern 4b — RAG Query Service
 *
 * <p>Handles the <em>retrieval + generation</em> half of the RAG pipeline.
 * Retrieves relevant document chunks via similarity search, injects them as
 * context into the system prompt, then calls the chat model.
 */
@Service
public class RagService {

    private static final int    TOP_K                = 5;
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private final ChatClient  chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient  = builder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * Answer a question using retrieved document context.
     *
     * @param question the user's natural-language question
     * @return grounded answer with source citations
     */
    public String answer(String question) {
        var docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );

        String context = docs.stream()
                .map(d -> d.getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = """
                You are a knowledgeable assistant. Answer questions using ONLY the provided context.
                If the context does not contain sufficient information, say so explicitly.
                Always cite the source document when possible.

                Context:
                """ + context;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }
}
