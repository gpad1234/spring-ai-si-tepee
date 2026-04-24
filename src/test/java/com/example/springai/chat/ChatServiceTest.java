package com.example.springai.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link ChatService}.
 *
 * <p>Tests the service layer business logic: input validation and ChatClient delegation.
 *
 * <p>Design note: The ChatService is very thin — it validates input and delegates to ChatClient.
 * Most of the behavior is verified through controller and integration tests.
 * This test focuses on the contract: invalid input should be rejected.
 */
class ChatServiceTest {

    @Test
    void chatWithNullMessageThrowsException() {
        // Verify input validation: null message should be rejected
        var mockChatClient = mock(ChatClient.class);
        var mockBuilder = mock(ChatClient.Builder.class);
        var service = new ChatService(mockChatClient, mockBuilder);

        assertThatThrownBy(() -> service.chat(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message cannot be blank");
    }

    @Test
    void chatWithBlankMessageThrowsException() {
        // Verify input validation: blank message should be rejected
        var mockChatClient = mock(ChatClient.class);
        var mockBuilder = mock(ChatClient.Builder.class);
        var service = new ChatService(mockChatClient, mockBuilder);

        assertThatThrownBy(() -> service.chat("   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message cannot be blank");
    }

    @Test
    void chatWithEmptyMessageThrowsException() {
        // Verify input validation: empty string message should be rejected
        var mockChatClient = mock(ChatClient.class);
        var mockBuilder = mock(ChatClient.Builder.class);
        var service = new ChatService(mockChatClient, mockBuilder);

        assertThatThrownBy(() -> service.chat("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message cannot be blank");
    }
}
