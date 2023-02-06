package ru.yandex.market.toloka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.toloka.model.Filter;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.PoolCloseReason;
import ru.yandex.market.toloka.model.PoolStatus;
import ru.yandex.market.toloka.model.Result;
import ru.yandex.market.toloka.model.ResultItem;
import ru.yandex.market.toloka.model.ResultItemStatus;
import ru.yandex.market.toloka.model.Skill;
import ru.yandex.market.toloka.model.Solution;
import ru.yandex.market.toloka.model.Task;
import ru.yandex.market.toloka.model.TaskSuite;
import ru.yandex.market.toloka.model.Trait;
import ru.yandex.market.toloka.model.UserSkill;
import ru.yandex.market.toloka.model.UserTrait;
import ru.yandex.market.toloka.operation.OperationStatus;
import ru.yandex.utils.Pair;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author galaev
 * @since 2019-06-10
 */
public class TolokaApiStub extends TolokaApi {
    public static final String SKILL_EQ = "EQ1";
    private Map<Integer, Pool> pools = new HashMap<>();
    private Multimap<Integer, TaskSuite> taskSuitesByPoolId = ArrayListMultimap.create();
    private Multimap<Integer, Result> resultsByPoolId = ArrayListMultimap.create();
    private Map<String, Skill> skills = new HashMap<>();
    private Set<String> traits = new HashSet<>();
    private Multimap<String, UserSkill> userSkills = ArrayListMultimap.create();
    private Multimap<String, String> userTraits = ArrayListMultimap.create();
    private int idSeq = 1;

    public TolokaApiStub(TolokaApiConfiguration configuration) {
        super(configuration, null);
    }

    @Override
    public Pool createPool(Pool pool) {
        return pools.put(pool.getId(), pool);
    }

    @Override
    public Pool getPoolInfo(int poolId) {
        return pools.get(poolId);
    }

    @Override
    public int clonePool(int basePoolId) {
        Pool pool = pools.get(basePoolId);
        Pool newPool = new Pool(pool);
        int newPoolId = ++idSeq;
        newPool.setId(newPoolId);
        newPool.setStatus(PoolStatus.OPEN);
        pools.put(newPoolId, newPool);
        return newPoolId;
    }

    @Override
    public Pool updatePool(Pool pool) {
        pools.put(pool.getId(), pool);
        return pool;
    }

    @Override
    public TolokaResponse closePool(int poolId) {
        Pool pool = pools.get(poolId);
        pool.setStatus(PoolStatus.CLOSED);
        TolokaResponse response = new TolokaResponse();
        response.setStatus(OperationStatus.SUCCESS);
        return response;
    }

    @Override
    public TolokaResponse openPool(int poolId) {
        Pool pool = pools.get(poolId);
        pool.setStatus(PoolStatus.OPEN);
        TolokaResponse response = new TolokaResponse();
        response.setStatus(OperationStatus.SUCCESS);
        return response;
    }

    @Override
    public TolokaResponse createTaskSuite(TaskSuite taskSuite,
                                          boolean allowDefaults, boolean openPool, boolean skipInvalidItems) {
        addTaskSuite(taskSuite);
        TolokaResponse response = new TolokaResponse();
        response.setStatus(OperationStatus.SUCCESS);
        return response;
    }

    @Override
    public TaskSuite changeTaskSuiteOrder(String taskSuiteId, double value) {
        return getTaskSuite(taskSuiteId).setIssuingOrderOverride(value);
    }

    @Override
    public TaskSuite createSingleTaskSuite(TaskSuite taskSuite, boolean allowDefaults, boolean openPool) {
        addTaskSuite(taskSuite);
        return taskSuite;
    }

