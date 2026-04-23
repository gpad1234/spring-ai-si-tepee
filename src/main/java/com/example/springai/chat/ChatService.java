package com.example.springai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Pattern 1 — Simple Chat Service
 *
 * <p>Handles single-turn Q&A without conversation history.
 *
 * <p>Key architectural decisions:
 * <ul>
 *   <li><strong>Prompt text lives in {@code src/main/resources/prompts/chat.st}</strong> —
 *       Never inline long strings. Enables editing prompts without recompilation.</li>
 *   <li><strong>System prompt override per-request</strong> — Callers may provide a custom
 *       system prompt; null means use the default from {@code AiConfig}.</li>
 *   <li><strong>Returns plain String</strong> — Callers decide serialization and error handling.
 *       This keeps the service focused on orchestration.</li>
 *   <li><strong>ChatClient is injected</strong> — Never call ChatModel directly; always go through
 *       the higher-level ChatClient which handles retries, streaming setup, etc.</li>
 * </ul>
 *
 * <p>Production considerations:
 * <ul>
 *   <li>Add timeout configuration: {@code spring.ai.anthropic.chat.options.timeout=30s}</li>
 *   <li>Add rate limiting if exposed to untrusted callers (Claude has token-per-minute limits).</li>
 *   <li>Consider adding span propagation for distributed tracing (Micrometer/Spring Cloud Sleuth).</li>
 * </ul>
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatClient.Builder chatClientBuilder;

    @Value("classpath:prompts/chat.st")
    private Resource chatPromptTemplate;

    public ChatService(ChatClient chatClient, ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClient;
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * Send a message and return the model's text reply.
     *
     * <p>If {@code systemPrompt} is provided and non-empty, it overrides the default
     * system prompt for this request only. Otherwise, uses the default system prompt
     * configured in {@code AiConfig}.
     *
     * @param message      the user message (required, non-blank)
     * @param systemPrompt optional per-request system prompt; null or blank uses default
     * @return the assistant's text reply
     * @throws IllegalArgumentException if message is blank
     */
    public String chat(String message, String systemPrompt) {
        // Validate input — message must be provided
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Message cannot be blank");
        }

        // Choose which ChatClient to use: default or override with custom system prompt
        ChatClient client = StringUtils.hasText(systemPrompt)
                ? chatClientBuilder.defaultSystem(systemPrompt).build()
                : chatClient;

        // Call Claude and return the text content
        return client.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * Variant: uses a StringTemplate (.st) prompt file with named variables.
     *
     * @param topic the template variable {@code {topic}}
     */
    public String chatWithTemplate(String topic) {
        PromptTemplate template = new PromptTemplate(chatPromptTemplate);
        var prompt = template.create(java.util.Map.of("topic", topic));

        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}
