# Product Design — Spring AI Reference Architecture

> A production-ready, self-contained reference implementation that demonstrates five core
> Spring AI integration patterns on top of Anthropic Claude, Spring Boot 3.3, and Java 21.

---

## 1. Product Vision

Developers building AI-powered Java services repeatedly solve the same problems: wiring a
language model into a Spring application, streaming tokens to a browser, extracting structured
data from free text, grounding answers in private documents, and extending the model with
custom tools. Each of these problems is solved once here, correctly, with clear seam points so
that any pattern can be lifted and dropped into a new project.

The reference architecture is not a framework — it is working code with documented intent.

---

## 2. Users

| Persona | Need |
|---|---|
| Spring developer new to AI | A working starting point with no boilerplate to write |
| Architect evaluating Spring AI | Verified patterns and production checklists |
| Team building a product feature | A self-contained package they can copy and adapt |

---

## 3. Feature Catalogue

### 3.1 Pattern 1 — Simple Chat (Stateless Q&A)

**What it does**
Accepts a user message, optionally a system prompt, and returns a complete text response from
Claude. Stateless — no conversation history is maintained between calls.

**Endpoint**
`POST /api/chat`  
Body: `{ "message": "...", "systemPrompt": "..." }`  
Response: `{ "response": "..." }`

**Design decisions**
- The shared `ChatClient` bean is never mutated. Per-request system prompt overrides are
  applied by building a new `ChatClient` from `ChatClient.Builder`.
- Prompt text lives in `src/main/resources/prompts/chat.st` so it can be edited without
  recompiling.

**Underlying tech**
`ChatClient` → `AnthropicChatModel` → Claude (configurable via `application.properties`).

---

### 3.2 Pattern 2 — Streaming (SSE Token Delivery)

**What it does**
Streams the model response as a sequence of Server-Sent Events. Each event carries one or
more tokens. The client can render text progressively without waiting for the full response.

**Endpoint**
`POST /api/stream`  
Body: `{ "message": "..." }`  
Response: `text/event-stream` — one `data:` line per token chunk.

**Design decisions**
- The controller returns `Flux<String>` directly. Spring MVC + virtual threads subscribe
  correctly without the need for WebFlux.
- `.onErrorResume()` converts any model exception into a terminal error event so the client
  can distinguish normal end-of-stream from a failure.
- Never calls `.block()` on the `Flux`.

**Underlying tech**
`ChatClient.prompt().stream().content()` → `Flux<String>` → Spring MVC SSE.

---

### 3.3 Pattern 3 — Structured Output (Typed Entity Extraction)

**What it does**
Submits free-form text and extracts a typed Java record from the model response. Used for
entity extraction (person, address, invoice), classification, and form-filling from
unstructured input.

**Endpoints**
`POST /api/extract/person` — extracts a `PersonRecord` (name, age, email, city)  
`POST /api/extract/product` — extracts a `ProductRecord` (name, price, category, description)

**Design decisions**
- Spring AI's `BeanOutputConverter` automatically appends the JSON schema to the prompt.
  The prompt must not re-describe the expected JSON — it creates conflicts.
- Java records are preferred over POJOs for immutability.
- All extracted fields are nullable; the model may return partial results.
- `@Validated` can be applied to the returned record for strict downstream validation.

**Underlying tech**
`.call().entity(RecordClass.class)` → `BeanOutputConverter` → JSON schema injection →
Claude → deserialised Java record.

---

### 3.4 Pattern 4 — RAG (Retrieval-Augmented Generation)

**What it does**
Ingests documents into a vector store, then answers questions by retrieving the most relevant
chunks and grounding Claude's response in them. Reduces hallucination on domain-specific or
private knowledge.

**Endpoints**
`POST /api/rag/ingest` — uploads a file (PDF, TXT, Markdown) and stores its chunks  
`POST /api/rag/ask` — answers a question using retrieved context

**Ingestion pipeline**
```
File upload
  → DocumentReader (TikaDocumentReader / TextReader)
    → TokenTextSplitter (chunk size + overlap configurable)
      → EmbeddingModel (local transformer or API-based)
        → VectorStore (SimpleVectorStore in dev, PgVector/Chroma in prod)
```

**Query pipeline**
```
Question
  → EmbeddingModel → similarity search → top-k chunks
    → QuestionAnswerAdvisor (injects chunks into prompt)
      → Claude
        → grounded answer
```

**Design decisions**
- `VectorStore` is the single swap point — change one bean in `AiConfig` to switch storage
  backends without touching service code.
- `SimpleVectorStore` is used for development. It is in-memory and not persistent across
  restarts.
- Chunk size and overlap are tuned in `application.properties`, not in code.
- The retrieval advisor is `QuestionAnswerAdvisor` from Spring AI — no manual prompt
  assembly needed.

**Underlying tech**
`spring-ai-transformers-spring-boot-starter` (local embeddings) ·
`spring-ai-pdf-document-reader` · `SimpleVectorStore` · `QuestionAnswerAdvisor`.

---

### 3.5 Pattern 5 — Tool Calling (Function Invocation)

**What it does**
Allows the model to invoke registered Java methods at runtime. Claude decides when a tool is
needed, calls it with structured arguments, and incorporates the result into its response.
Examples: weather lookup, database query, calculator, calendar check.

**Endpoint**
`POST /api/tools/chat`  
Body: `{ "message": "..." }`  
Response: `{ "response": "..." }` — model response after zero or more tool invocations.

