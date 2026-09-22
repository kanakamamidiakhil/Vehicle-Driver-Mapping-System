package com.vdms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "Maruti Suzuki Alto". */
    @Column(name = "make_model", nullable = false)
    private String makeModel;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    protected Vehicle() {
    }

    public Vehicle(String makeModel, String licensePlate) {
        this.makeModel = makeModel;
        this.licensePlate = licensePlate;
    }

    public Long getId() { return id; }
    public String getMakeModel() { return makeModel; }
    public void setMakeModel(String makeModel) { this.makeModel = makeModel; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
}
