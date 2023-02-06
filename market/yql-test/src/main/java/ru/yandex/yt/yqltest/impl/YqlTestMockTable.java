package ru.yandex.yt.yqltest.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class YqlTestMockTable extends YqlTestMockObject {
    private String name;

    private final YqlTestTableSchema schema = new YqlTestTableSchema();

    private final List<JsonNode> data = new ArrayList<>();

    public void addSchema(String value) {
        schema.getSchema().add(value);
    }

    public YqlTestTableSchema getSchema() {
        return schema;
    }

    public List<JsonNode> getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