**Registered tools (built-in examples)**
| Tool | Description |
|---|---|
| `getCurrentWeather` | Returns mock weather for a given city |
| `getCurrentDateTime` | Returns current date and time |
| `calculateExpression` | Evaluates a mathematical expression |

**Design decisions**
- Tools are plain Spring beans annotated with `@Tool` (Spring AI annotation on methods).
  The model selects them by reading their descriptions — descriptions must be written as
  documentation, not code comments.
- Tool registration is done once via `.tools(toolBean)` on the `ChatClient` call — not
  globally on the bean, keeping each endpoint's tool surface explicit.
- The model may call multiple tools in sequence within one user turn (agentic loop). Spring
  AI handles this automatically.

**Underlying tech**
`@Tool` annotation · `ChatClient.prompt().tools(bean).call()` · Anthropic tool-use API.

---

## 4. Cross-Cutting Concerns

### 4.1 Configuration Management

```
application.properties          ← checked-in defaults
application-local.properties    ← local overrides (git-ignored)
Environment variables           ← production secrets
```

No secrets are ever committed. API keys are referenced as `${ANTHROPIC_API_KEY}`.

### 4.2 Concurrency

Java 21 virtual threads are enabled via `spring.threads.virtual.enabled=true`. Each HTTP
request runs on a virtual thread. Blocking calls inside services (embedding generation,
Claude API calls) are safe because virtual threads park rather than block OS threads.

Streaming endpoints return `Flux<String>` and are handled by Spring MVC's reactive
support — no WebFlux dependency is required.

### 4.3 Observability

Spring AI auto-instruments `ChatClient` calls with Micrometer. Adding a Micrometer registry
(Prometheus, OTLP) and a tracing backend (Zipkin, Jaeger) requires no code changes — only
configuration.

Spring Boot Actuator is enabled for `/actuator/health`, `/actuator/info`, and metrics
endpoints.

### 4.4 Security

- API keys are environment variables only — never in source code or git history.
- No user-supplied data is interpolated unsanitised into system prompts.
- Production deployments should sit behind an API gateway or load balancer that handles
  TLS termination, authentication, and rate limiting.
- Rate limiting at the application layer can be added with Bucket4j; Claude also enforces
  token-per-minute limits at the provider level.

### 4.5 Testing Strategy

| Layer | Approach | Key tool |
|---|---|---|
| Service unit tests | Mock `ChatModel` | `spring-ai-test` / `Mockito` |
| Integration tests | Real API calls, tagged `integration` | JUnit 5 tag filter |
| Contract tests | Verify JSON schema of structured output | AssertJ |

Unit tests require no API key. Integration tests are gated on `ANTHROPIC_API_KEY` and run
with `mvn test -Dgroups=integration`.

---

## 5. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.3.0 |
| AI abstraction | Spring AI | 1.0.0 |
| AI model provider | Anthropic Claude | claude-3-5-sonnet (configurable) |
| Embedding model | ONNX local transformer | via `spring-ai-transformers` |
| Vector store (dev) | `SimpleVectorStore` | in-memory |
| Vector store (prod) | `PgVectorStore` or `ChromaVectorStore` | swap in `AiConfig` |
| Document parsing | TikaDocumentReader, PDF reader | Spring AI |
| Reactive streams | Project Reactor (`Flux`) | via Spring Boot |
| Concurrency | Java 21 virtual threads | via `spring.threads.virtual.enabled` |
| Build | Maven | 3.9+ |
| Testing | JUnit 5, Mockito, spring-ai-test | Spring Boot Test |
| Observability | Micrometer, Spring Boot Actuator | via Spring Boot |
| Prompt templates | StringTemplate 4 (`.st` files) | via Spring AI |

---

## 6. Data Flow Diagram

```
                        ┌───────────────────────────────────────┐
                        │         HTTP Client (curl / UI)       │
                        └──────────────┬────────────────────────┘
                                       │ REST
                        ┌──────────────▼────────────────────────┐
                        │            Controller                  │
                        │  (input validation, HTTP mapping)      │
                        └──────────────┬────────────────────────┘
                                       │
                        ┌──────────────▼────────────────────────┐
                        │             Service                    │
                        │  (AI orchestration, business rules)    │
                        └────────┬──────────────┬───────────────┘
                                 │              │
               ┌─────────────────▼──┐    ┌──────▼─────────────────┐
               │     ChatClient     │    │      VectorStore        │
               │ (Spring AI façade) │    │  (SimpleVectorStore /   │
               └─────────┬──────────┘    │   PgVector / Chroma)    │
                         │              └─────────────────────────┘
               ┌─────────▼──────────┐
               │  AnthropicChatModel│
               └─────────┬──────────┘
                         │ HTTPS
               ┌─────────▼──────────┐
               │  Anthropic Claude  │
               │   (API endpoint)   │
               └────────────────────┘
```

---

## 7. Extension Points

| What to change | Where to change it |
|---|---|
| Switch AI provider (OpenAI, Ollama) | Swap starter in `pom.xml`; update `AiConfig` |
| Switch vector store | Replace `SimpleVectorStore` bean in `AiConfig` |
| Add a new tool | Add `@Tool`-annotated method to `BuiltInTools`; register it in the service |
| Change a prompt | Edit the `.st` file in `src/main/resources/prompts/` |
| Add conversation memory | Use `MessageWindowChatMemory` advisor in `ChatClient` |
| Add multi-modal input | Pass `Media` objects into the `user()` block |

---

*Last updated: April 2026*
