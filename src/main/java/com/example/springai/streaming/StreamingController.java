package com.example.springai.streaming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Pattern 2 — Streaming Chat
 *
 * <p>Uses Server-Sent Events (SSE) to push tokens to the client as they arrive.
 * The client reads {@code text/event-stream}; each event contains one token chunk.
 *
 * <p>See docs/patterns/02-streaming.md for design rationale.
 */
@RestController
@RequestMapping("/api/stream")
public class StreamingController {

    private final StreamingService streamingService;

    public StreamingController(StreamingService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * POST /api/stream
     * Response: {@code text/event-stream} — token-by-token chunks.
     *
     * <p><b>Important:</b> Do NOT call {@code .block()} on the returned {@code Flux}.
     * Spring MVC handles subscription via its async support with virtual threads (Java 21).
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Valid @RequestBody StreamRequest request) {
        return streamingService.stream(request.message());
    }

    public record StreamRequest(@NotBlank String message) {}
}
