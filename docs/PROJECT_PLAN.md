# Project Plan — Spring AI Reference Architecture

> Covers the five development phases from initial scaffold to production-hardened release.
> Each phase is independently shippable.

---

## Milestones at a Glance

| Phase | Milestone | Deliverable |
|---|---|---|
| 1 | Foundation | Buildable skeleton, CI, dev tooling |
| 2 | Core AI Patterns | All five patterns implemented and unit-tested |
| 3 | Observability & Hardening | Production readiness: tracing, rate limits, error handling |
| 4 | Persistence & Scalability | Persistent vector store, externalized config, Docker |
| 5 | Documentation & Developer Experience | Guides, sample prompts, integration test suite |
| 6 | UI | Next.js "Neon Garden" frontend for all five patterns |

---

## Phase 1 — Foundation

**Goal:** A clean, buildable project that every developer on the team can run on day one.

### Tasks

| # | Task | Owner | Status |
|---|---|---|---|
| 1.1 | Initialize Spring Boot 3.3 project with Java 21 via Spring Initializr | Dev | Done |
| 1.2 | Add Spring AI BOM and Anthropic starter to `pom.xml` | Dev | Done |
| 1.3 | Configure `application.properties` — API key placeholder, model defaults | Dev | Done |
| 1.4 | Add `AiConfig.java` — `ChatClient` and `VectorStore` bean definitions | Dev | Done |
| 1.5 | Wire Spring Boot Actuator (`/actuator/health`) | Dev | Done |
| 1.6 | Enable Java 21 virtual threads (`spring.threads.virtual.enabled=true`) | Dev | Done |
| 1.7 | Set up Git repository and push initial commit | Dev | Done |
| 1.8 | Verify `mvn clean package -DskipTests` passes in CI | Dev | Done |

### Acceptance Criteria
- `mvn clean package -DskipTests` exits 0.
- `GET /actuator/health` returns `{"status":"UP"}`.
- No API key is present in any committed file.

---

## Phase 2 — Core AI Patterns

**Goal:** All five patterns are implemented, reachable via HTTP, and covered by unit tests.

### Tasks

#### Pattern 1 — Simple Chat
| # | Task | Status |
|---|---|---|
| 2.1.1 | Implement `ChatService.chat()` using `ChatClient.prompt().call().content()` | Done |
| 2.1.2 | Implement `ChatController` — `POST /api/chat` | Done |
| 2.1.3 | Create `src/main/resources/prompts/chat.st` | Done |
| 2.1.4 | Write `ChatServiceTest` with mocked `ChatModel` | Done |

#### Pattern 2 — Streaming
| # | Task | Status |
|---|---|---|
| 2.2.1 | Implement `StreamingService.stream()` returning `Flux<String>` | Done |
| 2.2.2 | Implement `StreamingController` — `POST /api/stream` (SSE) | Done |
| 2.2.3 | Add `.onErrorResume()` for stream error propagation | Done |

#### Pattern 3 — Structured Output
| # | Task | Status |
|---|---|---|
| 2.3.1 | Define `PersonRecord` and `ProductRecord` Java records | Done |
| 2.3.2 | Implement `StructuredOutputService` with `.call().entity()` | Done |
| 2.3.3 | Implement `StructuredOutputController` — `/api/extract/*` endpoints | Done |
| 2.3.4 | Create `src/main/resources/prompts/extraction.st` | Done |
| 2.3.5 | Write `StructuredOutputServiceTest` | Done |

#### Pattern 4 — RAG
| # | Task | Status |
|---|---|---|
| 2.4.1 | Implement `DocumentIngestionService` — file upload → chunk → embed → store | Done |
| 2.4.2 | Implement `RagService` with `QuestionAnswerAdvisor` | Done |
| 2.4.3 | Implement `RagController` — `POST /api/rag/ingest` and `POST /api/rag/ask` | Done |
| 2.4.4 | Create `src/main/resources/prompts/rag.st` | Done |
| 2.4.5 | Write `DocumentIngestionServiceTest` | Done |

#### Pattern 5 — Tool Calling
| # | Task | Status |
|---|---|---|
| 2.5.1 | Implement `BuiltInTools` with `@Tool` methods (weather, datetime, calculator) | Done |
| 2.5.2 | Implement `ToolCallingService` — register tools per call | Done |
| 2.5.3 | Implement `ToolCallingController` — `POST /api/tools/chat` | Done |

### Acceptance Criteria
- All five endpoints respond correctly to the curl examples in `docs/SETUP.md`.
- `mvn test` passes with no API key (all unit tests use mocked `ChatModel`).

---

## Phase 3 — Observability & Hardening

**Goal:** The service is safe to put in front of real users.

### Tasks

