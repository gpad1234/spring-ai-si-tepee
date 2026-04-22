"use client";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { postRagIngest, postRagAsk } from "@/lib/api";

export function useRag() {
  const [docs, setDocs] = useState<{ name: string; chunks: number }[]>([]);

  const ingestMutation = useMutation({
    mutationFn: (file: File) => postRagIngest(file),
    onSuccess: (data, file) =>
      setDocs((d) => [...d, { name: file.name, chunks: data.chunks }]),
  });

  const askMutation = useMutation({
    mutationFn: (question: string) => postRagAsk(question),
  });

  return {
    ingest:        ingestMutation.mutateAsync,
    ingestPending: ingestMutation.isPending,
    ingestError:   ingestMutation.error,
    docs,

    ask:           askMutation.mutate,
    answer:        askMutation.data?.answer,
    askPending:    askMutation.isPending,
    askError:      askMutation.error,
  };
}
