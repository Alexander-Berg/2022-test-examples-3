package ru.yandex.market.antifraud.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oroboros on 11.08.17.
 */
public class TestCpaOrderEvent implements TestEntity {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public final Map<String, Object> data = new HashMap<>();

    public void set(String field, Object value) {
        data.put(field, value);
    }

    public Object get(String field) {
        return data.get(field);
    }

    public <T> T get(String field, Class<T> clazz) {
        return (T) data.get(field.toLowerCase());
    }

    @Override
    @SneakyThrows
    public String toString() {
        return "TestClick " + jsonMapper.writeValueAsString(data);
    }
}
