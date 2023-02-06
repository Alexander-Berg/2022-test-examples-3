package ru.yandex.market.test.scenario;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public class TestScenarioContext {
    private Map<String, Object> data;

    public TestScenarioContext() {
        data = new HashMap<>();
    }

    public TestScenarioContext(Map<String, Object> data) {
        this.data = data;
    }

    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    public int size() {
        return data.size();
    }

    public Object get(Object key) {
        return data.get(key);
    }

    public Object put(String key, Object value) {
        return data.put(key, value);
    }

    public Object remove(Object key) {
        return data.remove(key);
    }

    public void clear() {
        data.clear();
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    private String dataString() {
        Iterator<Map.Entry<String, Object>> i = data.entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ; ) {
            Map.Entry<String, Object> e = i.next();
            String key = e.getKey();
            Object value = e.getValue();
            sb.append(key);
            sb.append('=');
            String valueString;
            if (value == data) {
                valueString = "(this Map)";
            } else if (value == null || !value.getClass().isArray()) {
                valueString = String.valueOf(value);
            } else {
                valueString = Arrays.deepToString(new Object[] {value});
            }

            sb.append(valueString);
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public String toString() {
        return "context{" + "data=" + dataString() +
                '}';
    }
}
