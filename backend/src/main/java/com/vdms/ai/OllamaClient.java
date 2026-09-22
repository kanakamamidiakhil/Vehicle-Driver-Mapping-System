package com.vdms.ai;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/** Minimal client for Ollama's native {@code /api/chat} endpoint with tool calling. */
@Component
public class OllamaClient implements LlmClient {

    private final RestClient http;
    private final ObjectMapper mapper;
    private final AiProperties props;

    public OllamaClient(RestClient.Builder builder, ObjectMapper mapper, AiProperties props) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(props.timeout());
        this.http = builder.baseUrl(props.baseUrl()).requestFactory(factory).build();
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public JsonNode chat(List<ObjectNode> messages, ArrayNode tools) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", props.model());
        body.put("stream", false);
        body.putArray("messages").addAll(messages);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
        }
        body.putObject("options").put("temperature", props.temperature());

        // Serialised up front so the request has a Content-Length rather than a chunked body.
        JsonNode response = http.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.writeValueAsString(body))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("message")) {
            throw new IllegalStateException("Empty response from Ollama");
        }
        return response.get("message");
    }

    @Override
    public boolean isReachable() {
        try {
            http.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String modelName() {
        return props.model();
    }
}
