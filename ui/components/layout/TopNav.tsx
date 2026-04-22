"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/cn";

const NAV = [
  { href: "/chat",    label: "Chat",    icon: "💬" },
  { href: "/stream",  label: "Stream",  icon: "⚡" },
  { href: "/extract", label: "Extract", icon: "🔍" },
  { href: "/rag",     label: "RAG",     icon: "📚" },
  { href: "/tools",   label: "Tools",   icon: "🔧" },
];

export function TopNav() {
  const path = usePathname();

  return (
    <header
      className="fixed top-0 left-0 right-0 z-40 flex h-14 items-center justify-between border-b px-6"
      style={{
        background: "var(--surface)",
        borderColor: "var(--border)",
      }}
    >
      {/* Logo */}
      <span
        className="text-lg font-bold tracking-tight"
        style={{ background: "linear-gradient(135deg,var(--violet),var(--cyan))", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}
      >
        Spring AI Lab
      </span>

      {/* Nav links */}
      <nav className="flex gap-1">
        {NAV.map(({ href, label, icon }) => {
          const active = path.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                active
                  ? "text-white"
                  : "hover:text-white"
              )}
              style={
                active
                  ? { background: "linear-gradient(135deg,var(--violet),var(--indigo))", color: "#fff" }
                  : { color: "var(--muted)" }
              }
            >
              <span>{icon}</span>
              {label}
            </Link>
          );
        })}
      </nav>

      {/* API status */}
      <div className="flex items-center gap-2 text-xs" style={{ color: "var(--muted)" }}>
        <span
          className="h-2 w-2 rounded-full"
          style={{ background: "var(--green)", boxShadow: "0 0 6px var(--green)" }}
        />
        API
      </div>
    </header>
  );
}
