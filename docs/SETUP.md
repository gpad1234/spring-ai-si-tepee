# Setup Guide

## Prerequisites

- Java 21+
- Maven 3.9+
- An Anthropic API key

## First Run

```bash
# 1. Set your API key (never hardcode it)
export ANTHROPIC_API_KEY=sk-ant-...

# 2. Build
mvn clean package -DskipTests

# 3. Run
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

## Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Simple chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is Spring AI?"}'

# Streaming
curl -N -X POST http://localhost:8080/api/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain RAG in 3 sentences"}'

# Entity extraction
curl -X POST http://localhost:8080/api/extract/person \
  -H "Content-Type: application/json" \
  -d '{"text": "Alice Smith, 32, from Berlin. Reach her at alice@example.com"}'

# RAG — ingest a document, then ask
curl -X POST http://localhost:8080/api/rag/ingest \
  -F "file=@/path/to/your/document.txt"

curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What does the document say about X?"}'

# Tool calling
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the weather in Tokyo today?"}'
```

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` | Yes | Anthropic API key |
| `SERVER_PORT` | No | HTTP port (default 8080) |

## Run Tests

```bash
# Unit tests only (no API key needed)
mvn test

# Integration tests (requires ANTHROPIC_API_KEY)
mvn test -Dgroups=integration
```

## Change the Model

In `application.properties`:

```properties
spring.ai.anthropic.chat.options.model=claude-3-5-sonnet-20241022
```

Available models: `claude-opus-4-5`, `claude-3-5-sonnet-20241022`, `claude-3-5-haiku-20241022`

## Switch Vector Store (Production)

See the Vector Store Swap section in [docs/PATTERNS.md](PATTERNS.md#vector-store-swap-production).
