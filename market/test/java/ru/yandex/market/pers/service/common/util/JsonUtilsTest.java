package ru.yandex.market.pers.service.common.util;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getStringValue() throws IOException {

        JsonNode jsonNode = objectMapper.readTree("{\"a\": \"abc\"}");
        Assertions.assertEquals("abc", JsonUtils.getStringValue(jsonNode, "a"));
    }

    @Test
    void getValue() throws IOException {
        JsonNode jsonNode = objectMapper.readTree("{\n" +
            "        \"count\": 1,\n" +
            "        \"count_by_value\": [\n" +
            "            {\n" +
            "                \"val\": 5,\n" +
            "                \"val_count\": 1\n" +
            "            }\n" +
            "        ],\n" +
            "        \"factor_id\": 2096,\n" +
            "        \"factor_name\": \"Время автономной работы\",\n" +
            "        \"factor_type\": 0,\n" +
            "        \"published\": false,\n" +
            "        \"value\": 5\n" +
            "    }");
        Object count_by_value = JsonUtils.getValue(jsonNode, "count_by_value_bad", new TypeReference<List<FactorCount>>() {});
        System.out.println(count_by_value);
    }

    public static class FactorCount {
        @JsonProperty("val")
        private int val;
        @JsonProperty("val_count")
        private int valCount;
    }
}
