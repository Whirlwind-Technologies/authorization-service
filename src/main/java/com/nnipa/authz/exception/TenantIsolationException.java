package com.nnipa.authz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when tenant isolation boundaries are violated.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class TenantIsolationException extends RuntimeException {

    public TenantIsolationException(String message) {
        super(message);
    }

    public TenantIsolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TenantIsolationException(UUID userId, UUID tenantId) {
        super(String.format("User %s does not have access to tenant %s", userId, tenantId));
    }

    public TenantIsolationException(UUID resourceId, UUID expectedTenantId, UUID actualTenantId) {
        super(String.format("Resource %s belongs to tenant %s, not tenant %s",
                resourceId, actualTenantId, expectedTenantId));
    }
}