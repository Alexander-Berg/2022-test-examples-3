package ru.yandex.market.mbo.db.modelstorage.stubs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransitionCalculated;
import ru.yandex.market.mbo.db.repo.ModelTransitionsCalculatedRepository;

/**
 * @author york
 */
public class ModelTransitionCalculatedRepositoryStub implements ModelTransitionsCalculatedRepository {

    private AtomicLong idGenerator = new AtomicLong();

    private Map<Long, ModelTransitionCalculated> transitions = new HashMap<>();

    @Override
    public List<ModelTransitionCalculated> save(Collection<ModelTransitionCalculated> entities) {
        entities.forEach(t -> {
            if (t.getId() == null) {
                t.setId(idGenerator.incrementAndGet());
            }
            transitions.put(t.getId(), t);
        });
        return new ArrayList<>(transitions.values());
    }

    @Override
    public List<ModelTransitionCalculated> find(Filter filter) {
        return transitions.values().stream()
            .filter(t -> filter.getEntityTypes().isEmpty() ||
                filter.getEntityTypes().contains(t.getEntityType()))
            .filter(t -> filter.getTypes().isEmpty() ||
                filter.getTypes().contains(t.getType()))
            .filter(t -> filter.getOldEntityIds().isEmpty() ||
                filter.getOldEntityIds().contains(t.getOldEntityId()))
            .filter(t -> filter.getNewEntityIds().isEmpty() ||
                filter.getNewEntityIds().contains(t.getNewEntityId()))
            .filter(t -> filter.getOldEntityDeleted() == null ||
                t.getOldEntityDeleted() == filter.getOldEntityDeleted())

            .collect(Collectors.toList());
    }

    @Override
    public void delete(Collection<Long> ids) {
        ids.forEach(transitions::remove);
    }
}
