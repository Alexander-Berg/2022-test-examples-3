package ru.yandex.direct.intapi.util;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;

import ru.yandex.direct.intapi.entity.turbolandings.controller.TurboLandingsControllerTest;
import ru.yandex.direct.utils.JsonUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

@ParametersAreNonnullByDefault
public class IntapiUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IntapiUtils() {
    }

    public static ObjectNode node(String fieldName, @Nullable Object value) {
        ObjectNode node = MAPPER.createObjectNode();
        if (value == null) {
            node.putNull(fieldName);
        } else if (value instanceof Integer) {
            node.put(fieldName, (Integer) value);
        } else if (value instanceof Long) {
            node.put(fieldName, (Long) value);
        } else if (value instanceof String) {
            node.put(fieldName, (String) value);
        } else if (value instanceof Boolean) {
            node.put(fieldName, (Boolean) value);
        } else if (value instanceof ObjectNode) {
            node.set(fieldName, (ObjectNode) value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
        }
        return node;
    }

    public static ObjectNode node(ObjectNode... nodes) {
        ObjectNode node = MAPPER.createObjectNode();
        for (var innerNode : nodes) {
            node.setAll(innerNode);
        }
        return node;
    }

    public static String jsonAsMultiLineString(ObjectNode node) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String jsonAsMultiLineString(ObjectNode... nodes) {
        return jsonAsMultiLineString(node(nodes));
    }

    public static String jsonAsCompactString(ObjectNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String jsonAsCompactString(ObjectNode... nodes) {
        return jsonAsCompactString(node(nodes));
    }

    public static <T> T getObjectFromResource(String resourcePath, Class<T> clazz) throws IOException {
        return JsonUtils.fromJson(
                IOUtils.toString(TurboLandingsControllerTest.class.getResourceAsStream(resourcePath), UTF_8),
                clazz);
    }

    public static <T> T getObjectFromResource(String resourcePath, TypeReference<T> typeRef) throws IOException {
        return JsonUtils.fromJson(
                IOUtils.toString(TurboLandingsControllerTest.class.getResourceAsStream(resourcePath), UTF_8),
                typeRef);
    }
}
