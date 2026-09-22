package com.vdms.service;

import com.vdms.domain.Assignment;
import com.vdms.domain.AssignmentStatus;
import com.vdms.domain.Driver;
import com.vdms.dto.AssignmentRequest;
import com.vdms.dto.AssignmentResponse;
import com.vdms.repository.AssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final AssignmentRepository assignments;
    private final DriverService driverService;
    private final VehicleService vehicleService;

    public AssignmentService(AssignmentRepository assignments, DriverService driverService, VehicleService vehicleService) {
        this.assignments = assignments;
        this.driverService = driverService;
        this.vehicleService = vehicleService;
    }

    public List<AssignmentResponse> findAll() {
        return assignments.findAllByOrderByStartTimeDesc().stream().map(AssignmentResponse::from).toList();
    }

    public List<AssignmentResponse> forDriver(long driverId, Collection<AssignmentStatus> statuses) {
        Driver d = driverService.get(driverId);
        List<Assignment> list = statuses == null || statuses.isEmpty()
                ? assignments.findByDriverOrderByStartTime(d)
                : assignments.findByDriverAndStatusInOrderByStartTime(d, statuses);
        return list.stream().map(AssignmentResponse::from).toList();
    }

    /** Admin search: assignments of drivers matched by name/phone with the given status. */
    public List<AssignmentResponse> search(SearchType type, String term, AssignmentStatus status) {
        List<Driver> matched = driverService.searchEntities(type, term);
        if (matched.isEmpty()) {
            return List.of();
        }
        return assignments.findByDriverInAndStatusOrderByStartTime(matched, status).stream()
                .map(AssignmentResponse::from).toList();
    }

    /** Admin sends an assignment request to a driver; it starts in status SENT. */
    @Transactional
    public AssignmentResponse create(AssignmentRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        Assignment a = new Assignment(driverService.get(req.driverId()), vehicleService.get(req.vehicleId()),
                req.startTime(), req.endTime());
        ensureNoOverlap(a);
        return AssignmentResponse.from(assignments.save(a));
    }

    @Transactional
    public AssignmentResponse accept(long id) {
        Assignment a = get(id);
        if (a.getStatus() != AssignmentStatus.SENT) {
            throw new ConflictException("Only pending (SENT) requests can be accepted; this one is " + a.getStatus());
        }
        ensureNoOverlap(a);
        a.setStatus(AssignmentStatus.ACCEPTED);
        return AssignmentResponse.from(a);
    }

    @Transactional
    public AssignmentResponse reject(long id) {
        Assignment a = get(id);
        if (a.getStatus() != AssignmentStatus.SENT) {
            throw new ConflictException("Only pending (SENT) requests can be rejected; this one is " + a.getStatus());
        }
        a.setStatus(AssignmentStatus.REJECTED);
        return AssignmentResponse.from(a);
    }

    /** Unassign: removes the assignment entirely. */
    @Transactional
    public void delete(long id) {
        assignments.delete(get(id));
    }

    /**
     * Same rules as the original model: neither the vehicle nor the driver may already hold an
     * ACCEPTED assignment that overlaps this time window.
     */
    private void ensureNoOverlap(Assignment a) {
        long selfId = a.getId() == null ? -1 : a.getId();
        if (!assignments.findVehicleOverlaps(a.getVehicle(), a.getStartTime(), a.getEndTime(),
                AssignmentStatus.ACCEPTED, selfId).isEmpty()) {
            throw new ConflictException("Vehicle " + a.getVehicle().getLicensePlate()
                    + " is already assigned between " + window(a));
        }
        if (!assignments.findDriverOverlaps(a.getDriver(), a.getStartTime(), a.getEndTime(),
                AssignmentStatus.ACCEPTED, selfId).isEmpty()) {
            throw new ConflictException("Driver " + a.getDriver().getName() + " is already busy between " + window(a));
        }
    }

    private static String window(Assignment a) {
        return a.getStartTime().format(FMT) + " and " + a.getEndTime().format(FMT);
    }

    private Assignment get(long id) {
        return assignments.findById(id).orElseThrow(() -> new NotFoundException("Assignment " + id + " not found"));
    }
}
