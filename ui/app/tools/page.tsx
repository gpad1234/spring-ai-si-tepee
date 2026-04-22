"use client";
import { useState, useRef, useEffect } from "react";
import { useTools } from "@/hooks/useTools";
import { cn } from "@/lib/cn";

// Heuristic: detect tool-call sections in the assistant response
function parseSegments(text: string): { type: "text" | "tool"; content: string }[] {
  const segments: { type: "text" | "tool"; content: string }[] = [];
  // Lines that look like tool usage: "Tool: …", "Called: …", JSON blocks, etc.
  const lines = text.split("\n");
  let buf = "";
  let inTool = false;

  for (const line of lines) {
    const isToolLine = /^(tool|function|called|result|calling):/i.test(line.trim()) || /^\{/.test(line.trim());
    if (isToolLine !== inTool) {
      if (buf.trim()) segments.push({ type: inTool ? "tool" : "text", content: buf.trim() });
      buf = "";
      inTool = isToolLine;
    }
    buf += line + "\n";
  }
  if (buf.trim()) segments.push({ type: inTool ? "tool" : "text", content: buf.trim() });
  return segments;
}

const SUGGESTIONS = [
  "What's the current weather in San Francisco?",
  "What time is it right now?",
  "Tell me a dad joke.",
  "What's 2+2? Also what's 100 * 50?",
];

export default function ToolsPage() {
  const { messages, send, isPending, error, clear } = useTools();
  const [input, setInput] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isPending]);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const msg = input.trim();
    if (!msg || isPending) return;
    send(msg);
    setInput("");
  };

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b px-6 py-3"
        style={{ borderColor: "var(--border)", background: "var(--surface)" }}>
        <div>
          <h1 className="text-base font-semibold" style={{ color: "var(--text)" }}>Tool Calling</h1>
          <p className="text-xs mt-0.5" style={{ color: "var(--muted)" }}>
            Claude autonomously calls registered Spring AI tools to answer your questions
          </p>
        </div>
        {messages.length > 0 && (
          <button onClick={clear} className="text-xs opacity-50 hover:opacity-100" style={{ color: "var(--muted)" }}>Clear</button>
        )}
      </div>

      {/* Suggestions */}
      {messages.length === 0 && (
        <div className="px-6 pt-6 flex flex-wrap gap-2">
          {SUGGESTIONS.map((s) => (
            <button
              key={s}
              onClick={() => { setInput(s); }}
              className="rounded-full border px-3 py-1.5 text-xs hover:border-violet-500 transition-colors"
              style={{ borderColor: "var(--border)", color: "var(--muted)", background: "var(--surface)" }}
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
        {messages.map((m, i) => {
          if (m.role === "user") {
            return (
              <div key={i} className="flex justify-end">
                <div className="max-w-[70%] rounded-2xl px-4 py-3 text-sm"
                  style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))", color: "#fff" }}>
                  {m.content}
                </div>
              </div>
            );
          }
          const segs = parseSegments(m.content);
          return (
            <div key={i} className="flex justify-start">
              <div className="max-w-[75%] space-y-2">
                {segs.map((seg, si) =>
                  seg.type === "tool" ? (
                    <div key={si} className="rounded-xl border px-4 py-3 font-mono text-xs"
                      style={{ borderColor: "var(--cyan)", background: "rgba(6,182,212,.06)", color: "var(--cyan)" }}>
                      {seg.content}
                    </div>
                  ) : (
                    <div key={si} className="rounded-2xl border px-4 py-3 text-sm leading-relaxed"
                      style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--text)" }}>
                      {seg.content}
                    </div>
                  )
                )}
              </div>
            </div>
          );
        })}
        {isPending && (
          <div className="flex justify-start">
            <div className="rounded-2xl border px-4 py-3 text-sm animate-pulse"
              style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--muted)" }}>
              Calling tools…
            </div>
          </div>
        )}
        {error && <p className="text-sm" style={{ color: "var(--rose)" }}>Error: {error.message}</p>}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form onSubmit={submit} className="border-t px-6 py-4 flex gap-3"
        style={{ borderColor: "var(--border)", background: "var(--surface)" }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask Claude to use a tool…"
          className="flex-1 rounded-xl border px-4 py-2.5 text-sm outline-none"
          style={{ background: "var(--surface2)", borderColor: "var(--border)", color: "var(--text)" }}
        />
        <button
          type="submit"
          disabled={isPending || !input.trim()}
          className={cn("rounded-xl px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-40")}
          style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))" }}
        >
          Send
        </button>
      </form>
    </div>
  );
}
