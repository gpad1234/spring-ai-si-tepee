"use client";
import { useState } from "react";
import { useExtract } from "@/hooks/useExtract";
import { cn } from "@/lib/cn";

const SAMPLES = {
  person:    "Meet Sarah Chen, 34, a software engineer from Austin. You can reach her at sarah.chen@example.com.",
  sentiment: "The new framework is incredibly intuitive and well-documented. It saved us weeks of boilerplate and the community support has been fantastic.",
};

type Mode = "person" | "sentiment";

function FieldBadge({ label, value }: { label: string; value: string | number | null }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl border p-3" style={{ borderColor: "var(--border)", background: "var(--surface2)" }}>
      <span className="text-[10px] font-semibold uppercase tracking-widest" style={{ color: "var(--muted)" }}>{label}</span>
      <span className="text-sm font-mono" style={{ color: value != null ? "var(--cyan)" : "var(--muted)" }}>
        {value != null ? String(value) : "null"}
      </span>
    </div>
  );
}

export default function ExtractPage() {
  const [mode, setMode] = useState<Mode>("person");
  const [text, setText] = useState(SAMPLES.person);
  const { extractPerson, personData, personPending, personError,
          extractSentiment, sentimentData, sentimentPending, sentimentError } = useExtract();

  const isPending = mode === "person" ? personPending : sentimentPending;
  const error     = mode === "person" ? personError   : sentimentError;
  const data      = mode === "person" ? personData    : sentimentData;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === "person") extractPerson(text);
    else extractSentiment(text);
  };

  return (
    <div className="mx-auto max-w-3xl px-6 py-8 space-y-6">
      <div>
        <h1 className="text-xl font-bold" style={{ color: "var(--text)" }}>Structured Output Extraction</h1>
        <p className="text-sm mt-1" style={{ color: "var(--muted)" }}>
          Paste any text and Claude will extract structured fields via Spring AI's output converter.
        </p>
      </div>

      {/* Mode toggle */}
      <div className="flex gap-2">
        {(["person", "sentiment"] as Mode[]).map((m) => (
          <button
            key={m}
            onClick={() => { setMode(m); setText(SAMPLES[m]); }}
            className={cn("rounded-lg px-4 py-1.5 text-sm font-medium capitalize transition-colors")}
            style={
              mode === m
                ? { background: "linear-gradient(135deg,var(--violet),var(--indigo))", color: "#fff" }
                : { background: "var(--surface)", color: "var(--muted)", border: "1px solid var(--border)" }
            }
          >
            {m}
          </button>
        ))}
      </div>

      <form onSubmit={submit} className="space-y-3">
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={5}
          className="w-full rounded-xl border p-4 text-sm outline-none resize-none"
          style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--text)" }}
        />
        <button
          type="submit"
          disabled={isPending || !text.trim()}
          className="rounded-xl px-6 py-2.5 text-sm font-semibold text-white disabled:opacity-40"
          style={{ background: "linear-gradient(135deg,var(--violet),var(--indigo))" }}
        >
          {isPending ? "Extracting…" : "Extract"}
        </button>
      </form>

      {error && <p className="text-sm" style={{ color: "var(--rose)" }}>Error: {error.message}</p>}

      {data && (
        <div>
          <h2 className="text-sm font-semibold mb-3" style={{ color: "var(--text)" }}>Extracted Fields</h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {Object.entries(data).map(([k, v]) => (
              <FieldBadge key={k} label={k} value={v as string | number | null} />
            ))}
          </div>
          <pre className="mt-4 rounded-xl border p-4 text-xs overflow-auto font-mono"
            style={{ background: "var(--surface)", borderColor: "var(--border)", color: "var(--cyan)" }}>
            {JSON.stringify(data, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
