package com.example.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Pattern 5 — Tool Definitions
 *
 * <p>Tools are plain Spring beans with methods annotated {@link Tool}.
 * The model decides when to call them; Spring AI handles serialization.
 *
 * <p>Rules for tool methods:
 * <ul>
 *   <li>Methods must be {@code public} and return a value the model can reason about.</li>
 *   <li>Use {@link ToolParam} to describe each parameter — this feeds the model's
 *       tool-selection reasoning.</li>
 *   <li>Keep tools focused — one responsibility per tool method.</li>
 *   <li>Throw {@link ToolExecutionException} on errors; the model receives the message
 *       and can choose to retry or inform the user.</li>
 * </ul>
 *
 * <p>See docs/patterns/05-tool-calling.md for design rationale.
 */
@Component
public class BuiltInTools {

    // ─── Weather (demo stub — replace with real HTTP call) ────────────────────

    @Tool(description = "Get the current weather for a given city. Returns temperature in Celsius and conditions.")
    public WeatherResult getWeather(
            @ToolParam(description = "The city name, e.g. 'London' or 'Tokyo'") String city) {
        // In production: call a real weather API here
        return new WeatherResult(city, 22.0, "Partly cloudy", Instant.now().toString());
    }

    public record WeatherResult(String city, double temperatureCelsius, String conditions, String asOf) {}

    // ─── Date / Time ──────────────────────────────────────────────────────────

    @Tool(description = "Get the current date and time in ISO-8601 format.")
    public Map<String, String> getCurrentDateTime() {
        return Map.of("iso8601", Instant.now().toString());
    }

    // ─── Calculator ───────────────────────────────────────────────────────────

    @Tool(description = "Evaluate a simple arithmetic expression and return the numeric result.")
    public Map<String, Double> calculate(
            @ToolParam(description = "A numeric expression, e.g. '3 * (7 + 2)'") String expression) {
        // Simple eval — use a safe expression library (e.g. exp4j) in production
        // This is a stub; do not use JavaScript engine in production code
        throw new ToolExecutionException("Calculator not yet wired to an expression evaluator — replace this stub.");
    }

    // ─── Exception type ───────────────────────────────────────────────────────

    public static class ToolExecutionException extends RuntimeException {
        public ToolExecutionException(String message) { super(message); }
    }
}
