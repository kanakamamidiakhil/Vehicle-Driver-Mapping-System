package com.vdms.ai;

import com.vdms.TestClockConfig;
import com.vdms.ai.AssistantDtos.AskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestClockConfig.class)
class RuleBasedAssistantTest {

    @Autowired RuleBasedAssistant assistant;
    @Autowired FleetTools tools;

    @Test
    void whoIsDrivingTheAltoWithAGivenPlate() {
        AskResponse r = assistant.answer("Who is driving the alto of number ts09ab1234?", null);
        assertThat(r.answer())
                .contains("Maruti Suzuki Alto (TS 09 AB 1234)")
                .contains("Ravi Kumar")
                .contains("9876543210")
                .doesNotContain("TS 07 XY 9999");
    }

    @Test
    void theOtherAltoHasOnlyAPendingRequest() {
        AskResponse r = assistant.answer("who drives alto TS 07 XY 9999", null);
        assertThat(r.answer()).contains("Nobody is driving").doesNotContain("Ravi");
    }

    @Test
    void timeAssignedToADriver() {
        AskResponse r = assistant.answer("What is the time assigned to Priya?", null);
        assertThat(r.answer())
                .contains("Priya Sharma")
                .contains("currently driving Maruti Suzuki Swift")
                .contains("Tue 22 Sep 2026, 11:00 → Tue 22 Sep 2026, 17:00 [ACCEPTED, current]")
                .contains("Hyundai i20");
    }

    @Test
    void modelNameAloneListsEveryMatchingVehicle() {
        assertThat(tools.matchVehicles("alto")).hasSize(2);
        assertThat(tools.matchVehicles("TS-09-AB-1234")).hasSize(1);
        assertThat(tools.matchVehicles("1234")).hasSize(1);
    }

    @Test
    void availabilityAndSummary() {
        assertThat(assistant.answer("Which vehicles are available now?", null).answer())
                .contains("Available vehicles (3)")
                .doesNotContain("TS 09 AB 1234");
        assertThat(assistant.answer("give me a summary", null).answer())
                .contains("4 drivers, 5 vehicles")
                .contains("Ravi Kumar");
    }
}
