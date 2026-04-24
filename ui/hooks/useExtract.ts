"use client";
import { useMutation } from "@tanstack/react-query";
import { postExtractPerson, postExtractSentiment } from "@/lib/api";

export function useExtract() {
  const personMutation = useMutation({
    mutationFn: (text: string) => postExtractPerson(text),
  });

  const sentimentMutation = useMutation({
    mutationFn: (text: string) => postExtractSentiment(text),
  });

  return {
    extractPerson:    personMutation.mutate,
    personData:       personMutation.data,
    personPending:    personMutation.isPending,
    personError:      personMutation.error,

    extractSentiment: sentimentMutation.mutate,
    sentimentData:    sentimentMutation.data,
    sentimentPending: sentimentMutation.isPending,
    sentimentError:   sentimentMutation.error,

    reset: () => { personMutation.reset(); sentimentMutation.reset(); },
  };
}
