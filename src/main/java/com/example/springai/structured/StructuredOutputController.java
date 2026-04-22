package com.example.springai.structured;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Pattern 3 — Structured Output
 *
 * <p>Instructs the model to return JSON that maps directly to a Java record.
 * Spring AI uses {@link org.springframework.ai.converter.BeanOutputConverter} to
 * inject the JSON schema into the prompt and parse the response.
 *
 * <p>See docs/patterns/03-structured-output.md for design rationale.
 */
@RestController
@RequestMapping("/api/extract")
public class StructuredOutputController {

    private final StructuredOutputService structuredOutputService;

    public StructuredOutputController(StructuredOutputService structuredOutputService) {
        this.structuredOutputService = structuredOutputService;
    }

    /**
     * POST /api/extract/person
     * Extracts a {@link PersonInfo} entity from free text.
     */
    @PostMapping("/person")
    public ResponseEntity<StructuredOutputService.PersonInfo> extractPerson(
            @Valid @RequestBody ExtractionRequest request) {
        return ResponseEntity.ok(structuredOutputService.extractPerson(request.text()));
    }

    /**
     * POST /api/extract/sentiment
     * Returns a sentiment analysis as a structured object.
     */
    @PostMapping("/sentiment")
    public ResponseEntity<StructuredOutputService.SentimentResult> analyzeSentiment(
            @Valid @RequestBody ExtractionRequest request) {
        return ResponseEntity.ok(structuredOutputService.analyzeSentiment(request.text()));
    }

    public record ExtractionRequest(@NotBlank String text) {}
}
