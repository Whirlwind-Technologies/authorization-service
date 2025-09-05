package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.AuthorizationRequest;
import com.nnipa.authz.entity.Permission;
import com.nnipa.authz.entity.Policy;
import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.enums.PolicyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for evaluating authorization policies.
 * Supports multiple policy types including Resource-Based, Identity-Based,
 * Attribute-Based (ABAC), Time-Based, and Conditional policies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * Evaluate a policy against an authorization request.
     * Returns ALLOW, DENY, or null (not applicable).
     *
     * @param policy The policy to evaluate
     * @param request The authorization request
     * @param userPermissions The user's current permissions
     * @return PolicyEffect (ALLOW/DENY) or null if policy doesn't apply
     */
    public PolicyEffect evaluate(
            Policy policy,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        log.debug("Evaluating policy: {} of type: {} for request: {}",
                policy.getName(), policy.getPolicyType(), request);

        try {
            // Check if policy is active and within date range
            if (!isPolicyActive(policy)) {
                log.debug("Policy {} is not active or outside date range", policy.getName());
                return null;
            }

            // Evaluate based on policy type
            PolicyEffect effect = switch (policy.getPolicyType()) {
                case RESOURCE_BASED -> evaluateResourceBasedPolicy(policy, request, userPermissions);
                case IDENTITY_BASED -> evaluateIdentityBasedPolicy(policy, request, userPermissions);
                case ATTRIBUTE_BASED -> evaluateAttributeBasedPolicy(policy, request);
                case TIME_BASED -> evaluateTimeBasedPolicy(policy, request);
                case CONDITIONAL -> evaluateConditionalPolicy(policy, request, userPermissions);
            };

            log.debug("Policy {} evaluation result: {}", policy.getName(), effect);
            return effect;

        } catch (Exception e) {
            log.error("Error evaluating policy: {}", policy.getName(), e);
            // In case of error, default to DENY for safety
            return PolicyEffect.DENY;
        }
    }

    /**
     * Batch evaluate multiple policies.
     *
     * @param policies List of policies to evaluate
     * @param request The authorization request
     * @param userPermissions The user's permissions
     * @return Final policy effect based on all policies
     */
    public PolicyEffect batchEvaluate(
            List<Policy> policies,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        // Sort policies by priority (higher priority first)
        List<Policy> sortedPolicies = policies.stream()
                .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
                .collect(Collectors.toList());

        boolean hasAllow = false;

        for (Policy policy : sortedPolicies) {
            PolicyEffect effect = evaluate(policy, request, userPermissions);

            // DENY takes immediate precedence
            if (effect == PolicyEffect.DENY) {
                log.debug("Policy {} returned DENY, stopping evaluation", policy.getName());
                return PolicyEffect.DENY;
            }

            if (effect == PolicyEffect.ALLOW) {
                hasAllow = true;
                // Continue checking for potential DENY in other policies
            }
        }

        return hasAllow ? PolicyEffect.ALLOW : PolicyEffect.DENY;
    }

    /**
     * Check if policy is currently active based on status and date range.
     */
    private boolean isPolicyActive(Policy policy) {
        if (!policy.isActive()) {
            return false;
        }

        Instant now = Instant.now();

        // Check start date
        if (policy.getStartDate() != null && policy.getStartDate().isAfter(now)) {
            log.debug("Policy {} has not started yet. Start date: {}",
                    policy.getName(), policy.getStartDate());
            return false;
        }

        // Check end date
        if (policy.getEndDate() != null && policy.getEndDate().isBefore(now)) {
            log.debug("Policy {} has expired. End date: {}",
                    policy.getName(), policy.getEndDate());
            return false;
        }

        return true;
    }

    /**
     * Evaluate resource-based policy.
     * Checks if user has required permissions for the resource.
     */
    private PolicyEffect evaluateResourceBasedPolicy(
            Policy policy,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        log.debug("Evaluating resource-based policy: {}", policy.getName());

        // Check if the policy applies to the requested resource
        boolean resourceMatches = policy.getResources().stream()
                .anyMatch(resource ->
                        resource.getResourceIdentifier().equals(request.getResourceId()) ||
                                resource.getResourceType().toString().equals(request.getResource())
                );

        if (!resourceMatches && request.getResourceId() != null) {
            log.debug("Policy {} does not apply to resource: {}",
                    policy.getName(), request.getResourceId());
            return null;
        }

        // Check if user has any of the policy's required permissions
        boolean hasRequiredPermission = policy.getPermissions().stream()
                .anyMatch(policyPerm -> userPermissions.stream()
                        .anyMatch(userPerm ->
                                userPerm.getResourceType().equals(policyPerm.getResourceType()) &&
                                        userPerm.getAction().equals(policyPerm.getAction()) &&
                                        userPerm.isActive()
                        )
                );

        if (!hasRequiredPermission) {
            log.debug("User lacks required permissions for policy: {}", policy.getName());
            return null;
        }

        // Evaluate additional conditions if present
        if (!policy.getConditions().isEmpty()) {
            if (!evaluateConditions(policy.getConditions(), request)) {
                log.debug("Policy conditions not met for: {}", policy.getName());
                return null;
            }
        }

        return policy.getEffect();
    }

    /**
     * Evaluate identity-based policy.
     * Checks if policy applies to the specific user/identity.
     */
    private PolicyEffect evaluateIdentityBasedPolicy(
            Policy policy,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        log.debug("Evaluating identity-based policy: {}", policy.getName());

        Map<String, Object> conditions = policy.getConditions();

        // Check if policy applies to this user
        if (conditions.containsKey("userId")) {
            String policyUserId = conditions.get("userId").toString();
            if (!policyUserId.equals(request.getUserId().toString())) {
                log.debug("Policy {} does not apply to user: {}",
                        policy.getName(), request.getUserId());
                return null;
            }
        }

        // Check if policy applies to user's groups
        if (conditions.containsKey("groups")) {
            @SuppressWarnings("unchecked")
            List<String> requiredGroups = (List<String>) conditions.get("groups");
            Map<String, Object> userAttributes = request.getAttributes();

            if (userAttributes.containsKey("groups")) {
                @SuppressWarnings("unchecked")
                List<String> userGroups = (List<String>) userAttributes.get("groups");
                boolean hasGroup = requiredGroups.stream()
                        .anyMatch(userGroups::contains);

                if (!hasGroup) {
                    log.debug("User not in required groups for policy: {}", policy.getName());
                    return null;
                }
            } else {
                return null;
            }
        }

        // Check if requested action matches policy permissions
        boolean actionMatches = policy.getPermissions().stream()
                .anyMatch(perm ->
                        perm.getResourceType().equals(request.getResource()) &&
                                perm.getAction().equals(request.getAction())
                );

        if (!actionMatches) {
            log.debug("Action does not match policy permissions");
            return null;
        }

        return policy.getEffect();
    }

    /**
     * Evaluate attribute-based policy (ABAC).
     * Uses SpEL expressions to evaluate complex attribute conditions.
     */
    private PolicyEffect evaluateAttributeBasedPolicy(
            Policy policy,
            AuthorizationRequest request) {

        log.debug("Evaluating attribute-based policy: {}", policy.getName());

        Map<String, Object> conditions = policy.getConditions();

        // Create evaluation context with all available data
        EvaluationContext context = createEvaluationContext(request, null);

        // Evaluate each condition
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String conditionName = condition.getKey();
            String expression = condition.getValue().toString();

            try {
                // Parse and evaluate the SpEL expression
                Expression exp = expressionParser.parseExpression(expression);
                Boolean result = exp.getValue(context, Boolean.class);

                log.debug("Condition '{}' with expression '{}' evaluated to: {}",
                        conditionName, expression, result);

                if (result == null || !result) {
                    log.debug("Condition '{}' failed for policy: {}",
                            conditionName, policy.getName());
                    return null;
                }
            } catch (Exception e) {
                log.error("Error evaluating ABAC condition '{}': {}", conditionName, expression, e);
                return null;
            }
        }

        log.debug("All conditions passed for policy: {}", policy.getName());
        return policy.getEffect();
    }

    /**
     * Evaluate time-based policy.
     * Checks if current time matches policy's time restrictions.
     */
    private PolicyEffect evaluateTimeBasedPolicy(
            Policy policy,
            AuthorizationRequest request) {

        log.debug("Evaluating time-based policy: {}", policy.getName());

        Map<String, Object> conditions = policy.getConditions();
        LocalDateTime now = LocalDateTime.now();

        // Check allowed hours (e.g., "09:00-17:00")
        if (conditions.containsKey("allowedHours")) {
            String allowedHours = conditions.get("allowedHours").toString();
            if (!isWithinAllowedHours(now, allowedHours)) {
                log.debug("Current time {} is outside allowed hours: {}", now, allowedHours);
                return null;
            }
        }

        // Check allowed days (e.g., "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
        if (conditions.containsKey("allowedDays")) {
            String allowedDays = conditions.get("allowedDays").toString();
            if (!isAllowedDay(now, allowedDays)) {
                log.debug("Current day {} is not in allowed days: {}",
                        now.getDayOfWeek(), allowedDays);
                return null;
            }
        }

        // Check timezone if specified
        if (conditions.containsKey("timezone")) {
            String timezone = conditions.get("timezone").toString();
            LocalDateTime tzTime = LocalDateTime.now(ZoneId.of(timezone));

            // Re-evaluate with timezone-specific time
            if (conditions.containsKey("allowedHours")) {
                String allowedHours = conditions.get("allowedHours").toString();
                if (!isWithinAllowedHours(tzTime, allowedHours)) {
                    log.debug("Timezone {} time {} is outside allowed hours",
                            timezone, tzTime);
                    return null;
                }
            }
        }

        // Check date range (e.g., for seasonal access)
        if (conditions.containsKey("dateRange")) {
            String dateRange = conditions.get("dateRange").toString();
            if (!isWithinDateRange(now, dateRange)) {
                log.debug("Current date is outside allowed range: {}", dateRange);
                return null;
            }
        }

        // Check if action matches the requested action
        if (conditions.containsKey("allowedActions")) {
            @SuppressWarnings("unchecked")
            List<String> allowedActions = (List<String>) conditions.get("allowedActions");
            if (!allowedActions.contains(request.getAction())) {
                log.debug("Action {} not in allowed actions: {}",
                        request.getAction(), allowedActions);
                return null;
            }
        }

        return policy.getEffect();
    }

    /**
     * Evaluate conditional policy with complex logic.
     * Supports complex SpEL expressions combining multiple conditions.
     */
    private PolicyEffect evaluateConditionalPolicy(
            Policy policy,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        log.debug("Evaluating conditional policy: {}", policy.getName());

        Map<String, Object> conditions = policy.getConditions();

        // Look for the main expression
        if (!conditions.containsKey("expression")) {
            log.error("Conditional policy {} missing 'expression' field", policy.getName());
            return null;
        }

        String expressionStr = conditions.get("expression").toString();

        try {
            // Create rich evaluation context
            EvaluationContext context = createEvaluationContext(request, userPermissions);

            // Add custom functions
            context.setVariable("hasPermission",
                    (java.util.function.BiPredicate<String, String>)
                            (resource, action) -> userPermissions.stream()
                                    .anyMatch(p -> p.getResourceType().equals(resource) &&
                                            p.getAction().equals(action))
            );

            context.setVariable("hasAnyPermission",
                    (java.util.function.Predicate<List<String>>)
                            (perms) -> perms.stream()
                                    .anyMatch(p -> {
                                        String[] parts = p.split(":");
                                        return userPermissions.stream()
                                                .anyMatch(perm -> perm.getResourceType().equals(parts[0]) &&
                                                        perm.getAction().equals(parts[1]));
                                    })
            );

            // Parse and evaluate the expression
            Expression expression = expressionParser.parseExpression(expressionStr);
            Boolean result = expression.getValue(context, Boolean.class);

            log.debug("Conditional expression '{}' evaluated to: {}", expressionStr, result);

            if (result != null && result) {
                return policy.getEffect();
            }

        } catch (Exception e) {
            log.error("Error evaluating conditional policy expression: {}", expressionStr, e);
        }

        return null;
    }

    /**
     * Evaluate simple key-value conditions.
     */
    private boolean evaluateConditions(Map<String, Object> conditions, AuthorizationRequest request) {
        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String key = condition.getKey();
            Object expectedValue = condition.getValue();

            // Get actual value from request
            Object actualValue = getValueFromRequest(key, request);

            // Handle different comparison types
            if (!compareValues(expectedValue, actualValue)) {
                log.debug("Condition failed: {} expected {} but got {}",
                        key, expectedValue, actualValue);
                return false;
            }
        }
        return true;
    }

    /**
     * Compare values with support for different types and operators.
     */
    private boolean compareValues(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }

        if (expected == null || actual == null) {
            return false;
        }

        // Handle string patterns (regex)
        if (expected instanceof String && ((String) expected).startsWith("regex:")) {
            String pattern = ((String) expected).substring(6);
            return Pattern.matches(pattern, actual.toString());
        }

        // Handle list contains
        if (expected instanceof List) {
            return ((List<?>) expected).contains(actual);
        }

        // Handle numeric comparisons
        if (expected instanceof String && ((String) expected).startsWith("gt:")) {
            double threshold = Double.parseDouble(((String) expected).substring(3));
            double actualValue = Double.parseDouble(actual.toString());
            return actualValue > threshold;
        }

        if (expected instanceof String && ((String) expected).startsWith("lt:")) {
            double threshold = Double.parseDouble(((String) expected).substring(3));
            double actualValue = Double.parseDouble(actual.toString());
            return actualValue < threshold;
        }

        // Default equals comparison
        return expected.equals(actual);
    }

    /**
     * Get value from request based on key path.
     */
    private Object getValueFromRequest(String key, AuthorizationRequest request) {
        // Handle nested keys (e.g., "attributes.department")
        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            if ("attributes".equals(parts[0])) {
                return request.getAttributes().get(parts[1]);
            }
        }

        // Direct request fields
        return switch (key) {
            case "userId" -> request.getUserId();
            case "tenantId" -> request.getTenantId();
            case "resource" -> request.getResource();
            case "action" -> request.getAction();
            case "resourceId" -> request.getResourceId();
            case "ipAddress" -> request.getIpAddress();
            case "userAgent" -> request.getUserAgent();
            default -> request.getAttributes().get(key);
        };
    }

    /**
     * Create SpEL evaluation context with all available data.
     */
    private EvaluationContext createEvaluationContext(
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        StandardEvaluationContext context = new StandardEvaluationContext();

        // Add request data
        context.setVariable("userId", request.getUserId());
        context.setVariable("tenantId", request.getTenantId());
        context.setVariable("resource", request.getResource());
        context.setVariable("action", request.getAction());
        context.setVariable("resourceId", request.getResourceId());
        context.setVariable("attributes", request.getAttributes());
        context.setVariable("ipAddress", request.getIpAddress());
        context.setVariable("userAgent", request.getUserAgent());

        // Add permissions if available
        if (userPermissions != null) {
            context.setVariable("permissions", userPermissions);
            context.setVariable("permissionNames",
                    userPermissions.stream()
                            .map(p -> p.getResourceType() + ":" + p.getAction())
                            .collect(Collectors.toSet())
            );
        }

        // Add time-related variables
        context.setVariable("now", Instant.now());
        context.setVariable("currentTime", LocalDateTime.now());
        context.setVariable("dayOfWeek", LocalDateTime.now().getDayOfWeek().toString());
        context.setVariable("hour", LocalDateTime.now().getHour());

        return context;
    }

    /**
     * Check if current time is within allowed hours.
     * Format: "HH:mm-HH:mm" (e.g., "09:00-17:00")
     */
    private boolean isWithinAllowedHours(LocalDateTime time, String allowedHours) {
        try {
            String[] parts = allowedHours.split("-");
            if (parts.length != 2) {
                log.error("Invalid allowedHours format: {}", allowedHours);
                return false;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String currentTimeStr = time.format(formatter);

            return currentTimeStr.compareTo(parts[0]) >= 0 &&
                    currentTimeStr.compareTo(parts[1]) <= 0;

        } catch (Exception e) {
            log.error("Error parsing allowed hours: {}", allowedHours, e);
            return false;
        }
    }

    /**
     * Check if current day is in allowed days.
     * Format: Comma-separated day names (e.g., "MONDAY,TUESDAY,WEDNESDAY")
     */
    private boolean isAllowedDay(LocalDateTime time, String allowedDays) {
        String currentDay = time.getDayOfWeek().toString();
        String[] days = allowedDays.split(",");

        for (String day : days) {
            if (day.trim().equalsIgnoreCase(currentDay)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if current date is within date range.
     * Format: "yyyy-MM-dd to yyyy-MM-dd"
     */
    private boolean isWithinDateRange(LocalDateTime time, String dateRange) {
        try {
            String[] parts = dateRange.split(" to ");
            if (parts.length != 2) {
                log.error("Invalid date range format: {}", dateRange);
                return false;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String currentDate = time.format(formatter);

            return currentDate.compareTo(parts[0]) >= 0 &&
                    currentDate.compareTo(parts[1]) <= 0;

        } catch (Exception e) {
            log.error("Error parsing date range: {}", dateRange, e);
            return false;
        }
    }
}