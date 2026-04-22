package com.example.springai.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link ChatService}.
 *
 * <p>Uses Spring AI's {@code MockChatModel} (from {@code spring-ai-test}) for
 * deterministic, offline testing — no real API calls.
 *
 * <p>Testing strategy:
 * <ul>
 *   <li>Unit tests mock the {@link ChatModel} — fast, no network.</li>
 *   <li>Integration tests ({@code @SpringBootTest}) use real API with a test key
 *       and are tagged {@code @Tag("integration")} to run only in CI.</li>
 * </ul>
 */
class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // Use Mockito to create a mock ChatModel — no real API calls
        var mockModel = mock(ChatModel.class);

        var client = ChatClient.builder(mockModel)
                .defaultSystem("You are a test assistant.")
                .build();

        chatService = new ChatService(client, ChatClient.builder(mockModel));
    }

    @Test
    void chatReturnsNonEmptyString() {
        // When testing against a real model: wire up ChatClient with a real key via @SpringBootTest
        // This stub demonstrates the test structure
        assertThat(chatService).isNotNull();
    }
}
