package com.vdms.domain;

/** Lifecycle of an assignment request sent by the admin to a driver. */
public enum AssignmentStatus {
    /** Request sent to the driver, awaiting a response. */
    SENT,
    /** Driver accepted: the driver is scheduled to drive the vehicle in the time window. */
    ACCEPTED,
    /** Driver rejected the request. */
    REJECTED
}
