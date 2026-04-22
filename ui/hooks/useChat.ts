"use client";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { postChat } from "@/lib/api";

export interface Message {
  role: "user" | "assistant";
  content: string;
}

export function useChat() {
  const [messages, setMessages] = useState<Message[]>([]);

  const mutation = useMutation({
    mutationFn: ({ message, systemPrompt }: { message: string; systemPrompt?: string }) =>
      postChat({ message, systemPrompt }),
    onSuccess: (data, variables) => {
      setMessages((prev) => [
        ...prev,
        { role: "user",      content: variables.message },
        { role: "assistant", content: data.response },
      ]);
    },
  });

  const send = (message: string, systemPrompt?: string) =>
    mutation.mutate({ message, systemPrompt });

  const clear = () => setMessages([]);

  return { messages, send, clear, isPending: mutation.isPending, error: mutation.error };
}
