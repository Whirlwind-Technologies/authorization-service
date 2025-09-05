package com.nnipa.authz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Authorization Service Application
 *
 * This service handles:
 * - Role-based access control (RBAC)
 * - Permission management and policy enforcement
 * - Resource-level authorization
 * - Tenant isolation and cross-tenant permissions
 *
 * Integration Points:
 * - Tenant Service: For tenant context and hierarchy
 * - Auth Service: Receives authentication events
 * - User Management Service: For user role assignments
 * - All Services: Provide authorization decisions
 */
@Slf4j
@SpringBootApplication(exclude = {
		RedisRepositoriesAutoConfiguration.class
})
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.nnipa.authz.repository")
@ConfigurationPropertiesScan("com.nnipa.authz.config")
public class AuthorizationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationServiceApplication.class, args);

		log.info("===========================================");
		log.info("NNIPA Authorization Service Started");
		log.info("===========================================");
		log.info("Core Capabilities:");
		log.info("- Role-Based Access Control (RBAC)");
		log.info("- Attribute-Based Access Control (ABAC)");
		log.info("- Policy-Based Authorization");
		log.info("- Resource-Level Permissions");
		log.info("- Tenant Isolation & Cross-Tenant Access");
		log.info("===========================================");
		log.info("Authorization Features:");
		log.info("- Hierarchical Roles with Inheritance");
		log.info("- Dynamic Permission Evaluation");
		log.info("- Policy Engine with Rules");
		log.info("- Resource Access Control Lists");
		log.info("- Permission Caching & Performance");
		log.info("===========================================");
		log.info("Integration Points:");
		log.info("- Tenant Service: Tenant validation & context");
		log.info("- Auth Service: User authentication status");
		log.info("- Event Streaming: Publishing auth events");
		log.info("- All Services: Authorization decisions");
		log.info("===========================================");
		log.info("Service running on port: 4003");
		log.info("===========================================");
	}
}