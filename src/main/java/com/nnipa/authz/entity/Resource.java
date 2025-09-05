package com.nnipa.authz.entity;

import com.nnipa.authz.enums.ResourceType;
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
 * Resource entity representing a protected resource in the system.
 */
@Entity
@Table(name = "resources", indexes = {
        @Index(name = "idx_resource_tenant", columnList = "tenant_id"),
        @Index(name = "idx_resource_type", columnList = "resource_type"),
        @Index(name = "idx_resource_identifier", columnList = "resource_identifier", unique = true),
        @Index(name = "idx_resource_parent", columnList = "parent_resource_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"policies", "parentResource", "childResources"})
@ToString(exclude = {"policies", "parentResource", "childResources"})
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "resource_identifier", nullable = false, unique = true, length = 255)
    private String resourceIdentifier;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_resource_id")
    private Resource parentResource;

    @OneToMany(mappedBy = "parentResource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Resource> childResources = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "resource_policies",
            joinColumns = @JoinColumn(name = "resource_id"),
            inverseJoinColumns = @JoinColumn(name = "policy_id")
    )
    @Builder.Default
    private Set<Policy> policies = new HashSet<>();

    @Column(name = "attributes", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

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