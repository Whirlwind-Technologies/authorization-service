package com.nnipa.authz.event.consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.nnipa.authz.dto.request.AssignRoleRequest;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.repository.RoleRepository;
import com.nnipa.authz.service.TenantSyncService;
import com.nnipa.authz.service.UserRoleService;
import com.nnipa.proto.tenant.TenantCreatedEvent;
import com.nnipa.proto.tenant.TenantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer for tenant-related events with manual protobuf deserialization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantEventConsumer {

    private final TenantSyncService tenantSyncService;
    private final UserRoleService userRoleService;
    private final RoleRepository roleRepository;

    @KafkaListener(
            topics = "${kafka.topics.tenant-created:nnipa.events.tenant.created}",
            groupId = "authz-service-tenant-events"
    )
    public void handleTenantCreated(
            @Payload byte[] eventData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            Acknowledgment acknowledgment) {

        log.info("Received tenant created event: topic={}, partition={}, offset={}, key={}",
                topic, partition, offset, messageKey);

        try {
            // Deserialize the protobuf event
            TenantCreatedEvent event = TenantCreatedEvent.parseFrom(eventData);

            // Extract correlation ID and user ID from event metadata
            String correlationId = event.getMetadata().getCorrelationId();
            String userId = event.getMetadata().getUserId();
            UUID tenantId = UUID.fromString(event.getTenant().getTenantId());

            log.info("Parsed tenant created event: tenantId={}, tenantCode={}, userId={}, correlationId={}",
                    tenantId, event.getTenant().getTenantCode(), userId, correlationId);

            // Create default roles for new tenant - pass correlation ID for tracing
            tenantSyncService.createDefaultRolesForTenant(tenantId, correlationId);

            // Assign TENANT_ADMIN role to the user who created the tenant
            if (!userId.trim().isEmpty()) {
                assignTenantAdminRole(UUID.fromString(userId), tenantId, correlationId);
            } else {
                log.warn("No user ID found in tenant created event metadata for tenant: {}", tenantId);
            }

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed tenant created event: tenantId={}, correlationId={}",
                    tenantId, correlationId);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse protobuf message: topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, messageKey, e);

            // For parse errors, we should acknowledge to avoid infinite retry
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error handling tenant created event: topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, messageKey, e);

            // Don't acknowledge - this will cause the message to be retried
            // In production, you might want to implement dead letter queue logic here
            throw new RuntimeException("Failed to process tenant created event", e);
        }
    }

    /**
     * Assign TENANT_ADMIN role to the user who created the tenant.
     */
    private void assignTenantAdminRole(UUID userId, UUID tenantId, String correlationId) {
        try {
            log.info("Assigning TENANT_ADMIN role to user {} for tenant {} [correlationId: {}]",
                    userId, tenantId, correlationId);

            // Add correlation ID to MDC for tracing
            org.slf4j.MDC.put("correlationId", correlationId);

            // Find the TENANT_ADMIN role for this tenant
            Role tenantAdminRole = roleRepository.findByTenantIdAndNameAndIsActiveTrue(tenantId, "TENANT_ADMIN")
                    .orElseThrow(() -> new RuntimeException(
                            "TENANT_ADMIN role not found for tenant: " + tenantId));

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

            log.info("Successfully assigned TENANT_ADMIN role to user {} for tenant {} [correlationId: {}]",
                    userId, tenantId, correlationId);

        } catch (Exception e) {
            log.error("Failed to assign TENANT_ADMIN role to user {} for tenant {} [correlationId: {}]",
                    userId, tenantId, correlationId, e);
            throw new RuntimeException("Failed to assign TENANT_ADMIN role", e);
        } finally {
            // Clean up MDC
            org.slf4j.MDC.remove("correlationId");
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.tenant-deactivated:nnipa.events.tenant.deactivated}",
            groupId = "authz-service-tenant-events"
    )
    public void handleTenantDeactivated(
            @Payload byte[] eventData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            Acknowledgment acknowledgment) {

        log.info("Received tenant deactivated event: topic={}, partition={}, offset={}, key={}",
                topic, partition, offset, messageKey);

        try {
            // Deserialize the protobuf event
            TenantDeactivatedEvent event = TenantDeactivatedEvent.parseFrom(eventData);

            // Extract correlation ID from event metadata (assuming TenantDeactivatedEvent has metadata field)
            // Note: If TenantDeactivatedEvent doesn't have metadata, you might need to add it to the proto
            String correlationId = event.hasMetadata() ? event.getMetadata().getCorrelationId() : null;

            log.info("Parsed tenant deactivated event: tenantId={}, correlationId={}",
                    event.getTenantId(), correlationId);

            // Deactivate all roles and permissions for tenant - pass correlation ID for tracing
            tenantSyncService.deactivateTenantAuthorization(
                    UUID.fromString(event.getTenantId()),
                    correlationId
            );

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed tenant deactivated event: tenantId={}, correlationId={}",
                    event.getTenantId(), correlationId);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse protobuf message: topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, messageKey, e);

            // For parse errors, we should acknowledge to avoid infinite retry
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error handling tenant deactivated event: topic={}, partition={}, offset={}, key={}",
                    topic, partition, offset, messageKey, e);

            // Don't acknowledge - this will cause the message to be retried
            throw new RuntimeException("Failed to process tenant deactivated event", e);
        }
    }
}