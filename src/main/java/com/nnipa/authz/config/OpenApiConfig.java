package com.nnipa.authz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Authorization Service.
 * Provides comprehensive API documentation for authorization and permission management endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${server.port:4003}")
    private String serverPort;

    @Value("${spring.application.name:authorization-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(components())
                .security(Arrays.asList(
                        new SecurityRequirement().addList("bearerAuth"),
                        new SecurityRequirement().addList("apiKey")
                ))
                .tags(apiTags());
    }

    private Info apiInfo() {
        return new Info()
                .title("Authorization Service API")
                .description("NNIPA Platform Authorization Service - Handles Role-Based Access Control (RBAC), " +
                        "Attribute-Based Access Control (ABAC), policy-based authorization, resource-level permissions, " +
                        "and tenant isolation with cross-tenant access management." +
                        "\n\n" +
                        "## Key Features\n" +
                        "- **Role-Based Access Control (RBAC)**: Hierarchical roles with inheritance\n" +
                        "- **Attribute-Based Access Control (ABAC)**: Context-aware authorization decisions\n" +
                        "- **Policy-Based Authorization**: Dynamic permission evaluation with rules engine\n" +
                        "- **Resource-Level Permissions**: Fine-grained access control for specific resources\n" +
                        "- **Tenant Isolation**: Multi-tenant security with cross-tenant permissions\n" +
                        "- **Permission Caching**: High-performance authorization with intelligent caching\n" +
                        "\n\n" +
                        "## Authorization Architecture\n" +
                        "- **Permission Evaluation**: Real-time authorization decisions based on user context\n" +
                        "- **Policy Engine**: Rule-based access control with configurable policies\n" +
                        "- **Resource Access Control Lists**: Granular permissions per resource type\n" +
                        "- **Audit Logging**: Complete audit trail for authorization decisions\n" +
                        "\n\n" +
                        "## Integration Points\n" +
                        "- **Tenant Service**: Tenant validation and context resolution\n" +
                        "- **Auth Service**: User authentication status verification\n" +
                        "- **Event Streaming**: Publishing authorization events via Kafka\n" +
                        "- **All Services**: Authorization decision enforcement\n" +
                        "\n\n" +
                        "## Authentication\n" +
                        "This service requires JWT Bearer token authentication obtained from the Auth Service. " +
                        "All authorization requests must include valid authentication tokens for proper " +
                        "permission evaluation and tenant context resolution.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("NNIPA Platform Team")
                        .email("authz-support@nnipa.cloud")
                        .url("https://nnipa.cloud"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://nnipa.cloud/license"));
    }

    private List<Server> servers() {
        String basePath = StringUtils.hasText(contextPath) ? contextPath : "";

        Server gatewayServer = new Server()
                .url("http://localhost:4000/api/v1/authz")
                .description("Via API Gateway (Recommended)");

        Server directServer = new Server()
                .url("http://localhost:" + serverPort + basePath)
                .description("Direct Access (Development Only)");

        Server devServer = new Server()
                .url("https://dev.nnipa.com/api/v1/authz")
                .description("Development Environment");

        Server stagingServer = new Server()
                .url("https://staging.nnipa.com/api/v1/authz")
                .description("Staging Environment");

        Server prodServer = new Server()
                .url("https://api.nnipa.com/api/v1/authz")
                .description("Production Environment");

        return Arrays.asList(gatewayServer, directServer, devServer, stagingServer, prodServer);
    }

    private Components components() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token obtained from Auth Service. " +
                                        "Include in Authorization header as: Bearer {token}"))
                .addSecuritySchemes("apiKey",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key for service-to-service communication. " +
                                        "Include in X-API-Key header."));
    }

    private List<Tag> apiTags() {
        return Arrays.asList(
                new Tag()
                        .name("Authorization")
                        .description("Core authorization and permission checking endpoints. " +
                                "Used to determine if users have permission to perform specific actions on resources."),

                new Tag()
                        .name("Roles")
                        .description("Role management endpoints for creating, updating, and managing user roles. " +
                                "Supports hierarchical roles with inheritance and tenant-specific role assignments."),

                new Tag()
                        .name("Permissions")
                        .description("Permission management endpoints for defining and managing system permissions. " +
                                "Includes resource-level permissions and permission inheritance."),

                new Tag()
                        .name("Policies")
                        .description("Policy management endpoints for creating and managing authorization policies. " +
                                "Supports attribute-based access control (ABAC) and dynamic policy evaluation."),

                new Tag()
                        .name("Resources")
                        .description("Resource management endpoints for defining and managing protected resources. " +
                                "Includes resource access control lists and resource-specific permissions."),

                new Tag()
                        .name("Tenant Authorization")
                        .description("Tenant-specific authorization endpoints for managing multi-tenant access control. " +
                                "Includes tenant isolation and cross-tenant permission management."),

                new Tag()
                        .name("User Permissions")
                        .description("User-specific permission endpoints for querying and managing individual user permissions. " +
                                "Includes effective permissions calculation and permission inheritance resolution."),

                new Tag()
                        .name("Audit")
                        .description("Authorization audit endpoints for tracking and querying authorization decisions. " +
                                "Provides complete audit trails for compliance and security monitoring."),

                new Tag()
                        .name("Health & Monitoring")
                        .description("Service health and monitoring endpoints for operational visibility. " +
                                "Includes health checks, metrics, and service status information.")
        );
    }
}