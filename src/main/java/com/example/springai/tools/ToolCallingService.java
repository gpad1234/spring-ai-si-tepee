package com.example.springai.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Pattern 5 — Tool Calling Service
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Tools are registered per-call via {@code .tools()} — this keeps the tool set
 *       composable and avoids polluting the default {@link ChatClient} bean.</li>
 *   <li>Inject the tool bean directly, not a collection — makes dependencies explicit
 *       and aids testability.</li>
 *   <li>Spring AI automatically handles the tool-call loop: model decides → tool executes →
 *       result fed back → model responds. No manual orchestration needed.</li>
 * </ul>
 */
@Service
public class ToolCallingService {

    private final ChatClient.Builder chatClientBuilder;
    private final BuiltInTools builtInTools;

    public ToolCallingService(ChatClient.Builder chatClientBuilder, BuiltInTools builtInTools) {
        this.chatClientBuilder = chatClientBuilder;
        this.builtInTools = builtInTools;
    }

    /**
     * Chat with access to all built-in tools.
     * The model will call them as needed to answer the question.
     */
    public String chatWithTools(String message) {
        return chatClientBuilder
                .defaultSystem("You are a helpful assistant with access to tools. Use them when needed.")
                .build()
                .prompt()
                .user(message)
                .tools(builtInTools)
                .call()
                .content();
    }
}