    private void addTaskSuite(TaskSuite taskSuite) {
        taskSuite.setId(newId());
        taskSuite.getTasks().forEach(t -> t.setId(newId()));
        taskSuite.setCreated(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME));
        taskSuitesByPoolId.put(taskSuite.getPoolId(), taskSuite);
    }

    @Override
    public List<TaskSuite> getTaskSuites(int poolId) {
        return new ArrayList<>(taskSuitesByPoolId.get(poolId));
    }

    @Override
    public Task createTask(Task task, boolean allowDefaults, boolean openPool) {
        task.setId(newId());
        TaskSuite taskSuite = new TaskSuite()
            .setId(newId())
            .setPoolId(task.getPoolId())
            .setTasks(Collections.singletonList(task));

        createTaskSuite(taskSuite, allowDefaults, openPool, true);
        return task;
    }

    public Collection<TaskSuite> getAllTaskSuites() {
        return taskSuitesByPoolId.values();
    }

    @Override
    public List<ResultItem> getResult(int poolId) {
        Collection<Result> results = resultsByPoolId.get(poolId);
        return results.size() > 0 ? results.iterator().next().getItems() : Collections.emptyList();
    }

    @Override
    public List<ResultItem> getResult(int poolId, Optional<String> taskId, ResultItemStatus... statuses) {
        List<ResultItemStatus> statusesList = Arrays.asList(statuses);
        Collection<Result> results = resultsByPoolId.get(poolId);
        return results.stream().flatMap(r -> r.getItems().stream())
            .filter(resultItem -> statusesList.contains(resultItem.getStatus()))
            .filter(resultItem -> checkTasks(resultItem.getTasks(), taskId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ResultItem> getResult(int poolId, Optional<String> taskId, Optional<String> lastSubmittedDate,
                                      Optional<String> lastSubmittedUpper,
                                      ResultItemStatus... statuses) {
        List<ResultItemStatus> statusesList = Arrays.asList(statuses);
        Collection<Result> results = resultsByPoolId.get(poolId);
        return results.stream().flatMap(r -> r.getItems().stream())
                .filter(resultItem -> statusesList.contains(resultItem.getStatus()))
                .filter(resultItem -> checkTasks(resultItem.getTasks(), taskId))
                .filter(resultItem -> !lastSubmittedDate.isPresent()
                        || resultItem.getSubmitted().compareTo(lastSubmittedDate.get()) >= 0)
                .collect(Collectors.toList());
    }

    @Override
    public Skill getOrCreateSkillByName(String skillName) {
        return skills.computeIfAbsent(skillName, name -> {
            Skill skill = new Skill();
            skill.setId(String.valueOf(++idSeq));
            skill.setName(name);
            return skill;
        });
    }

    public String filtersToString(Filter filter) {
        if (filter == null) {
            return "";
        }
        if (filter.getAnd() != null && filter.getAnd().size() > 0) {
            return filter.getAnd().stream()
                    .map(this::filtersToString)
                    .sorted()
                    .collect(Collectors.joining(" & "));
        } else
        if (filter.getOr() != null && filter.getOr().size() > 0){
            return filter.getOr().stream()
                    .map(this::filtersToString)
                    .sorted()
                    .collect(Collectors.joining(" || "));
        } else {
            //right now only skill filters
            String skillName = skills.values().stream()
                .filter(s -> s.getId().equals(filter.getKey()))
                .map(Skill::getName)
                .findFirst()
                .orElse("???");
            return skillName + filter.getOperator() + filter.getValue().getIntegerValue();
        }
    }

    public void addActiveAssignment(int poolId) {
        addAssignment(poolId, ResultItemStatus.ACTIVE);
    }

    public void addAssignment(String taskId, ResultItemStatus resultItemStatus) {
        Pair<String, Task> taskWithSuite = findTaskAndSuite(taskId);
        int poolId = taskWithSuite.getSecond().getPoolId();
        List<ResultItem> activeResult = new ArrayList<>();
        activeResult.add(new ResultItem()
            .setPoolId(poolId)
            .setId(newId())
            .setTaskSuiteId(taskWithSuite.getFirst())
            .setStatus(resultItemStatus)
            .setTasks(Collections.singletonList(taskWithSuite.getSecond()))
        );
        List<ResultItem> currentResult = getResult(poolId);
        activeResult.addAll(currentResult);
        resultsByPoolId.put(poolId, new Result(activeResult, false));
    }

    public void addAssignment(int poolId, ResultItemStatus resultItemStatus) {
        addAssignment(poolId, newId(), resultItemStatus);
    }

    public void addAssignment(int poolId, String taskSuiteId, ResultItemStatus resultItemStatus) {
        List<ResultItem> activeResult = new ArrayList<>();
        activeResult.add(new ResultItem()
            .setPoolId(poolId)
            .setId(newId())
            .setTaskSuiteId(taskSuiteId)
            .setStatus(resultItemStatus));
        List<ResultItem> currentResult = getResult(poolId);
        activeResult.addAll(currentResult);
        resultsByPoolId.put(poolId, new Result(activeResult, false));
    }

    public void removeAssignments(int poolId) {
        resultsByPoolId.removeAll(poolId);
    }

    public void removeAssignments(int poolId, String taskSuiteId) {
        resultsByPoolId.get(poolId).removeIf(r -> r.getItems().stream()
                .filter(ri -> ri.getTaskSuiteId().equals(taskSuiteId))
                .findFirst()
                .isPresent()
        );
    }

    public <T extends IResponseItem> void addResults(int poolId, List<T> resultsList, String workerId) {
        addResults(poolId, resultsList, workerId, new JsonUtils.DefaultJsonSerializer<>());
    }

    public <T extends IResponseItem> void addYangTaskResults(String taskId,
                                                             List<T> resultsList,
                                                             String workerId,
                                                             String key) {
        addYangTaskResults(taskId, workerId,
            Collections.singletonMap(key, convertResultsToJson(resultsList)));
    }

    public <T extends IResponseItem> void addYangTaskResults(String taskId,
                                                             String workerId,
                                                             Map<String, Object> results) {
        Pair<String, Task> taskWithSuiteId = findTaskAndSuite(taskId);
        Task task = taskWithSuiteId.getSecond();
        completePool(task.getPoolId());
        List<ResultItem> resultItems = new ArrayList<>();
        resultItems.add(new ResultItem()
            .setId(newId())
            .setTaskSuiteId(taskWithSuiteId.getFirst())
            .setUserId(workerId)
            .setPoolId(task.getPoolId())
            .setStatus(ResultItemStatus.ACCEPTED)
            .setTasks(Collections.singletonList(task))
            .setSubmitted(TolokaApi.DATE_FORMAT.format(new Date()))
            .setSolutions(Collections.singletonList(new Solution(results))));

        resultsByPoolId.put(task.getPoolId(),  new Result(resultItems, false));
    }

    public <T extends IResponseItem> void addResults(int poolId,
                                                     List<T> resultsList,
                                                     String workerId,
                                                     JsonSerializer<T> serializer) {
        addResults(poolId, Optional.empty(), resultsList, workerId, serializer);
    }

    public <T extends IResponseItem> void addResults(int poolId,
                                                     Optional<String> taskId,
                                                     List<T> resultsList,
                                                     String workerId,
                                                     JsonSerializer<T> serializer) {
        completePool(poolId);

        List<JsonNode> nodes = convertResultsToJson(resultsList, serializer);

        List<ResultItem> resultItems = new ArrayList<>();
        String taskSuiteId = null;
        List<Task> tasks = null;
        for (TaskSuite taskSuite : taskSuitesByPoolId.get(poolId)) {
            if (checkTasks(taskSuite.getTasks(), taskId)) {
                taskSuiteId = taskSuite.getId();
                tasks = taskSuite.getTasks();
                break;
            }
        }
        assert taskSuiteId != null;

        for (JsonNode jsonItem : nodes) {
            Map<String, Object> outputValues = new HashMap<>();
            jsonItem.fields().forEachRemaining(entry -> outputValues.put(entry.getKey(), entry.getValue()));
            resultItems.add(new ResultItem()
                .setId(newId())
                .setTaskSuiteId(taskSuiteId)
                .setPoolId(poolId)
                .setUserId(workerId)
                .setTasks(tasks)
                .setStatus(ResultItemStatus.ACCEPTED)
                .setSubmitted(TolokaApi.DATE_FORMAT.format(new Date()))
                .setSolutions(Collections.singletonList(new Solution(outputValues))));
        }
        Result result = new Result(resultItems, false);
        resultsByPoolId.put(poolId, result);
    }

    public <T extends IResponseItem> void addSingleTaskResults(int poolId,
                                                               List<T> resultsList,
                                                               String workerId,
                                                               String key) {
        List<JsonNode> nodes = convertResultsToJson(resultsList);
        completePool(poolId);

        List<TaskSuite> suites = getTaskSuites(poolId);
        assertThat(suites).hasSize(1);
        List<Task> tasks = suites.get(0).getTasks();
        assertThat(tasks).hasSize(1);

        List<ResultItem> resultItems = new ArrayList<>();
        resultItems.add(new ResultItem()
                .setId(newId())
                .setTaskSuiteId(suites.get(0).getId())
                .setUserId(workerId)
                .setPoolId(poolId)
                .setStatus(ResultItemStatus.ACCEPTED)
                .setTasks(Collections.singletonList(tasks.get(0)))
                .setSolutions(Collections.singletonList(new Solution(Collections.singletonMap(key, nodes)))));

        resultsByPoolId.put(poolId,  new Result(resultItems, false));
    }

    public <T extends IResponseItem> void addResultsBlueLogsScheme(int poolId, List<T> resultsList,
                                                                   boolean inspection,
                                                                   String workerId) {
        completePool(poolId);

        List<JsonNode> nodes = convertResultsToJson(resultsList);

        Map<String, Object> outputValues = new HashMap<>();
        if (inspection) {
            outputValues.put("output", nodes.get(0));
        } else {
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("task_result", nodes);
            outputValues.put("output", taskResult);
        }

        Result result = createResult(poolId, workerId, outputValues);
        resultsByPoolId.put(poolId, result);
    }

    @Override
    public Task getTask(String taskId) {
        return taskSuitesByPoolId.values().stream()
            .flatMap(ts -> ts.getTasks().stream())
            .filter(t -> t.getId().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No such task"));
    }

    @Override
    public TaskSuite getTaskSuite(String taskSuiteId) {
        return taskSuitesByPoolId.values().stream()
            .filter(t -> t.getId().equals(taskSuiteId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No such task suite"));
    }

    public Double getTaskSuiteOrder(String taskSuiteId) {
        return getTaskSuite(taskSuiteId).getIssuingOrderOverride();
    }

    @Override
    public TaskSuite cancelTaskSuite(String taskSuiteId) {
        return taskSuitesByPoolId.values().stream()
            .filter(ts -> ts.getId().equals(taskSuiteId))
            .findFirst()
            .map(ts -> {
                ts.setOverlap(0);
                return ts;
            }).orElseThrow(() -> new IllegalArgumentException("No such task suite"));
    }

    @Override
    public Task cancelTask(String taskId) {
        Task task = getTask(taskId);
        task.setOverlap(0);
        return task;
    }

    @Override
    public void addTrait(String trait) {
        traits.add(trait);
    }

    @Override
    public List<Trait> getAllTraits() {
        return traits.stream().map(t -> {
            Trait tr = new Trait();
            tr.setName(t);
            return tr;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserTrait> getUserTraits(String workerId) {
        return userTraits.get(workerId).stream().map(tr -> new UserTrait(workerId, tr)).collect(Collectors.toList());
    }

    @Override
    public Trait.ModifyUserTraitsResponse modifyUserTraits(String workerId, Collection<String> traitsToAdd,
                                                           Collection<String> traitsToRemove) {
        Collection<String> curUserTraits = userTraits.get(workerId);
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();

        curUserTraits.removeIf(tr -> {
            boolean r = traitsToRemove.contains(tr);
            if (r) {
                removed.add(tr);
            }
            return r;
        });
        traitsToAdd.stream().filter(tr -> !curUserTraits.contains(tr)).forEach(tr -> {
            added.add(tr);
            curUserTraits.add(tr);
        });
        return new Trait.ModifyUserTraitsResponse(workerId, added, removed);
    }

    @Override
    public UserSkill addUserSkill(String workerId, String skillId, double value) {
        UserSkill result = new UserSkill(newId(), skillId, workerId, value);
        userSkills.put(workerId, result);
        return result;
    }

    @Override
    public List<UserSkill> getUserSkills(String workerId) {
        return new ArrayList<>(userSkills.get(workerId));
    }

    @Override
    public void deleteUserSkill(UserSkill userSkill) {
        userSkills.values().removeIf(us -> us.getId().equals(userSkill.getId()));
    }

    public <T extends IResponseItem> void addResultsModeration(int poolId, List<T> resultsList,
                                                               String workerId) {
        Result result = createResult(poolId, workerId, convertModeration(resultsList));
        resultsByPoolId.put(poolId, result);
    }

    public <T extends IResponseItem> Map<String, Object> convertModeration(List<T> results) {
        List<JsonNode> jsonResults = convertResultsToJson(results);
        Map<String, Object> wrappedResults = Collections.singletonMap("results", jsonResults);
        return Collections.singletonMap("output", wrappedResults);
    }

    public <T extends IResponseItem> Map<String, Object> convertBlueLogs(List<T> results) {
        List<JsonNode> jsonResults = convertResultsToJson(results);
        Map<String, Object> wrappedResults = Collections.singletonMap("task_result", jsonResults);
        return Collections.singletonMap("output", wrappedResults);
    }

    public <T extends IResponseItem> Map<String, Object> convertBlueLogsInspection(List<T> results) {
        List<JsonNode> jsonResults = convertResultsToJson(results);
        return Collections.singletonMap("output", jsonResults.get(0));
    }

    private Pair<String, Task> findTaskAndSuite(String taskId) {
        return taskSuitesByPoolId.values().stream()
            .flatMap(ts -> ts.getTasks().stream().map(t -> Pair.makePair(ts.getId(), t)))
            .filter(t -> t.getSecond().getId().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No such task"));
    }

    private <T extends IResponseItem> List<JsonNode> convertResultsToJson(List<T> resultsList) {
        return convertResultsToJson(resultsList, new JsonUtils.DefaultJsonSerializer<>());
    }

    private <T extends IResponseItem> List<JsonNode> convertResultsToJson(List<T> resultsList,
                                                                          JsonSerializer<T> serializer) {
        ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(
                new SimpleModule()
                    .addSerializer((Class<? extends T>) resultsList.get(0).getClass(), serializer)
            );

        List<JsonNode> nodes = new ArrayList<>();
        for (T result : resultsList) {
            nodes.add(objectMapper.valueToTree(result));
        }
        return nodes;
    }

    private Result createResult(int poolId, String workerId, Map<String, Object> outputValues) {
        List<ResultItem> resultItems = new ArrayList<>();
        taskSuitesByPoolId.get(poolId).forEach(taskSuite ->
            resultItems.add(new ResultItem()
                .setId(newId())
                .setTaskSuiteId(taskSuite.getId())
                .setUserId(workerId)
                .setPoolId(poolId)
                .setTasks(taskSuite.getTasks())
                .setSolutions(Collections.singletonList(new Solution(outputValues)))));
        return new Result(resultItems, false);
    }

    private void completePool(int poolId) {
        Pool pool = getPoolInfo(poolId);
        pool.setStatus(PoolStatus.CLOSED);
        pool.setLastCloseReason(PoolCloseReason.COMPLETED);
        updatePool(pool);
    }

    private boolean checkTasks(List<Task> tasks, Optional<String> taskId) {
        return taskId.isPresent() ? tasks
            .stream().filter(t -> t.getId().equals(taskId.get())).findAny().isPresent() : true;
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }

}
