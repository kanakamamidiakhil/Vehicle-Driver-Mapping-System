package com.vdms.ai;

import com.vdms.domain.AssignmentStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.Locale;

/** Declares the fleet tools in the JSON-schema format the LLM expects and executes the calls it makes. */
@Component
public class ToolRegistry {

    private final FleetTools tools;
    private final ObjectMapper mapper;
    private final ArrayNode definitions;

    public ToolRegistry(FleetTools tools, ObjectMapper mapper) {
        this.tools = tools;
        this.mapper = mapper;
        this.definitions = buildDefinitions();
    }

    public ArrayNode definitions() {
        return definitions;
    }

    /** Runs a tool and returns its JSON result; failures are returned as {"error": ...} for the model to read. */
    public String execute(String name, JsonNode args) {
        try {
            return mapper.writeValueAsString(dispatch(name, args == null ? mapper.createObjectNode() : args));
        } catch (RuntimeException e) {
            ObjectNode err = mapper.createObjectNode().put("error", e.getMessage());
            return mapper.writeValueAsString(err);
        }
    }

    private Object dispatch(String name, JsonNode args) {
        LocalDateTime now = tools.now();
        return switch (name) {
            case "find_vehicle" -> tools.findVehicle(required(args, "query"),
                    TimeParser.dateTime(text(args, "at"), now, now));
            case "find_driver" -> tools.findDriver(required(args, "query"), status(args),
                    TimeParser.date(text(args, "date"), now));
            case "list_assignments" -> tools.listAssignments(status(args), TimeParser.date(text(args, "date"), now));
            case "check_availability" -> tools.checkAvailability(
                    TimeParser.dateTime(text(args, "start"), now, now),
                    TimeParser.dateTime(text(args, "end"), now, null));
            case "fleet_summary" -> tools.fleetSummary();
            default -> throw new IllegalArgumentException("Unknown tool: " + name);
        };
    }

    private static String text(JsonNode args, String field) {
        JsonNode n = args.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String s = n.isString() ? n.asString() : n.toString();
        return s.isBlank() || s.equalsIgnoreCase("null") ? null : s;
    }

    private static String required(JsonNode args, String field) {
        String v = text(args, field);
        if (v == null) {
            throw new IllegalArgumentException("'" + field + "' is required");
        }
        return v;
    }

    private static AssignmentStatus status(JsonNode args) {
        String s = text(args, "status");
        if (s == null || s.equalsIgnoreCase("any") || s.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return AssignmentStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status must be one of SENT, ACCEPTED, REJECTED");
        }
    }

    // ---------------------------------------------------------------- JSON schema

    private ArrayNode buildDefinitions() {
        ArrayNode defs = mapper.createArrayNode();
        defs.add(tool("find_vehicle",
                "Look up vehicles by license plate (full or partial; spaces/case ignored) or by make/model name "
                        + "such as 'Alto' or 'Swift'. Returns who is driving each vehicle at the given time "
                        + "(default: now), pending requests, and its assignment schedule. Use for questions like "
                        + "'who is driving the Alto TS09AB1234?'.",
                new String[][]{
                        {"query", "License plate and/or make/model exactly as the user wrote it"},
                        {"at", "Optional ISO date-time (yyyy-MM-ddTHH:mm) to check instead of now"}},
                "query"));
        defs.add(tool("find_driver",
                "Look up drivers by name (full or partial), phone number or email. Returns contact details, "
                        + "the vehicle they are driving now (if any) and their assignments with start/end times. "
                        + "Use for questions like 'what time is Ravi assigned?' or 'which car does Priya drive?'.",
                new String[][]{
                        {"query", "Driver name, phone or email"},
                        {"status", "Optional filter: SENT, ACCEPTED or REJECTED"},
                        {"date", "Optional day filter yyyy-MM-dd (or 'today' / 'tomorrow')"}},
                "query"));
        defs.add(tool("list_assignments",
                "List assignments across the whole fleet, optionally filtered by status and/or day.",
                new String[][]{
                        {"status", "Optional filter: SENT, ACCEPTED or REJECTED"},
                        {"date", "Optional day filter yyyy-MM-dd (or 'today' / 'tomorrow')"}}));
        defs.add(tool("check_availability",
                "Find vehicles and drivers that are free (no accepted assignment) during a time window. "
                        + "Defaults to right now.",
                new String[][]{
                        {"start", "Optional window start, ISO date-time"},
                        {"end", "Optional window end, ISO date-time"}}));
        defs.add(tool("fleet_summary",
                "Overall counts of drivers, vehicles and assignments, plus who is on duty right now.",
                new String[][]{}));
        return defs;
    }

    private ObjectNode tool(String name, String description, String[][] params, String... required) {
        ObjectNode fn = mapper.createObjectNode();
        fn.put("name", name);
        fn.put("description", description);
        ObjectNode schema = fn.putObject("parameters");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        for (String[] p : params) {
            props.putObject(p[0]).put("type", "string").put("description", p[1]);
        }
        ArrayNode req = schema.putArray("required");
        for (String r : required) {
            req.add(r);
        }
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.put("type", "function");
        wrapper.set("function", fn);
        return wrapper;
    }
}
