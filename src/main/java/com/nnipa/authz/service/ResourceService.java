package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.CreateResourceRequest;
import com.nnipa.authz.dto.request.UpdateResourceRequest;
import com.nnipa.authz.dto.response.PolicyResponse;
import com.nnipa.authz.dto.response.ResourceResponse;
import com.nnipa.authz.entity.Policy;
import com.nnipa.authz.entity.Resource;
import com.nnipa.authz.enums.ResourceType;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.DuplicateResourceException;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.mapper.PolicyMapper;
import com.nnipa.authz.mapper.ResourceMapper;
import com.nnipa.authz.repository.PolicyRepository;
import com.nnipa.authz.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing protected resources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final PolicyRepository policyRepository;
    private final ResourceMapper resourceMapper;
    private final PolicyMapper policyMapper;
    private final AuthorizationEventPublisher eventPublisher;

    /**
     * Create a new protected resource.
     */
    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceResponse createResource(CreateResourceRequest request) {
        log.info("Creating resource: {} for tenant: {}",
                request.getResourceIdentifier(), request.getTenantId());

        // Check for duplicate
        if (resourceRepository.existsByResourceIdentifierAndTenantId(
                request.getResourceIdentifier(), request.getTenantId())) {
            throw new DuplicateResourceException(
                    "Resource with identifier already exists: " + request.getResourceIdentifier()
            );
        }

        Resource resource = Resource.builder()
                .tenantId(request.getTenantId())
                .resourceIdentifier(request.getResourceIdentifier())
                .resourceType(request.getResourceType())
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(request.getOwnerId())
                .attributes(request.getMetadata())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .createdBy(request.getCreatedBy())
                .build();

        // Set parent resource if provided
        if (request.getParentResourceId() != null) {
            Resource parent = resourceRepository.findById(request.getParentResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent resource not found: " + request.getParentResourceId()
                    ));
            resource.setParentResource(parent);
        }

        resource = resourceRepository.save(resource);

        log.info("Resource created successfully: {}", resource.getId());

        return resourceMapper.toResponse(resource);
    }

    /**
     * Update a resource.
     */
    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceResponse updateResource(UUID resourceId, UpdateResourceRequest request) {
        log.info("Updating resource: {}", resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (request.getName() != null) {
            resource.setName(request.getName());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getOwnerId() != null) {
            resource.setOwnerId(request.getOwnerId());
        }
        if (request.getMetadata() != null) {
            resource.setAttributes(request.getMetadata());
        }
        if (request.getIsPublic() != null) {
            resource.setPublic(request.getIsPublic());
        }

        resource = resourceRepository.save(resource);

        return resourceMapper.toResponse(resource);
    }

    /**
     * Get resource by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "resources", key = "#resourceId")
    public ResourceResponse getResource(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
        return resourceMapper.toResponse(resource);
    }

    /**
     * Get resource by identifier.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "resources", key = "#identifier")
    public ResourceResponse getResourceByIdentifier(String identifier) {
        Resource resource = resourceRepository.findByResourceIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with identifier: " + identifier
                ));
        return resourceMapper.toResponse(resource);
    }

    /**
     * Get resources for a tenant.
     */
    @Transactional(readOnly = true)
    public Page<ResourceResponse> getTenantResources(UUID tenantId, String resourceType, Pageable pageable) {
        log.debug("Getting resources for tenant: {}, type: {}", tenantId, resourceType);

        Page<Resource> resources;
        if (resourceType != null) {
            ResourceType type = ResourceType.valueOf(resourceType.toUpperCase());
            resources = resourceRepository.findByTenantIdAndResourceType(tenantId, type, pageable);
        } else {
            resources = resourceRepository.findByTenantId(tenantId, pageable);
        }

        return resources.map(resourceMapper::toResponse);
    }

    /**
     * Delete a resource.
     */
    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public void deleteResource(UUID resourceId) {
        log.info("Deleting resource: {}", resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        // Check for child resources
        List<Resource> children = resourceRepository.findByParentResourceId(resourceId);
        if (!children.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete resource with child resources. Delete children first."
            );
        }

        resourceRepository.delete(resource);
    }

    /**
     * Attach a policy to a resource.
     */
    @Transactional
    @CacheEvict(value = {"resources", "policies"}, allEntries = true)
    public void attachPolicy(UUID resourceId, UUID policyId) {
        log.info("Attaching policy {} to resource {}", policyId, resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        resource.getPolicies().add(policy);
        resourceRepository.save(resource);
    }

    /**
     * Detach a policy from a resource.
     */
    @Transactional
    @CacheEvict(value = {"resources", "policies"}, allEntries = true)
    public void detachPolicy(UUID resourceId, UUID policyId) {
        log.info("Detaching policy {} from resource {}", policyId, resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        resource.getPolicies().removeIf(p -> p.getId().equals(policyId));
        resourceRepository.save(resource);
    }

    /**
     * Get policies attached to a resource.
     */
    @Transactional(readOnly = true)
    public List<PolicyResponse> getResourcePolicies(UUID resourceId) {
        log.debug("Getting policies for resource: {}", resourceId);

        List<Policy> policies = policyRepository.findByResourceId(resourceId);

        return policies.stream()
                .map(policyMapper::toResponse)
                .collect(Collectors.toList());
    }
}