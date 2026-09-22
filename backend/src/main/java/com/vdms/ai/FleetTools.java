package com.vdms.ai;

import com.vdms.domain.Assignment;
import com.vdms.domain.AssignmentStatus;
import com.vdms.domain.Driver;
import com.vdms.domain.Vehicle;
import com.vdms.repository.AssignmentRepository;
import com.vdms.repository.DriverRepository;
import com.vdms.repository.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Read-only lookups over drivers, vehicles and assignments. These are the "tools" the LLM calls to
 * ground its answers in the database, and they also back the rule-based fallback.
 */
@Component
@Transactional(readOnly = true)
public class FleetTools {

    static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("EEE dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final int MAX_MATCHES = 5;
    private static final int MAX_SLOTS = 15;

    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final AssignmentRepository assignments;
    private final Clock clock;

    public FleetTools(DriverRepository drivers, VehicleRepository vehicles, AssignmentRepository assignments, Clock clock) {
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- result shapes

    public record DriverInfo(long id, String name, String phone, String email) {
        static DriverInfo of(Driver d) {
            return new DriverInfo(d.getId(), d.getName(), d.getPhone(), d.getEmail());
        }
    }

    public record VehicleInfo(long id, String makeModel, String licensePlate) {
        static VehicleInfo of(Vehicle v) {
            return new VehicleInfo(v.getId(), v.getMakeModel(), v.getLicensePlate());
        }
    }

    /** One assignment, with times pre-formatted and its timing relative to now (PAST / CURRENT / UPCOMING). */
    public record Slot(long assignmentId, String driverName, String driverPhone, String vehicle, String licensePlate,
                       String start, String end, AssignmentStatus status, String timing) {
    }

    public record VehicleReport(VehicleInfo vehicle, String checkedAt, Slot driverAtThatTime,
                                List<Slot> pendingRequestsAtThatTime, List<Slot> schedule) {
    }

    public record VehicleLookup(String query, List<VehicleReport> matches) {
    }

    public record DriverReport(DriverInfo driver, Slot drivingNow, List<Slot> assignments) {
    }

    public record DriverLookup(String query, List<DriverReport> matches) {
    }

    public record AssignmentList(String status, String date, List<Slot> assignments) {
    }

    public record Availability(String from, String to, List<VehicleInfo> freeVehicles, List<DriverInfo> freeDrivers,
                               List<Slot> busy) {
    }

    public record FleetSummary(String now, long drivers, long vehicles, long acceptedAssignments,
                               long pendingRequests, long rejectedRequests, List<Slot> onDutyNow) {
    }

    // ---------------------------------------------------------------- tools

    /** Vehicles matching a plate or make/model, with who drives them at {@code at} and their schedule. */
    public VehicleLookup findVehicle(String query, LocalDateTime at) {
        LocalDateTime now = now();
        LocalDateTime when = at == null ? now : at;
        List<VehicleReport> reports = matchVehicles(query).stream().limit(MAX_MATCHES).map(v -> {
            List<Assignment> all = assignments.findByVehicleOrderByStartTime(v);
            Slot driver = all.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.ACCEPTED && a.covers(when))
                    .findFirst().map(a -> slot(a, now)).orElse(null);
            List<Slot> pending = all.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.SENT && a.covers(when))
                    .map(a -> slot(a, now)).toList();
            return new VehicleReport(VehicleInfo.of(v), when.format(HUMAN), driver, pending, schedule(all, now));
        }).toList();
        return new VehicleLookup(query, reports);
    }

