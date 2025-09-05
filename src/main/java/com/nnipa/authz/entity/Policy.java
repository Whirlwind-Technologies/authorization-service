package com.nnipa.authz.entity;

import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.enums.PolicyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Policy entity for fine-grained access control rules.
 */
@Entity
@Table(name = "policies", indexes = {
        @Index(name = "idx_policy_tenant", columnList = "tenant_id"),
        @Index(name = "idx_policy_name_tenant", columnList = "name, tenant_id", unique = true),
        @Index(name = "idx_policy_type", columnList = "policy_type"),
        @Index(name = "idx_policy_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"permissions", "resources"})
@ToString(exclude = {"permissions", "resources"})
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "policy_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyType policyType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PolicyEffect effect = PolicyEffect.DENY;

    @ManyToMany
    @JoinTable(
            name = "policy_permissions",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @ManyToMany(mappedBy = "policies")
    @Builder.Default
    private Set<Resource> resources = new HashSet<>();

    @Column(name = "conditions", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> conditions = new HashMap<>();

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

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