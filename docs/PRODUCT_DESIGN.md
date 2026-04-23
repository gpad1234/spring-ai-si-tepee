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
| **UI framework** | **Next.js 15 (App Router)** | **React 19, TypeScript 5** |
| **UI styling** | **Tailwind CSS v4 + shadcn/ui** | **Radix UI primitives** |
| **UI animation** | **Framer Motion 11** | **Token stream, transitions** |
| **UI data fetching** | **TanStack Query 5** | **Async state management** |

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
| Change UI theme | Edit `ui/tailwind.config.ts` — all colours are CSS variables |
| Point UI at a different API | Set `NEXT_PUBLIC_API_BASE_URL` in `ui/.env.local` |

---

## 8. User Interface

### 8.1 Design Language — "Neon Garden"

The UI is dark-first with vivid accent colours that make AI activity feel alive. Key
principles:

- **Dark canvas** `#0a0a0f` — near-black with a cool blue undertone.
- **Violet → indigo gradient** `#7c3aed → #4f46e5` — primary brand colour, used on
  buttons, active nav items, and streaming cursors.
- **Cyan spark** `#06b6d4` — secondary accent for tool invocation badges and highlights.
- **Glassmorphism surfaces** — cards use `backdrop-blur-md` + `bg-white/5` so the dark
  background bleeds through slightly.
- **Amber pulse** `#f59e0b` — animated blinking cursor while tokens stream in.
- **Rose** `#f43f5e` — errors and destructive actions only.
- **Typography** — Inter (body) + JetBrains Mono (code blocks, JSON output, tool args).
- **Motion** — subtle: 150 ms ease-out transitions; token stream character-by-character
  reveal via Framer Motion; page transitions are slide-in from the right.

### 8.2 Application Shell

```
┌─────────────────────────────────────────────────────────────┐
│  ◈ Spring AI  [Chat] [Stream] [Extract] [RAG] [Tools]  ···  │  ← Top nav bar
├──────────────┬──────────────────────────────────────────────┤
│              │                                              │
│  Sidebar     │            Main content area                 │
│  (pattern    │            (page-specific)                   │
│   switcher   │                                              │
│   + history) │                                              │
│              │                                              │
└──────────────┴──────────────────────────────────────────────┘
```

The sidebar collapses to icon-only on viewports narrower than 1024 px. Each nav item
glows with the violet accent when active. History (last 20 inputs) is stored in
`localStorage` — no backend session required.

### 8.3 Views

#### View 1 — Chat (`/chat`)

Full-screen conversational UI. Matches the mental model users have from consumer chat apps.

- Message bubbles: user messages right-aligned (violet background), AI messages
  left-aligned (glass card).
- Markdown rendered inside AI bubbles (headings, bold, code blocks, lists).
- Typing indicator (three bouncing dots, cyan) while waiting for the first token.
- Input bar pinned to bottom with a send button and an optional system-prompt drawer
  (collapsed by default, expand with a chevron).
- **Pattern used:** `POST /api/chat`

#### View 2 — Stream (`/stream`)

Identical layout to Chat but every token appears individually as it arrives over SSE.
A blinking amber cursor sits at the end of the in-progress response. Once the stream
ends the cursor fades out.

- Uses the browser's native `EventSource` API — no polling, no long-polling.
- A subtle left-border glow (violet) pulses on the AI bubble while the stream is open.
- **Pattern used:** `POST /api/stream` (`text/event-stream`)

#### View 3 — Extract (`/extract`)

Split-pane layout:

```
┌──────────────────────┬──────────────────────────────────────┐
│  Free text input     │  Extracted entity card               │
│                      │                                      │
│  [Person] [Product]  │  { name: "Alice Smith"               │
│  toggle tabs         │    age: 32                           │
│                      │    email: "alice@example.com"        │
│  [Text area]         │    city: "Berlin" }                  │
│                      │                                      │
│  [Extract ▶]         │  Field badges: green = filled        │
│                      │             grey  = null/missing     │
└──────────────────────┴──────────────────────────────────────┘
```

- Entity type is selected via pill tabs (Person / Product). More tabs can be added
  as new records are implemented on the backend.
- The right panel renders the JSON as a styled card with coloured field badges —
  green for extracted values, grey for nulls — so partial extraction is immediately
  visible.
- A copy-to-clipboard button (top-right of the card) copies the raw JSON.
- **Pattern used:** `POST /api/extract/person`, `POST /api/extract/product`

#### View 4 — RAG (`/rag`)

Three-column layout:

