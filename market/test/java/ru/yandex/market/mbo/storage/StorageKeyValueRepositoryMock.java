package ru.yandex.market.mbo.storage;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;

public class StorageKeyValueRepositoryMock extends GenericMapperRepositoryMock<StorageKeyValue, String>
        implements StorageKeyValueRepository {

    public StorageKeyValueRepositoryMock() {
        super(null, StorageKeyValue::getKey);
    }

    @Override
    protected String nextId() {
        return null;
    }
}
