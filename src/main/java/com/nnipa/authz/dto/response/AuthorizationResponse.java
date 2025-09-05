package com.nnipa.authz.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for authorization checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {

    private boolean allowed;

    private String reason;

    private List<String> grantedPermissions;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static AuthorizationResponse allowed(String reason, List<String> permissions) {
        return AuthorizationResponse.builder()
                .allowed(true)
                .reason(reason)
                .grantedPermissions(permissions)
                .build();
    }

    public static AuthorizationResponse denied(String reason) {
        return AuthorizationResponse.builder()
                .allowed(false)
                .reason(reason)
                .build();
    }
}