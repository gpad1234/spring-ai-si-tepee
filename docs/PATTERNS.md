# Pattern 1 — Simple Chat

## When to Use

Single-turn Q&A, stateless interactions, no conversation history needed.

## Key Classes

| Class | Role |
|---|---|
| `ChatClient` | Fluent API — built once in `AiConfig`, injected everywhere |
| `PromptTemplate` | Loads `.st` files; substitutes `{variable}` placeholders |

## Request Flow

```
POST /api/chat
  → ChatController.chat()
    → ChatService.chat(message, systemPrompt)
      → ChatClient.prompt().user().call().content()
        → Claude API
```

## Decision Points

**Default vs per-request system prompt**
The default `ChatClient` bean carries a system prompt set in `AiConfig`. For per-request overrides, build a new client from `ChatClient.Builder` — do not mutate the shared bean.

**Prompt files vs inline strings**
Keep prompt text in `src/main/resources/prompts/*.st`. Inline strings are fine for one-liners; anything with instructions, rules, or multiple sentences belongs in a file.

## Code Reference

- `src/main/java/com/example/springai/chat/ChatService.java`
- `src/main/resources/prompts/chat.st`

---

# Pattern 2 — Streaming

## When to Use

Long responses where UX benefits from progressive rendering (chat UI, dashboards).

## Key Classes

| Class | Role |
|---|---|
| `ChatClient.stream()` | Returns `Flux<String>` of token chunks |
| Spring MVC SSE | Auto-handles `Flux` when return type is `Flux<T>` + `produces = TEXT_EVENT_STREAM_VALUE` |

## Decision Points

**Do NOT block on Flux**
Never call `.block()` in a controller. Spring MVC + virtual threads subscribe correctly when the method returns a `Flux`.

**Error handling in streams**
Use `.onErrorResume()` on the `Flux` to convert exceptions into a terminal error event — the client can distinguish "stream ended normally" from "stream failed".

## Code Reference

- `src/main/java/com/example/springai/streaming/StreamingService.java`

---

# Pattern 3 — Structured Output

## When to Use

Entity extraction, classification, form filling, any scenario needing a typed Java object from the model.

## Key Classes

| Class | Role |
|---|---|
| `BeanOutputConverter` | Injects JSON schema into prompt; parses response |
| `.call().entity(Class)` | Fluent shorthand — preferred over manual converter |

## Decision Points

**Record vs POJO**
Prefer Java records for immutability. All fields should be nullable unless the model is reliably consistent — extraction can be partial.

**Schema injection**
Spring AI appends the JSON schema automatically. Do **not** manually describe the JSON format in your prompt — it creates conflicts.

**Validation**
Run `@Validated` on the returned object if strict validation is needed. The model can hallucinate valid-looking but semantically wrong values.

## Code Reference

- `src/main/java/com/example/springai/structured/StructuredOutputService.java`

---

# Pattern 4 — RAG (Retrieval-Augmented Generation)

## When to Use

Answering questions from a private document corpus; reducing hallucination on domain knowledge.

## Pipeline

```
INGEST (one-time / batch):
  Resource → TextReader → TokenTextSplitter → EmbeddingModel → VectorStore

QUERY (per request):
  Question → QuestionAnswerAdvisor → VectorStore.similaritySearch()
           → Retrieved chunks injected into prompt → Claude → Answer
```

## Chunk Tuning

| Parameter | Default | Impact |
|---|---|---|
| Chunk size | 800 tokens | Larger = more context per chunk, less granular retrieval |
| Overlap | 100 tokens | Prevents context loss at chunk boundaries |
| Top-K | 5 | Number of chunks retrieved; higher = more context, higher cost |
| Similarity threshold | 0.7 | Lower = more recall, more noise |

## Vector Store Swap (production)

In `AiConfig.java`, replace `SimpleVectorStore` (in-memory, no persistence) with:

```java
// PostgreSQL + pgvector
@Bean
public VectorStore vectorStore(JdbcTemplate jdbc, EmbeddingModel em) {
    return new PgVectorStore(jdbc, em);
}
```

No changes needed in `DocumentIngestionService` or `RagService`.

## Decision Points

**Anti-hallucination system prompt**
The RAG service system prompt explicitly instructs the model to answer only from context and to say so when context is insufficient. Never omit this.

**Metadata for citations**
Always attach a `source` metadata field during ingestion — `QuestionAnswerAdvisor` makes it available in the prompt for citations.

## Code Reference

- `src/main/java/com/example/springai/rag/DocumentIngestionService.java`
- `src/main/java/com/example/springai/rag/RagService.java`
- `src/main/resources/prompts/rag.st`

---

# Pattern 5 — Tool Calling

## When to Use

The model needs real-time data (weather, prices), needs to take actions (send email, query DB), or must perform calculations it cannot reliably do in its head.

## Key Annotations

| Annotation | Where | Purpose |
|---|---|---|
| `@Tool` | Method | Declares a callable tool; `description` is the model's selection hint |
| `@ToolParam` | Parameter | Describes the parameter to the model |

## Decision Points

**Tool description quality**
The model uses `description` to decide which tool to call. Be specific about inputs, outputs, and when to use vs not use the tool.

**Tool registration scope**
Register tools per-call via `.tools(bean)`, not on the default `ChatClient` bean. This keeps tool access explicit and auditable.

**Automatic loop**
Spring AI handles the call loop automatically: model requests tool → Spring executes it → result sent back → model continues. No manual orchestration.

**Security**
Tools that perform write operations (DB inserts, external API calls) should include authorization checks. Never expose destructive tools without confirmation logic.

## Code Reference

- `src/main/java/com/example/springai/tools/BuiltInTools.java`
- `src/main/java/com/example/springai/tools/ToolCallingService.java`
