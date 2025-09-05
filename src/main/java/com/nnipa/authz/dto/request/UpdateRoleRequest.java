package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10000, message = "Priority must not exceed 10000")
    private Integer priority;

    @Min(value = 1, message = "Max users must be at least 1")
    private Integer maxUsers;

    private Boolean isActive;

    private String updatedBy;

    // Special flag to allow updating system roles (use with caution)
    private Boolean allowSystemUpdate;
}