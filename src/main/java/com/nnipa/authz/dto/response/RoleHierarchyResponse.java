package com.nnipa.authz.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleHierarchyResponse {

    private RoleResponse role;

    private List<RoleResponse> parentHierarchy;

    private List<RoleResponse> childRoles;

    private List<PermissionResponse> allPermissions;
}