package ru.yandex.market.markup2.core.stubs.persisters;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.markup2.dao.KvStoragePersister;

/**
 * @author shadoff
 * created on 9/22/20
 */
public class KvStoragePersisterStub extends KvStoragePersister {
    private final Map<String, String> map = new HashMap<>();

    @Override
    public String getValue(String key) {
        return map.get(key);
    }

    @Override
    public Long getLongValue(String key) {
        try {
            return Long.valueOf(map.get(key));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void setValue(String key, String value) {
        map.put(key, value);
    }
}
