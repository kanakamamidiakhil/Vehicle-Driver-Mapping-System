package com.vdms.web;

import com.vdms.TestClockConfig;
import com.vdms.config.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@Transactional
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    /** Builds a JSON object from alternating keys and values. */
    private String json(String... keyValues) {
        Map<String, String> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put(keyValues[i], keyValues[i + 1]);
        }
        return mapper.writeValueAsString(body);
    }

    @Test
    void driverLoginChecksThePassword() throws Exception {
        mvc.perform(post("/api/auth/driver/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "RAVI@fleet.com", "password", DemoDataSeeder.DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ravi Kumar"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/driver/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "ravi@fleet.com", "password", DemoDataSeeder.DEMO_PASSWORD + "-x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDriverValidatesAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/drivers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"phone\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(3)));
        mvc.perform(post("/api/drivers").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Copy", "email", "ravi@fleet.com", "phone", "9999999999",
                                "password", DemoDataSeeder.DEMO_PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void searchAssignmentsByDriverNameAndStatus() throws Exception {
        mvc.perform(get("/api/assignments/search").param("type", "NAME").param("term", "priya").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vehicleMakeModel").value("Hyundai i20"));
    }

    @Test
    void assistantEndpointAnswersWithoutARunningModel() throws Exception {
        mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Who is driving the Alto TS 09 AB 1234?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("RULE_BASED"))
                .andExpect(jsonPath("$.toolCalls[*].tool", hasItem("find_vehicle")));
    }
}
