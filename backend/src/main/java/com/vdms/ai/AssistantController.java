package com.vdms.ai;

import com.vdms.ai.AssistantDtos.AskRequest;
import com.vdms.ai.AssistantDtos.AskResponse;
import com.vdms.ai.AssistantDtos.StatusResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AssistantController {

    private final FleetAssistant assistant;

    public AssistantController(FleetAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return assistant.ask(request.question().trim(), request.history());
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return assistant.status();
    }
}
