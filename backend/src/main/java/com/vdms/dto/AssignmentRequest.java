package com.vdms.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AssignmentRequest(
        @NotNull Long driverId,
        @NotNull Long vehicleId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime) {
}
