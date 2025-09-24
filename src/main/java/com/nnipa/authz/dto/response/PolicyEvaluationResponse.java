package com.nnipa.authz.dto.response;

import com.nnipa.authz.enums.PolicyEffect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyEvaluationResponse {
    private UUID policyId;
    private String policyName;
    private PolicyEffect effect;
    private Boolean evaluated;
    private String reason;
    private Instant evaluatedAt;
}