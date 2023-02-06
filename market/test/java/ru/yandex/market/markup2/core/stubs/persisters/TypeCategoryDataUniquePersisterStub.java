package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.dataUnique.TypeCategoryDataUniquePersister;
import ru.yandex.market.markup2.entries.dataUnique.TypeCategoryItem;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 24.05.2018
 */
public class TypeCategoryDataUniquePersisterStub extends TypeCategoryDataUniquePersister implements IPersisterStub {
    private final DefaultPersisterStub<Long, TypeCategoryItem> storage;

    public TypeCategoryDataUniquePersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TypeCategoryDataUniquePersisterStub(DefaultPersisterStub<Long, TypeCategoryItem> storage) {
        this.storage = storage;
    }

    public TypeCategoryDataUniquePersisterStub copy() {
        return new TypeCategoryDataUniquePersisterStub(this.storage);
    }

    @Override
    public List<TypeCategoryItem> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<TypeCategoryItem> getValues(int typeId, int categoryId) {
        return storage.getAllValues().stream()
            .filter(t -> t.getCategoryId() == categoryId && t.getTaskTypeId() == typeId)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<Long> upsert(List<TypeCategoryItem> items) {
        return storage.upsertAll(items, TypeCategoryItem::getId);
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
