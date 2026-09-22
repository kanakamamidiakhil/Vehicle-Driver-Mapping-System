package com.vdms.dto;

import com.vdms.domain.Vehicle;

public record VehicleResponse(Long id, String makeModel, String licensePlate) {

    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getMakeModel(), v.getLicensePlate());
    }
}
