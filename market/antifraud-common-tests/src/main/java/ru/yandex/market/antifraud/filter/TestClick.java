package ru.yandex.market.antifraud.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import ru.yandex.market.antifraud.filter.fields.FilterConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oroboros on 11.08.17.
 */
public class TestClick implements TestEntity {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public final Map<String, Object> data = new HashMap<>();

    public TestClick set(String field, Object value) {
        data.put(field.toLowerCase(), value);
        return this;
    }

    public Object get(String field) {
        return data.get(field.toLowerCase());
    }

    public <T> T get(String field, Class<T> clazz) {
        return (T) data.get(field.toLowerCase());
    }

    public TestClick setFilter(FilterConstants f) {
        return set("filter", f.id());
    }

    public TestClick setNotFiltered() {
        setFilter(FilterConstants.FILTER_0);
        return this;
    }

    public void setNotFilteredByReasonOf(String field, Object value) {
        set(field, value);
        setNotFiltered();
    }

    public FilterConstants getFilter() {
        return FilterConstants.valueOf("FILTER_" + get("filter", Integer.class));
    }

    @Override
    @SneakyThrows
    public String toString() {
        return "TestClick " + jsonMapper.writeValueAsString(data);
    }
}
