package com.nnipa.authz.mapper;

import com.nnipa.authz.dto.response.ResourceResponse;
import com.nnipa.authz.entity.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    @Mapping(source = "parentResource.id", target = "parentResourceId")
    @Mapping(source = "parentResource.name", target = "parentResourceName")
    @Mapping(source = "attributes", target = "attributes") // Map attributes field
    @Mapping(expression = "java(resource.getPolicies() != null ? resource.getPolicies().size() : 0)",
            target = "policyCount")
    ResourceResponse toResponse(Resource resource);

    List<ResourceResponse> toResponseList(List<Resource> resources);
}