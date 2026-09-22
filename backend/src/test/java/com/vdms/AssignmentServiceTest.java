package com.vdms;

import com.vdms.domain.AssignmentStatus;
import com.vdms.dto.AssignmentRequest;
import com.vdms.dto.AssignmentResponse;
import com.vdms.dto.DriverRequest;
import com.vdms.dto.DriverResponse;
import com.vdms.dto.VehicleRequest;
import com.vdms.dto.VehicleResponse;
import com.vdms.service.AssignmentService;
import com.vdms.service.ConflictException;
import com.vdms.service.DriverService;
import com.vdms.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestClockConfig.class)
@Transactional
class AssignmentServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2030, 1, 1, 9, 0);

    @Autowired AssignmentService assignments;
    @Autowired DriverService drivers;
    @Autowired VehicleService vehicles;

    @Test
    void acceptingAnOverlappingRequestForTheSameVehicleIsRejected() {
        VehicleResponse car = vehicles.create(new VehicleRequest("Tata Nexon", "ap 01 zz 0001"));
        DriverResponse a = driver("Anil", "anil@x.com", "7000000001");
        DriverResponse b = driver("Bala", "bala@x.com", "7000000002");

        AssignmentResponse first = assignments.create(new AssignmentRequest(a.id(), car.id(), T0, T0.plusHours(8)));
        AssignmentResponse second = assignments.create(new AssignmentRequest(b.id(), car.id(), T0.plusHours(4), T0.plusHours(10)));
        assertThat(assignments.accept(first.id()).status()).isEqualTo(AssignmentStatus.ACCEPTED);

        assertThatThrownBy(() -> assignments.accept(second.id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("AP 01 ZZ 0001");
        assertThatThrownBy(() -> assignments.create(new AssignmentRequest(b.id(), car.id(), T0.plusHours(1), T0.plusHours(2))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void driverCannotAcceptTwoOverlappingAssignments() {
        VehicleResponse car1 = vehicles.create(new VehicleRequest("Kia Seltos", "AP 02 ZZ 0002"));
        VehicleResponse car2 = vehicles.create(new VehicleRequest("Kia Sonet", "AP 03 ZZ 0003"));
        DriverResponse d = driver("Chitra", "chitra@x.com", "7000000003");

        AssignmentResponse x = assignments.create(new AssignmentRequest(d.id(), car1.id(), T0, T0.plusHours(8)));
        AssignmentResponse y = assignments.create(new AssignmentRequest(d.id(), car2.id(), T0.plusHours(2), T0.plusHours(3)));
        assignments.accept(x.id());

        assertThatThrownBy(() -> assignments.accept(y.id())).hasMessageContaining("already busy");
        assertThat(assignments.reject(y.id()).status()).isEqualTo(AssignmentStatus.REJECTED);
        assertThat(assignments.forDriver(d.id(), List.of(AssignmentStatus.SENT, AssignmentStatus.ACCEPTED)))
                .extracting(AssignmentResponse::id).containsExactly(x.id());
    }

    @Test
    void endMustBeAfterStart() {
        VehicleResponse car = vehicles.create(new VehicleRequest("Honda City", "AP 04 ZZ 0004"));
        DriverResponse d = driver("Deepa", "deepa@x.com", "7000000004");
        assertThatThrownBy(() -> assignments.create(new AssignmentRequest(d.id(), car.id(), T0, T0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nearbyFindsOnDutyDriversSortedByDistance() {
        // Seeded: Ravi at (2,3) and Priya at (5,1) are on accepted assignments at 12:00.
        var result = drivers.nearby(0, 0, 10, TestClockConfig.NOW);
        assertThat(result).extracting(r -> r.driver().name()).containsExactly("Ravi Kumar", "Priya Sharma");
        assertThat(drivers.nearby(0, 0, 4, TestClockConfig.NOW)).extracting(r -> r.driver().name())
                .containsExactly("Ravi Kumar");
    }

    private DriverResponse driver(String name, String email, String phone) {
        return drivers.create(new DriverRequest(name, email, phone, "secret1", 0.0, 0.0));
    }
}
