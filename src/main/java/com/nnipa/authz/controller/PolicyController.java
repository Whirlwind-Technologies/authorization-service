package com.nnipa.authz.controller;

import com.nnipa.authz.dto.request.CreatePolicyRequest;
import com.nnipa.authz.dto.request.PolicyEvaluationRequest;
import com.nnipa.authz.dto.request.UpdatePolicyRequest;
import com.nnipa.authz.dto.response.PolicyResponse;
import com.nnipa.authz.dto.response.PolicyEvaluationResponse;
import com.nnipa.authz.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Authorization policy management")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Create policy")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<PolicyResponse> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request) {
        log.info("Creating policy: {}", request.getName());
        PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{policyId}")
    @Operation(summary = "Update policy")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody UpdatePolicyRequest request) {
        log.info("Updating policy: {}", policyId);
        PolicyResponse response = policyService.updatePolicy(policyId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable UUID policyId) {
        PolicyResponse response = policyService.getPolicy(policyId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get policies for tenant")
    public ResponseEntity<Page<PolicyResponse>> getTenantPolicies(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String policyType,
            Pageable pageable) {
        Page<PolicyResponse> policies =
                policyService.getTenantPolicies(tenantId, policyType, pageable);
        return ResponseEntity.ok(policies);
    }

    @DeleteMapping("/{policyId}")
    @Operation(summary = "Delete policy")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<Void> deletePolicy(@PathVariable UUID policyId) {
        log.info("Deleting policy: {}", policyId);
        policyService.deletePolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{policyId}/activate")
    @Operation(summary = "Activate policy")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<PolicyResponse> activatePolicy(@PathVariable UUID policyId) {
        PolicyResponse response = policyService.activatePolicy(policyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{policyId}/deactivate")
    @Operation(summary = "Deactivate policy")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<PolicyResponse> deactivatePolicy(@PathVariable UUID policyId) {
        PolicyResponse response = policyService.deactivatePolicy(policyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate policy for testing")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<PolicyEvaluationResponse> evaluatePolicy(
            @RequestBody PolicyEvaluationRequest request) {
        PolicyEvaluationResponse response = policyService.evaluatePolicy(request);
        return ResponseEntity.ok(response);
    }
}