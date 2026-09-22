package com.vdms.config;

import com.vdms.domain.Assignment;
import com.vdms.domain.AssignmentStatus;
import com.vdms.domain.Driver;
import com.vdms.domain.Vehicle;
import com.vdms.repository.AssignmentRepository;
import com.vdms.repository.DriverRepository;
import com.vdms.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Fills an empty database with a few drivers, vehicles and assignments (timed relative to now) so the
 * UI and the AI assistant have something to show. Enabled with {@code app.seed-demo-data=true}.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "driver123";

    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final AssignmentRepository assignments;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataSeeder(DriverRepository drivers, VehicleRepository vehicles, AssignmentRepository assignments,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (drivers.count() > 0 || vehicles.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        Driver ravi = drivers.save(new Driver("Ravi Kumar", "ravi@fleet.com", "9876543210", hash, 2.0, 3.0));
        Driver priya = drivers.save(new Driver("Priya Sharma", "priya@fleet.com", "9123456780", hash, 5.0, 1.0));
        Driver arjun = drivers.save(new Driver("Arjun Reddy", "arjun@fleet.com", "9988776655", hash, 8.0, 8.0));
        Driver sneha = drivers.save(new Driver("Sneha Patel", "sneha@fleet.com", "9000011111", hash, 1.0, 1.0));

        Vehicle alto = vehicles.save(new Vehicle("Maruti Suzuki Alto", "TS 09 AB 1234"));
        Vehicle alto2 = vehicles.save(new Vehicle("Maruti Suzuki Alto", "TS 07 XY 9999"));
        Vehicle swift = vehicles.save(new Vehicle("Maruti Suzuki Swift", "TS 10 CD 5678"));
        Vehicle i20 = vehicles.save(new Vehicle("Hyundai i20", "KA 01 EF 4321"));
        Vehicle innova = vehicles.save(new Vehicle("Toyota Innova Crysta", "MH 12 GH 8765"));

        LocalDateTime hour = LocalDateTime.now(clock).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime tomorrow = hour.toLocalDate().plusDays(1).atStartOfDay();
        LocalDateTime yesterday = hour.toLocalDate().minusDays(1).atStartOfDay();

        save(ravi, alto, hour.minusHours(2), hour.plusHours(6), AssignmentStatus.ACCEPTED);
        save(priya, swift, hour.minusHours(1), hour.plusHours(5), AssignmentStatus.ACCEPTED);
        save(arjun, innova, tomorrow.plusHours(6), tomorrow.plusHours(14), AssignmentStatus.ACCEPTED);
        save(sneha, alto2, tomorrow.plusHours(10), tomorrow.plusHours(18), AssignmentStatus.SENT);
        save(priya, i20, tomorrow.plusHours(18), tomorrow.plusHours(22), AssignmentStatus.SENT);
        save(arjun, alto, yesterday.plusHours(8), yesterday.plusHours(16), AssignmentStatus.ACCEPTED);
        save(sneha, swift, yesterday.plusHours(9), yesterday.plusHours(17), AssignmentStatus.REJECTED);
        log.info("Seeded demo data: 4 drivers (password '{}'), 5 vehicles, 7 assignments", DEMO_PASSWORD);
    }

    private void save(Driver d, Vehicle v, LocalDateTime start, LocalDateTime end, AssignmentStatus status) {
        Assignment a = new Assignment(d, v, start, end);
        a.setStatus(status);
        assignments.save(a);
    }
}
