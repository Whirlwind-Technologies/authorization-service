package com.nnipa.authz.mapper;

import com.nnipa.authz.dto.response.PolicyResponse;
import com.nnipa.authz.entity.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class, ResourceMapper.class})
public interface PolicyMapper {

    @Mapping(source = "permissions", target = "permissions")
    @Mapping(source = "resources", target = "resources")
    PolicyResponse toResponse(Policy policy);

    List<PolicyResponse> toResponseList(List<Policy> policies);
}