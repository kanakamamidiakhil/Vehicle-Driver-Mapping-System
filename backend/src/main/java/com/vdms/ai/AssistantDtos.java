package com.vdms.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request/response types of the assistant API. */
public final class AssistantDtos {

    private AssistantDtos() {
    }

    /** A previous turn of the conversation, so follow-ups like "and his phone number?" work. */
    public record ChatTurn(@NotBlank String role, @NotBlank @Size(max = 4000) String content) {
    }

    public record AskRequest(@NotBlank @Size(max = 1000) String question, @Valid List<ChatTurn> history) {
    }

    public enum Mode {
        /** Answer composed by the LLM from tool results. */
        LLM,
        /** Answer produced by the built-in rule-based responder (LLM disabled or unreachable). */
        RULE_BASED
    }

    public record ToolCallTrace(String tool, String arguments) {
    }

    public record AskResponse(String answer, Mode mode, String model, List<ToolCallTrace> toolCalls, String notice) {
    }

    public record StatusResponse(boolean enabled, String provider, String model, String baseUrl, boolean reachable) {
    }
}
