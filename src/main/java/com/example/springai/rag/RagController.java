package com.example.springai.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Pattern 4c — RAG REST Controller
 *
 * <p>See docs/patterns/04-rag.md for design rationale.
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;
    private final DocumentIngestionService ingestionService;

    public RagController(RagService ragService, DocumentIngestionService ingestionService) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
    }

    /**
     * POST /api/rag/ingest
     * Upload a plain-text document to the vector store.
     */
    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(
            @RequestParam("file") MultipartFile file) throws IOException {
        var resource = new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        };
        ingestionService.ingest(resource, file.getOriginalFilename());
        return ResponseEntity.accepted().build();
    }

    /**
     * POST /api/rag/ask
     * Ask a question grounded in ingested documents.
     */
    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        String answer = ragService.answer(request.question());
        return ResponseEntity.ok(new AskResponse(answer));
    }

    public record AskRequest(@NotBlank String question) {}
    public record AskResponse(String answer) {}
}
