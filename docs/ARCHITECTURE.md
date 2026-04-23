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

## Spring AI 1.0.0 Migration: Problems & Fixes

Issues encountered migrating to Spring AI 1.0.0 and how each was resolved.

| Problem | Root Cause | Fix Applied |
|---|---|---|
| `spring-ai-anthropic-spring-boot-starter` not found | Spring AI 1.0.0 renamed all starters | Changed to `spring-ai-starter-model-anthropic` in `pom.xml` |
| `spring-ai-vector-store` not on classpath | No longer transitively included in 1.0.0 | Added `spring-ai-vector-store` as an explicit dependency |
| Native `libtokenizers.dylib` crash at startup | DJL/ONNX transformer starter requires platform-native library not bundled for `osx-x86_64` | Removed transformer starter entirely; replaced with `HashEmbeddingModel` — pure Java, zero native deps |
| `new SimpleVectorStore(embeddingModel)` compile error | Constructor removed in 1.0.0; builder pattern required | Updated to `SimpleVectorStore.builder(embeddingModel).build()` |
| `SearchRequest.defaults().withTopK()` compile error | Fluent setter API replaced by builder pattern | Updated to `SearchRequest.builder().query(q).topK(5).build()` |
| `QuestionAnswerAdvisor` not found | Moved to `spring-ai-advisors-vector-store`; not available locally for 1.0.0 | Rewrote `RagService` to do manual retrieval (`vectorStore.similaritySearch()`) + context injection via `.system()` |
| `ChatModel.builder()` compile error in tests | Static builder factory does not exist; `ChatModel` is an interface | Replaced with `mock(ChatModel.class)` via Mockito |
| `target/` (75 MB jar) committed to git | No `.gitignore` existed at project root | Created `.gitignore`, ran `git rm -r --cached target/` |

### HashEmbeddingModel — Zero-Native Embedding

`config/HashEmbeddingModel.java` solves a critical build problem by providing a zero-dependency embedding model designed specifically for development, demos, and testing.

#### Why It Exists

Semantic embeddings normally require:
- **ONNX runtime** (300+ MB) — loads platform-specific native libraries at startup
- **DJL or Transformers starter** — adds ~50 transitive dependencies, increases cold start to 30+ seconds
- **API calls** — OpenAI, Cohere, etc. require network and API keys

Traditional approaches caused:
- Native library crashes (`libtokenizers.dylib` not found on `osx-x86_64`)
- 10x longer cold start during development
- API costs and network latency in integration tests
- Bloated `target/` artifact size

#### How It Works

Generates deterministic **384-dimensional float vectors** using chained SHA-256 hashing:

```java
// For text "hello world":
Block 0: SHA-256("hello world") → 8 floats
Block 1: SHA-256(digest₀)      → 8 floats
Block 2: SHA-256(digest₁)      → 8 floats
...repeat 48 times...
Result:  384-dim L2-normalized vector
```

Key properties:
- **Deterministic** — same text always produces the same vector
- **L2-normalized** — cosine similarity works via dot-product
- **Semantically approximate** — words with similar letters hash similarly
- **Pure Java** — no downloads, no native libraries, no ONNX runtime
- **Fast** — cold start under 5 seconds vs. 30+ seconds with transformers

#### When to Use

| Context | Use HashEmbeddingModel | Use Real Embedding Model |
|---|---|---|
| **Local dev** | ✅ | — |
| **CI/unit tests** | ✅ | — |
| **Docker build** | ✅ | — |
| **Demo RAG pipeline** | ✅ | — |
| **Production retrieval** | ❌ | ✅ |
| **Real semantic search** | ❌ | ✅ |

#### Swap for Production

In `config/AiConfig.java`:

```java
// Remove or rename the HashEmbeddingModel bean.
// Add one of:

@Bean
public EmbeddingModel embeddingModel() {
    // Option 1: OpenAI text-embedding-3-small
    return new OpenAiEmbeddingModel(openAiApi);
}

@Bean
public EmbeddingModel embeddingModel() {
    // Option 2: Local Ollama (semantic + offline)
    return new OllamaEmbeddingModel(client, "nomic-embed-text:latest");
}

@Bean
public EmbeddingModel embeddingModel() {
    // Option 3: Anthropic (paired with Claude)
    return new AnthropicEmbeddingModel(client);
}
```

Update `application.properties`:

```properties
spring.ai.vectorstore.type=pgvector    # swap SimpleVectorStore for persistent store
spring.ai.embedding.model=text-embedding-3-small
```

The `VectorStore` and `RagService` implementations remain unchanged — they depend only on the `EmbeddingModel` abstraction.

## Production Readiness Checklist

- [ ] Replace `SimpleVectorStore` with a persistent store (see [04-rag.md](patterns/04-rag.md))
- [ ] Add `spring.threads.virtual.enabled=true` for virtual thread support
- [ ] Configure `Micrometer` + tracing (Zipkin/OTLP) — Spring AI auto-instruments `ChatClient`
- [ ] Set `spring.ai.anthropic.chat.options.timeout` for production SLAs
- [ ] Add rate-limiting (Bucket4j or API Gateway) — Claude has token-per-minute limits
- [ ] Review and tighten `management.endpoints.web.exposure.include`
