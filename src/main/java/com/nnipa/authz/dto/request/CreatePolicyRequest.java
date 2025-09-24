package com.nnipa.authz.dto.request;

import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.enums.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePolicyRequest {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Policy name is required")
    private String name;

    private String description;

    @NotNull(message = "Policy type is required")
    private PolicyType policyType;

    @NotNull(message = "Policy effect is required")
    private PolicyEffect effect;

    private Integer priority;
    private Map<String, Object> conditions;
    private List<UUID> permissionIds;
    private List<UUID> resourceIds;
    private Instant startDate;
    private Instant endDate;
    private String createdBy;
}