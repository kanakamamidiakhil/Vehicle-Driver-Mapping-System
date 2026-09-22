package com.vdms.repository;

import com.vdms.domain.Assignment;
import com.vdms.domain.AssignmentStatus;
import com.vdms.domain.Driver;
import com.vdms.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findAllByOrderByStartTimeDesc();

    List<Assignment> findByDriverOrderByStartTime(Driver driver);

    List<Assignment> findByDriverAndStatusInOrderByStartTime(Driver driver, Collection<AssignmentStatus> statuses);

    List<Assignment> findByDriverInAndStatusOrderByStartTime(Collection<Driver> drivers, AssignmentStatus status);

    List<Assignment> findByVehicleOrderByStartTime(Vehicle vehicle);

    List<Assignment> findByStatusOrderByStartTime(AssignmentStatus status);

    @Query("""
            select a from Assignment a
            where a.vehicle = :vehicle and a.status = :status
              and a.startTime < :end and a.endTime > :start and a.id <> :excludeId
            """)
    List<Assignment> findVehicleOverlaps(@Param("vehicle") Vehicle vehicle,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("status") AssignmentStatus status,
                                         @Param("excludeId") long excludeId);

    @Query("""
            select a from Assignment a
            where a.driver = :driver and a.status = :status
              and a.startTime < :end and a.endTime > :start and a.id <> :excludeId
            """)
    List<Assignment> findDriverOverlaps(@Param("driver") Driver driver,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("status") AssignmentStatus status,
                                        @Param("excludeId") long excludeId);

    /** Assignments with the given status that overlap the window [start, end]. */
    @Query("""
            select a from Assignment a
            where a.status = :status and a.startTime <= :end and a.endTime >= :start
            order by a.startTime
            """)
    List<Assignment> findActiveBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("status") AssignmentStatus status);
}
