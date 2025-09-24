package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.CreatePolicyRequest;
import com.nnipa.authz.dto.request.PolicyEvaluationRequest;
import com.nnipa.authz.dto.request.UpdatePolicyRequest;
import com.nnipa.authz.dto.response.PolicyEvaluationResponse;
import com.nnipa.authz.dto.response.PolicyResponse;
import com.nnipa.authz.entity.Permission;
import com.nnipa.authz.entity.Policy;
import com.nnipa.authz.entity.Resource;
import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.enums.PolicyType;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.DuplicateResourceException;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.mapper.PolicyMapper;
import com.nnipa.authz.repository.PermissionRepository;
import com.nnipa.authz.repository.PolicyRepository;
import com.nnipa.authz.repository.ResourceRepository;
import com.nnipa.authz.service.PolicyEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing authorization policies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PermissionRepository permissionRepository;
    private final ResourceRepository resourceRepository;
    private final PolicyMapper policyMapper;
    private final PolicyEvaluationService policyEvaluationService;
    private final AuthorizationEventPublisher eventPublisher;

    /**
     * Create a new policy.
     */
    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        log.info("Creating policy: {} for tenant: {}", request.getName(), request.getTenantId());

        // Check for duplicate
        if (policyRepository.existsByNameAndTenantId(request.getName(), request.getTenantId())) {
            throw new DuplicateResourceException("Policy with name already exists: " + request.getName());
        }

        Policy policy = Policy.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .description(request.getDescription())
                .policyType(request.getPolicyType())
                .effect(request.getEffect())
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .conditions(request.getConditions())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .createdBy(request.getCreatedBy())
                .build();

        // Add permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(
                    permissionRepository.findAllById(request.getPermissionIds())
            );
            policy.setPermissions(permissions);
        }

        // Add resources if provided
        if (request.getResourceIds() != null && !request.getResourceIds().isEmpty()) {
            Set<Resource> resources = new HashSet<>(
                    resourceRepository.findAllById(request.getResourceIds())
            );
            policy.setResources(resources);
        }

        policy = policyRepository.save(policy);

        eventPublisher.publishPolicyCreatedEvent(
                policy.getId(), policy.getTenantId(), policy.getName(),
                policy.getPolicyType().name(), policy.getEffect().name(), request.getCreatedBy()
        );

        return policyMapper.toResponse(policy);
    }

    /**
     * Update an existing policy.
     */
    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public PolicyResponse updatePolicy(UUID policyId, UpdatePolicyRequest request) {
        log.info("Updating policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        if (request.getName() != null) {
            policy.setName(request.getName());
        }
        if (request.getDescription() != null) {
            policy.setDescription(request.getDescription());
        }
        if (request.getPolicyType() != null) {
            policy.setPolicyType(request.getPolicyType());
        }
        if (request.getEffect() != null) {
            policy.setEffect(request.getEffect());
        }
        if (request.getPriority() != null) {
            policy.setPriority(request.getPriority());
        }
        if (request.getConditions() != null) {
            policy.setConditions(request.getConditions());
        }
        if (request.getStartDate() != null) {
            policy.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            policy.setEndDate(request.getEndDate());
        }

        policy = policyRepository.save(policy);

        return policyMapper.toResponse(policy);
    }

    /**
     * Get policy by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "policies", key = "#policyId")
    public PolicyResponse getPolicy(UUID policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        return policyMapper.toResponse(policy);
    }

    /**
     * Get policies for a tenant with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<PolicyResponse> getTenantPolicies(UUID tenantId, String policyType, Pageable pageable) {
        log.debug("Getting policies for tenant: {}, type: {}", tenantId, policyType);

        Page<Policy> policies;
        if (policyType != null) {
            PolicyType type = PolicyType.valueOf(policyType.toUpperCase());
            policies = policyRepository.findWithFilters(tenantId, type, true, null, pageable);
        } else {
            policies = policyRepository.findByTenantId(tenantId, pageable);
        }

        return policies.map(policyMapper::toResponse);
    }

    /**
     * Delete a policy.
     */
    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public void deletePolicy(UUID policyId) {
        log.info("Deleting policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        policyRepository.delete(policy);
    }

    /**
     * Activate a policy.
     */
    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public PolicyResponse activatePolicy(UUID policyId) {
        log.info("Activating policy: {}", policyId);

        policyRepository.activatePolicy(policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        return policyMapper.toResponse(policy);
    }

    /**
     * Deactivate a policy.
     */
    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public PolicyResponse deactivatePolicy(UUID policyId) {
        log.info("Deactivating policy: {}", policyId);

        policyRepository.deactivatePolicy(policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        return policyMapper.toResponse(policy);
    }

    /**
     * Evaluate a policy for testing purposes.
     */
    @Transactional(readOnly = true)
    public PolicyEvaluationResponse evaluatePolicy(PolicyEvaluationRequest request) {
        log.debug("Evaluating policy: {} for testing", request.getPolicyId());

        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + request.getPolicyId()));

        Set<Permission> userPermissions = new HashSet<>();
        if (request.getUserId() != null && request.getTenantId() != null) {
            userPermissions = permissionRepository.findUserPermissions(
                    request.getUserId(), request.getTenantId()
            );
        }

        PolicyEffect effect = policyEvaluationService.evaluate(
                policy, request.getAuthorizationRequest(), userPermissions
        );

        eventPublisher.publishPolicyEvaluatedEvent(
                policy.getId(), request.getUserId(), request.getResource(),
                request.getAction(), effect != null ? effect.toString() : "NOT_APPLICABLE"
        );

        return PolicyEvaluationResponse.builder()
                .policyId(policy.getId())
                .policyName(policy.getName())
                .effect(effect)
                .evaluated(true)
                .reason(effect != null ? "Policy evaluated successfully" : "Policy not applicable")
                .evaluatedAt(Instant.now())
                .build();
    }
}