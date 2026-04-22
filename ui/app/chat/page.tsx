"use client";
import { useState, useRef, useEffect } from "react";
import { useChat } from "@/hooks/useChat";
import { cn } from "@/lib/cn";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

export default function ChatPage() {
  const { messages, send, clear, isPending } = useChat();
  const [input, setInput]   = useState("");
  const [sysprompt, setSys] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isPending]);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const msg = input.trim();
    if (!msg || isPending) return;
    send(msg, sysprompt || undefined);
    setInput("");
  };

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col">
      {/* System prompt bar */}
      <div className="border-b px-6 py-2 flex items-center gap-3" style={{ borderColor: "var(--border)", background: "var(--surface)" }}>
        <span className="text-xs font-medium" style={{ color: "var(--muted)" }}>System&nbsp;prompt</span>
        <input
          value={sysprompt}
          onChange={(e) => setSys(e.target.value)}
          placeholder="Optional system instruction…"
          className="flex-1 bg-transparent text-sm outline-none placeholder:opacity-40"
          style={{ color: "var(--text)" }}
        />
        {messages.length > 0 && (
          <button onClick={clear} className="text-xs opacity-50 hover:opacity-100" style={{ color: "var(--muted)" }}>
            Clear
          </button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-6 space-y-4">
        {messages.length === 0 && (
          <div className="flex h-full items-center justify-center text-sm opacity-30" style={{ color: "var(--muted)" }}>
            Send a message to start chatting with Claude…
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={cn("flex", m.role === "user" ? "justify-end" : "justify-start")}>
            <div
              className="max-w-[70%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
              style={
                m.role === "user"
                  ? { background: "linear-gradient(135deg,var(--violet),var(--indigo))", color: "#fff" }
                  : { background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }
              }
            >
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{m.content}</ReactMarkdown>
            </div>
          </div>
        ))}
        {isPending && (
          <div className="flex justify-start">
            <div className="rounded-2xl px-4 py-3 text-sm" style={{ background: "var(--surface)", border: "1px solid var(--border)" }}>
              <span className="animate-pulse opacity-60" style={{ color: "var(--muted)" }}>Claude is thinking…</span>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
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
          placeholder="Type a message…"
          className="flex-1 rounded-xl border px-4 py-2.5 text-sm outline-none focus:ring-2"
          style={{
            background: "var(--surface2)",
            borderColor: "var(--border)",
            color: "var(--text)",
            // @ts-expect-error css var
            "--tw-ring-color": "var(--violet)",
          }}
        />
        <button
          type="submit"
          disabled={isPending || !input.trim()}
          className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition-opacity disabled:opacity-40"
          style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))" }}
        >
          Send
        </button>
      </form>
    </div>
  );
}
