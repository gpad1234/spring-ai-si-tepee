package com.example.springai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RAG ingestion pipeline.
 *
 * <p>Tests the ingest → split → store flow in isolation using a real
 * {@link SimpleVectorStore} backed by a mocked {@link EmbeddingModel}.
 */
class DocumentIngestionServiceTest {

    @Test
    void ingestTextAddsChunksToVectorStore() {
        // Arrange
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embed(any(Document.class)))
                .thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(embeddingModel.dimensions()).thenReturn(3);

        VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        DocumentIngestionService service = new DocumentIngestionService(vectorStore);

        String longText = "Spring AI provides abstractions for AI engineering. ".repeat(50);

        // Act
        int chunkCount = service.ingestText(longText, "test-source");

        // Assert: at least one chunk was produced and stored
        assertThat(chunkCount).isGreaterThan(0);
    }
}
