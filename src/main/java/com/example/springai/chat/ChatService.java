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
 * <p>Key decisions:
 * <ul>
 *   <li>Prompt text lives in {@code src/main/resources/prompts/chat.st} — never inline long strings.</li>
 *   <li>The {@link ChatClient} bean from {@code AiConfig} carries the default system prompt;
 *       callers may override it per-request.</li>
 *   <li>Return plain {@code String}; callers decide serialization.</li>
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
     * @param message      the user message
     * @param systemPrompt optional per-request system prompt; null uses the default
     */
    public String chat(String message, String systemPrompt) {
        ChatClient client = StringUtils.hasText(systemPrompt)
                ? chatClientBuilder.defaultSystem(systemPrompt).build()
                : chatClient;

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
