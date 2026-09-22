package com.vdms.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/** A chat-completion LLM that supports tool (function) calling. */
public interface LlmClient {

    /**
     * Sends the conversation and returns the assistant message, which has either {@code content}
     * or a {@code tool_calls} array of {@code {"function": {"name": ..., "arguments": {...}}}}.
     */
    JsonNode chat(List<ObjectNode> messages, ArrayNode tools);

    boolean isReachable();

    String modelName();
}
