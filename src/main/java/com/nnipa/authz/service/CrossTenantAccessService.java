package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.CrossTenantAccessRequest;
import com.nnipa.authz.dto.response.CrossTenantAccessResponse;
import com.nnipa.authz.entity.CrossTenantAccess;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.exception.ValidationException;
import com.nnipa.authz.repository.CrossTenantAccessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing cross-tenant access permissions.
 * Enables secure data sharing between tenants with proper authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossTenantAccessService {

    private final CrossTenantAccessRepository crossTenantAccessRepository;
    private final AuthorizationEventPublisher eventPublisher;

    /**
     * Grant cross-tenant access.
     */
    @Transactional
    @CacheEvict(value = "cross-tenant-access", allEntries = true)
    public CrossTenantAccessResponse grantAccess(CrossTenantAccessRequest request) {
        log.info("Granting cross-tenant access from {} to {} for resource {}",
                request.getSourceTenantId(), request.getTargetTenantId(), request.getResourceType());

        // Validate request
        validateCrossTenantRequest(request);

        // Check for existing access
        if (crossTenantAccessRepository.existsBySourceTenantIdAndTargetTenantIdAndResourceType(
                request.getSourceTenantId(),
                request.getTargetTenantId(),
                request.getResourceType())) {
            throw new ValidationException("Cross-tenant access already exists");
        }

        // Create cross-tenant access
        CrossTenantAccess access = CrossTenantAccess.builder()
                .sourceTenantId(request.getSourceTenantId())
                .targetTenantId(request.getTargetTenantId())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .permissions(request.getPermissions())
                .grantedBy(request.getGrantedBy())
                .grantedAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .conditions(request.getConditions())
                .isActive(true)
                .build();

        access = crossTenantAccessRepository.save(access);

        // Publish event
        eventPublisher.publishCrossTenantAccessGrantedEvent(
                access.getSourceTenantId(),
                access.getTargetTenantId(),
                access.getResourceType(),
                request.getGrantedBy()
        );

        return mapToResponse(access);
    }

    /**
     * Check if cross-tenant access is allowed.
     * Fixed method name from hassCrossTenantAccess to hasCrossTenantAccess
     */
    @Cacheable(value = "cross-tenant-access",
            key = "#sourceTenantId + ':' + #targetTenantId + ':' + #resourceType + ':' + #action")
    public boolean hasCrossTenantAccess(
            UUID sourceTenantId,
            UUID targetTenantId,
            String resourceType,
            String action) {

        List<CrossTenantAccess> accessList = crossTenantAccessRepository
                .findActiveAccessByTenants(sourceTenantId, targetTenantId, resourceType);

        return accessList.stream().anyMatch(access ->
                access.isActive() &&
                        (access.getExpiresAt() == null || access.getExpiresAt().isAfter(Instant.now())) &&
                        access.getPermissions().contains(action)
        );
    }

    /**
     * Revoke cross-tenant access.
     */
    @Transactional
    @CacheEvict(value = "cross-tenant-access", allEntries = true)
    public void revokeAccess(UUID accessId, String revokedBy) {
        log.info("Revoking cross-tenant access: {}", accessId);

        CrossTenantAccess access = crossTenantAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResourceNotFoundException("Cross-tenant access not found"));

        access.setActive(false);
        access.setRevokedBy(revokedBy);
        access.setRevokedAt(Instant.now());
        crossTenantAccessRepository.save(access);

        // Publish event
        eventPublisher.publishCrossTenantAccessRevokedEvent(
                access.getSourceTenantId(),
                access.getTargetTenantId(),
                access.getResourceType(),
                revokedBy
        );
    }

    /**
     * List all active cross-tenant accesses for a tenant.
     */
    @Transactional(readOnly = true)
    public List<CrossTenantAccessResponse> listTenantAccesses(UUID tenantId) {
        List<CrossTenantAccess> accesses = crossTenantAccessRepository
                .findActiveByTenant(tenantId);

        return accesses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateCrossTenantRequest(CrossTenantAccessRequest request) {
        if (request.getSourceTenantId().equals(request.getTargetTenantId())) {
            throw new ValidationException("Source and target tenant cannot be the same");
        }

        if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
            throw new ValidationException("At least one permission must be specified");
        }
    }

    private CrossTenantAccessResponse mapToResponse(CrossTenantAccess access) {
        return CrossTenantAccessResponse.builder()
                .id(access.getId())
                .sourceTenantId(access.getSourceTenantId())
                .targetTenantId(access.getTargetTenantId())
                .resourceType(access.getResourceType())
                .resourceId(access.getResourceId())
                .permissions(access.getPermissions())
                .grantedBy(access.getGrantedBy())
                .grantedAt(access.getGrantedAt())
                .expiresAt(access.getExpiresAt())
                .isActive(access.isActive())
                .build();
    }
}