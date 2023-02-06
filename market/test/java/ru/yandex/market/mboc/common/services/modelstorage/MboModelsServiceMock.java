package ru.yandex.market.mboc.common.services.modelstorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.OperationStatus;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;

public class MboModelsServiceMock implements MboModelsService {

    private final Map<Long, ModelStorage.Model> models = new LinkedHashMap<>();
    private final Map<Long, OperationStatus> specialResults = new LinkedHashMap<>();
    private final Map<Long, OperationStatus> specialResultsCalledOnce = new LinkedHashMap<>();
    private int crushesBeforeCorrectSave = 0;

    public void setCrushesBeforeCorrectSave(int crushesBeforeCorrectSave) {
        this.crushesBeforeCorrectSave = crushesBeforeCorrectSave;
    }

    @Override
    public Map<Long, Model> loadModels(Iterable<Long> ids) {
        return loadModels(ids, Collections.emptySet(), Collections.emptySet(), false);
    }

    @Override
    public Map<Long, Model> loadModels(Iterable<Long> ids,
                                       Set<String> modelParamsToKeep,
                                       Set<Long> paramIdsToKeep,
                                       boolean withDeleted) {
        Map<Long, Model> result = new LinkedHashMap<>();

        for (Long id : ids) {
            ModelStorage.Model model = models.get(id);
            if (model != null
                && (!model.getDeleted() || withDeleted)
            ) {
                result.put(id, ModelConverter.convert(model, modelParamsToKeep, paramIdsToKeep));
            }
        }

        return result;
    }

    @Override
    public Map<Long, Model> loadModels(Multimap<Long, Long> categoryToModelIds) {
        return loadModels(categoryToModelIds.values());
    }

    @Override
    public Map<Long, SimpleModel> loadSimpleModels(Iterable<Long> ids) {
        return loadModels(ids, Collections.emptySet(), Collections.emptySet(), false).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public List<ModelStorage.Model> loadRawModels(Long categoryId, Collection<Long> modelIds) {
        return modelIds.stream()
            .map(models::get)
            .filter(Objects::nonNull)
            .filter(ModelStorage.Model::hasCategoryId)
            .filter(model -> Objects.equals(model.getCategoryId(), categoryId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelStorage.Model> loadRawModels(Collection<Long> modelIds) {
        return modelIds.stream()
            .map(models::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<OperationStatus> saveModels(Collection<ModelStorage.Model> updatedModels) {
        if (crushesBeforeCorrectSave-- > 0) {
            throw new RuntimeException("Some crush on saveModels");
        }

        List<OperationStatus> result = new ArrayList<>();
        for (ModelStorage.Model model : updatedModels) {
            OperationStatus saveResult;
            if (specialResults.containsKey(model.getId())) {
                saveResult = specialResults.get(model.getId());
            } else if (specialResultsCalledOnce.containsKey(model.getId())) {
                saveResult = specialResultsCalledOnce.get(model.getId());
                specialResultsCalledOnce.remove(model.getId());
            } else {
                saveResult = OperationStatus.newBuilder()
                    .setModel(model)
                    .setType(ModelStorage.OperationType.CHANGE)
                    .setModelId(model.getId())
                    .setStatus(ModelStorage.OperationStatusType.OK)
                    .build();
            }
            if (saveResult.getStatus() == ModelStorage.OperationStatusType.OK) {
                models.put(model.getId(), model);
            }
            result.add(saveResult);
        }
        return result;
    }

    @Override
    public List<OperationStatus> updateMdmParameters(Collection<ModelStorage.Model> models, String context) {
        return saveModels(models);
    }

    public void clearSpecialResultsForModel(Long modelId) {
        specialResults.remove(modelId);
        specialResultsCalledOnce.remove(modelId);
    }

    public void setSpecialResultsForModel(Long modelId, OperationStatus newResult) {
        specialResults.put(modelId, newResult);
    }

    public void setSpecialResultCalledOnceForModel(Long modelId, OperationStatus newResult) {
        specialResultsCalledOnce.put(modelId, newResult);
    }
}
