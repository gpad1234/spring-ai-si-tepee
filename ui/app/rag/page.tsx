"use client";
import { useState, useRef } from "react";
import { useRag } from "@/hooks/useRag";

export default function RagPage() {
  const { ingest, ingestPending, ingestError, docs, ask, answer, askPending, askError } = useRag();
  const [question, setQuestion] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const handleFiles = async (files: FileList | null) => {
    if (!files?.length) return;
    for (const f of Array.from(files)) await ingest(f);
  };

  const submitQ = (e: React.FormEvent) => {
    e.preventDefault();
    const q = question.trim();
    if (!q || askPending) return;
    ask(q);
  };

  return (
    <div className="mx-auto max-w-3xl px-6 py-8 space-y-8">
      <div>
        <h1 className="text-xl font-bold" style={{ color: "var(--text)" }}>Retrieval-Augmented Generation</h1>
        <p className="text-sm mt-1" style={{ color: "var(--muted)" }}>
          Upload documents then ask questions — Claude answers using only your content.
        </p>
      </div>

      {/* Drop zone */}
      <div
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => { e.preventDefault(); setDragging(false); handleFiles(e.dataTransfer.files); }}
        onClick={() => fileRef.current?.click()}
        className="cursor-pointer rounded-2xl border-2 border-dashed p-10 text-center transition-colors"
        style={{
          borderColor: dragging ? "var(--violet)" : "var(--border)",
          background: dragging ? "rgba(124,58,237,.08)" : "var(--surface)",
        }}
      >
        <input ref={fileRef} type="file" accept=".txt,.md,.pdf" multiple className="hidden" onChange={(e) => handleFiles(e.target.files)} />
        <p className="text-sm font-medium" style={{ color: "var(--text)" }}>
          {ingestPending ? "Ingesting…" : "Drop files here or click to browse"}
        </p>
        <p className="text-xs mt-1" style={{ color: "var(--muted)" }}>.txt · .md · .pdf</p>
      </div>
      {ingestError && <p className="text-sm" style={{ color: "var(--rose)" }}>Ingest error: {ingestError.message}</p>}

      {/* Doc list */}
      {docs.length > 0 && (
        <div className="space-y-2">
          <h2 className="text-sm font-semibold" style={{ color: "var(--text)" }}>Indexed Documents</h2>
          {docs.map((d, i) => (
            <div key={i} className="flex items-center justify-between rounded-xl border px-4 py-2.5"
              style={{ borderColor: "var(--border)", background: "var(--surface)" }}>
              <span className="text-sm" style={{ color: "var(--text)" }}>{d.name}</span>
              <span className="text-xs rounded-full px-2 py-0.5" style={{ background: "var(--surface2)", color: "var(--cyan)" }}>
                {d.chunks} chunks
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Ask */}
      <form onSubmit={submitQ} className="flex gap-3">
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about your documents…"
          disabled={docs.length === 0}
          className="flex-1 rounded-xl border px-4 py-2.5 text-sm outline-none disabled:opacity-40"
          style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--text)" }}
        />
        <button
          type="submit"
          disabled={askPending || !question.trim() || docs.length === 0}
          className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-40"
          style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))" }}
        >
          {askPending ? "…" : "Ask"}
        </button>
      </form>
      {askError && <p className="text-sm" style={{ color: "var(--rose)" }}>Error: {askError.message}</p>}

      {answer && (
        <div className="rounded-2xl border p-5 text-sm leading-relaxed"
          style={{ borderColor: "var(--border)", background: "var(--surface)", color: "var(--text)" }}>
          {answer}
        </div>
      )}
    </div>
  );
}
