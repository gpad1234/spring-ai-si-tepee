package com.example.springai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResultMetadata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Zero-native-dependency EmbeddingModel for development and demos.
 *
 * <p>Produces a deterministic 384-dimensional float vector from any text string
 * using repeated SHA-256 hashing — no ONNX runtime, no DJL, no native libraries.
 *
 * <p>Semantic similarity is approximate (hash-based), which is sufficient for
 * demo RAG pipelines. For production, swap this bean for an API-backed model
 * (e.g. OpenAI text-embedding-3-small) or restore the transformers starter.
 */
public class HashEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 384;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++) {
            float[] vec = embed(request.getInstructions().get(i));
            embeddings.add(new Embedding(vec, i, EmbeddingResultMetadata.EMPTY));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] seed = text.getBytes(StandardCharsets.UTF_8);
            float[] vector = new float[DIMENSIONS];

            for (int block = 0; block < DIMENSIONS / 8; block++) {
                byte[] digest = md.digest(seed);
                for (int b = 0; b < 8 && (block * 8 + b) < DIMENSIONS; b++) {
                    vector[block * 8 + b] = digest[b] / 128.0f;
                }
                seed = digest; // chain: next block hashes the previous digest
            }

            // L2-normalise so cosine similarity works via dot-product
            float norm = 0f;
            for (float v : vector) norm += v * v;
            norm = (float) Math.sqrt(norm);
            if (norm > 0f) for (int i = 0; i < vector.length; i++) vector[i] /= norm;

            return vector;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
