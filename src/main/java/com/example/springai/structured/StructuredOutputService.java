package com.example.springai.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

/**
 * Pattern 3 — Structured Output Service
 *
 * <p>Key decisions:
 * <ul>
 *   <li>{@code .entity(Class)} is the preferred fluent API — it creates a
 *       {@link BeanOutputConverter} internally and appends the JSON schema to the prompt.</li>
 *   <li>Target types must be Java records or POJOs with a no-arg constructor.
 *       Jackson annotations can refine field names and nullability.</li>
 *   <li>Keep extraction prompts in {@code prompts/extraction.st}; the schema is injected
 *       automatically — do not manually describe the JSON format in the prompt.</li>
 * </ul>
 */
@Service
public class StructuredOutputService {

    private final ChatClient chatClient;

    public StructuredOutputService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // ─── Target types ─────────────────────────────────────────────────────────

    /**
     * Extracted person information. All fields nullable — the model may not find them.
     */
    public record PersonInfo(
            String name,
            Integer age,
            String email,
            String city
    ) {}

    /**
     * Sentiment analysis result.
     */
    public record SentimentResult(
            Sentiment sentiment,
            double confidenceScore,    // 0.0 – 1.0
            String reasoning
    ) {}

    public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL, MIXED }

    // ─── Service methods ──────────────────────────────────────────────────────

    /**
     * Extract person information from unstructured text.
     */
    public PersonInfo extractPerson(String text) {
        return chatClient.prompt()
                .system("Extract person information from the provided text. Return null for fields not present.")
                .user("Extract person info from this text:\n\n" + text)
                .call()
                .entity(PersonInfo.class);
    }

    /**
     * Perform sentiment analysis on text.
     */
    public SentimentResult analyzeSentiment(String text) {
        return chatClient.prompt()
                .system("You are a precise sentiment analysis engine.")
                .user("Analyse the sentiment of this text:\n\n" + text)
                .call()
                .entity(SentimentResult.class);
    }
}
