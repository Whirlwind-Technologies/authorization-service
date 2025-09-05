package com.nnipa.authz.mapper;

import com.nnipa.authz.dto.response.RoleResponse;
import com.nnipa.authz.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Role entity and DTOs.
 */
@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    @Mapping(source = "parentRole.id", target = "parentRoleId")
    @Mapping(source = "parentRole.name", target = "parentRoleName")
    @Mapping(source = "permissions", target = "permissions", qualifiedByName = "mapPermissions")
    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponseList(List<Role> roles);

    @Named("mapPermissions")
    default List<com.nnipa.authz.dto.response.PermissionResponse> mapPermissions(
            java.util.Set<com.nnipa.authz.entity.RolePermission> rolePermissions) {
        if (rolePermissions == null) {
            return null;
        }
        return rolePermissions.stream()
                .map(rp -> PermissionMapper.INSTANCE.toResponse(rp.getPermission()))
                .collect(Collectors.toList());
    }
}