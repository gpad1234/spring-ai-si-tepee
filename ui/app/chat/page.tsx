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
  const composerRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isPending]);

  const submit = (e?: React.FormEvent) => {
    e?.preventDefault();
    const msg = input.trim();
    if (!msg || isPending) return;
    send(msg, sysprompt || undefined);
    setInput("");
  };

  const onComposerKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  };

  const insertAroundSelection = (prefix: string, suffix = prefix) => {
    const el = composerRef.current;
    if (!el) return;

    const start = el.selectionStart;
    const end = el.selectionEnd;
    const selected = input.slice(start, end);
    const next = `${input.slice(0, start)}${prefix}${selected}${suffix}${input.slice(end)}`;

    setInput(next);

    requestAnimationFrame(() => {
      el.focus();
      const caret = selected.length === 0 ? start + prefix.length : end + prefix.length + suffix.length;
      el.setSelectionRange(caret, caret);
    });
  };

  const insertLinePrefix = (prefix: string) => {
    const el = composerRef.current;
    if (!el) return;

    const start = el.selectionStart;
    const end = el.selectionEnd;
    const selected = input.slice(start, end) || "item";
    const lines = selected.split("\n").map((line) => `${prefix}${line}`).join("\n");
    const next = `${input.slice(0, start)}${lines}${input.slice(end)}`;

    setInput(next);

    requestAnimationFrame(() => {
      el.focus();
      const caret = start + lines.length;
      el.setSelectionRange(caret, caret);
    });
  };

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col bg-inherit">
      {/* System prompt bar */}
      <div className="border-b px-6 py-2 flex items-center gap-3 flex-shrink-0" style={{ borderColor: "var(--border)", background: "var(--surface)" }}>
        <span className="text-xs font-medium whitespace-nowrap" style={{ color: "var(--muted)" }}>System&nbsp;prompt</span>
        <input
          value={sysprompt}
          onChange={(e) => setSys(e.target.value)}
          placeholder="Optional system instruction…"
          className="flex-1 bg-transparent text-sm outline-none placeholder:opacity-40"
          style={{ color: "var(--text)" }}
        />
        {messages.length > 0 && (
          <button onClick={clear} className="text-xs opacity-50 hover:opacity-100 flex-shrink-0" style={{ color: "var(--muted)" }}>
            Clear
          </button>
        )}
      </div>

      {/* Rich composer */}
      <form
        onSubmit={submit}
        className="border-b px-6 py-4 flex flex-col gap-3 flex-shrink-0 relative z-10"
        style={{ borderColor: "var(--border)", background: "var(--surface)" }}
      >
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => insertAroundSelection("**")}
            className="rounded-lg border px-2.5 py-1 text-xs"
            style={{ borderColor: "var(--border)", color: "var(--muted)", background: "var(--surface2)" }}
          >
            Bold
          </button>
          <button
            type="button"
            onClick={() => insertAroundSelection("`")}
            className="rounded-lg border px-2.5 py-1 text-xs"
            style={{ borderColor: "var(--border)", color: "var(--muted)", background: "var(--surface2)" }}
          >
            Inline code
          </button>
          <button
            type="button"
            onClick={() => insertLinePrefix("- ")}
            className="rounded-lg border px-2.5 py-1 text-xs"
            style={{ borderColor: "var(--border)", color: "var(--muted)", background: "var(--surface2)" }}
          >
            List
          </button>
          <button
            type="button"
            onClick={() => insertAroundSelection("```\n", "\n```")}
            className="rounded-lg border px-2.5 py-1 text-xs"
            style={{ borderColor: "var(--border)", color: "var(--muted)", background: "var(--surface2)" }}
          >
            Code block
          </button>
        </div>

        <textarea
          ref={composerRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onComposerKeyDown}
          placeholder="Write a message in rich text (Markdown supported)…"
          rows={3}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none focus:ring-2 bg-clip-padding relative z-20 resize-y min-h-24 max-h-56"
          style={{
            background: "var(--surface2)",
            borderColor: "var(--border)",
            color: "var(--text)",
            // @ts-expect-error css var
            "--tw-ring-color": "var(--violet)",
          }}
        />

        <div className="flex items-center justify-between gap-3">
          <p className="text-xs" style={{ color: "var(--muted)" }}>
            Enter sends, Shift+Enter adds a new line.
          </p>
          <button
            type="submit"
            disabled={isPending || !input.trim()}
            className="shrink-0 rounded-xl px-6 py-2.5 text-sm font-semibold text-white transition-opacity disabled:opacity-40 whitespace-nowrap relative z-10"
            style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))" }}
          >
            {isPending ? "Sending…" : "Send"}
          </button>
        </div>
      </form>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-6 space-y-4 relative">
        {messages.length === 0 && (
          <div className="flex h-full items-center justify-center text-sm opacity-30" style={{ color: "var(--muted)" }}>
            Send a message to start chatting with Claude…
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={cn("flex", m.role === "user" ? "justify-end" : "justify-start")}>
            <div
              className="max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
              style={
                m.role === "user"
                  ? { background: "linear-gradient(135deg,var(--violet),var(--indigo))", color: "#fff" }
                  : { background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }
              }
            >
              <div className={cn("chat-markdown", m.role === "user" ? "chat-markdown-user" : "chat-markdown-assistant")}>
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{m.content}</ReactMarkdown>
              </div>
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
    </div>
  );
}
