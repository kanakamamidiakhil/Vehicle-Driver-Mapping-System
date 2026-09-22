package com.vdms.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for the fleet assistant. The assistant talks to a self-hosted LLM served by
 * <a href="https://ollama.com">Ollama</a> (e.g. Llama 3.x, Qwen 2.5, Mistral) — the model runs
 * locally and no third-party chatbot API is involved.
 *
 * @param enabled       when false the assistant only uses the built-in rule-based answering
 * @param baseUrl       Ollama server URL
 * @param model         any Ollama model that supports tool calling
 * @param timeout       max time to wait for one model response
 * @param maxToolRounds how many rounds of tool calls the model may make before it must answer
 * @param temperature   sampling temperature; kept low so answers stick to the data
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        boolean enabled,
        String baseUrl,
        String model,
        Duration timeout,
        int maxToolRounds,
        double temperature) {

    public AiProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:11434";
        if (model == null || model.isBlank()) model = "llama3.2";
        if (timeout == null) timeout = Duration.ofSeconds(120);
        if (maxToolRounds <= 0) maxToolRounds = 4;
    }
}
