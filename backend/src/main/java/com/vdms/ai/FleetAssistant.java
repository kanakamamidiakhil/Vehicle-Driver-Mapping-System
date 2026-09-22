package com.vdms.ai;

import com.vdms.ai.AssistantDtos.AskResponse;
import com.vdms.ai.AssistantDtos.ChatTurn;
import com.vdms.ai.AssistantDtos.Mode;
import com.vdms.ai.AssistantDtos.StatusResponse;
import com.vdms.ai.AssistantDtos.ToolCallTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Answers natural-language questions about the fleet. The LLM decides which database lookups
 * (tools) to run, reads their JSON results and writes the answer; it never sees or writes SQL.
 */
@Service
public class FleetAssistant {

    private static final Logger log = LoggerFactory.getLogger(FleetAssistant.class);
    private static final int MAX_HISTORY = 6;

    private static final String SYSTEM_PROMPT = """
            You are FleetBot, the assistant of a Vehicle Driver Mapping System. Admins and drivers ask you about \
            vehicles, drivers and assignments (which driver is scheduled to drive which vehicle, and when).
            Current date and time: %s. Use it to interpret "now", "today", "tomorrow" and to write ISO date-times.

            Rules:
            - ALWAYS call a tool to look up data before answering a question about vehicles, drivers or assignments. \
            Never invent names, phone numbers, license plates or times.
            - Vehicles may be named by model (e.g. "Alto", "Swift") and/or license plate. Pass what the user wrote \
            to find_vehicle; spacing and letter case in plates do not matter.
            - Assignment status: ACCEPTED = confirmed (the driver drives the vehicle in that window); SENT = request \
            waiting for the driver's reply; REJECTED = declined. Only ACCEPTED means someone is actually driving.
            - If a lookup returns no matches, say so plainly. If several vehicles or drivers match, list them briefly.
            - Reply concisely in plain text (short sentences or "•" bullets, no markdown tables). Include the \
            driver name, phone, vehicle, license plate and start/end times when relevant.""";

    private final LlmClient llm;
    private final ToolRegistry tools;
    private final RuleBasedAssistant fallback;
    private final FleetTools fleet;
    private final AiProperties props;
    private final ObjectMapper mapper;

    public FleetAssistant(LlmClient llm, ToolRegistry tools, RuleBasedAssistant fallback, FleetTools fleet,
                          AiProperties props, ObjectMapper mapper) {
        this.llm = llm;
        this.tools = tools;
        this.fallback = fallback;
        this.fleet = fleet;
        this.props = props;
        this.mapper = mapper;
    }

    public AskResponse ask(String question, List<ChatTurn> history) {
        if (!props.enabled()) {
            return fallback.answer(question, "LLM is disabled (app.ai.enabled=false); answered with built-in rules.");
        }
        try {
            return askLlm(question, history == null ? List.of() : history);
        } catch (RuntimeException e) {
            log.warn("LLM request failed, using rule-based fallback: {}", e.toString());
            return fallback.answer(question, "The language model (" + llm.modelName() + " at " + props.baseUrl()
                    + ") could not be reached, so this answer comes from the built-in rules.");
        }
    }

    public StatusResponse status() {
        return new StatusResponse(props.enabled(), "ollama", llm.modelName(), props.baseUrl(),
                props.enabled() && llm.isReachable());
    }

    private AskResponse askLlm(String question, List<ChatTurn> history) {
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(message("system", SYSTEM_PROMPT.formatted(fleet.now().format(FleetTools.HUMAN))));
        history.stream()
                .filter(t -> t.role().equals("user") || t.role().equals("assistant"))
                .skip(Math.max(0, history.size() - MAX_HISTORY))
                .forEach(t -> messages.add(message(t.role(), t.content())));
        messages.add(message("user", question));

        List<ToolCallTrace> trace = new ArrayList<>();
        for (int round = 0; round <= props.maxToolRounds(); round++) {
            boolean toolsAllowed = round < props.maxToolRounds();
            JsonNode reply = llm.chat(messages, toolsAllowed ? tools.definitions() : null);
            List<JsonNode> calls = toolCalls(reply);
            if (calls.isEmpty() || !toolsAllowed) {
                String content = reply.path("content").asString("").trim();
                if (content.isEmpty()) {
                    throw new IllegalStateException("Model returned an empty answer");
                }
                return new AskResponse(content, Mode.LLM, llm.modelName(), trace, null);
            }
            ObjectNode assistantMsg = message("assistant", reply.path("content").asString(""));
            assistantMsg.putArray("tool_calls").addAll(calls);
            messages.add(assistantMsg);
            for (JsonNode call : calls) {
                String name = call.path("function").path("name").asString("");
                JsonNode args = arguments(call.path("function").get("arguments"));
                trace.add(new ToolCallTrace(name, args.toString()));
                ObjectNode toolMsg = message("tool", tools.execute(name, args));
                toolMsg.put("tool_name", name);
                messages.add(toolMsg);
            }
        }
        throw new IllegalStateException("Tool-calling loop ended without an answer");
    }

    /**
     * Tool calls from the structured {@code tool_calls} field. Small local models sometimes print the
     * call as JSON text instead, so that form is recognised too.
     */
    private List<JsonNode> toolCalls(JsonNode reply) {
        List<JsonNode> calls = new ArrayList<>();
        reply.path("tool_calls").forEach(calls::add);
        if (calls.isEmpty()) {
            String content = reply.path("content").asString("").trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                try {
                    JsonNode parsed = mapper.readTree(content);
                    String name = parsed.path("name").asString("");
                    JsonNode args = parsed.has("parameters") ? parsed.get("parameters") : parsed.get("arguments");
                    if (!name.isEmpty() && args != null && isKnownTool(name)) {
                        ObjectNode call = mapper.createObjectNode();
                        call.putObject("function").put("name", name).set("arguments", args);
                        calls.add(call);
                    }
                } catch (JacksonException ignored) {
                    // plain answer that happens to look like JSON
                }
            }
        }
        return calls;
    }

    private boolean isKnownTool(String name) {
        for (JsonNode def : tools.definitions()) {
            if (def.path("function").path("name").asString("").equals(name.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** Arguments normally arrive as an object, but some models send a JSON string. */
    private JsonNode arguments(JsonNode raw) {
        if (raw == null || raw.isNull()) {
            return mapper.createObjectNode();
        }
        if (raw.isString()) {
            try {
                return mapper.readTree(raw.asString());
            } catch (JacksonException e) {
                return mapper.createObjectNode();
            }
        }
        return raw;
    }

    private ObjectNode message(String role, String content) {
        return mapper.createObjectNode().put("role", role).put("content", content);
    }
}
