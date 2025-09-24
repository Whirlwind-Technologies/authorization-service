package com.nnipa.authz.scheduler;

import com.nnipa.authz.repository.PolicyRepository;
import com.nnipa.authz.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "authz.cleanup.enabled", havingValue = "true")
public class AuthorizationCleanupScheduler {

    private final PolicyRepository policyRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Scheduled(cron = "${authz.cleanup.expired-policies.cron}")
    @Transactional
    public void cleanupExpiredPolicies() {
        log.info("Starting expired policies cleanup");

        var expiredPolicies = policyRepository.findExpiredPolicies(Instant.now());

        for (var policy : expiredPolicies) {
            policy.setActive(false);
            policyRepository.save(policy);
        }

        log.info("Deactivated {} expired policies", expiredPolicies.size());
    }

    @Scheduled(cron = "${authz.cleanup.expired-permissions.cron}")
    @Transactional
    public void cleanupExpiredPermissions() {
        log.info("Starting expired permissions cleanup");

        int deleted = rolePermissionRepository.deleteExpiredPermissions(Instant.now());

        log.info("Removed {} expired role-permission assignments", deleted);
    }
}