package com.vdms.service;

/** A request that is well-formed but clashes with existing data (duplicates, overlapping schedules). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
