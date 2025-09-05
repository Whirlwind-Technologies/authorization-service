package com.nnipa.authz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track user-role assignments.
 */
@Entity
@Table(name = "user_roles", indexes = {
        @Index(name = "idx_user_role", columnList = "user_id, role_id, tenant_id", unique = true),
        @Index(name = "idx_user_role_user", columnList = "user_id"),
        @Index(name = "idx_user_role_role", columnList = "role_id"),
        @Index(name = "idx_user_role_tenant", columnList = "tenant_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assigned_by", length = 255)
    private String assignedBy;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}