package com.example.springai.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pattern 1 — Simple Chat
 *
 * <p>REST endpoint for single-turn Q&A without conversation history.
 * Suitable for stateless interactions where each request is independent.
 *
 * <p>Key design patterns:
 * <ul>
 *   <li><strong>Controller responsibility</strong>: HTTP input/output mapping, validation.
 *       Business logic lives in {@link ChatService}.</li>
 *   <li><strong>Request/response records</strong>: Clear, immutable contracts.
 *       {@code ChatRequest.systemPrompt} is optional — null means use the default.</li>
 *   <li><strong>Error handling</strong>: {@code @Valid} ensures non-blank messages.
 *       Service-layer exceptions bubble up to global error handler (if present).</li>
 * </ul>
 *
 * <p>API contract:
 * <pre>
 * POST /api/chat
 * Content-Type: application/json
 *
 * Request:
 *   {
 *     "message": "What is Spring AI?",
 *     "systemPrompt": "You are a helpful assistant. Be concise." (optional)
 *   }
 *
 * Response (200 OK):
 *   {
 *     "response": "Spring AI is a framework for building..."
 *   }
 *
 * Response (400 Bad Request):
 *   message is blank or missing
 * </pre>
 *
 * <p>Example with curl:
 * <pre>
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "What is Spring AI?"}'
 *
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "message": "Explain RAG in 3 sentences",
 *     "systemPrompt": "You are a concise expert. Answer in bullets."
 *   }'
 * </pre>
 *
 * <p>See docs/PATTERNS.md for rationale and comparison with streaming and RAG patterns.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * POST /api/chat
     *
     * <p>Sends a user message to Claude and returns the assistant's reply.
     *
     * @param request the chat request with message and optional system prompt
     * @return HTTP 200 with ChatResponse containing the assistant's reply
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            String reply = chatService.chat(request.message(), request.systemPrompt());
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (IllegalArgumentException e) {
            // Caught input validation errors from ChatService
            return ResponseEntity.badRequest().build();
        }
    }

    // ─── Request / Response records ──────────────────────────────────────────

    /**
     * Request body for the chat endpoint.
     *
     * @param message      user's question or statement (required, non-blank)
     * @param systemPrompt optional system prompt to override the default;
     *                     if null or blank, uses the default from AiConfig
     */
    public record ChatRequest(
            @NotBlank(message = "message cannot be blank") String message,
            String systemPrompt
    ) {}

    /**
     * Response body for the chat endpoint.
     *
     * @param response the assistant's text reply
     */
    public record ChatResponse(String response) {}
}
