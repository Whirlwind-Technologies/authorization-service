package com.nnipa.authz.controller;

import com.nnipa.authz.dto.request.CreatePermissionRequest;
import com.nnipa.authz.dto.response.PermissionResponse;
import com.nnipa.authz.service.PermissionService;
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
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Permission management operations")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "List all permissions")
    public ResponseEntity<Page<PermissionResponse>> listPermissions(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Boolean isSystem,
            Pageable pageable) {
        Page<PermissionResponse> permissions = permissionService.listPermissions(
                resourceType, isSystem, pageable);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{permissionId}")
    @Operation(summary = "Get permission by ID")
    public ResponseEntity<PermissionResponse> getPermission(@PathVariable UUID permissionId) {
        PermissionResponse response = permissionService.getPermission(permissionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resource-types")
    @Operation(summary = "Get all resource types")
    public ResponseEntity<List<String>> getResourceTypes() {
        List<String> resourceTypes = permissionService.getDistinctResourceTypes();
        return ResponseEntity.ok(resourceTypes);
    }

    @GetMapping("/actions")
    @Operation(summary = "Get all available actions")
    public ResponseEntity<List<String>> getActions() {
        List<String> actions = permissionService.getDistinctActions();
        return ResponseEntity.ok(actions);
    }

    @PostMapping("/role/{roleId}/assign")
    @Operation(summary = "Assign permissions to role")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<Void> assignPermissionsToRole(
            @PathVariable UUID roleId,
            @RequestBody List<UUID> permissionIds) {
        permissionService.assignPermissionsToRole(roleId, permissionIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/role/{roleId}/permission/{permissionId}")
    @Operation(summary = "Remove permission from role")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId) {
        permissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "Get permissions for a role")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(
            @PathVariable UUID roleId,
            @RequestParam(defaultValue = "false") boolean includeInherited) {
        List<PermissionResponse> permissions =
                permissionService.getRolePermissions(roleId, includeInherited);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/user/{userId}/tenant/{tenantId}")
    @Operation(summary = "Get all permissions for a user in a tenant")
    public ResponseEntity<List<PermissionResponse>> getUserPermissions(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        List<PermissionResponse> permissions =
                permissionService.getUserPermissions(userId, tenantId);
        return ResponseEntity.ok(permissions);
    }
}