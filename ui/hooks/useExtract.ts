"use client";
import { useMutation } from "@tanstack/react-query";
import { postExtractPerson, postExtractProduct } from "@/lib/api";

export function useExtract() {
  const personMutation = useMutation({
    mutationFn: (text: string) => postExtractPerson(text),
  });

  const productMutation = useMutation({
    mutationFn: (text: string) => postExtractProduct(text),
  });

  return {
    extractPerson:  personMutation.mutate,
    personData:     personMutation.data,
    personPending:  personMutation.isPending,
    personError:    personMutation.error,

    extractProduct: productMutation.mutate,
    productData:    productMutation.data,
    productPending: productMutation.isPending,
    productError:   productMutation.error,

    reset: () => { personMutation.reset(); productMutation.reset(); },
  };
}
