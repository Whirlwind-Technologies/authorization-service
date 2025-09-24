package com.nnipa.authz.controller;

import com.nnipa.authz.dto.response.RoleResponse;
import com.nnipa.authz.dto.response.UserRoleResponse;
import com.nnipa.authz.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
@Tag(name = "User Roles", description = "User role assignment queries")
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping("/user/{userId}/tenant/{tenantId}")
    @Operation(summary = "Get user roles in tenant")
    public ResponseEntity<List<RoleResponse>> getUserRolesInTenant(
            @PathVariable UUID userId,
            @PathVariable UUID tenantId) {
        List<RoleResponse> roles = userRoleService.getUserRoles(userId, tenantId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/role/{roleId}/users")
    @Operation(summary = "Get users assigned to role")
    public ResponseEntity<List<UserRoleResponse>> getUsersWithRole(
            @PathVariable UUID roleId,
            @RequestParam UUID tenantId) {
        List<UserRoleResponse> users = userRoleService.getUsersWithRole(roleId, tenantId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/user/{userId}/all-tenants")
    @Operation(summary = "Get user roles across all tenants")
    public ResponseEntity<List<UserRoleResponse>> getUserRolesAllTenants(
            @PathVariable UUID userId) {
        List<UserRoleResponse> roles = userRoleService.getUserRolesAllTenants(userId);
        return ResponseEntity.ok(roles);
    }
}