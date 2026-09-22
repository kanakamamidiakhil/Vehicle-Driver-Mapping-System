package com.vdms.web;

import com.vdms.domain.AssignmentStatus;
import com.vdms.dto.AssignmentRequest;
import com.vdms.dto.AssignmentResponse;
import com.vdms.service.AssignmentService;
import com.vdms.service.SearchType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AssignmentResponse> list() {
        return assignmentService.findAll();
    }

    @GetMapping("/search")
    public List<AssignmentResponse> search(@RequestParam SearchType type,
                                           @RequestParam String term,
                                           @RequestParam(defaultValue = "ACCEPTED") AssignmentStatus status) {
        return assignmentService.search(type, term, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse create(@Valid @RequestBody AssignmentRequest request) {
        return assignmentService.create(request);
    }

    @PostMapping("/{id}/accept")
    public AssignmentResponse accept(@PathVariable long id) {
        return assignmentService.accept(id);
    }

    @PostMapping("/{id}/reject")
    public AssignmentResponse reject(@PathVariable long id) {
        return assignmentService.reject(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable long id) {
        assignmentService.delete(id);
    }
}
