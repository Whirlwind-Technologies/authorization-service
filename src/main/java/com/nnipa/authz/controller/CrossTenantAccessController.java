package com.nnipa.authz.controller;

import com.nnipa.authz.dto.request.CrossTenantAccessRequest;
import com.nnipa.authz.dto.response.CrossTenantAccessResponse;
import com.nnipa.authz.service.CrossTenantAccessService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/cross-tenant")
@RequiredArgsConstructor
@Tag(name = "Cross-Tenant Access", description = "Manage cross-tenant permissions")
public class CrossTenantAccessController {

    private final CrossTenantAccessService crossTenantAccessService;

    @PostMapping("/grant")
    @Operation(summary = "Grant cross-tenant access")
    @PreAuthorize("hasAuthority('CROSS_TENANT_MANAGE')")
    public ResponseEntity<CrossTenantAccessResponse> grantAccess(
            @Valid @RequestBody CrossTenantAccessRequest request) {
        log.info("Granting cross-tenant access");
        CrossTenantAccessResponse response = crossTenantAccessService.grantAccess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{accessId}")
    @Operation(summary = "Revoke cross-tenant access")
    @PreAuthorize("hasAuthority('CROSS_TENANT_MANAGE')")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable UUID accessId,
            @RequestParam String revokedBy) {
        log.info("Revoking cross-tenant access: {}", accessId);
        crossTenantAccessService.revokeAccess(accessId, revokedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "List cross-tenant accesses for a tenant")
    @PreAuthorize("hasAuthority('CROSS_TENANT_VIEW')")
    public ResponseEntity<List<CrossTenantAccessResponse>> listTenantAccesses(
            @PathVariable UUID tenantId) {
        List<CrossTenantAccessResponse> accesses =
                crossTenantAccessService.listTenantAccesses(tenantId);
        return ResponseEntity.ok(accesses);
    }
}