package com.nnipa.authz.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for permission information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {

    private UUID id;

    private String resourceType;

    private String action;

    private String description;

    private String riskLevel;

    private boolean requiresMfa;

    private boolean requiresApproval;

    private boolean isSystem;

    private boolean isActive;

    private Instant createdAt;
}