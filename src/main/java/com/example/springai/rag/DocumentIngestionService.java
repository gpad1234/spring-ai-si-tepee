package com.example.springai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pattern 4a — RAG Document Ingestion
 *
 * <p>Responsible for the <em>ingest</em> half of the RAG pipeline:
 * load → split → embed → store.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>Chunk size of ~800 tokens with 100-token overlap balances retrieval precision
 *       and context preservation. Tune per domain.</li>
 *   <li>Metadata (source, page, section) is attached at ingestion time — critical for
 *       citation in responses.</li>
 *   <li>{@link VectorStore} is injected; swap the implementation in {@code AiConfig}
 *       without touching this class.</li>
 * </ul>
 *
 * <p>See docs/patterns/04-rag.md for design rationale.
 */
@Service
public class DocumentIngestionService {

    private static final int CHUNK_SIZE   = 800;   // tokens
    private static final int CHUNK_OVERLAP = 100;  // tokens

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(CHUNK_OVERLAP)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
    }

    /**
     * Ingest a plain-text {@link Resource} into the vector store.
     *
     * @param resource the document resource (file, classpath, URL)
     * @param source   metadata label used for citations (e.g. "policy-2024.pdf")
     */
    public int ingest(Resource resource, String source) {
        var reader = new TextReader(resource);
        reader.getCustomMetadata().put("source", source);

        List<Document> docs = reader.get();
        List<Document> chunks = splitter.apply(docs);
        vectorStore.add(chunks);
        return chunks.size();
    }

    /**
     * Ingest raw text content directly (e.g. from a database or API).
     */
    public int ingestText(String content, String source) {
        var document = new Document(content, java.util.Map.of("source", source));
        List<Document> chunks = splitter.apply(List.of(document));
        vectorStore.add(chunks);
        return chunks.size();
    }
}
