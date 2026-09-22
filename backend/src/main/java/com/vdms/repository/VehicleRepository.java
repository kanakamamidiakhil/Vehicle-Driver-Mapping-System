package com.vdms.repository;

import com.vdms.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByLicensePlateIgnoreCase(String licensePlate);
}
