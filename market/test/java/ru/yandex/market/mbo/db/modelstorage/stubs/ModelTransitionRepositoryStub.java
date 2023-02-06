package ru.yandex.market.mbo.db.modelstorage.stubs;

import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.database.repo.OffsetFilter;
import ru.yandex.market.mbo.database.repo.SortOrder;
import ru.yandex.market.mbo.database.repo.Sorting;
import ru.yandex.market.mbo.db.repo.ModelTransitionsRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 */
public class ModelTransitionRepositoryStub implements ModelTransitionsRepository {

    private AtomicLong idGenerator = new AtomicLong();

    private List<ModelTransition> transitions = new ArrayList<>();

    public List<ModelTransition> getTransitions() {
        return transitions;
    }

    public long getTransitionsCount() {
        return transitions.size();
    }

    @Override
    public ModelTransition getById(Long id) {
        return transitions.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst().orElse(null);
    }

    @Override
    public List<ModelTransition> getByIds(Collection<Long> ids) {
        return transitions.stream()
            .filter(t -> ids.contains(t.getId()))
            .collect(Collectors.toList());
    }

    @Override
    public void setTransitionsExported(Collection<Long> ids) {
        transitions.stream()
            .filter(t -> ids.contains(t.getId()))
            .forEach(t -> t.setExportedDate(LocalDateTime.now()));
    }

    @Override
    public ModelTransition save(ModelTransition entity) {
        return save(Collections.singletonList(entity)).stream()
            .findFirst().get();
    }

    @Override
    public List<ModelTransition> save(Collection<ModelTransition> entities) {
        Long actionId = idGenerator.incrementAndGet();
        entities.forEach(t -> {
            t.setActionId(actionId);
            t.setId(idGenerator.incrementAndGet());
        });
        transitions.addAll(entities);
        return new ArrayList<>(entities);
    }

    @Override
    public List<ModelTransition> find(Filter filter) {
        return transitions.stream()
            .filter(t -> filter.getNotExportedYet() == null ||
                (filter.getNotExportedYet() && t.getExportedDate() == null))
            .filter(t -> filter.getEntityTypes().isEmpty() || filter.getEntityTypes().contains(t.getEntityType()))
            .filter(t -> filter.getStartId() == null || t.getId() >= filter.getStartId())
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelTransition> find(Filter filter, Sorting<SortBy> sorting, OffsetFilter offset) {
        Comparator<ModelTransition> comparator;
        switch (sorting.getField()) {
            case ID:
                comparator = Comparator.comparing(ModelTransition::getId);
                break;
            default:
                throw new UnsupportedOperationException("Sorting by " + sorting.getField() + " is not supported");
        }
        if (sorting.getOrder() == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return find(filter).stream()
            .sorted(comparator)
            .skip(offset.getOffset())
            .limit(offset.getLimit())
            .collect(Collectors.toList());
    }

    @Override
    public void removeExportedClusterTransitions() {
        transitions.removeIf(t -> t.getEntityType() == EntityType.CLUSTER && t.getExportedDate() != null);
    }

    @Override
    public int removeExportedUndeleteTransitions(Collection<EntityType> entityTypes) {
        int initial = transitions.size();
        Predicate<ModelTransition> testDelete =
            t -> t.getReason() == ModelTransitionReason.UNDELETE
                && entityTypes.contains(t.getEntityType())
                && t.getExportedDate() != null;
        Set<Long> undeletedModelIds = transitions.stream()
            .filter(testDelete)
            .map(ModelTransition::getNewEntityId)
            .collect(Collectors.toSet());
        transitions.removeIf(testDelete);
        transitions.removeIf(t -> undeletedModelIds.contains(t.getOldEntityId())
            && t.getOldEntityDeleted()
            && t.getExportedDate() != null);
        return (initial - transitions.size());
    }

    @Override
    public int count(Filter filter) {
        return find(filter).size();
    }
}
