package ru.yandex.market.mbo.db.repo;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelParamsImportStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ModelParamsImportStatusRepositoryStub implements ModelParamsImportStatusRepository {

    private final AtomicLong idGenerator = new AtomicLong();
    private final List<ModelParamsImportStatus> storage = new ArrayList<>();

    @Override
    public List<ModelParamsImportStatus> save(Collection<ModelParamsImportStatus> entity) {
        List<ModelParamsImportStatus> result = new ArrayList<>();
        for (ModelParamsImportStatus status : entity) {
            result.add(save(status));
        }
        return result;
    }

    private ModelParamsImportStatus save(ModelParamsImportStatus entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        } else {
            storage.removeIf(i -> i.getId().equals(entity.getId()));
        }
        storage.add(entity);
        return entity;
    }

    @Override
    public List<ModelParamsImportStatus> find(Filter filter) {
        return storage.stream()
            .filter(s -> s.getImportId().equals(filter.getImportId()))
            .filter(s -> filter.getRowIndexes().contains(s.getRowIndex()))
            .filter(s -> s.getVerification() == filter.isVerification())
            .collect(Collectors.toList());
    }

    @Override
    public int count(Filter filter) {
        throw new UnsupportedOperationException();
    }
}
