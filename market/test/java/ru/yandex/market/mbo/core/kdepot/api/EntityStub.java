package ru.yandex.market.mbo.core.kdepot.api;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityStub implements Entity {

    private Map<String, List<String>> attributes = new HashMap<>();

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public Long getTypeId() {
        return null;
    }

    @Override
    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    @Override
    public List<String> getAttribute(String attributeName) {
        List<String> attr = attributes.get(attributeName);
        if (CollectionUtils.isEmpty(attr)) {
            return null;
        }
        return attr;
    }

    public void setAttributes(String attributeName, List<String> values) {
        attributes.put(attributeName, values);
    }

    public void setAttribute(String attributeName, String value) {
        ArrayList<String> list = new ArrayList<>();
        list.add(value);
        setAttributes(attributeName, list);
    }

    public void setAttribute(String attributeName, Object value) {
        setAttribute(attributeName, value.toString());
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }
}
