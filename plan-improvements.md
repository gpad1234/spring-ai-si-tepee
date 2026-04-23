# Improvement Plan

> Follow-up plan based on the current repository state before starting the next delivery phase.
> Focuses on correctness gaps, contract alignment, test depth, documentation drift, and production hardening.

---

## Current Snapshot

What is already in good shape:

- Backend tests pass with `mvn test`.
- Frontend production build passes with `npm run build`.
- Package boundaries are clean: controller -> service -> config/tooling.
- Setup and pattern coverage are broad enough to treat this as a solid reference project.

What needs attention before expanding scope:

- Frontend and backend API response contracts are inconsistent.
- Structured-output endpoints and docs no longer describe the same feature set.
- Several docs still describe superseded implementation details.
- Current tests are too shallow to catch schema drift and integration breakage.
- Some production-readiness guidance exists only in docs, not in runtime configuration.

---

## Priority Order

1. Fix API and UI contract mismatches.
2. Add tests that lock those contracts in place.
3. Reconcile docs with the current implementation.
4. Tighten runtime defaults and hardening boundaries.
5. Clean up developer-experience gaps.

---

## Phase A - Contract Alignment

**Goal:** Make the backend payloads, frontend fetch wrappers, and UI expectations match exactly.

### Tasks

| # | Task | Scope |
|---|---|---|
| A.1 | Standardize chat response naming (`reply` vs `response`) | Spring controller + UI API client |
| A.2 | Standardize tool-calling response naming (`reply` vs `response`) | Spring controller + UI API client |
| A.3 | Fix RAG ingest contract so backend returns a JSON body the UI can consume, or update the UI to handle `202 No Content` correctly | Spring controller + UI hook |
| A.4 | Choose one structured-output feature set and make code, UI, and docs agree (`person + sentiment` or `person + product`) | Spring controller/service + Next UI + docs |
| A.5 | Review all curl examples in setup docs against the actual controller contracts | `docs/SETUP.md` |

### Acceptance Criteria

- Chat UI renders the assistant reply from the real backend without relying on mismatched property names.
- Tool-calling UI renders the assistant reply from the real backend without fallback assumptions.
- RAG ingest no longer fails at `response.json()` because of an empty backend response.
- Structured-output page only calls endpoints that actually exist.

---

## Phase B - Contract Tests

**Goal:** Add focused tests that fail when API contracts or response shapes drift.

### Tasks

| # | Task | Scope |
|---|---|---|
| B.1 | Replace placeholder chat unit test with a behavior assertion on returned content | `src/test/java/.../chat` |
| B.2 | Strengthen RAG ingestion test to assert chunks are stored or returned metadata is correct | `src/test/java/.../rag` |
| B.3 | Add controller-level tests for JSON response field names and HTTP status codes | backend web layer |
| B.4 | Add at least one frontend test for each critical fetch contract (`chat`, `rag ingest`, `tools`) using mocked responses | `ui` |
| B.5 | Add one end-to-end smoke path for UI -> API proxy -> backend shape validation | optional integration layer |

### Acceptance Criteria

- A backend response shape change causes a failing test.
- A frontend expectation mismatch causes a failing test.
- Placeholder assertions like "service is not null" are removed from critical paths.

---

## Phase C - Documentation Reconciliation

**Goal:** Make the documentation describe the system that actually exists today.

### Tasks

| # | Task | Scope |
|---|---|---|
| C.1 | Update RAG docs to reflect manual retrieval plus prompt injection instead of `QuestionAnswerAdvisor` | architecture and pattern docs |
| C.2 | Update structured-output docs to match the chosen endpoint set | product and setup docs |
| C.3 | Verify whether virtual threads are actually enabled; either add the property or correct the docs | config + docs |
| C.4 | Update the project plan so completed items do not contradict the codebase | `docs/PROJECT_PLAN.md` |
| C.5 | Replace the default Next.js scaffold README with project-specific UI instructions | `ui/README.md` |
| C.6 | Add a short root-level improvement summary reference from main docs if useful | root docs |

### Acceptance Criteria

- No doc claims an endpoint, pattern, or runtime setting that is absent from the code.
- Setup instructions and curl examples work against the current implementation.
- The UI README explains this project, not the default scaffold template.

---

## Phase D - Runtime Hardening

**Goal:** Move the project closer to safe operational defaults without overcomplicating the reference architecture.

### Tasks

| # | Task | Scope |
|---|---|---|
| D.1 | Split development and production-oriented configuration using profiles | Spring config |
| D.2 | Revisit default model selection for cost and latency; avoid expensive defaults unless explicitly intended | `application.properties` |
| D.3 | Restrict actuator detail exposure outside local development | management config |
| D.4 | Add structured error handling with `@ControllerAdvice` | backend web layer |
| D.5 | Define production swap points more concretely for embeddings and vector store | config + docs |
| D.6 | Decide whether demo tools should remain stubs or be fenced clearly behind demo-only wording | tool layer + docs |

### Acceptance Criteria

- Local development remains easy.
- Production-oriented settings are no longer mixed with demo defaults.
- Errors return stable JSON payloads instead of framework defaults.

---

## Phase E - Developer Experience Cleanup

**Goal:** Reduce confusion for the next contributor before new features are added.

### Tasks

| # | Task | Scope |
|---|---|---|
| E.1 | Remove or fix unused imports currently reported by the editor | backend + tests |
| E.2 | Add a short "known demo limitations" section for embeddings, vector store, and stubbed tools | docs |
| E.3 | Add a root README if the repo is intended for external consumption | root docs |
| E.4 | Ensure generated build output stays ignored and out of review noise | repo hygiene |

### Acceptance Criteria

- A new contributor can determine what is demo-only, what is production-ready, and what is planned next.
- Basic editor warnings are reduced.

---

## Recommended Immediate Next Phase

If we want the fastest path to a more reliable project, the next implementation pass should be:

1. Phase A - Contract Alignment
2. Phase B - Contract Tests
3. Phase C - Documentation Reconciliation

That sequence fixes the highest-risk issues first and gives the project a stable baseline before any broader hardening or new feature work.

---

## Validation Notes

This plan is based on the current repository state that was verified by:

- `mvn test` passing for the backend
- `npm run build` passing for the frontend
- direct review of controllers, services, UI fetch wrappers, hooks, and project docs