package com.vdms.ai;

import com.vdms.TestClockConfig;
import com.vdms.ai.AssistantDtos.AskResponse;
import com.vdms.ai.AssistantDtos.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/** Drives the tool-calling loop with a scripted LLM to check the wiring between model and database. */
@SpringBootTest
@Import({TestClockConfig.class, FleetAssistantTest.ScriptedLlmConfig.class})
class FleetAssistantTest {

    @Autowired FleetAssistant assistant;
    @Autowired ScriptedLlm llm;
    @Autowired ObjectMapper mapper;

    @BeforeEach
    void reset() {
        llm.requests.clear();
        llm.script.clear();
    }

    @Test
    void modelCallsFindVehicleAndAnswersFromTheToolResult() {
        llm.script.add(msgs -> toolCall("find_vehicle", "{\"query\":\"Alto TS09AB1234\"}"));
        llm.script.add(msgs -> {
            String toolResult = msgs.getLast().path("content").asString();
            assertThat(msgs.getLast().path("role").asString()).isEqualTo("tool");
            assertThat(toolResult).contains("Ravi Kumar").contains("TS 09 AB 1234").doesNotContain("TS 07 XY 9999");
            return answer("Ravi Kumar (9876543210) is driving the Alto TS 09 AB 1234 until 18:00.");
        });

        AskResponse r = assistant.ask("Who is driving the alto TS09AB1234?", List.of());

        assertThat(r.mode()).isEqualTo(Mode.LLM);
        assertThat(r.model()).isEqualTo("scripted");
        assertThat(r.answer()).contains("Ravi Kumar");
        assertThat(r.toolCalls()).singleElement().satisfies(t -> assertThat(t.tool()).isEqualTo("find_vehicle"));
        // First request carries the system prompt with the current time and the tool definitions.
        assertThat(llm.requests.getFirst().getFirst().path("content").asString()).contains("Tue 22 Sep 2026, 12:00");
        assertThat(llm.toolsOffered.getFirst()).isTrue();
    }

    @Test
    void toolCallPrintedAsTextByASmallModelIsStillExecuted() {
        llm.script.add(msgs -> answer("{\"name\": \"find_driver\", \"parameters\": {\"query\": \"Priya\", \"status\": \"ACCEPTED\"}}"));
        llm.script.add(msgs -> {
            assertThat(msgs.getLast().path("content").asString()).contains("Maruti Suzuki Swift").doesNotContain("Hyundai i20");
            return answer("Priya Sharma is assigned the Swift from 11:00 to 17:00 today.");
        });

        AskResponse r = assistant.ask("What is the time assigned to Priya?", List.of());

        assertThat(r.mode()).isEqualTo(Mode.LLM);
        assertThat(r.toolCalls()).extracting(AssistantDtos.ToolCallTrace::tool).containsExactly("find_driver");
    }

    @Test
    void badToolArgumentsAreReportedBackToTheModel() {
        llm.script.add(msgs -> toolCall("find_driver", "{}"));
        llm.script.add(msgs -> {
            assertThat(msgs.getLast().path("content").asString()).contains("'query' is required");
            return answer("Which driver do you mean?");
        });
        assertThat(assistant.ask("when is he working?", List.of()).answer()).isEqualTo("Which driver do you mean?");
    }

    @Test
    void fallsBackToRulesWhenTheModelFails() {
        llm.script.add(msgs -> { throw new IllegalStateException("connection refused"); });

        AskResponse r = assistant.ask("Who is driving the alto TS09AB1234?", List.of());

        assertThat(r.mode()).isEqualTo(Mode.RULE_BASED);
        assertThat(r.notice()).contains("could not be reached");
        assertThat(r.answer()).contains("Ravi Kumar");
    }

    private JsonNode toolCall(String name, String argsJson) {
        ObjectNode msg = mapper.createObjectNode().put("role", "assistant").put("content", "");
        msg.putArray("tool_calls").addObject().putObject("function")
                .put("name", name).set("arguments", mapper.readTree(argsJson));
        return msg;
    }

    private JsonNode answer(String text) {
        return mapper.createObjectNode().put("role", "assistant").put("content", text);
    }

    static class ScriptedLlm implements LlmClient {
        final List<Function<List<ObjectNode>, JsonNode>> script = new ArrayList<>();
        final List<List<ObjectNode>> requests = new ArrayList<>();
        final List<Boolean> toolsOffered = new ArrayList<>();

        @Override
        public JsonNode chat(List<ObjectNode> messages, ArrayNode tools) {
            requests.add(List.copyOf(messages));
            toolsOffered.add(tools != null && !tools.isEmpty());
            if (script.isEmpty()) {
                throw new IllegalStateException("script exhausted");
            }
            return script.removeFirst().apply(messages);
        }

        @Override
        public boolean isReachable() {
            return true;
        }

        @Override
        public String modelName() {
            return "scripted";
        }
    }

    @TestConfiguration
    static class ScriptedLlmConfig {
        @Bean
        @Primary
        ScriptedLlm scriptedLlm() {
            return new ScriptedLlm();
        }
    }
}