```
┌───────────────┬────────────────────────┬───────────────────┐
│  Drop zone    │  Document list         │  Q&A chat         │
│               │                        │                   │
│  Drag a PDF,  │  ● report.pdf   ✓      │  [Question input] │
│  TXT, or MD   │  ● notes.txt    ✓      │  [Ask ▶]          │
│  here, or     │  ● spec.md      ✓      │                   │
│  [Browse]     │                        │  Answer with      │
│               │  [Clear all]           │  source chunks    │
│               │                        │  highlighted      │
└───────────────┴────────────────────────┴───────────────────┘
```

- Drop zone accepts PDF, TXT, and Markdown. A progress ring shows ingestion status.
- The document list shows file name, size, and a green tick once ingestion completes.
  Ingested documents persist in `localStorage` metadata (names only — actual data is
  in the vector store).
- The answer panel optionally shows the retrieved source chunks below the answer,
  collapsed by default, expandable.
- **Pattern used:** `POST /api/rag/ingest`, `POST /api/rag/ask`

#### View 5 — Tools (`/tools`)

Chat interface with a collapsible side panel that shows every tool invocation in the
current turn:

```
┌────────────────────────────────┬────────────────────────────┐
│  Chat (same as View 1)         │  Tool calls this turn      │
│                                │                            │
│  User: "What's the weather     │  ⚡ getCurrentWeather       │
│         in Tokyo?"             │     args: { city: Tokyo }  │
│                                │     result: 22°C, sunny    │
│  AI: "It's 22°C and sunny in   │                            │
│       Tokyo today."            │  ⚡ getCurrentDateTime      │
│                                │     args: {}               │
│  [Input bar]                   │     result: 2026-04-22...  │
└────────────────────────────────┴────────────────────────────┘
```

- Each tool invocation renders as a cyan badge in the side panel with the tool name,
  serialised args, and the raw return value.
- The panel is empty when no tools have been called and shows a subtle dashed border.
- **Pattern used:** `POST /api/tools/chat`

### 8.4 Tech Stack (UI)

| Technology | Version | Role |
|---|---|---|
| Next.js | 15 (App Router) | Framework — routing, SSR, API proxy |
| React | 19 | UI rendering |
| TypeScript | 5 | Type safety |
| Tailwind CSS | v4 | Utility-first styling, design tokens |
| shadcn/ui | latest | Accessible component primitives (Radix UI) |
| Framer Motion | 11 | Token stream animation, transitions |
| TanStack Query | 5 | Async state, mutation loading states |
| `EventSource` | browser native | SSE streaming (View 2) |
| React Markdown | 9 | Markdown rendering in chat bubbles |
| Inter | via `next/font` | Body typeface |
| JetBrains Mono | via `next/font` | Monospace / code blocks |

### 8.5 Project Structure (UI)

```
ui/
├── app/
│   ├── layout.tsx            global shell: top nav, sidebar, theme provider
│   ├── page.tsx              redirect → /chat
│   ├── chat/page.tsx
│   ├── stream/page.tsx
│   ├── extract/page.tsx
│   ├── rag/page.tsx
│   └── tools/page.tsx
├── components/
│   ├── layout/
│   │   ├── TopNav.tsx
│   │   └── Sidebar.tsx
│   ├── chat/
│   │   ├── MessageBubble.tsx
│   │   ├── InputBar.tsx
│   │   └── TypingIndicator.tsx
│   ├── stream/
│   │   └── StreamPane.tsx    uses EventSource + useStreamTokens hook
│   ├── extract/
│   │   ├── ExtractForm.tsx
│   │   └── JsonCard.tsx
│   ├── rag/
│   │   ├── DropZone.tsx
│   │   ├── DocumentList.tsx
│   │   └── RagChat.tsx
│   └── tools/
│       ├── ToolChat.tsx
│       └── ToolCallBadge.tsx
├── hooks/
│   ├── useChat.ts            wraps POST /api/chat
│   ├── useStream.ts          wraps EventSource → POST /api/stream
│   ├── useExtract.ts         wraps POST /api/extract/*
│   ├── useRag.ts             wraps ingest + ask
│   └── useTools.ts           wraps POST /api/tools/chat
├── lib/
│   ├── api.ts                typed fetch wrappers for all 5 backend endpoints
│   └── cn.ts                 clsx + tailwind-merge utility
├── public/
└── tailwind.config.ts        design tokens (colours, fonts, animation)
```

### 8.6 Backend Integration

The UI runs on `http://localhost:3000` in development. The Spring Boot API runs on
`http://localhost:8080`. Two options for connecting them:

**Option A — Next.js API proxy (recommended for dev)**  
Add a `rewrites()` rule in `next.config.ts`:
```ts
async rewrites() {
  return [{ source: '/api/:path*', destination: 'http://localhost:8080/api/:path*' }]
}
```
The browser never sees cross-origin requests. No CORS configuration needed on the backend.

