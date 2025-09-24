package com.nnipa.authz.event.consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.nnipa.authz.dto.request.AssignRoleRequest;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.UserRole;
import com.nnipa.authz.repository.RoleRepository;
import com.nnipa.authz.repository.UserRoleRepository;
import com.nnipa.authz.service.TenantSyncService;
import com.nnipa.authz.service.UserRoleService;
import com.nnipa.proto.tenant.TenantCreatedEvent;
import com.nnipa.proto.tenant.TenantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Consumer for tenant-related events with comprehensive error handling.
 * Implements idempotent processing and graceful error recovery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantEventConsumer {

    private final TenantSyncService tenantSyncService;
    private final UserRoleService userRoleService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @KafkaListener(
            topics = "${kafka.topics.tenant-created:nnipa.events.tenant.created}",
            groupId = "authz-service-tenant-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTenantCreated(
            @Payload byte[] eventData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            Acknowledgment acknowledgment) {

        String correlationId = null;
        String tenantIdStr = null;
        String userIdStr = null;

        log.info("Received tenant created event: topic={}, partition={}, offset={}, key={}",
                topic, partition, offset, messageKey);

        try {
            // Deserialize the protobuf event
            TenantCreatedEvent event = TenantCreatedEvent.parseFrom(eventData);

            // Extract raw string values for logging
            correlationId = event.getMetadata().getCorrelationId();
            userIdStr = event.getMetadata().getUserId();
            tenantIdStr = event.getTenant().getTenantId();
            String tenantCode = event.getTenant().getTenantCode();

            log.info("Processing tenant created event: tenantId={}, tenantCode={}, userId={}, correlationId={}",
                    tenantIdStr, tenantCode, userIdStr, correlationId);

            // Set MDC for tracing throughout the processing
            org.slf4j.MDC.put("correlationId", correlationId != null ? correlationId : "unknown");
            org.slf4j.MDC.put("tenantId", tenantIdStr != null ? tenantIdStr : "unknown");

            // Early validation of UUID formats - fail fast on invalid data
            UUID tenantId;
            UUID userId = null;

            try {
                // Validate tenant ID (required)
                if (tenantIdStr == null || tenantIdStr.trim().isEmpty()) {
                    throw new NonRetryableException("Tenant ID is null or empty in event data");
                }
                tenantId = UUID.fromString(tenantIdStr.trim());

                // Validate user ID (optional but must be valid if present)
                if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                    userId = UUID.fromString(userIdStr.trim());
                }

            } catch (IllegalArgumentException e) {
                String errorMsg = String.format("Invalid UUID format in tenant created event - " +
                        "tenantId='%s', userId='%s'", tenantIdStr, userIdStr);
                log.error(errorMsg + " - acknowledging to prevent retry loop", e);
                acknowledgment.acknowledge();
                return;
            } catch (NonRetryableException e) {
                log.error("Validation failed for tenant created event: {} - acknowledging to prevent retry loop",
                        e.getMessage(), e);
                acknowledgment.acknowledge();
                return;
            }

            // Process tenant creation with validated UUIDs
            processTenantCreation(tenantId, userId, correlationId);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed tenant created event: tenantId={}, correlationId={}",
                    tenantId, correlationId);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse protobuf message - acknowledging to prevent retry loop: " +
                            "topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, messageKey, e);
            // Always acknowledge parse errors to prevent infinite retries
            acknowledgment.acknowledge();

        } catch (NonRetryableException e) {
            log.error("Non-retryable error processing tenant created event - acknowledging: " +
                    "tenantId={}, userId={}, correlationId={}", tenantIdStr, userIdStr, correlationId, e);
            // Acknowledge non-retryable errors to prevent infinite retries
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Retryable error processing tenant created event - will retry: " +
                    "tenantId={}, userId={}, correlationId={}", tenantIdStr, userIdStr, correlationId, e);
            // Don't acknowledge - let Kafka retry with backoff
            throw new RuntimeException("Failed to process tenant created event", e);

        } finally {
            // Always clean up MDC
            org.slf4j.MDC.clear();
        }
    }
    /**
     * Process tenant creation with idempotent operations and proper error classification.
     */
    private void processTenantCreation(UUID tenantId, UUID userId, String correlationId) {
        try {
            // Create default roles for new tenant (idempotent operation)
            createDefaultRolesIdempotent(tenantId, correlationId);

            // Assign TENANT_ADMIN role to the user who created the tenant
            if (userId != null) {
                assignTenantAdminRoleIdempotent(userId, tenantId, correlationId);
            } else {
                log.warn("No user ID found in tenant created event metadata for tenant: {} - " +
                        "skipping TENANT_ADMIN role assignment", tenantId);
            }

        } catch (Exception e) {
            log.error("Failed to process tenant creation for tenant: {}", tenantId, e);
            throw e; // Let the calling method handle retry logic
        }
    }

    /**
     * Create default roles with idempotent behavior - won't fail if roles already exist.
     */
    private void createDefaultRolesIdempotent(UUID tenantId, String correlationId) {
        try {
            tenantSyncService.createDefaultRolesForTenant(tenantId, correlationId);
            log.debug("Created default roles for tenant: {}", tenantId);

        } catch (DataIntegrityViolationException e) {
            // Check if this is a duplicate key constraint violation
            if (isDuplicateKeyError(e)) {
                log.info("Default roles already exist for tenant {} - continuing (idempotent behavior)", tenantId);
                // This is expected behavior for duplicate events - not an error
            } else {
                // Other data integrity issues should be retried
                log.warn("Data integrity violation creating roles for tenant {}: {}", tenantId, e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            log.error("Failed to create default roles for tenant {}", tenantId, e);
            throw e;
        }
    }

    /**
     * Assign TENANT_ADMIN role with idempotent behavior.
     */
    private void assignTenantAdminRoleIdempotent(UUID userId, UUID tenantId, String correlationId) {
        try {
            log.debug("Assigning TENANT_ADMIN role to user {} for tenant {}", userId, tenantId);

            // Find the TENANT_ADMIN role for this tenant
            Role tenantAdminRole = roleRepository.findByTenantIdAndNameAndIsActiveTrue(tenantId, "TENANT_ADMIN")
                    .orElseThrow(() -> new NonRetryableException(
                            "TENANT_ADMIN role not found for tenant: " + tenantId));

            // Check if role is already assigned (idempotent check)
            if (isRoleAlreadyAssigned(userId, tenantAdminRole.getId(), tenantId)) {
                log.info("TENANT_ADMIN role already assigned to user {} for tenant {} - continuing", userId, tenantId);
                return;
            }

            // Create role assignment request
            AssignRoleRequest request = AssignRoleRequest.builder()
                    .userId(userId)
                    .roleId(tenantAdminRole.getId())
                    .tenantId(tenantId)
                    .roleName("TENANT_ADMIN")
                    .assignedBy("SYSTEM")
                    .build();

            // Assign the role
            userRoleService.assignRoleToUser(request);

            log.info("Successfully assigned TENANT_ADMIN role to user {} for tenant {}", userId, tenantId);

        } catch (DataIntegrityViolationException e) {
            if (isDuplicateKeyError(e)) {
                log.info("TENANT_ADMIN role already assigned to user {} for tenant {} - continuing", userId, tenantId);
                // This is idempotent behavior - not an error
            } else {
                log.error("Data integrity violation assigning role to user {} for tenant {}", userId, tenantId, e);
                throw e;
            }
        } catch (NonRetryableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to assign TENANT_ADMIN role to user {} for tenant {}", userId, tenantId, e);
            throw new RuntimeException("Failed to assign TENANT_ADMIN role", e);
        }
    }

    /**
     * Check if a role is already assigned to a user.
     * This checks for active role assignments only.
     */
    private boolean isRoleAlreadyAssigned(UUID userId, UUID roleId, UUID tenantId) {
        try {
            // Find the specific assignment and check if it's active
            Optional<UserRole> existingAssignment = userRoleRepository
                    .findByUserIdAndRoleIdAndTenantId(userId, roleId, tenantId);

            return existingAssignment.isPresent() && existingAssignment.get().isActive();
        } catch (Exception e) {
            log.debug("Error checking role assignment - will proceed with assignment: {}", e.getMessage());
            return false;
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.tenant-deactivated:nnipa.events.tenant.deactivated}",
            groupId = "authz-service-tenant-events",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTenantDeactivated(
            @Payload byte[] eventData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            Acknowledgment acknowledgment) {

        String correlationId = null;
        String tenantId = null;

        log.info("Received tenant deactivated event: topic={}, partition={}, offset={}, key={}",
                topic, partition, offset, messageKey);

        try {
            // Deserialize the protobuf event
            TenantDeactivatedEvent event = TenantDeactivatedEvent.parseFrom(eventData);

            // Extract correlation ID and tenant ID
            correlationId = event.hasMetadata() ? event.getMetadata().getCorrelationId() : null;
            tenantId = event.getTenantId();

            log.info("Processing tenant deactivated event: tenantId={}, correlationId={}", tenantId, correlationId);

            // Set MDC for tracing
            org.slf4j.MDC.put("correlationId", correlationId);
            org.slf4j.MDC.put("tenantId", tenantId);

            // Process tenant deactivation
            processTenanDeactivation(tenantId, correlationId);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed tenant deactivated event: tenantId={}, correlationId={}",
                    tenantId, correlationId);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse protobuf message - acknowledging: topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, messageKey, e);
            acknowledgment.acknowledge();

        } catch (NonRetryableException e) {
            log.error("Non-retryable error processing tenant deactivated event - acknowledging: " +
                    "tenantId={}, correlationId={}", tenantId, correlationId, e);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Retryable error processing tenant deactivated event - will retry: " +
                    "tenantId={}, correlationId={}", tenantId, correlationId, e);
            throw new RuntimeException("Failed to process tenant deactivated event", e);

        } finally {
            org.slf4j.MDC.clear();
        }
    }

    /**
     * Process tenant deactivation with proper error handling.
     */
    private void processTenanDeactivation(String tenantIdStr, String correlationId) {
        try {
            UUID tenantId = UUID.fromString(tenantIdStr);

            // Deactivate all roles and permissions for tenant
            tenantSyncService.deactivateTenantAuthorization(tenantId, correlationId);

        } catch (IllegalArgumentException e) {
            throw new NonRetryableException("Invalid tenant ID format: " + tenantIdStr, e);
        }
    }

    /**
     * Check if the exception is a duplicate key constraint violation.
     */
    private boolean isDuplicateKeyError(DataIntegrityViolationException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("duplicate key") ||
                message.contains("unique constraint") ||
                message.contains("uk_role_name_tenant");
    }

    /**
     * Custom exception for non-retryable errors.
     */
    public static class NonRetryableException extends RuntimeException {
        public NonRetryableException(String message) {
            super(message);
        }

        public NonRetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}