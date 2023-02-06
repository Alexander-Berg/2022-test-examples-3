package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.dataUnique.ConfigGroupDataUniquePersister;
import ru.yandex.market.markup2.entries.dataUnique.ConfigGroupItem;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 24.05.2018
 */
public class ConfigGroupDataUniquePersisterStub extends ConfigGroupDataUniquePersister implements IPersisterStub {
    private final DefaultPersisterStub<Long, ConfigGroupItem> storage;

    public ConfigGroupDataUniquePersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private ConfigGroupDataUniquePersisterStub(DefaultPersisterStub<Long, ConfigGroupItem> storage) {
        this.storage = storage;
    }

    public ConfigGroupDataUniquePersisterStub copy() {
        return new ConfigGroupDataUniquePersisterStub(this.storage);
    }

    @Override
    public List<ConfigGroupItem> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<ConfigGroupItem> getValues(int configGroupId) {
        return storage.getByValue(configGroupId, ConfigGroupItem::getConfigGroupId);
    }

    @Override
    public Collection<Long> upsert(List<ConfigGroupItem> items) {
        return storage.upsertAll(items, ConfigGroupItem::getId);
    }

    @Override
    public long generateNextLong() {
        return storage.generateNextLong();
    }

    @Override
    public int generateNextInt() {
        return storage.generateNextInt();
    }

    @Override
    public List<Long> generateNextLongs(int count) {
        return storage.generateNextLongs(count);
    }

    @Override
    public List<Integer> generateNextInts(int count) {
        return storage.generateNextInts(count);
    }
}
