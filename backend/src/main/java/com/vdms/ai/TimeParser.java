package com.vdms.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Lenient parsing of the date/time strings an LLM puts into tool arguments. */
final class TimeParser {

    private static final List<DateTimeFormatter> DATE_TIMES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

    private TimeParser() {
    }

    /** Returns {@code fallback} when the value is blank or "now"; throws for unparseable values. */
    static LocalDateTime dateTime(String value, LocalDateTime now, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String v = value.trim();
        switch (v.toLowerCase()) {
            case "now", "current", "currently" -> { return now; }
            case "today" -> { return now.toLocalDate().atStartOfDay(); }
            case "tomorrow" -> { return now.toLocalDate().plusDays(1).atStartOfDay(); }
            default -> { }
        }
        String withoutZone = v.replaceAll("(Z|[+-]\\d{2}:?\\d{2})$", "");
        for (DateTimeFormatter f : DATE_TIMES) {
            try {
                return LocalDateTime.parse(withoutZone, f);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return date(v, now).atStartOfDay();
    }

    /** Parses yyyy-MM-dd, "today", "tomorrow", "yesterday" or the date part of a date-time; null when blank. */
    static LocalDate date(String value, LocalDateTime now) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().toLowerCase();
        return switch (v) {
            case "today", "now" -> now.toLocalDate();
            case "tomorrow" -> now.toLocalDate().plusDays(1);
            case "yesterday" -> now.toLocalDate().minusDays(1);
            default -> {
                try {
                    yield LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Unrecognised date '" + value + "', expected yyyy-MM-dd");
                }
            }
        };
    }
}