    /** Drivers matching a name/phone/email with their assignments, optionally filtered by status and day. */
    public DriverLookup findDriver(String query, AssignmentStatus status, LocalDate date) {
        LocalDateTime now = now();
        List<DriverReport> reports = matchDrivers(query).stream().limit(MAX_MATCHES).map(d -> {
            List<Assignment> all = assignments.findByDriverOrderByStartTime(d);
            Slot drivingNow = all.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.ACCEPTED && a.covers(now))
                    .findFirst().map(a -> slot(a, now)).orElse(null);
            List<Assignment> filtered = all.stream()
                    .filter(a -> status == null || a.getStatus() == status)
                    .filter(a -> date == null || onDay(a, date))
                    .toList();
            return new DriverReport(DriverInfo.of(d), drivingNow, schedule(filtered, now));
        }).toList();
        return new DriverLookup(query, reports);
    }

    public AssignmentList listAssignments(AssignmentStatus status, LocalDate date) {
        LocalDateTime now = now();
        Stream<Assignment> all = status == null
                ? assignments.findAll().stream().sorted(Comparator.comparing(Assignment::getStartTime))
                : assignments.findByStatusOrderByStartTime(status).stream();
        List<Assignment> filtered = all.filter(a -> date == null || onDay(a, date)).toList();
        return new AssignmentList(status == null ? "ANY" : status.name(), date == null ? "ANY" : date.toString(),
                schedule(filtered, now));
    }

    /** Vehicles and drivers with no ACCEPTED assignment overlapping [from, to]. */
    public Availability checkAvailability(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = now();
        LocalDateTime start = from == null ? now : from;
        LocalDateTime end = to == null || to.isBefore(start) ? start : to;
        List<Assignment> busy = assignments.findActiveBetween(start, end, AssignmentStatus.ACCEPTED);
        Set<Long> busyVehicles = busy.stream().map(a -> a.getVehicle().getId()).collect(Collectors.toSet());
        Set<Long> busyDrivers = busy.stream().map(a -> a.getDriver().getId()).collect(Collectors.toSet());
        return new Availability(start.format(HUMAN), end.format(HUMAN),
                vehicles.findAll().stream().filter(v -> !busyVehicles.contains(v.getId()))
                        .sorted(Comparator.comparing(Vehicle::getMakeModel)).map(VehicleInfo::of).toList(),
                drivers.findAll().stream().filter(d -> !busyDrivers.contains(d.getId()))
                        .sorted(Comparator.comparing(Driver::getName)).map(DriverInfo::of).toList(),
                busy.stream().map(a -> slot(a, now)).toList());
    }

    public FleetSummary fleetSummary() {
        LocalDateTime now = now();
        List<Assignment> all = assignments.findAll();
        return new FleetSummary(now.format(HUMAN), drivers.count(), vehicles.count(),
                count(all, AssignmentStatus.ACCEPTED), count(all, AssignmentStatus.SENT), count(all, AssignmentStatus.REJECTED),
                assignments.findActiveBetween(now, now, AssignmentStatus.ACCEPTED).stream().map(a -> slot(a, now)).toList());
    }

    // ---------------------------------------------------------------- matching

    /**
     * Plate match first (case/space/dash-insensitive, full or partial); otherwise matches words of
     * the make/model, so "alto" finds "Maruti Suzuki Alto". Works on a bare query or a whole sentence.
     */
    public List<Vehicle> matchVehicles(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        String qn = alnum(q);
        List<Vehicle> all = vehicles.findAll();
        List<Vehicle> byPlate = all.stream().filter(v -> {
            String plate = alnum(v.getLicensePlate());
            return qn.contains(plate) || (qn.length() >= 3 && plate.contains(qn));
        }).toList();
        if (!byPlate.isEmpty()) {
            return byPlate;
        }
        Set<String> queryWords = words(q);
        return all.stream().filter(v -> {
            String model = v.getMakeModel().toLowerCase(Locale.ROOT);
            return model.contains(q) || words(model).stream().anyMatch(queryWords::contains);
        }).sorted(Comparator.comparing(Vehicle::getMakeModel)).toList();
    }

    /** Matches drivers by full/partial name, a first/last name appearing in the query, email or phone. */
    public List<Driver> matchDrivers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        String digits = q.replaceAll("\\D", "");
        Set<String> queryWords = words(q);
        List<Driver> all = drivers.findAll();
        List<Driver> exact = all.stream().filter(d -> {
            String name = d.getName().toLowerCase(Locale.ROOT);
            String phone = d.getPhone().replaceAll("\\D", "");
            return name.contains(q) || q.contains(name)
                    || q.contains(d.getEmail().toLowerCase(Locale.ROOT))
                    || (digits.length() >= 4 && (phone.contains(digits) || digits.contains(phone)));
        }).toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return all.stream()
                .filter(d -> words(d.getName().toLowerCase(Locale.ROOT)).stream().anyMatch(queryWords::contains))
                .sorted(Comparator.comparing(Driver::getName)).toList();
    }

    // ---------------------------------------------------------------- helpers

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** Current and upcoming assignments first, then the most recent past ones. */
    private List<Slot> schedule(List<Assignment> list, LocalDateTime now) {
        List<Assignment> upcoming = list.stream().filter(a -> !a.getEndTime().isBefore(now))
                .sorted(Comparator.comparing(Assignment::getStartTime)).toList();
        List<Assignment> past = list.stream().filter(a -> a.getEndTime().isBefore(now))
                .sorted(Comparator.comparing(Assignment::getStartTime).reversed()).limit(3).toList();
        return Stream.concat(upcoming.stream(), past.stream()).limit(MAX_SLOTS).map(a -> slot(a, now)).toList();
    }

    static Slot slot(Assignment a, LocalDateTime now) {
        String timing = a.getEndTime().isBefore(now) ? "PAST" : a.getStartTime().isAfter(now) ? "UPCOMING" : "CURRENT";
        return new Slot(a.getId(), a.getDriver().getName(), a.getDriver().getPhone(),
                a.getVehicle().getMakeModel(), a.getVehicle().getLicensePlate(),
                a.getStartTime().format(HUMAN), a.getEndTime().format(HUMAN), a.getStatus(), timing);
    }

    private static boolean onDay(Assignment a, LocalDate day) {
        return !a.getStartTime().toLocalDate().isAfter(day) && !a.getEndTime().toLocalDate().isBefore(day);
    }

    private static long count(List<Assignment> all, AssignmentStatus status) {
        return all.stream().filter(a -> a.getStatus() == status).count();
    }

    private static String alnum(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static Set<String> words(String s) {
        return Arrays.stream(s.split("[^a-z0-9]+")).filter(w -> w.length() >= 3).collect(Collectors.toSet());
    }
}
