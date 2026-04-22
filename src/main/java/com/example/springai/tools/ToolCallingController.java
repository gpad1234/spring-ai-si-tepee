package com.example.springai.tools;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pattern 5 — Tool Calling Controller & Service
 *
 * <p>The {@link ChatClient} is given access to tools per-request via
 * {@code .tools(toolBean)}. The model autonomously decides if and when to call them.
 *
 * <p>See docs/patterns/05-tool-calling.md for design rationale.
 */
@RestController
@RequestMapping("/api/tools")
public class ToolCallingController {

    private final ToolCallingService toolCallingService;

    public ToolCallingController(ToolCallingService toolCallingService) {
        this.toolCallingService = toolCallingService;
    }

    /**
     * POST /api/tools/chat
     * Sends a message with access to all registered tools.
     */
    @PostMapping("/chat")
    public ResponseEntity<ToolChatResponse> chat(@Valid @RequestBody ToolChatRequest request) {
        String reply = toolCallingService.chatWithTools(request.message());
        return ResponseEntity.ok(new ToolChatResponse(reply));
    }

    public record ToolChatRequest(@NotBlank String message) {}
    public record ToolChatResponse(String reply) {}
}
