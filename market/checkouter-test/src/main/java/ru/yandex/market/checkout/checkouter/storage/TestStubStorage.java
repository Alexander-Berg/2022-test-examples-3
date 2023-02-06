package ru.yandex.market.checkout.checkouter.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.storage.EntityGroup;
import ru.yandex.market.checkout.storage.Sequence;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.storage.StorageCallback;
import ru.yandex.market.checkout.storage.StorageSequence;
import ru.yandex.market.checkout.storage.err.EntityGroupNotFoundException;
import ru.yandex.market.checkout.storage.err.StorageException;

/**
 * @author sergey-fed
 */
public class TestStubStorage implements Storage {

    @Override
    public long getNextSequenceVal(Sequence sequence) throws StorageException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StorageSequence getSequence(Sequence sequence) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, K extends Comparable<K>> T createEntityGroup(
            EntityGroup<K> entityGroup, StorageCallback<T> callback) throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, K extends Comparable<K>> T updateEntityGroup(
            EntityGroup<K> entityGroup, StorageCallback<T> callback) throws StorageException,
            EntityGroupNotFoundException {
        return callback.doQuery();
    }

    @Override
    public <T, K extends Comparable<K>> T createOrUpdateEntityGroup(
            EntityGroup<K> entityGroup, StorageCallback<T> callback) throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, K extends Comparable<K>> T deleteEntityGroup(
            EntityGroup<K> entityGroup, StorageCallback<T> callback) throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, K extends Comparable<K>> T deleteEntityGroups(
            List<EntityGroup<K>> entityGroupList, StorageCallback<T> callback) throws StorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, K extends Comparable<K>> T updateEntityGroups(
            List<EntityGroup<K>> entityGroupList, StorageCallback<T> callback) throws StorageException {
        return null;
    }

    @Nonnull
    private static <T> Map<Integer, T> wrapIntoMap(T result, int index) {
//        Assert.notNull(result);

        Map<Integer, T> wrapper = new HashMap<>();
        wrapper.put(index, result);
        return wrapper;
    }
}
