"use client";
import { useState, useCallback, useRef } from "react";
import { postStream } from "@/lib/api";

export function useStream() {
  const [tokens, setTokens] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError]   = useState<Error | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const stream = useCallback(async (message: string) => {
    abortRef.current?.abort();
    const ctrl = new AbortController();
    abortRef.current = ctrl;

    setTokens("");
    setError(null);
    setIsStreaming(true);

    try {
      const res = await postStream(message);
      const reader = res.body?.getReader();
      if (!reader) throw new Error("No response body");

      const decoder = new TextDecoder();
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        if (ctrl.signal.aborted) break;
        setTokens((t) => t + decoder.decode(value, { stream: true }));
      }
    } catch (e: unknown) {
      if ((e as Error).name !== "AbortError") setError(e as Error);
    } finally {
      setIsStreaming(false);
    }
  }, []);

  const stop = () => abortRef.current?.abort();
  const clear = () => setTokens("");

  return { tokens, stream, stop, clear, isStreaming, error };
}
