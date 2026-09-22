package com.vdms.web;

import com.vdms.dto.DriverResponse;
import com.vdms.dto.LoginRequest;
import com.vdms.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DriverService driverService;

    public AuthController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/driver/login")
    public DriverResponse driverLogin(@Valid @RequestBody LoginRequest request) {
        return driverService.login(request.email(), request.password());
    }
}
