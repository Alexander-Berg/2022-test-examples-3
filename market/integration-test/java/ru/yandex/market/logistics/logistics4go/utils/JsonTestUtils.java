package ru.yandex.market.logistics.logistics4go.utils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

@ParametersAreNonnullByDefault
public class JsonTestUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @SneakyThrows
    public static JsonNode toJsonNode(String value) {
        return OBJECT_MAPPER.readTree(value);
    }

    @Nonnull
    public static JsonNode fileToJson(String path) {
        return toJsonNode(IntegrationTestUtils.extractFileContent(path));
    }
}
