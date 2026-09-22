package com.vdms.web;

import com.vdms.domain.AssignmentStatus;
import com.vdms.dto.AssignmentResponse;
import com.vdms.dto.DriverRequest;
import com.vdms.dto.DriverResponse;
import com.vdms.dto.NearbyDriverResponse;
import com.vdms.service.AssignmentService;
import com.vdms.service.DriverService;
import com.vdms.service.SearchType;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final AssignmentService assignmentService;

    public DriverController(DriverService driverService, AssignmentService assignmentService) {
        this.driverService = driverService;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<DriverResponse> list() {
        return driverService.findAll();
    }

    @GetMapping("/{id}")
    public DriverResponse get(@PathVariable long id) {
        return driverService.findById(id);
    }

    @GetMapping("/search")
    public List<DriverResponse> search(@RequestParam SearchType type, @RequestParam String term) {
        return driverService.search(type, term);
    }

    @GetMapping("/nearby")
    public List<NearbyDriverResponse> nearby(@RequestParam double x,
                                             @RequestParam double y,
                                             @RequestParam double range,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return driverService.nearby(x, y, range, at);
    }

    @GetMapping("/{id}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable long id,
                                                @RequestParam(required = false) List<AssignmentStatus> status) {
        return assignmentService.forDriver(id, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse create(@Valid @RequestBody DriverRequest request) {
        return driverService.create(request);
    }

    @PutMapping("/{id}")
    public DriverResponse update(@PathVariable long id, @Valid @RequestBody DriverRequest request) {
        return driverService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        driverService.delete(id);
    }
}
