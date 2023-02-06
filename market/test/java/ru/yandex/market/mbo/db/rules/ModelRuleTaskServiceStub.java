package ru.yandex.market.mbo.db.rules;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.common.processing.ConcurrentUpdateException;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.rules.ModelProcessDetails;
import ru.yandex.market.mbo.gwt.models.rules.ModelProcessStatus;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleResult;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTask;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTaskDetails;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTaskStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class ModelRuleTaskServiceStub implements ModelRuleTaskService {

    private Map<Long, ModelRuleTask> taskBySetId = new HashMap<>();
    private Map<Long, List<ModelRuleTask>> taskByCategoryId = new HashMap<>();

    private int periodForRandom = 1000;

    private ModelRuleTask genNewTask(long setId, long categoryId) {
        ModelRuleTask task = new ModelRuleTask();
        ModelRuleSet set = new ModelRuleSet();
        set.setId(setId);
        set.setCategoryId(categoryId);
        task.setRuleSet(set);
        taskBySetId.put(setId, task);
        taskByCategoryId.computeIfAbsent(categoryId, v -> new ArrayList<>()).add(task);
        return task;
    }


    @Override
    public List<ModelRuleTask> getTaskList(Set<Long> ruleSetIds) {
        if (CollectionUtils.isEmpty(ruleSetIds)) {
            return Collections.emptyList();
        }
        int categoryId = new Random().nextInt(periodForRandom);
        return ruleSetIds.stream().filter(Objects::nonNull)
            .map(id -> taskBySetId.getOrDefault(id, genNewTask(id, categoryId))).collect(toList());
    }

    @Override
    public List<ModelRuleTask> getTaskList(long categoryId) {
        int setId = new Random().nextInt(periodForRandom);
        return taskByCategoryId.getOrDefault(categoryId, Collections.singletonList(genNewTask(setId, categoryId)));
    }

    @Override
    public List<ModelRuleTask> getTaskQueue(long categoryId) {
        return getTaskList(categoryId);
    }

    @Override
    public void registerTaskResume(ModelRuleTask task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelRuleTaskStatus registerModelProcessResult(long ruleTaskId, CommonModel model,
                                                          ModelProcessStatus processStatus, String description,
                                                          Collection<ParameterValues> newParamValues, Map<Long,
        ParameterValues> oldParamValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelRuleTaskStatus registerModelRollbackResult(ModelProcessDetails modelState,
                                                           ModelProcessStatus rollbackStatus, String description,
                                                           List<Long> revertedParamIds, Long categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, ModelProcessDetails> processTaskModels(Long taskId, List<ModelProcessStatus> statusesParam) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ModelProcessDetails> getModelProcessDetails(Long taskId, long categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelRuleTaskDetails getCommonStatisticForTask(Long taskId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelRuleResult getModelChangedParameters(Long modelRuleTaskId, long modelId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelRuleTask getTask(long taskId, boolean fetchRuleSet) {
        Optional<ModelRuleTask> t1 = taskBySetId.values().stream().filter(t -> t.getId() == taskId).findAny();
        if (t1.isPresent()) {
            return t1.get();
        }
        Optional<ModelRuleTask> t2 =
            taskByCategoryId.values().stream().flatMap(List::stream).filter(t -> t.getId() == taskId).findAny();
        if (t2.isPresent()) {
            return t2.get();
        }
        return null;
    }

    @Override
    public void failTaskInProgress(long taskId, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelRuleTask createTask(ModelRuleSet ruleSet, long uid, Consumer<ModelRuleTask> creationCallback)
        throws OperationException {
        ModelRuleTask task = new ModelRuleTask();
        task.setRuleSet(ruleSet);
        taskBySetId.put(ruleSet.getId(), task);
        taskByCategoryId.computeIfAbsent(ruleSet.getCategoryId(), v -> new ArrayList<>()).add(task);
        return task;
    }

    @Override
    public ModelRuleTask rollbackTask(Long modelRuleTaskId, long uid, Consumer<ModelRuleTask> rollbackCallback)
        throws OperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTaskStatus(ModelRuleTask ruleTask, Long uid, ModelRuleTaskStatus status, String message)
        throws ConcurrentUpdateException {
        ModelRuleTask task = getTask(ruleTask.getId(), false);
        task.setStatus(status);
    }
}
