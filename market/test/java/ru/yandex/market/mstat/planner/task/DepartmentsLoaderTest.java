package ru.yandex.market.mstat.planner.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

public class DepartmentsLoaderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void t() throws IOException {
        JsonNode result = MAPPER.readTree("{\n" +
            "  \"links\": {}, \n" +
            "  \"page\": 1, \n" +
            "  \"limit\": 2000, \n" +
            "  \"result\": [\n" +
            "  ], \n" +
            "  \"total\": 12, \n" +
            "  \"pages\": 1\n" +
            "}");
        System.out.println("Size: " + result.get("result").size());
        for(JsonNode dep: result.get("result")) {
            System.out.println(dep.get("name").asText());
        }
    }
}
