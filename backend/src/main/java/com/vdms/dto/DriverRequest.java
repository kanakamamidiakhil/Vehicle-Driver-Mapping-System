package com.vdms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for registering or updating a driver. Password is optional on update. */
public record DriverRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{7,14}", message = "must be 7-14 digits, optionally starting with +") String phone,
        @Size(min = 4, max = 100) String password,
        Double locationX,
        Double locationY) {
}
