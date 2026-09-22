package com.vdms.service;

import com.vdms.domain.Vehicle;
import com.vdms.dto.VehicleRequest;
import com.vdms.dto.VehicleResponse;
import com.vdms.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicles;

    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    public List<VehicleResponse> findAll() {
        return vehicles.findAll().stream()
                .sorted(Comparator.comparing(Vehicle::getMakeModel, String.CASE_INSENSITIVE_ORDER))
                .map(VehicleResponse::from)
                .toList();
    }

    public VehicleResponse findById(long id) {
        return VehicleResponse.from(get(id));
    }

    @Transactional
    public VehicleResponse create(VehicleRequest req) {
        String plate = normalizePlate(req.licensePlate());
        if (vehicles.existsByLicensePlateIgnoreCase(plate)) {
            throw new ConflictException("A vehicle with license plate " + plate + " already exists");
        }
        return VehicleResponse.from(vehicles.save(new Vehicle(req.makeModel().trim(), plate)));
    }

    @Transactional
    public VehicleResponse update(long id, VehicleRequest req) {
        Vehicle v = get(id);
        String plate = normalizePlate(req.licensePlate());
        if (!v.getLicensePlate().equalsIgnoreCase(plate) && vehicles.existsByLicensePlateIgnoreCase(plate)) {
            throw new ConflictException("A vehicle with license plate " + plate + " already exists");
        }
        v.setMakeModel(req.makeModel().trim());
        v.setLicensePlate(plate);
        return VehicleResponse.from(v);
    }

    @Transactional
    public void delete(long id) {
        vehicles.delete(get(id));
    }

    Vehicle get(long id) {
        return vehicles.findById(id).orElseThrow(() -> new NotFoundException("Vehicle " + id + " not found"));
    }

    /** Upper-cases the plate and collapses repeated whitespace, e.g. "ts 09  ab 1234" -> "TS 09 AB 1234". */
    static String normalizePlate(String plate) {
        return plate.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}
