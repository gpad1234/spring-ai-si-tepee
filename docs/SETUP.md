# Setup Guide

## Prerequisites

- Java 21+
- Maven 3.9+
- An Anthropic API key

## First Run

### Backend (Spring Boot)

```bash
# 1. Set your API key (never hardcode it)
export ANTHROPIC_API_KEY=sk-ant-...

# 2. Build
mvn clean package -DskipTests

# 3. Run
mvn spring-boot:run
```

Backend starts on `http://localhost:8080`.

### Frontend (Next.js)

Requires Node 18+. Run in a separate terminal:

```bash
cd ui
npm install      # first time only
npm run dev
```

UI starts on `http://localhost:3000`. All `/api/*` requests are proxied to the backend automatically.

> Both servers must be running to use the UI. Start the backend first.

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

# Entity extraction — person
curl -X POST http://localhost:8080/api/extract/person \
  -H "Content-Type: application/json" \
  -d '{"text": "Alice Smith, 32, from Berlin. Reach her at alice@example.com"}'

# Entity extraction — sentiment
curl -X POST http://localhost:8080/api/extract/sentiment \
  -H "Content-Type: application/json" \
  -d '{"text": "This framework is fantastic. Saved us weeks of work."}'

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

## MCP Apps Preview (Pattern 6)

Use this when you want to run the MCP server and rich UI resource.

### Recommended Runtime (Boot 4 Preview Shim)

Use the dedicated runtime module for MCP transport compatibility:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
mvn -f mcp-preview-runtime/pom.xml spring-boot:run -Dspring-boot.run.arguments="--server.port=3001"
```

MCP endpoint: `http://localhost:3001/mcp`

### Start MCP Preview Server

This profile is still useful for source-level validation in the main project:

Run with the MCP preview profile and override the port to 3001:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run -Pmcp-apps-preview -Dspring-boot.run.arguments="--server.port=3001"
```

MCP endpoint: `http://localhost:3001/mcp`

### Current Limitation (Important)

At the moment, this repository uses Spring Boot 3.3 on the mainline stack.
The MCP preview profile uses Spring AI MCP starter 2.0.0-M3, which expects newer
Spring Web APIs than the runtime provided by Boot 3.3.

Observed runtime symptoms:
- `NoSuchMethodError: HttpHeaders.asMultiValueMap()`
- `NoSuchMethodError: HttpHeaders.headerNames()`

Impact:
- The app can start and register MCP tools/resources.
- Requests to `/mcp` return HTTP 500 in this mixed-version state.

Workaround for now:
- Use `mvn test -Pmcp-apps-preview` to validate MCP wiring, contracts, and preview tests in the main module.
- Use `mcp-preview-runtime/pom.xml` (Boot 4) for live MCP host/manual endpoint testing.

Resolution path:
- Keep mainline on Boot 3.3 for current app stability and run MCP host tests via the dedicated Boot 4 preview runtime module until a full-stack upgrade is scheduled.

### Validate MCP Preview Profile

```bash
mvn test -Pmcp-apps-preview
```

### MCP Jam Configuration

- Transport: Streamable HTTP
- URL: `http://localhost:3001/mcp`

### Claude Desktop (Proxy via mcp-remote)

Claude Desktop may require proxying Streamable HTTP via STDIO:

```json
{
  "mcpServers": {
    "spring-ai-mcp-preview": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://localhost:3001/mcp"
      ]
    }
  }
}
```

### Quick MCP App Smoke Check

1. Ask the host assistant to run: `open-rich-chat`
2. Confirm the "Rich Chat Workspace" UI appears.
3. Click "Share to Chat Context".
4. Invoke tool `rich-chat-respond` with:
  - `message`: your prompt (for example, "Summarize the latest draft")
  - `systemPrompt`: optional override (for example, "Be concise")
5. Confirm the reply is generated by the backend `ChatService` and reflects the latest draft/context.

### Host Tool Invocation Examples

Use these argument payloads when manually invoking `rich-chat-respond` from an MCP host tool panel.

Example 1 (with system prompt):

```json
{
  "message": "Summarize the current draft in 3 bullets.",
  "systemPrompt": "Be concise and action-oriented."
}
```

Example 2 (no system prompt override):

```json
{
  "message": "Turn the latest draft into a short status update for engineering leadership."
}
```

Expected behavior:
- The tool returns a text response from backend `ChatService`.
- The response can incorporate context previously pushed from the MCP app UI via "Share to Chat Context".
