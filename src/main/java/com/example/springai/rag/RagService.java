package com.example.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Pattern 4b — RAG Query Service
 *
 * <p>Handles the <em>retrieval + generation</em> half of the RAG pipeline.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>{@link QuestionAnswerAdvisor} wires retrieval into the chat pipeline declaratively —
 *       no manual similarity-search calls in service code.</li>
 *   <li>Similarity threshold 0.7 and top-K 5 are starting points; tune against your corpus.</li>
 *   <li>The prompt template instructs the model to cite sources and admit ignorance when
 *       retrieved context is insufficient — prevents hallucination.</li>
 *   <li>A dedicated {@link ChatClient} is built here to isolate the RAG system prompt from
 *       the default client bean.</li>
 * </ul>
 *
 * <p>See docs/patterns/04-rag.md for design rationale.
 */
@Service
public class RagService {

    private static final int    TOP_K               = 5;
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private final ChatClient ragChatClient;

    @Value("classpath:prompts/rag.st")
    private Resource ragPromptTemplate;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        var searchRequest = SearchRequest.defaults()
                .withTopK(TOP_K)
                .withSimilarityThreshold(SIMILARITY_THRESHOLD);

        this.ragChatClient = builder
                .defaultSystem("""
                        You are a knowledgeable assistant. Answer questions using ONLY the provided context.
                        If the context does not contain sufficient information, say so explicitly.
                        Always cite the source document when possible.
                        """)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, searchRequest))
                .build();
    }

    /**
     * Answer a question using retrieved document context.
     *
     * @param question the user's natural-language question
     * @return grounded answer with source citations
     */
    public String answer(String question) {
        return ragChatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
