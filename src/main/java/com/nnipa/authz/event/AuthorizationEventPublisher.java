package com.nnipa.authz.event;

import com.google.protobuf.Timestamp;
import com.nnipa.authz.dto.request.AuthorizationRequest;
import com.nnipa.proto.authz.*;
import com.nnipa.proto.common.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for authorization-related events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationEventPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.authorization-checked:nnipa.events.authz.checked}")
    private String authorizationCheckedTopic;

    @Value("${kafka.topics.role-created:nnipa.events.authz.role-created}")
    private String roleCreatedTopic;

    @Value("${kafka.topics.role-updated:nnipa.events.authz.role-updated}")
    private String roleUpdatedTopic;

    @Value("${kafka.topics.role-deleted:nnipa.events.authz.role-deleted}")
    private String roleDeletedTopic;

    @Value("${kafka.topics.role-assigned:nnipa.events.authz.role-assigned}")
    private String roleAssignedTopic;

    @Value("${kafka.topics.role-revoked:nnipa.events.authz.role-revoked}")
    private String roleRevokedTopic;

    @Value("${kafka.topics.permission-granted:nnipa.events.authz.permission-granted}")
    private String permissionGrantedTopic;

    @Value("${kafka.topics.permission-revoked:nnipa.events.authz.permission-revoked}")
    private String permissionRevokedTopic;

    @Value("${kafka.topics.policy-created:nnipa.events.authz.policy-created}")
    private String policyCreatedTopic;

    @Value("${kafka.topics.policy-evaluated:nnipa.events.authz.policy-evaluated}")
    private String policyEvaluatedTopic;

    /**
     * Publish authorization checked event.
     */
    public void publishAuthorizationEvent(AuthorizationRequest request) {
        try {
            AuthorizationCheckedEvent.Builder eventBuilder = AuthorizationCheckedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setUserId(request.getUserId().toString())
                    .setTenantId(request.getTenantId().toString())
                    .setResource(request.getResource())
                    .setAction(request.getAction())
                    .setCheckedAt(toProtobufTimestamp(Instant.now()));

            // Add optional fields
            if (request.getIpAddress() != null) {
                eventBuilder.setIpAddress(request.getIpAddress());
            }

            if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
                Map<String, String> context = convertToStringMap(request.getAttributes());
                eventBuilder.putAllContext(context);
            }

            AuthorizationCheckedEvent event = eventBuilder.build();

            sendEvent(authorizationCheckedTopic, request.getUserId().toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing authorization event", e);
        }
    }

    /**
     * Publish role created event.
     */
    public void publishRoleCreatedEvent(UUID roleId, UUID tenantId, String roleName,
                                        String description, String createdBy) {
        try {
            RoleCreatedEvent.Builder eventBuilder = RoleCreatedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setRoleId(roleId.toString())
                    .setTenantId(tenantId.toString())
                    .setRoleName(roleName)
                    .setCreatedAt(toProtobufTimestamp(Instant.now()));

            if (description != null) {
                eventBuilder.setDescription(description);
            }

            if (createdBy != null) {
                eventBuilder.setCreatedBy(createdBy);
            }

            RoleCreatedEvent event = eventBuilder.build();
            sendEvent(roleCreatedTopic, roleId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing role created event", e);
        }
    }

    /**
     * Publish role updated event.
     */
    public void publishRoleUpdatedEvent(UUID roleId, UUID tenantId, Map<String, String> changes,
                                        String updatedBy) {
        try {
            RoleUpdatedEvent.Builder eventBuilder = RoleUpdatedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setRoleId(roleId.toString())
                    .setTenantId(tenantId.toString())
                    .putAllChanges(changes)
                    .setUpdatedAt(toProtobufTimestamp(Instant.now()));

            if (updatedBy != null) {
                eventBuilder.setUpdatedBy(updatedBy);
            }

            RoleUpdatedEvent event = eventBuilder.build();
            sendEvent(roleUpdatedTopic, roleId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing role updated event", e);
        }
    }

    /**
     * Publish role deleted event.
     */
    public void publishRoleDeletedEvent(UUID roleId, UUID tenantId, String deletedBy) {
        try {
            RoleDeletedEvent.Builder eventBuilder = RoleDeletedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setRoleId(roleId.toString())
                    .setTenantId(tenantId.toString())
                    .setDeletedAt(toProtobufTimestamp(Instant.now()));

            if (deletedBy != null) {
                eventBuilder.setDeletedBy(deletedBy);
            }

            RoleDeletedEvent event = eventBuilder.build();
            sendEvent(roleDeletedTopic, roleId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing role deleted event", e);
        }
    }

    /**
     * Publish role assigned event.
     */
    public void publishRoleAssignedEvent(UUID userId, UUID roleId, UUID tenantId,
                                         String roleName, String assignedBy) {
        try {
            RoleAssignedEvent.Builder eventBuilder = RoleAssignedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setUserId(userId.toString())
                    .setRoleId(roleId.toString())
                    .setTenantId(tenantId.toString())
                    .setAssignedAt(toProtobufTimestamp(Instant.now()));

            if (roleName != null) {
                eventBuilder.setRoleName(roleName);
            }

            if (assignedBy != null) {
                eventBuilder.setAssignedBy(assignedBy);
            }

            RoleAssignedEvent event = eventBuilder.build();
            sendEvent(roleAssignedTopic, userId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing role assigned event", e);
        }
    }

    /**
     * Publish role revoked event.
     */
    public void publishRoleRevokedEvent(UUID userId, UUID roleId, UUID tenantId,
                                        String roleName, String revokedBy, String reason) {
        try {
            RoleRevokedEvent.Builder eventBuilder = RoleRevokedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setUserId(userId.toString())
                    .setRoleId(roleId.toString())
                    .setTenantId(tenantId.toString())
                    .setRevokedAt(toProtobufTimestamp(Instant.now()));

            if (roleName != null) {
                eventBuilder.setRoleName(roleName);
            }

            if (revokedBy != null) {
                eventBuilder.setRevokedBy(revokedBy);
            }

            if (reason != null) {
                eventBuilder.setReason(reason);
            }

            RoleRevokedEvent event = eventBuilder.build();
            sendEvent(roleRevokedTopic, userId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing role revoked event", e);
        }
    }

    /**
     * Publish permission granted event.
     */
    public void publishPermissionGrantedEvent(UUID userId, UUID tenantId, UUID roleId,
                                              UUID permissionId, String resource,
                                              String resourceType, String action, String grantedBy) {
        try {
            PermissionGrantedEvent.Builder eventBuilder = PermissionGrantedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setUserId(userId.toString())
                    .setTenantId(tenantId.toString())
                    .setRoleId(roleId.toString())
                    .setPermissionId(permissionId.toString())
                    .setResource(resource)
                    .setResourceType(resourceType)
                    .setAction(action)
                    .setGrantedAt(toProtobufTimestamp(Instant.now()));

            if (grantedBy != null) {
                eventBuilder.setGrantedBy(grantedBy);
            }

            PermissionGrantedEvent event = eventBuilder.build();
            sendEvent(permissionGrantedTopic, roleId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing permission granted event", e);
        }
    }

    /**
     * Publish permission revoked event.
     */
    public void publishPermissionRevokedEvent(UUID roleId, UUID permissionId, String revokedBy) {
        try {
            PermissionRevokedEvent.Builder eventBuilder = PermissionRevokedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setRoleId(roleId.toString())
                    .setPermissionId(permissionId.toString())
                    .setRevokedAt(toProtobufTimestamp(Instant.now()));

            if (revokedBy != null) {
                eventBuilder.setRevokedBy(revokedBy);
            }

            PermissionRevokedEvent event = eventBuilder.build();
            sendEvent(permissionRevokedTopic, roleId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing permission revoked event", e);
        }
    }

    /**
     * Publish policy created event.
     */
    public void publishPolicyCreatedEvent(UUID policyId, UUID tenantId, String policyName,
                                          String policyType, String effect, String createdBy) {
        try {
            PolicyCreatedEvent.Builder eventBuilder = PolicyCreatedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setPolicyId(policyId.toString())
                    .setTenantId(tenantId.toString())
                    .setPolicyName(policyName)
                    .setPolicyType(policyType)
                    .setEffect(effect)
                    .setCreatedAt(toProtobufTimestamp(Instant.now()));

            if (createdBy != null) {
                eventBuilder.setCreatedBy(createdBy);
            }

            PolicyCreatedEvent event = eventBuilder.build();
            sendEvent(policyCreatedTopic, policyId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing policy created event", e);
        }
    }

    /**
     * Publish policy evaluated event.
     */
    public void publishPolicyEvaluatedEvent(UUID policyId, UUID userId,
                                            String resource, String action, String result) {
        try {
            PolicyEvaluatedEvent event = PolicyEvaluatedEvent.newBuilder()
                    .setMetadata(createEventMetadata())
                    .setPolicyId(policyId.toString())
                    .setUserId(userId.toString())
                    .setResource(resource)
                    .setAction(action)
                    .setResult(result)
                    .setEvaluatedAt(toProtobufTimestamp(Instant.now()))
                    .build();

            sendEvent(policyEvaluatedTopic, userId.toString(), event.toByteArray());
        } catch (Exception e) {
            log.error("Error publishing policy evaluated event", e);
        }
    }

    // ========== Helper Methods ==========

    /**
     * Send event to Kafka topic.
     */
    private void sendEvent(String topic, String key, byte[] event) {
        CompletableFuture<SendResult<String, byte[]>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic: {}", topic, ex);
            } else {
                log.debug("Event published successfully to topic: {} with key: {}", topic, key);
            }
        });
    }

    /**
     * Create event metadata.
     */
    private EventMetadata createEventMetadata() {
        return EventMetadata.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setSourceService("authorization-service")
                .setVersion("1.0")
                .setTimestamp(toProtobufTimestamp(Instant.now()))
                .build();
    }

    /**
     * Convert Instant to Protobuf Timestamp.
     */
    private Timestamp toProtobufTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Convert Map<String, Object> to Map<String, String>.
     */
    private Map<String, String> convertToStringMap(Map<String, Object> objectMap) {
        Map<String, String> stringMap = new HashMap<>();
        objectMap.forEach((key, value) -> {
            if (value != null) {
                stringMap.put(key, value.toString());
            }
        });
        return stringMap;
    }
}