| # | Task | Notes |
|---|---|---|
| 3.1 | Add Micrometer registry (`micrometer-registry-prometheus`) | Exposes `/actuator/prometheus` |
| 3.2 | Configure distributed tracing (Micrometer Tracing + Zipkin or OTLP exporter) | Spring AI auto-instruments `ChatClient` calls |
| 3.3 | Add response timeout for Claude API calls | `spring.ai.anthropic.chat.options.timeout` |
| 3.4 | Add application-level rate limiting with Bucket4j | Protect against token-per-minute bursts |
| 3.5 | Add global `@ControllerAdvice` for structured error responses | Return `{ "error": "..." }` on 4xx/5xx |
| 3.6 | Tighten Actuator exposure | Expose only `health`, `info`, `prometheus` |
| 3.7 | Add `spring.ai.chat.client.observations.include-input=true` | Trace prompt content in dev only |
| 3.8 | Write load test baseline (k6 or Gatling) | Establish p95 latency budget |

### Acceptance Criteria
- `GET /actuator/prometheus` returns Spring AI span metrics.
- Errors from the model return a structured JSON body, not a stack trace.
- A k6 smoke test at 10 RPS passes with p95 < 5 s (network-dependent).

---

## Phase 4 — Persistence & Scalability

**Goal:** The service can be deployed to a real environment without in-memory state loss.

### Tasks

| # | Task | Notes |
|---|---|---|
| 4.1 | Replace `SimpleVectorStore` with `PgVectorStore` | Single bean swap in `AiConfig`; add `pgvector` to `pom.xml` |
| 4.2 | Add `docker-compose.yml` — PostgreSQL with `pgvector` extension | Dev environment setup |
| 4.3 | Add `Dockerfile` for the Spring Boot application | Multi-stage build, distroless base |
| 4.4 | Externalise chunk size and overlap to `application.properties` | Already partially done — complete and document |
| 4.5 | Add `application-prod.properties` profile | PgVector URL, pool sizes, log levels |
| 4.6 | Configure Spring Boot graceful shutdown | `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase` |
| 4.7 | Add health indicator for vector store connectivity | Custom `HealthIndicator` bean |

### Acceptance Criteria
- `docker compose up` starts Postgres + app with a working RAG flow.
- Restarting the app does not lose ingested documents.
- `GET /actuator/health` reports the vector store as UP.

---

## Phase 5 — Documentation & Developer Experience

**Goal:** Any developer can understand, run, and extend the project in under 30 minutes.

### Tasks

| # | Task | Notes |
|---|---|---|
| 5.1 | Write `docs/SETUP.md` — first-run, curl examples, model switching | Done |
| 5.2 | Write `docs/ARCHITECTURE.md` — layered diagram, abstractions, swap points | Done |
| 5.3 | Write `docs/PATTERNS.md` — when to use each pattern, decision points | Done |
| 5.4 | Write `docs/PRODUCT_DESIGN.md` — feature catalogue, tech stack, data flow | Done |
| 5.5 | Write `docs/PROJECT_PLAN.md` — this document | In progress |
| 5.6 | Add integration test suite (`mvn test -Dgroups=integration`) | Requires `ANTHROPIC_API_KEY` |
| 5.7 | Add `CONTRIBUTING.md` — coding conventions, PR checklist | |
| 5.8 | Add `README.md` at project root with badges and quick-start | |
| 5.9 | Record a short demo walkthrough (optional) | |

### Acceptance Criteria
- A developer unfamiliar with the project can run `mvn spring-boot:run` and hit all five
  endpoints following only `docs/SETUP.md`.
- All integration tests pass with a valid `ANTHROPIC_API_KEY`.

---

## Phase 6 — User Interface

**Goal:** A polished, dark-first "Neon Garden" web app that makes every pattern
interactive and visually demonstrates what each one does.

**Design tokens**
- Background `#0a0a0f`, primary gradient violet `#7c3aed` → indigo `#4f46e5`
- Cyan accent `#06b6d4`, amber stream cursor `#f59e0b`, rose error `#f43f5e`
- Fonts: Inter (body), JetBrains Mono (code/JSON)

### Tasks

#### Setup
| # | Task | Notes |
|---|---|---|
| 6.1 | Scaffold Next.js 15 app in `ui/` with TypeScript and Tailwind CSS v4 | `npx create-next-app@latest ui --ts --tailwind --app` |
| 6.2 | Install shadcn/ui and initialise component library | `npx shadcn@latest init` |
| 6.3 | Install Framer Motion, TanStack Query, React Markdown | |
| 6.4 | Define design tokens in `tailwind.config.ts` — colours, fonts, animations | Neon Garden palette |
| 6.5 | Build `TopNav` and collapsible `Sidebar` components | Active state uses violet gradient |
| 6.6 | Add Next.js `rewrites()` proxy to `http://localhost:8080/api/*` | Avoids CORS in dev |

