package com.nnipa.authz.event.consumer;

import com.nnipa.authz.service.TenantSyncService;
import com.nnipa.proto.tenant.TenantCreatedEvent;
import com.nnipa.proto.tenant.TenantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer for tenant-related events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantEventConsumer {

    private final TenantSyncService tenantSyncService;

    @KafkaListener(
            topics = "${kafka.topics.tenant-created:nnipa.events.tenant.created}",
            groupId = "authz-service-tenant-events"
    )
    public void handleTenantCreated(TenantCreatedEvent event) {
        log.info("Received tenant created event: {}", event.getTenant().getTenantId());

        try {
            // Create default roles for new tenant
            tenantSyncService.createDefaultRolesForTenant(
                    UUID.fromString(event.getTenant().getTenantId())
            );
        } catch (Exception e) {
            log.error("Error handling tenant created event", e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.tenant-deactivated:nnipa.events.tenant.deactivated}",
            groupId = "authz-service-tenant-events"
    )
    public void handleTenantDeactivated(TenantDeactivatedEvent event) {
        log.info("Received tenant deactivated event: {}", event.getTenantId());

        try {
            // Deactivate all roles and permissions for tenant
            tenantSyncService.deactivateTenantAuthorization(
                    UUID.fromString(event.getTenantId())
            );
        } catch (Exception e) {
            log.error("Error handling tenant deactivated event", e);
        }
    }
}