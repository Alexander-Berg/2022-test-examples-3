package ru.yandex.market.mboc.common.services.modelstorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import lombok.Getter;

import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;

/**
 * @author s-ermakov
 */
public class ModelStorageCachingServiceMock implements ModelStorageCachingService {

    private final Map<ModelKey, Model> modelsMap = new HashMap<>();
    private final Map<Long, ModelKey> modelIdToModelKey = new HashMap<>();
    @Getter
    private final Set<Long> reloaded = new HashSet<>();
    private Model autoModel;
    private final Set<Long> notRestorableModels = new HashSet<>();

    public ModelStorageCachingServiceMock addModel(Model model) {
        ModelKey oldKey = this.modelIdToModelKey.remove(model.getId());
        if (oldKey != null) {
            this.modelsMap.remove(oldKey);
        }

        ModelKey key = new ModelKey(model);
        this.modelsMap.put(key, model);
        this.modelIdToModelKey.put(model.getId(), key);
        return this;
    }

    public ModelStorageCachingServiceMock setAutoModel(Model autoModel) {
        this.autoModel = autoModel;
        return this;
    }

    @Override
    public Map<Long, Model> getModelsFromMboThenPg(Collection<Long> ids) {
        Map<Long, Model> result = new HashMap<>();
        for (Long id : ids) {
            Optional<Model> model = getModel(id);
            if (model.isPresent()) {
                result.put(id, model.get());
            } else if (autoModel != null) {
                result.put(id, autoModel.copy().setId(id).setTitle("auto-model #" + id));
            }
        }
        return result;
    }

    @Override
    public Map<Long, Model> getModelsFromMboThenPg(Multimap<Long, Long> categoryToModelIds) {
        return getModelsFromMboThenPg(categoryToModelIds.values());
    }

    @Override
    public Map<Long, Model> getModelsFromPgOnly(Collection<Long> ids) {
        return getModelsFromMboThenPg(ids);
    }

    @Override
    public Map<Long, Model> getModelsFromPgThenMbo(Collection<Long> ids) {
        return getModelsFromMboThenPg(ids);
    }

    @Override
    public Map<Long, Model> getModelsFromPgThenMbo(Collection<Long> ids, Predicate<Model> reload, boolean withDeleted) {
        Map<Long, Model> modelsFromPgThenMbo = getModelsFromPgThenMbo(ids);
        reloaded.clear();
        modelsFromPgThenMbo.values().forEach(m -> {
            if (reload.test(m)) {
                reloaded.add(m.getId());
            }
        });
        return modelsFromPgThenMbo;
    }

    @Override
    public Map<Long, SimpleModel> getSimpleModelsFromPgThenMbo(Collection<Long> skuIds, Predicate<SimpleModel> reload) {
        return getModelsFromPgThenMbo(skuIds).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void restore(Model model) {
        if (!notRestorableModels.contains(model.getId())) {
            model.setDeleted(false);
            addModel(model);
        }
    }

    public void markModelNotRestorable(long modelId) {
        notRestorableModels.add(modelId);
    }

    public void unmarkModelNotRestorable(long modelId) {
        notRestorableModels.remove(modelId);
    }

    @Override
    public Map<Long, Model> getModelsFromMboOnly(Collection<Long> ids) {
        return getModelsFromMboThenPg(ids);
    }

    public Optional<Model> getModel(long modelId) {
        return Optional.ofNullable(modelIdToModelKey.get(modelId))
            .flatMap(key -> Optional.ofNullable(modelsMap.get(key)));
    }

    public void clear() {
        modelIdToModelKey.clear();
        modelsMap.clear();
    }

    private static class ModelKey {
        private final long categoryId;
        private final long modelId;

        private ModelKey(long categoryId, long modelId) {
            Preconditions.checkArgument(categoryId > 0, "category should be positive");
            Preconditions.checkArgument(modelId > 0, "modelId should be positive");
            this.categoryId = categoryId;
            this.modelId = modelId;
        }

        private ModelKey(Model model) {
            this(model.getCategoryId(), model.getId());
        }

        public long getCategoryId() {
            return categoryId;
        }

        public long getModelId() {
            return modelId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ModelKey modelKey = (ModelKey) o;
            return categoryId == modelKey.categoryId &&
                modelId == modelKey.modelId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(categoryId, modelId);
        }

        @Override
        public String toString() {
            return "ModelKey{" +
                "categoryId=" + categoryId +
                ", modelId=" + modelId +
                '}';
        }
    }
}
