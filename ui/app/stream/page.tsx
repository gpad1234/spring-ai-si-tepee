"use client";
import { useState } from "react";
import { useStream } from "@/hooks/useStream";

export default function StreamPage() {
  const { tokens, stream, stop, clear, isStreaming, error } = useStream();
  const [input, setInput] = useState("");

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const msg = input.trim();
    if (!msg) return;
    clear();
    stream(msg);
  };

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col">
      {/* Header */}
      <div className="border-b px-6 py-4" style={{ borderColor: "var(--border)", background: "var(--surface)" }}>
        <h1 className="text-base font-semibold" style={{ color: "var(--text)" }}>Streaming Responses</h1>
        <p className="text-xs mt-0.5" style={{ color: "var(--muted)" }}>
          Watch tokens arrive in real-time from the backend via SSE
        </p>
      </div>

      {/* Token display */}
      <div className="flex-1 overflow-y-auto px-6 py-6">
        <div
          className="min-h-[200px] rounded-2xl border p-5 font-mono text-sm leading-relaxed whitespace-pre-wrap"
          style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--text)" }}
        >
          {tokens || (
            <span className="opacity-30" style={{ color: "var(--muted)" }}>
              Streaming output will appear here…
            </span>
          )}
          {isStreaming && (
            <span
              className="inline-block ml-0.5 h-4 w-0.5 align-text-bottom animate-[blink_1s_step-end_infinite]"
              style={{ background: "var(--amber)" }}
            />
          )}
        </div>
        {error && (
          <p className="mt-3 text-sm" style={{ color: "var(--rose)" }}>Error: {error.message}</p>
        )}
      </div>

      {/* Input */}
      <form
        onSubmit={submit}
        className="border-t px-6 py-4 flex gap-3"
        style={{ borderColor: "var(--border)", background: "var(--surface)" }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask something to stream…"
          className="flex-1 rounded-xl border px-4 py-2.5 text-sm outline-none"
          style={{ background: "var(--surface2)", borderColor: "var(--border)", color: "var(--text)" }}
        />
        {isStreaming ? (
          <button
            type="button"
            onClick={stop}
            className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white"
            style={{ background: "var(--rose)" }}
          >
            Stop
          </button>
        ) : (
          <button
            type="submit"
            disabled={!input.trim()}
            className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-40"
            style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))" }}
          >
            Stream
          </button>
        )}
      </form>
    </div>
  );
}
