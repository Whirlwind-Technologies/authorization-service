package com.nnipa.authz.controller;

import com.nnipa.authz.dto.request.CreateResourceRequest;
import com.nnipa.authz.dto.request.UpdateResourceRequest;
import com.nnipa.authz.dto.response.PolicyResponse;
import com.nnipa.authz.dto.response.ResourceResponse;
import com.nnipa.authz.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Resource management operations")
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @Operation(summary = "Create resource")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody CreateResourceRequest request) {
        log.info("Creating resource: {}", request.getResourceIdentifier());
        ResourceResponse response = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{resourceId}")
    @Operation(summary = "Update resource")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable UUID resourceId,
            @Valid @RequestBody UpdateResourceRequest request) {
        ResourceResponse response = resourceService.updateResource(resourceId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<ResourceResponse> getResource(@PathVariable UUID resourceId) {
        ResourceResponse response = resourceService.getResource(resourceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/identifier/{identifier}")
    @Operation(summary = "Get resource by identifier")
    public ResponseEntity<ResourceResponse> getResourceByIdentifier(
            @PathVariable String identifier) {
        ResourceResponse response = resourceService.getResourceByIdentifier(identifier);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get resources for tenant")
    public ResponseEntity<Page<ResourceResponse>> getTenantResources(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String resourceType,
            Pageable pageable) {
        Page<ResourceResponse> resources =
                resourceService.getTenantResources(tenantId, resourceType, pageable);
        return ResponseEntity.ok(resources);
    }

    @DeleteMapping("/{resourceId}")
    @Operation(summary = "Delete resource")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ResponseEntity<Void> deleteResource(@PathVariable UUID resourceId) {
        log.info("Deleting resource: {}", resourceId);
        resourceService.deleteResource(resourceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{resourceId}/policies/{policyId}")
    @Operation(summary = "Attach policy to resource")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ResponseEntity<Void> attachPolicy(
            @PathVariable UUID resourceId,
            @PathVariable UUID policyId) {
        resourceService.attachPolicy(resourceId, policyId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{resourceId}/policies/{policyId}")
    @Operation(summary = "Detach policy from resource")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ResponseEntity<Void> detachPolicy(
            @PathVariable UUID resourceId,
            @PathVariable UUID policyId) {
        resourceService.detachPolicy(resourceId, policyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{resourceId}/policies")
    @Operation(summary = "Get policies for resource")
    public ResponseEntity<List<PolicyResponse>> getResourcePolicies(
            @PathVariable UUID resourceId) {
        List<PolicyResponse> policies = resourceService.getResourcePolicies(resourceId);
        return ResponseEntity.ok(policies);
    }
}