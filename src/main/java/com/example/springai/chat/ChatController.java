package com.example.springai.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pattern 1 — Simple Chat
 *
 * <p>REST endpoint for single-turn Q&A. Suitable for stateless interactions where
 * conversation history is not needed.
 *
 * <p>See docs/patterns/01-chat.md for design rationale.
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
     * Body: { "message": "...", "systemPrompt": "..." (optional) }
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = chatService.chat(request.message(), request.systemPrompt());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    // ─── Request / Response records ──────────────────────────────────────────

    public record ChatRequest(
            @NotBlank String message,
            String systemPrompt          // optional override of default system prompt
    ) {}

    public record ChatResponse(String reply) {}
}
