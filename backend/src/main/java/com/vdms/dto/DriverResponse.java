package com.vdms.dto;

import com.vdms.domain.Driver;

public record DriverResponse(Long id, String name, String email, String phone, Double locationX, Double locationY) {

    public static DriverResponse from(Driver d) {
        return new DriverResponse(d.getId(), d.getName(), d.getEmail(), d.getPhone(), d.getLocationX(), d.getLocationY());
    }
}
