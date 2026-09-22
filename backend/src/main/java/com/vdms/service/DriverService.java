package com.vdms.service;

import com.vdms.domain.Assignment;
import com.vdms.domain.AssignmentStatus;
import com.vdms.domain.Driver;
import com.vdms.dto.AssignmentResponse;
import com.vdms.dto.DriverRequest;
import com.vdms.dto.DriverResponse;
import com.vdms.dto.NearbyDriverResponse;
import com.vdms.repository.AssignmentRepository;
import com.vdms.repository.DriverRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository drivers;
    private final AssignmentRepository assignments;
    private final PasswordEncoder passwordEncoder;

    public DriverService(DriverRepository drivers, AssignmentRepository assignments, PasswordEncoder passwordEncoder) {
        this.drivers = drivers;
        this.assignments = assignments;
        this.passwordEncoder = passwordEncoder;
    }

    public List<DriverResponse> findAll() {
        return drivers.findAll().stream()
                .sorted(Comparator.comparing(Driver::getName, String.CASE_INSENSITIVE_ORDER))
                .map(DriverResponse::from)
                .toList();
    }

    public DriverResponse findById(long id) {
        return DriverResponse.from(get(id));
    }

    public List<DriverResponse> search(SearchType type, String term) {
        return searchEntities(type, term).stream().map(DriverResponse::from).toList();
    }

    List<Driver> searchEntities(SearchType type, String term) {
        String t = term == null ? "" : term.trim();
        if (t.isEmpty()) {
            return List.of();
        }
        return switch (type) {
            case NAME -> drivers.findByNameContainingIgnoreCaseOrderByName(t);
            case PHONE -> drivers.findByPhoneContainingOrderByName(t);
        };
    }

    @Transactional
    public DriverResponse create(DriverRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        String email = req.email().trim().toLowerCase();
        if (drivers.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A driver with email " + email + " already exists");
        }
        if (drivers.existsByPhone(req.phone().trim())) {
            throw new ConflictException("A driver with phone " + req.phone() + " already exists");
        }
        Driver d = new Driver(req.name().trim(), email, req.phone().trim(),
                passwordEncoder.encode(req.password()), req.locationX(), req.locationY());
        return DriverResponse.from(drivers.save(d));
    }

    @Transactional
    public DriverResponse update(long id, DriverRequest req) {
        Driver d = get(id);
        String email = req.email().trim().toLowerCase();
        if (!d.getEmail().equalsIgnoreCase(email) && drivers.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A driver with email " + email + " already exists");
        }
        if (!d.getPhone().equals(req.phone().trim()) && drivers.existsByPhone(req.phone().trim())) {
            throw new ConflictException("A driver with phone " + req.phone() + " already exists");
        }
        d.setName(req.name().trim());
        d.setEmail(email);
        d.setPhone(req.phone().trim());
        d.setLocationX(req.locationX());
        d.setLocationY(req.locationY());
        if (req.password() != null && !req.password().isBlank()) {
            d.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return DriverResponse.from(d);
    }

    @Transactional
    public void delete(long id) {
        drivers.delete(get(id));
    }

    public DriverResponse login(String email, String password) {
        return drivers.findByEmailIgnoreCase(email.trim())
                .filter(d -> passwordEncoder.matches(password, d.getPasswordHash()))
                .map(DriverResponse::from)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
    }

    /**
     * Drivers within {@code range} of point (x, y) who are on an accepted assignment at {@code at},
     * nearest first. Mirrors the "nearby drivers" feature of the original application.
     */
    public List<NearbyDriverResponse> nearby(double x, double y, double range, LocalDateTime at) {
        List<NearbyDriverResponse> result = new ArrayList<>();
        List<Assignment> active = assignments.findActiveBetween(at, at, AssignmentStatus.ACCEPTED);
        for (Assignment a : active) {
            Driver d = a.getDriver();
            if (!d.hasLocation()) {
                continue;
            }
            double distance = Math.hypot(d.getLocationX() - x, d.getLocationY() - y);
            if (distance <= range && result.stream().noneMatch(r -> r.driver().id().equals(d.getId()))) {
                result.add(new NearbyDriverResponse(DriverResponse.from(d), distance, AssignmentResponse.from(a)));
            }
        }
        result.sort(Comparator.comparingDouble(NearbyDriverResponse::distance));
        return result;
    }

    Driver get(long id) {
        return drivers.findById(id).orElseThrow(() -> new NotFoundException("Driver " + id + " not found"));
    }
}
