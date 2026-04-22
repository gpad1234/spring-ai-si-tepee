"use client";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { postToolsChat } from "@/lib/api";

export interface ToolMessage {
  role: "user" | "assistant";
  content: string;
}

export function useTools() {
  const [messages, setMessages] = useState<ToolMessage[]>([]);

  const mutation = useMutation({
    mutationFn: (message: string) => postToolsChat(message),
    onSuccess: (data, message) => {
      setMessages((prev) => [
        ...prev,
        { role: "user",      content: message },
        { role: "assistant", content: data.response },
      ]);
    },
  });

  return {
    messages,
    send:      (m: string) => mutation.mutate(m),
    isPending: mutation.isPending,
    error:     mutation.error,
    clear:     () => setMessages([]),
  };
}
