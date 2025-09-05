package com.nnipa.authz.mapper;

import com.nnipa.authz.dto.response.PermissionResponse;
import com.nnipa.authz.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Mapper for Permission entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionMapper INSTANCE = Mappers.getMapper(PermissionMapper.class);

    @Mapping(source = "riskLevel", target = "riskLevel")
    PermissionResponse toResponse(Permission permission);

    List<PermissionResponse> toResponseList(List<Permission> permissions);
}