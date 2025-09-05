package com.nnipa.authz.controller;

import com.nnipa.authz.dto.request.CreateRoleRequest;
import com.nnipa.authz.dto.request.UpdateRoleRequest;
import com.nnipa.authz.dto.request.AssignRoleRequest;
import com.nnipa.authz.dto.response.RoleResponse;
import com.nnipa.authz.service.RoleService;
import com.nnipa.authz.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for role management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management operations")
public class RoleController {

    private final RoleService roleService;
    private final UserRoleService userRoleService;

    @PostMapping
    @Operation(summary = "Create role", description = "Create a new role")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.info("Creating role: {}", request.getName());
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{roleId}")
    @Operation(summary = "Update role", description = "Update an existing role")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("Updating role: {}", roleId);
        RoleResponse response = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roleId}")
    @Operation(summary = "Get role", description = "Get role by ID")
    public ResponseEntity<RoleResponse> getRole(@PathVariable UUID roleId) {
        RoleResponse response = roleService.getRole(roleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get tenant roles", description = "Get all roles for a tenant")
    public ResponseEntity<List<RoleResponse>> getTenantRoles(@PathVariable UUID tenantId) {
        List<RoleResponse> responses = roleService.getTenantRoles(tenantId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{roleId}")
    @Operation(summary = "Delete role", description = "Delete a role")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        log.info("Deleting role: {}", roleId);
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign role", description = "Assign a role to a user")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Void> assignRole(@Valid @RequestBody AssignRoleRequest request) {
        log.info("Assigning role {} to user {}", request.getRoleId(), request.getUserId());
        userRoleService.assignRoleToUser(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/assign/{userId}/{roleId}")
    @Operation(summary = "Revoke role", description = "Revoke a role from a user")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Void> revokeRole(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @RequestParam UUID tenantId) {
        log.info("Revoking role {} from user {}", roleId, userId);
        userRoleService.revokeRoleFromUser(userId, roleId, tenantId);
        return ResponseEntity.noContent().build();
    }
}