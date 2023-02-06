package ru.yandex.market.psku.postprocessor.service.migration.convertor;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.psku.postprocessor.common.db.dao.PppKeyValueStoreDao;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class PppKeyValueStoreDaoMock extends PppKeyValueStoreDao {

    private final Map<String, Object> map = new HashMap<>();

    public PppKeyValueStoreDaoMock() {
        super(null);
    }

    @Override
    public Long getLong(String key) {
        return (Long) map.get(key);
    }

    @Override
    public void putLong(String key, Long value) {
        map.put(key, value);
    }

    @Override
    public Integer getInt(String key) {
        return (Integer) map.get(key);
    }

    @Override
    public void putInt(String key, Integer value) {
        map.put(key, value);
    }
}
