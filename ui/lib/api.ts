// Typed fetch wrappers for all 5 Spring AI backend endpoints

const BASE = "";  // rewrites proxy handles /api/* → localhost:8080/api/*

// ── Types ─────────────────────────────────────────────────────────

export interface ChatRequest  { message: string; systemPrompt?: string; }
export interface ChatResponse { response: string; }

export interface ExtractPersonResponse {
  name:  string | null;
  age:   number | null;
  email: string | null;
  city:  string | null;
}
export interface ExtractSentimentResponse {
  sentiment:       string;
  confidenceScore: number;
  reasoning:       string;
}

export interface RagIngestResponse { message: string; chunks: number; }
export interface RagAskRequest     { question: string; }
export interface RagAskResponse    { answer: string; }

export interface ToolsResponse     { response: string; }

// ── Chat ──────────────────────────────────────────────────────────

export async function postChat(req: ChatRequest): Promise<ChatResponse> {
  const res = await fetch(`${BASE}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ── Stream ────────────────────────────────────────────────────────
// Returns the raw Response so the caller can read the SSE stream.
export async function postStream(message: string): Promise<Response> {
  const res = await fetch(`${BASE}/api/stream`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res;
}

// ── Structured Output ─────────────────────────────────────────────

export async function postExtractPerson(text: string): Promise<ExtractPersonResponse> {
  const res = await fetch(`${BASE}/api/extract/person`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function postExtractSentiment(text: string): Promise<ExtractSentimentResponse> {
  const res = await fetch(`${BASE}/api/extract/sentiment`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ── RAG ───────────────────────────────────────────────────────────

export async function postRagIngest(file: File): Promise<RagIngestResponse> {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${BASE}/api/rag/ingest`, { method: "POST", body: form });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export async function postRagAsk(question: string): Promise<RagAskResponse> {
  const res = await fetch(`${BASE}/api/rag/ask`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ── Tools ─────────────────────────────────────────────────────────

export async function postToolsChat(message: string): Promise<ToolsResponse> {
  const res = await fetch(`${BASE}/api/tools/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}
