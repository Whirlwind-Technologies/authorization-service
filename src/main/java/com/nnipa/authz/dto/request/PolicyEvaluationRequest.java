package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationRequest {

    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    private UUID userId;
    private UUID tenantId;
    private String resource;
    private String action;
    private String resourceId;
    private Map<String, Object> attributes;

    // Convert to AuthorizationRequest for evaluation
    public AuthorizationRequest getAuthorizationRequest() {
        return AuthorizationRequest.builder()
                .userId(userId)
                .tenantId(tenantId)
                .resource(resource)
                .action(action)
                .resourceId(resourceId)
                .attributes(attributes)
                .build();
    }
}