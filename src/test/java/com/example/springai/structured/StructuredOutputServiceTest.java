package com.example.springai.structured;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StructuredOutputService}.
 *
 * <p>See docs/patterns/03-structured-output.md for the testing strategy note:
 * structured output tests should also include a "schema contract" test that
 * verifies the target records still match what the model is expected to return
 * (run periodically in CI against a live model).
 */
class StructuredOutputServiceTest {

    @Test
    void personInfoRecordHasExpectedFields() {
        // Verify the record contract hasn't silently changed
        var info = new StructuredOutputService.PersonInfo("Alice", 30, "alice@example.com", "London");
        assertThat(info.fullName()).isEqualTo("Alice");
        assertThat(info.age()).isEqualTo(30);
        assertThat(info.email()).isEqualTo("alice@example.com");
        assertThat(info.location()).isEqualTo("London");
    }

    @Test
    void sentimentEnumCoversAllCases() {
        var values = StructuredOutputService.Sentiment.values();
        assertThat(values).containsExactlyInAnyOrder(
                StructuredOutputService.Sentiment.POSITIVE,
                StructuredOutputService.Sentiment.NEGATIVE,
                StructuredOutputService.Sentiment.NEUTRAL,
                StructuredOutputService.Sentiment.MIXED
        );
    }
}