#### View 1 — Chat (`/chat`)
| # | Task | Notes |
|---|---|---|
| 6.7 | Build `MessageBubble` — user (violet bg) and AI (glass card) variants | Markdown rendered via React Markdown |
| 6.8 | Build `InputBar` — textarea, send button, optional system-prompt drawer | |
| 6.9 | Build `TypingIndicator` — three bouncing cyan dots | Shown while awaiting first token |
| 6.10 | Wire `useChat` hook to `POST /api/chat` via TanStack Query | |

#### View 2 — Stream (`/stream`)
| # | Task | Notes |
|---|---|---|
| 6.11 | Build `useStream` hook using browser `EventSource` | Appends tokens to state on each `message` event |
| 6.12 | Build `StreamPane` — re-uses `MessageBubble`; adds amber blinking cursor while stream is open | |
| 6.13 | Handle stream error events — show rose error banner | |

#### View 3 — Extract (`/extract`)
| # | Task | Notes |
|---|---|---|
| 6.14 | Build `ExtractForm` — textarea + Person/Product pill tabs | |
| 6.15 | Build `JsonCard` — renders extracted record with green/grey field badges | Null fields shown in grey |
| 6.16 | Add copy-to-clipboard button on `JsonCard` | |
| 6.17 | Wire `useExtract` hook to `POST /api/extract/person` and `/product` | |

#### View 4 — RAG (`/rag`)
| # | Task | Notes |
|---|---|---|
| 6.18 | Build `DropZone` — drag-and-drop + file browser; accepts PDF, TXT, MD | Progress ring during ingestion |
| 6.19 | Build `DocumentList` — file name, size, green tick on success | Metadata persisted in `localStorage` |
| 6.20 | Build `RagChat` — question input + answer with optional source chunks panel | Source chunks collapsed by default |
| 6.21 | Wire ingestion to `POST /api/rag/ingest` and questions to `POST /api/rag/ask` | |

#### View 5 — Tools (`/tools`)
| # | Task | Notes |
|---|---|---|
| 6.22 | Build `ToolCallBadge` — cyan card showing tool name, args, result | |
| 6.23 | Build `ToolChat` — chat layout with collapsible tool-invocation side panel | Panel shows all tool calls in current turn |
| 6.24 | Wire `useTools` hook to `POST /api/tools/chat` | |

#### Polish & Quality
| # | Task | Notes |
|---|---|---|
| 6.25 | Add page transitions (Framer Motion `AnimatePresence`) | Slide-in from right |
| 6.26 | Make layout responsive — sidebar collapses to icons < 1024 px | |
| 6.27 | Keyboard shortcuts: `Cmd+Enter` to send, `Esc` to cancel stream | |
| 6.28 | Dark/light mode toggle (dark is default) | CSS variable swap |
| 6.29 | Write Playwright smoke tests for each view | Happy path only |
| 6.30 | Update `docs/SETUP.md` with `cd ui && npm run dev` instructions | |

### Acceptance Criteria
- `npm run dev` inside `ui/` starts the app at `http://localhost:3000`.
- All five views are reachable and functional with the Spring Boot backend running.
- Playwright smoke tests pass (`npx playwright test`).
- Lighthouse accessibility score ≥ 90.

---

## Future Backlog (Post-v1)

These items are scoped out of v1 but are natural next steps.

| Item | Rationale |
|---|---|
| Conversation memory (`MessageWindowChatMemory`) | Enables multi-turn chat sessions |
| Multi-modal input (image + text) | Claude supports vision — pass `Media` objects |
| OpenAI / Ollama provider switch | Demonstrate provider portability |
| LangGraph-style agentic loop | Multi-step autonomous task execution |
| Chroma or Redis vector store option | Alternative to PgVector for teams without Postgres |
| Prompt versioning / A-B testing | Track prompt changes and their effect on quality |
| Fine-tuned evaluation harness | Automated regression tests for answer quality |
| Kubernetes Helm chart | Production deployment on AKS / EKS / GKE |

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Anthropic API rate limits during testing | Medium | High | Use mocked `ChatModel` for unit tests; throttle integration tests |
| Spring AI API changes between milestones | Low | Medium | Pin `spring-ai.version` in BOM; review release notes on upgrade |
| `SimpleVectorStore` data loss on restart | High (by design) | Low (dev only) | Document clearly; Phase 4 replaces it with PgVector |
| Local embedding model memory usage | Medium | Medium | ONNX transformer loads ~200 MB; document minimum heap setting |
| Prompt quality regression | Low | Medium | Phase 5 integration tests catch regressions before merge |
| CORS misconfiguration between UI and API | Medium | Medium | Use Next.js proxy in dev; explicit `CorsConfigurationSource` bean in prod |
| SSE connection drops on mobile / proxies | Medium | Low | Add reconnect logic in `useStream` hook with exponential backoff |
| Tailwind v4 / shadcn incompatibility | Low | Medium | Pin exact versions; upgrade only after checking shadcn release notes |

---

*Last updated: April 2026*
