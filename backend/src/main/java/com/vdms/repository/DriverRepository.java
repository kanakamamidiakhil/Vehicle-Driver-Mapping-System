package com.vdms.repository;

import com.vdms.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmailIgnoreCase(String email);

    List<Driver> findByNameContainingIgnoreCaseOrderByName(String name);

    List<Driver> findByPhoneContainingOrderByName(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}
