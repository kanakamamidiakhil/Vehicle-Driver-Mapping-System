package com.vdms;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Pins "now" to 22 Sep 2026 12:00 so the demo data (seeded relative to now) is deterministic. */
@TestConfiguration
public class TestClockConfig {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 22, 12, 0);

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }
}
