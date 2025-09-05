package com.nnipa.authz.controller;

import com.nnipa.authz.dto.request.AuthorizationRequest;
import com.nnipa.authz.dto.response.AuthorizationResponse;
import com.nnipa.authz.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authorization operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/authz")
@RequiredArgsConstructor
@Tag(name = "Authorization", description = "Authorization and permission checks")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @PostMapping("/check")
    @Operation(summary = "Check authorization",
            description = "Check if a user has permission to perform an action on a resource")
    public ResponseEntity<AuthorizationResponse> checkAuthorization(
            @Valid @RequestBody AuthorizationRequest request,
            @RequestHeader(value = "X-User-IP", required = false) String ipAddress,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        log.debug("Authorization check requested: {}", request);

        if (ipAddress != null) {
            request.setIpAddress(ipAddress);
        }
        if (userAgent != null) {
            request.setUserAgent(userAgent);
        }

        AuthorizationResponse response = authorizationService.authorize(request);

        return ResponseEntity.ok(response);
    }
}