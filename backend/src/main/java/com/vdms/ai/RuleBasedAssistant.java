package com.vdms.ai;

import com.vdms.ai.AssistantDtos.AskResponse;
import com.vdms.ai.AssistantDtos.Mode;
import com.vdms.ai.AssistantDtos.ToolCallTrace;
import com.vdms.ai.FleetTools.DriverReport;
import com.vdms.ai.FleetTools.Slot;
import com.vdms.ai.FleetTools.VehicleReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic answering used when the LLM is disabled or unreachable. It spots a license plate,
 * model name or driver name in the question and reports from the same tools the LLM uses.
 */
@Component
public class RuleBasedAssistant {

    private static final Pattern AVAILABILITY = Pattern.compile("\\b(available|free|idle|unassigned|not assigned)\\b");
    private static final Pattern SUMMARY = Pattern.compile("\\b(summary|overview|how many|count|total|pending|requests?|on duty)\\b");
    private static final Pattern WHO = Pattern.compile("\\b(who|whom|driver of|driving the|drives the)\\b");
    private static final int MAX_LINES = 8;

    private final FleetTools tools;

    public RuleBasedAssistant(FleetTools tools) {
        this.tools = tools;
    }

    public AskResponse answer(String question, String notice) {
        String q = question.toLowerCase(Locale.ROOT);
        List<ToolCallTrace> trace = new ArrayList<>();
        boolean vehicleMentioned = !tools.matchVehicles(question).isEmpty();
        boolean driverMentioned = !tools.matchDrivers(question).isEmpty();

        String answer;
        if (vehicleMentioned && (!driverMentioned || WHO.matcher(q).find())) {
            trace.add(new ToolCallTrace("find_vehicle", question));
            answer = describeVehicles(tools.findVehicle(question, null).matches());
        } else if (driverMentioned) {
            trace.add(new ToolCallTrace("find_driver", question));
            answer = describeDrivers(tools.findDriver(question, null, null).matches());
        } else if (AVAILABILITY.matcher(q).find()) {
            trace.add(new ToolCallTrace("check_availability", "now"));
            answer = describeAvailability(tools.checkAvailability(null, null));
        } else if (SUMMARY.matcher(q).find()) {
            trace.add(new ToolCallTrace("fleet_summary", ""));
            answer = describeSummary(tools.fleetSummary());
        } else {
            answer = """
                    I couldn't find a vehicle, license plate or driver name in that question. Try for example:
                    • "Who is driving the Alto TS 09 AB 1234?"
                    • "What time is Ravi Kumar assigned?"
                    • "Which vehicles are available now?"
                    • "Give me a fleet summary\"""";
        }
        return new AskResponse(answer, Mode.RULE_BASED, null, trace, notice);
    }

    private String describeVehicles(List<VehicleReport> reports) {
        StringBuilder sb = new StringBuilder();
        for (VehicleReport r : reports) {
            if (!sb.isEmpty()) sb.append("\n\n");
            String name = r.vehicle().makeModel() + " (" + r.vehicle().licensePlate() + ")";
            Slot d = r.driverAtThatTime();
            if (d != null) {
                sb.append(name).append(" is being driven by ").append(d.driverName())
                        .append(" (phone ").append(d.driverPhone()).append(") — assigned ")
                        .append(d.start()).append(" to ").append(d.end()).append('.');
            } else {
                sb.append("Nobody is driving ").append(name).append(" as of ").append(r.checkedAt()).append('.');
                r.schedule().stream()
                        .filter(s -> s.status().name().equals("ACCEPTED") && s.timing().equals("UPCOMING"))
                        .findFirst()
                        .ifPresent(s -> sb.append(" Next confirmed driver: ").append(s.driverName())
                                .append(" (phone ").append(s.driverPhone()).append("), ")
                                .append(s.start()).append(" to ").append(s.end()).append('.'));
            }
            for (Slot p : r.pendingRequestsAtThatTime()) {
                sb.append("\nPending request (not yet accepted): ").append(p.driverName())
                        .append(", ").append(p.start()).append(" to ").append(p.end()).append('.');
            }
        }
        return sb.toString();
    }

    private String describeDrivers(List<DriverReport> reports) {
        StringBuilder sb = new StringBuilder();
        for (DriverReport r : reports) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append(r.driver().name()).append(" (phone ").append(r.driver().phone())
                    .append(", ").append(r.driver().email()).append(")");
            if (r.drivingNow() != null) {
                sb.append(" is currently driving ").append(r.drivingNow().vehicle())
                        .append(" (").append(r.drivingNow().licensePlate()).append(") until ")
                        .append(r.drivingNow().end()).append('.');
            } else {
                sb.append(" is not driving right now.");
            }
            if (r.assignments().isEmpty()) {
                sb.append("\nNo assignments on record.");
            } else {
                sb.append("\nAssignments:");
                r.assignments().stream().limit(MAX_LINES).forEach(s -> sb.append("\n• ")
                        .append(s.vehicle()).append(" (").append(s.licensePlate()).append("): ")
                        .append(s.start()).append(" → ").append(s.end())
                        .append(" [").append(s.status()).append(", ").append(s.timing().toLowerCase(Locale.ROOT)).append(']'));
            }
        }
        return sb.toString();
    }

    private String describeAvailability(FleetTools.Availability a) {
        StringBuilder sb = new StringBuilder("As of ").append(a.from()).append(":");
        sb.append("\nAvailable vehicles (").append(a.freeVehicles().size()).append("): ");
        sb.append(a.freeVehicles().isEmpty() ? "none" : String.join(", ",
                a.freeVehicles().stream().map(v -> v.makeModel() + " (" + v.licensePlate() + ")").toList()));
        sb.append("\nAvailable drivers (").append(a.freeDrivers().size()).append("): ");
        sb.append(a.freeDrivers().isEmpty() ? "none" : String.join(", ",
                a.freeDrivers().stream().map(FleetTools.DriverInfo::name).toList()));
        return sb.toString();
    }

    private String describeSummary(FleetTools.FleetSummary s) {
        StringBuilder sb = new StringBuilder("Fleet summary as of ").append(s.now()).append(":\n")
                .append("• ").append(s.drivers()).append(" drivers, ").append(s.vehicles()).append(" vehicles\n")
                .append("• ").append(s.acceptedAssignments()).append(" accepted assignments, ")
                .append(s.pendingRequests()).append(" pending requests, ")
                .append(s.rejectedRequests()).append(" rejected");
        if (s.onDutyNow().isEmpty()) {
            sb.append("\n• Nobody is on duty right now.");
        } else {
            sb.append("\nOn duty now:");
            s.onDutyNow().stream().limit(MAX_LINES).forEach(slot -> sb.append("\n• ").append(slot.driverName())
                    .append(" — ").append(slot.vehicle()).append(" (").append(slot.licensePlate())
                    .append(") until ").append(slot.end()));
        }
        return sb.toString();
    }
}
