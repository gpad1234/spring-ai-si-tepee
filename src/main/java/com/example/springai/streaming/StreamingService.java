package com.example.springai.streaming;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Pattern 2 — Streaming Service
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Return {@link Flux}&lt;String&gt; — never subscribe here; the caller owns the subscription.</li>
 *   <li>{@code ChatClient.stream()} returns a reactive stream that back-pressures correctly.</li>
 *   <li>Error handling: {@code onErrorResume} converts model errors into a terminal error event
 *       so the client knows the stream ended abnormally.</li>
 * </ul>
 */
@Service
public class StreamingService {

    private final ChatClient chatClient;

    public StreamingService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Returns a cold {@link Flux} of text chunks. Subscription starts when
     * the HTTP response is opened by the client.
     */
    public Flux<String> stream(String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .onErrorResume(ex -> Flux.error(new StreamingException("Stream failed", ex)));
    }

    public static class StreamingException extends RuntimeException {
        public StreamingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
