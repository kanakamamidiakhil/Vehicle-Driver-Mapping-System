package com.vdms.dto;

public record NearbyDriverResponse(DriverResponse driver, double distance, AssignmentResponse activeAssignment) {
}
