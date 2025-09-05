package com.nnipa.authz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Permission entity representing an action that can be performed on a resource.
 */
@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permission_resource_action", columnList = "resource_type, action", unique = true),
        @Index(name = "idx_permission_system", columnList = "is_system"),
        @Index(name = "idx_permission_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"rolePermissions", "policies"})
@ToString(exclude = {"rolePermissions", "policies"})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Policy> policies = new HashSet<>();

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "requires_mfa")
    @Builder.Default
    private boolean requiresMfa = false;

    @Column(name = "requires_approval")
    @Builder.Default
    private boolean requiresApproval = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}