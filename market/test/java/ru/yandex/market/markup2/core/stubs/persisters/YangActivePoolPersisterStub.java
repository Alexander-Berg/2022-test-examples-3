package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.YangActivePoolPersister;
import ru.yandex.market.markup2.entries.yang.YangActivePool;
import ru.yandex.market.markup2.entries.yang.YangActivePoolKey;

import java.util.HashMap;
import java.util.Map;

/**
 * @author york
 * @since 18.11.2019
 */
public class YangActivePoolPersisterStub extends YangActivePoolPersister implements IPersisterStub {
    private Map<YangActivePoolKey, YangActivePool> storage = new HashMap<>();

    public YangActivePoolPersisterStub() { }

    private YangActivePoolPersisterStub(Map<YangActivePoolKey, YangActivePool> storage) {
        this.storage = storage;
    }

    public YangActivePoolPersisterStub copy() {
        return new YangActivePoolPersisterStub(this.storage);
    }

    @Override
    public YangActivePool getByKey(YangActivePoolKey key) {
        return storage.get(key);
    }

    @Override
    public void save(YangActivePool activePool) {
        storage.put(activePool.getPoolKey(), activePool);
    }
}
