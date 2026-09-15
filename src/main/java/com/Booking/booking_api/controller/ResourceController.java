package com.Booking.booking_api.controller;

import com.Booking.booking_api.dto.ResourceRequest;
import com.Booking.booking_api.entity.Resource;
import com.Booking.booking_api.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
@SecurityRequirement(name = "Bearer Authentication")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(summary = "View all bookable resources")
    @GetMapping
    public List<Resource> getAllResources() {
        return resourceService.getAllResources();
    }

    @Operation(summary = "View one resource")
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(@PathVariable Long id) {
        return resourceService.getResourceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a resource - ADMIN only")
    @PostMapping
    public ResponseEntity<Resource> createResource(
            @Valid @RequestBody ResourceRequest request) {

        Resource resource = new Resource();
        resource.setName(request.getName().trim());
        resource.setType(request.getType().trim());
        resource.setDescription(request.getDescription());
        resource.setAvailable(
                request.getAvailable() == null ? Boolean.TRUE : request.getAvailable());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.saveResource(resource));
    }

    @Operation(summary = "Update a resource - ADMIN only")
    @PutMapping("/{id}")
    public ResponseEntity<Resource> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request) {

        Resource existing = resourceService.getResourceById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Resource not found"));

        existing.setName(request.getName().trim());
        existing.setType(request.getType().trim());
        existing.setDescription(request.getDescription());
        existing.setAvailable(
                request.getAvailable() == null ? Boolean.TRUE : request.getAvailable());

        return ResponseEntity.ok(resourceService.saveResource(existing));
    }

    @Operation(summary = "Delete a resource - ADMIN only")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        if (resourceService.getResourceById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
