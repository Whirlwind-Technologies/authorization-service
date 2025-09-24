package com.nnipa.authz.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResourceRequest {
    private String name;
    private String description;
    private UUID ownerId;
    private Map<String, Object> metadata;
    private Boolean isPublic;
}