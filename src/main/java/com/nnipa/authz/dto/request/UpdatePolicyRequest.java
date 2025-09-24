package com.nnipa.authz.dto.request;

import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.enums.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePolicyRequest {
    private String name;
    private String description;
    private PolicyType policyType;
    private PolicyEffect effect;
    private Integer priority;
    private Map<String, Object> conditions;
    private Instant startDate;
    private Instant endDate;
}