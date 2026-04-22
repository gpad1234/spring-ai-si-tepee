# Architecture

## Layered Design

```
HTTP Client
    │
    ▼
Controller          (input validation, HTTP mapping)
    │
    ▼
Service             (AI orchestration, business rules)
    │
    ├──► ChatClient  (Spring AI abstraction — provider-agnostic)
    │        │
    │        ▼
    │    Anthropic Claude (or any other model)
    │
    └──► VectorStore (retrieval — swappable implementation)
```

Every layer has a single responsibility. Controllers never call `ChatClient` directly.

## Key Abstractions

| Abstraction | What it hides | Swap with |
|---|---|---|
| `ChatClient` | Model provider, auth, retry | Any Spring AI model starter |
| `VectorStore` | Embedding store | `PgVectorStore`, `ChromaVectorStore`, `RedisVectorStore` |
| `EmbeddingModel` | Embedding provider | `OpenAiEmbeddingModel`, `OllamaEmbeddingModel` |
| `.st` prompt files | Prompt text | Edit without recompiling |

## Concurrency Model

- Spring MVC with Java 21 virtual threads (`spring.threads.virtual.enabled=true`)
- Streaming endpoints return `Flux<String>` — Spring handles async subscription
- Never call `.block()` on a `Flux` in a controller method

## Configuration Hierarchy

```
application.properties          ← defaults
application-local.properties    ← local overrides (git-ignored)
Environment variables           ← production secrets (ANTHROPIC_API_KEY)
```

Secrets are **never** committed. Use `${ANTHROPIC_API_KEY}` in properties.

## Module Boundaries

```
com.example.springai/
├── config/        ← Bean wiring only (AiConfig)
├── chat/          ← Pattern 1: simple Q&A
├── streaming/     ← Pattern 2: SSE token streaming
├── structured/    ← Pattern 3: typed entity extraction
├── rag/           ← Pattern 4: retrieval-augmented generation
└── tools/         ← Pattern 5: function/tool calling
```

Each package is independently usable — copy a package into a new project as a starting point.

## Production Readiness Checklist

- [ ] Replace `SimpleVectorStore` with a persistent store (see [04-rag.md](patterns/04-rag.md))
- [ ] Add `spring.threads.virtual.enabled=true` for virtual thread support
- [ ] Configure `Micrometer` + tracing (Zipkin/OTLP) — Spring AI auto-instruments `ChatClient`
- [ ] Set `spring.ai.anthropic.chat.options.timeout` for production SLAs
- [ ] Add rate-limiting (Bucket4j or API Gateway) — Claude has token-per-minute limits
- [ ] Review and tighten `management.endpoints.web.exposure.include`
