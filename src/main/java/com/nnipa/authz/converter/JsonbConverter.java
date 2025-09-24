package com.nnipa.authz.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Converter for storing Map<String, Object> as JSONB in PostgreSQL.
 * Automatically converts between Java Map and PostgreSQL JSONB column type.
 */
@Slf4j
@Component
@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert Map to JSON String for database storage.
     */
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.trace("Converting Map to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting Map to JSON", e);
            throw new IllegalArgumentException("Unable to convert Map to JSON string", e);
        }
    }

    /**
     * Convert JSON String from database to Map.
     */
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }

        try {
            Map<String, Object> map = objectMapper.readValue(
                    dbData,
                    new TypeReference<Map<String, Object>>() {}
            );
            log.trace("Converting JSON to Map: {}", dbData);
            return map;
        } catch (IOException e) {
            log.error("Error converting JSON to Map: {}", dbData, e);
            // Return empty map instead of throwing exception to prevent data loss
            return new HashMap<>();
        }
    }
}