package ru.yandex.market.mbo.db.repo;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelParamsImport;
import ru.yandex.market.mbo.database.repo.OffsetFilter;
import ru.yandex.market.mbo.database.repo.Sorting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ModelParamsImportRepositoryStub implements ModelParamsImportRepository {

    private AtomicLong idGenerator = new AtomicLong();
    private List<ModelParamsImport> storage = new ArrayList<>();

    @Override
    public ModelParamsImport getById(Long id) {
        return storage.stream()
            .filter(i -> i.getId().equals(id))
            .findFirst().orElse(null);
    }

    @Override
    public ModelParamsImport save(ModelParamsImport entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        } else {
            storage.removeIf(i -> i.getId().equals(entity.getId()));
        }
        storage.add(entity);
        return entity;
    }

    @Override
    public List<ModelParamsImport> find(Filter filter, Sorting<SortBy> sorting, OffsetFilter offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int count(Filter filter) {
        throw new UnsupportedOperationException();
    }
}
