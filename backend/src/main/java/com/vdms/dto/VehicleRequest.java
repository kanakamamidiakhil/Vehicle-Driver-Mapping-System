package com.vdms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank @Size(max = 255) String makeModel,
        @NotBlank @Size(max = 20) String licensePlate) {
}
