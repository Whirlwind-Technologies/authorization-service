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
 * Role entity representing a collection of permissions.
 * Supports hierarchical roles with parent-child relationships.
 */
@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_role_tenant", columnList = "tenant_id"),
        @Index(name = "idx_role_name_tenant", columnList = "name, tenant_id", unique = true),
        @Index(name = "idx_role_parent", columnList = "parent_role_id"),
        @Index(name = "idx_role_system", columnList = "is_system")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"permissions", "childRoles", "parentRole"})
@ToString(exclude = {"permissions", "childRoles", "parentRole"})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private Role parentRole;

    @OneToMany(mappedBy = "parentRole", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Role> childRoles = new HashSet<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RolePermission> permissions = new HashSet<>();

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}