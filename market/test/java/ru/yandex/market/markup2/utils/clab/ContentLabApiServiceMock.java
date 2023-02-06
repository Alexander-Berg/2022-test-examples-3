package ru.yandex.market.markup2.utils.clab;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.clab.api.http.ContentLabApi;

public class ContentLabApiServiceMock implements ContentLabApiService {

    private Map<Long, ContentLabApi.YangTaskModelDetails> yangTaskModelDetailsMap = new HashMap<>();
    private Map<Long, List<ContentLabApi.YangTaskStatus>> yangTaskStatusMap = new HashMap<>();
    private Map<Integer, Set<Long>> modelsByCategory = new HashMap<>();

    @Override
    public List<ContentLabApi.YangTaskModel> getYangTaskModels(Collection<Long> excludedModelIds) {
        return yangTaskModelDetailsMap.entrySet().stream()
            .filter(e -> !excludedModelIds.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .map(ContentLabApi.YangTaskModelDetails::getModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<ContentLabApi.YangTaskModel> getYangTaskModels(int categoryId, Collection<Long> excludedModelIds) {
        Set<Long> modelsForCategory = modelsByCategory.getOrDefault(categoryId, Collections.emptySet());
        return yangTaskModelDetailsMap.entrySet().stream()
            .filter(e -> modelsForCategory.contains(e.getKey()) && !excludedModelIds.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .map(ContentLabApi.YangTaskModelDetails::getModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<ContentLabApi.YangTaskModelDetails> getYangTaskModelDetails(List<ContentLabApi.YangTaskModel> models) {
        Set<Long> modelIds = models.stream()
            .map(ContentLabApi.YangTaskModel::getModelId)
            .collect(Collectors.toSet());
        return yangTaskModelDetailsMap.entrySet().stream()
            .filter(e -> modelIds.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public void updateYangTaskStatus(ContentLabApi.YangTaskModel model, List<ContentLabApi.YangTaskStatus> statuses,
                                     String workerStaffLogin) {
        yangTaskStatusMap.put(model.getModelId(), statuses);
    }

    public void addModel(ContentLabApi.YangTaskModelDetails modelDetails) {
        yangTaskModelDetailsMap.put(modelDetails.getModel().getModelId(), modelDetails);
        int categoryId = (int) modelDetails.getModel().getCategoryId();
        if (!modelsByCategory.containsKey(categoryId)) {
            modelsByCategory.put(categoryId, new HashSet<>());
        }
        modelsByCategory.get(categoryId).add(modelDetails.getModel().getModelId());
    }

    public List<ContentLabApi.YangTaskStatus> getStatuses(long modelId) {
        return yangTaskStatusMap.get(modelId);
    }
}
