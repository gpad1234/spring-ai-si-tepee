# Spring AI Reference Architecture

Gold-standard Spring AI project with **Anthropic Claude**, **Spring Boot 3.3**, **Java 21**, **Maven**.
Covers five reusable patterns with production-ready implementation and documentation.

## Quick Start

```bash
export ANTHROPIC_API_KEY=sk-ant-...
mvn clean package -DskipTests
mvn spring-boot:run
```

See [docs/SETUP.md](docs/SETUP.md) for full setup, curl examples, and model switching.

## Project Structure

```
src/main/java/com/example/springai/
├── config/       AiConfig.java         — ChatClient + VectorStore beans
├── chat/         Pattern 1: simple Q&A
├── streaming/    Pattern 2: SSE token streaming
├── structured/   Pattern 3: typed entity extraction
├── rag/          Pattern 4: retrieval-augmented generation
└── tools/        Pattern 5: function/tool calling

src/main/resources/
├── application.properties
└── prompts/      *.st template files (chat.st, rag.st, extraction.st)

docs/
├── ARCHITECTURE.md   layered design, abstractions, production checklist
├── PATTERNS.md       decision guide for all 5 patterns
└── SETUP.md          first-run, curl examples, env vars
```

## Architecture Decisions

- Controllers never call `ChatClient` directly — always go through a Service
- `VectorStore` bean in `AiConfig` is the single swap point for the storage backend
- Prompt text lives in `src/main/resources/prompts/*.st` — never inline long strings
- Each `src/main/java/.../` package is self-contained and can be copied to a new project

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full layered diagram.

## Pattern Quick Reference

| Pattern | Package | Endpoint | Key API |
|---|---|---|---|
| Simple Chat | `chat/` | `POST /api/chat` | `ChatClient.prompt().call().content()` |
| Streaming | `streaming/` | `POST /api/stream` (SSE) | `ChatClient.prompt().stream().content()` |
| Structured Output | `structured/` | `POST /api/extract/*` | `.call().entity(Record.class)` |
| RAG | `rag/` | `POST /api/rag/ask` | `QuestionAnswerAdvisor` |
| Tool Calling | `tools/` | `POST /api/tools/chat` | `@Tool`, `.tools(bean)` |

See [docs/PATTERNS.md](docs/PATTERNS.md) for when-to-use, decision points, and tuning guidance.

## Key Conventions

- **Never hardcode API keys** — use `${ANTHROPIC_API_KEY}` in `application.properties`
- **Prompt files over inline strings** — edit prompts without recompiling
- **`ChatClient` over `ChatModel`** — higher-level, advisor-capable, preferred
- **`Flux` ownership** — never call `.block()` on a `Flux` in a controller; return it
- **Swap vector stores via `AiConfig`** — `SimpleVectorStore` (dev) → `PgVectorStore` (prod)
- **Tool descriptions drive selection** — write them like documentation, not code comments

## Dependencies

Spring AI BOM version: `${spring-ai.version}` (see `pom.xml`).
Anthropic starter: `spring-ai-anthropic-spring-boot-starter`.
Spring AI milestones/snapshots require the `spring-milestones` repository — already configured in `pom.xml`.

## Testing

```bash
mvn test                          # unit tests (no API key needed)
mvn test -Dgroups=integration     # integration tests (requires ANTHROPIC_API_KEY)
```

Unit tests mock `ChatModel` via `spring-ai-test`. See `src/test/` for examples.