**Option B — CORS on the backend (recommended for prod separation)**  
Add a `CorsConfigurationSource` bean in `AiConfig` allowing the UI origin. Set
`NEXT_PUBLIC_API_BASE_URL=https://api.yourdomain.com` in the production environment.

---

## 9. MCP Apps Design Blueprint (Phase 0: Design Only)

### 9.1 Goal

Blend chat with an embedded, rich interaction surface using MCP Apps, while preserving the current HTTP API patterns and the existing Next.js web UI.

Primary outcome:
- Add a sixth reusable pattern for MCP Apps without breaking Patterns 1-5.

### 9.2 Non-Goals

- No runtime migration in this phase.
- No forced replacement of the existing Next.js UI.
- No protocol-level compatibility hacks beyond documented host constraints.

### 9.3 Version and Compatibility Constraints

Current repository baseline:
- Spring AI: 1.0.0
- Spring Boot: 3.3.0

MCP Apps requirement from Spring guidance:
- Spring AI 2.0.0-M3 or newer for MCP metadata support on tools and resources.

Design implication:
- MCP Apps are a parallel capability that should be introduced behind a feature branch and validated in isolation before merging.

### 9.4 Experience Model

The user can ask for a workflow in chat (for example, "open rich chat workspace"), and the assistant opens an embedded app panel that supports direct interaction.

Hybrid interaction loop:
1. User asks in chat.
2. Assistant invokes MCP tool.
3. Host renders MCP app resource.
4. User interacts in UI.
5. UI updates model context with structured state.
6. Assistant answers with awareness of latest UI state.

### 9.5 Architecture (Target)

```
MCP Host (Claude Desktop / MCP Jam)
    |
    | streamable HTTP
    v
Spring Boot MCP Server
    |- @McpTool method: open-rich-chat
    |    meta.ui.resourceUri -> ui://chat/rich-chat.html
    |
    |- @McpResource method: rich chat HTML app
         mimeType: text/html;profile=mcp-app
         meta.ui.csp.resourceDomains: allowed UI script origins
```

Design principles:
- Keep MCP app resource serving in its own package (mcpapp/).
- Keep business logic reusable by existing services (chat/, tools/, rag/).
- Avoid duplicated orchestration logic between REST controllers and MCP handlers.

### 9.6 Pattern 6 Definition

Pattern name:
- Pattern 6 - MCP Apps (Chat + Embedded Rich UI)

Core elements:
- MCP tool that advertises a UI resource.
- MCP resource that serves HTML/JS/CSS.
- UI client using ext-apps integration to:
  - update model context
  - send message
  - optionally call server tools

### 9.7 Contract Design

Tool contract:
- Name: open-rich-chat
- Description: Opens the rich chat MCP app and initializes assistant-facing context.
- Return: A short confirmation string for host logs.

Resource contract:
- URI: ui://chat/rich-chat.html
- MIME type: text/html;profile=mcp-app
- CSP metadata: allow only required resource domains.

Context contract from UI to host:
- State payload represented as text summaries and lightweight structured fields.
- Each update should be idempotent and include a compact "current state" summary.

### 9.8 Security and Safety Design

- CSP default deny; explicit allowlist only.
- No inline secrets in HTML or JavaScript.
- Tool descriptions must clearly state purpose and expected side effects.
- Host and server logs should avoid storing sensitive free-form user text where possible.

### 9.9 Operational Design

- Preferred transport: streamable HTTP.
- Default MCP server port target: 3001.
- Endpoint target: /mcp.
- Existing REST API remains on current port for web UI workflows.

### 9.10 Testing Design

Minimum acceptance test set before merge:
1. Tool discovery test: host sees open-rich-chat.
2. Resource resolution test: host can load ui://chat/rich-chat.html.
3. Context update test: UI update is reflected in subsequent assistant turn.
4. Regression test: existing REST endpoints continue to pass current tests.

### 9.11 Delivery Plan

Phase 0 - Design (current)
- Finalize contracts, package boundaries, and migration prerequisites.

Phase 1 - Enablement
- Upgrade Spring AI line to MCP-app-capable version in a dedicated branch.
- Add MCP server dependency and streamable transport config.

Phase 2 - Skeleton
- Implement minimal @McpTool and @McpResource pair.
- Ship static "hello" MCP app for host validation.

Phase 3 - Rich Chat App
- Implement rich chat UI resource with context update flow.
- Wire callServerTool/sendMessage where needed.

Phase 4 - Hardening
- Add CSP tightening, structured logging, and compatibility notes per host.
- Add docs and reproducible run instructions.

### 9.12 Exit Criteria for Design Sign-Off

- Pattern 6 responsibilities are clear and non-overlapping with Patterns 1-5.
- Tool/resource/context contracts are frozen for first implementation.
- Migration risk (Spring AI version change) is explicitly acknowledged and staged.

---

*Last updated: April 2026*
