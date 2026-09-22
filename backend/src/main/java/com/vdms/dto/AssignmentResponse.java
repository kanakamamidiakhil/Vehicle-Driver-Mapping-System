package com.vdms.dto;

import com.vdms.domain.Assignment;
import com.vdms.domain.AssignmentStatus;

import java.time.LocalDateTime;

public record AssignmentResponse(
        Long id,
        Long driverId,
        String driverName,
        String driverPhone,
        Long vehicleId,
        String vehicleMakeModel,
        String licensePlate,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AssignmentStatus status) {

    public static AssignmentResponse from(Assignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getDriver().getId(), a.getDriver().getName(), a.getDriver().getPhone(),
                a.getVehicle().getId(), a.getVehicle().getMakeModel(), a.getVehicle().getLicensePlate(),
                a.getStartTime(), a.getEndTime(), a.getStatus());
    }
}
