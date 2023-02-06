package ru.yandex.market.markup3.yang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.toloka.client.staging.trait.Trait;
import ru.yandex.toloka.client.staging.trait.TraitSearchRequest;
import ru.yandex.toloka.client.v1.BatchCreateResult;
import ru.yandex.toloka.client.v1.ModificationResult;
import ru.yandex.toloka.client.v1.SearchResult;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolution;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionOperation;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionSearchRequest;
import ru.yandex.toloka.client.v1.aggregatedsolutions.PoolAggregatedSolutionRequest;
import ru.yandex.toloka.client.v1.assignment.Assignment;
import ru.yandex.toloka.client.v1.assignment.AssignmentSearchRequest;
import ru.yandex.toloka.client.v1.assignment.AssignmentStatus;
import ru.yandex.toloka.client.v1.operation.Operation;
import ru.yandex.toloka.client.v1.operation.OperationStatus;
import ru.yandex.toloka.client.v1.pool.Pool;
import ru.yandex.toloka.client.v1.pool.PoolCloneOperation;
import ru.yandex.toloka.client.v1.pool.PoolOpenOperation;
import ru.yandex.toloka.client.v1.pool.PoolStatus;
import ru.yandex.toloka.client.v1.skill.Skill;
import ru.yandex.toloka.client.v1.solution.Solution;
import ru.yandex.toloka.client.v1.task.Task;
import ru.yandex.toloka.client.v1.task.TaskCreateRequestParameters;
import ru.yandex.toloka.client.v1.task.TaskSearchRequest;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuite;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteCreateRequestParameters;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteOverlapPatch;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuitePatch;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteSearchRequest;

public class TolokaClientMock extends AbstractTolokaClientMock {

    private final Map<String, Pool> poolMap = new HashMap<>();

    private final Map<String, TaskSuite> taskSuiteMap = new HashMap<>();

    private final Map<String, Task> tasksMap = new LinkedHashMap<>();

    private final Map<String, Map<String, Object>> taskAggregatedResults = new HashMap<>();

    private final Map<String, Assignment> assignmentsByTaskSuite = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Operation> operationMap = new HashMap<>();

    private final AtomicInteger ids = new AtomicInteger(0);

    private List<Task> findTaskResult = Collections.emptyList();

    private boolean shouldThrowOnce = false;
    private boolean shouldCreateOnThrowOnce = false;

    @Override
    public ModificationResult<Pool> createPool(Pool pool) {
        if (pool.getId() == null) {
            ReflectionTestUtils.setField(pool, "id", nextId());
        }
        ReflectionTestUtils.setField(pool, "status", PoolStatus.CLOSED);
        poolMap.put(pool.getId(), pool);
        return new ModificationResult<>(pool, true);
    }

    @Override
    public ModificationResult<Pool> updatePool(String s, Pool pool) {
        poolMap.put(pool.getId(), pool);
        return new ModificationResult<>(pool, false);
    }

    private String nextId() {
        return String.valueOf(ids.incrementAndGet());
    }

    @Override
    public PoolCloneOperation clonePool(String poolId) {
        Pool pool = getPool(poolId);
        if (pool == null) {
            throw new IllegalArgumentException("Clone pool failed pool not found id=" + poolId);
        }
        JsonNode node = objectMapper.valueToTree(pool);
        try {
            Pool newPool = objectMapper.treeToValue(node, Pool.class);
            Pool created = createPool(newPool).getResult();

            String operationId = generateId();
            PoolCloneOperation.Parameters parameters = new PoolCloneOperation.Parameters();
            ReflectionTestUtils.setField(parameters, "poolId", poolId);
            Map<String, Object> details = Map.of("pool_id", created.getId());

            PoolCloneOperation operation = Mockito.mock(PoolCloneOperation.class);
            Mockito.when(operation.getId()).thenReturn(operationId);
            Mockito.when(operation.getParameters()).thenReturn(parameters);
            Mockito.when(operation.getDetailsAsMap()).thenReturn(details);
            Mockito.when(operation.getStatus()).thenReturn(OperationStatus.SUCCESS);

            operationMap.put(operationId, operation);
            return operation;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AggregatedSolutionOperation aggregateSolutionsByPool(PoolAggregatedSolutionRequest request) {
        String operationId = generateId();

        AggregatedSolutionOperation operation = Mockito.mock(AggregatedSolutionOperation.class);
        Mockito.when(operation.getId()).thenReturn(operationId);
        Mockito.when(operation.getStatus()).thenReturn(OperationStatus.PENDING);
        operationMap.put(operationId, operation);
        return operation;
    }

    public void finishAllOperations() {
        operationMap.forEach((operationId, operation) -> {
            Mockito.when(operation.getStatus()).thenReturn(OperationStatus.SUCCESS);
        });
    }

    @Override
    public SearchResult<AggregatedSolution> findAggregatedSolutions(String operationId,
                                                                    AggregatedSolutionSearchRequest request) {
        SearchResult<AggregatedSolution> searchResult = new SearchResult<>();

        List<AggregatedSolution> items = tasksMap.values().stream()
            .filter(task -> taskAggregatedResults.containsKey(task.getId())).map(task -> {
                AggregatedSolution solution = new AggregatedSolution();
                ReflectionTestUtils.setField(solution, "taskId", task.getId());
                ReflectionTestUtils.setField(solution, "poolId", task.getPoolId());
                ReflectionTestUtils.setField(solution, "confidence", 0.5);
                ReflectionTestUtils.setField(solution, "outputValues", taskAggregatedResults.get(task.getId()));
                return solution;
            }).collect(Collectors.toList());

        ReflectionTestUtils.setField(searchResult, "items", items);
        return searchResult;
    }

    @Override
    public <T extends Operation<?, ?>> T getOperation(String operationId) {
        return (T) operationMap.get(operationId);
    }

    @Override
    public Pool getPool(String poolId) {
        return poolMap.get(poolId);
    }

    public void setPoolClosed(String poolId) {
        Pool pool = poolMap.get(poolId);
        ReflectionTestUtils.setField(pool, "status", PoolStatus.CLOSED);
    }

    @Override
    public ModificationResult<TaskSuite> createTaskSuite(TaskSuite taskSuite,
                                                         TaskSuiteCreateRequestParameters params) {
        if (shouldThrowOnce) {
            shouldThrowOnce = false;
            if (shouldCreateOnThrowOnce) {
                shouldCreateOnThrowOnce = false;
                setId(taskSuite);
                taskSuite.getTasks().forEach(this::setId);
                taskSuiteMap.put(taskSuite.getId(), taskSuite);
            }
            throw new RuntimeException("Should throw");
        }
        setId(taskSuite);
        taskSuite.getTasks().forEach(this::setId);
        taskSuiteMap.put(taskSuite.getId(), taskSuite);
        if (params.getOpenPool()) {
            Pool pool = poolMap.get(taskSuite.getPoolId());
            ReflectionTestUtils.setField(pool, "status", PoolStatus.OPEN);
        }
        return new ModificationResult<>(taskSuite, true);
    }

    @Override
    public BatchCreateResult<Task> createTasks(List<Task> tasks, TaskCreateRequestParameters parameters) {
        var resultMap = new LinkedHashMap<Integer, Task>();
        AtomicInteger i = new AtomicInteger(0);
        tasks.stream().peek(this::setId).forEach(it -> {
            tasksMap.put(it.getId(), it);
            resultMap.put(i.getAndIncrement(), it);
        });
        if (shouldThrowOnce) {
            shouldThrowOnce = false;
            throw new RuntimeException("Should throw");
        }
        var result = new BatchCreateResult<Task>();
        ReflectionTestUtils.setField(result, "items", resultMap);
        ReflectionTestUtils.setField(result, "validationsErrors", Map.of());

        if (parameters.getOpenPool()) {
            Pool pool = poolMap.get(tasks.get(0).getPoolId());
            ReflectionTestUtils.setField(pool, "status", PoolStatus.OPEN);
        }
        return result;
    }

    @Override
    public SearchResult<Assignment> findAssignments(AssignmentSearchRequest request) {
        //limits, sort and filters not implemented
        Map<String, Object> queryParameters = request.getQueryParameters();
        String poolId = queryParameters.containsKey("pool_id") ? queryParameters.get("pool_id").toString() : null;
        String status = queryParameters.containsKey("status") ? queryParameters.get("status").toString() : null;
        SearchResult<Assignment> result = new SearchResult<>();
        assignmentsByTaskSuite.values().stream()
            .filter(a -> {
                boolean filtered = true;
                if (poolId != null) {
                    filtered = filtered && a.getPoolId().equals(poolId);
                }
                if (status != null) {
                    filtered = filtered && a.getStatus().toString().equals(status);
                }
                return filtered;
            })
            .forEach(a -> result.getItems().add(a));

        return result;
    }

    @Override
    public ModificationResult<TaskSuite> patchTaskSuite(String taskSuiteId, TaskSuitePatch patch) {
        var taskSuite = taskSuiteMap.get(taskSuiteId);
        if (taskSuite == null) {
            throw new IllegalArgumentException("TaskSuite not found id = " + taskSuiteId);
        }
        ReflectionTestUtils.setField(taskSuite, "issuingOrderOverride", patch.getIssuingOrderOverride());
        ReflectionTestUtils.setField(taskSuite, "overlap", patch.getOverlap());
        return new ModificationResult<>(taskSuite, false);
    }

    @Override
    public SearchResult<TaskSuite> findTaskSuites(TaskSuiteSearchRequest request) {
        //limits, sort and filters not implemented
        Map<String, Object> queryParameters = request.getQueryParameters();
        String poolId = queryParameters.containsKey("pool_id") ? queryParameters.get("pool_id").toString() : null;
        SearchResult<TaskSuite> result = new SearchResult<>();
        taskSuiteMap.values().stream()
            .filter(a -> {
                boolean filtered = true;
                if (poolId != null) {
                    filtered = filtered && a.getPoolId().equals(poolId);
                }
                return filtered;
            })
            .forEach(a -> result.getItems().add(a));
        return result;
    }

    @Override
    public SearchResult<Task> findTasks(TaskSearchRequest request) {
        SearchResult<Task> result = new SearchResult<>();
        findTaskResult.forEach(task -> result.getItems().add(task));
        return result;
    }

    public void setFindTasksResult(List<Task> result) {
        findTaskResult = result;
    }

    public void setFindTasksResultFromTaskMap() {
        setFindTasksResult(new ArrayList<>(tasksMap.values()));
    }

    @Override
    public ModificationResult<TaskSuite> setOverlapOrMin(String taskSuiteId, TaskSuiteOverlapPatch overlapPatch) {
        var taskSuite = taskSuiteMap.get(taskSuiteId);
        if (taskSuite == null) {
            throw new IllegalArgumentException("TaskSuite not found id = " + taskSuiteId);
        }
        ReflectionTestUtils.setField(taskSuite, "overlap", overlapPatch.getOverlap());
        return new ModificationResult<>(taskSuite, false);
    }

    @Override
    public TaskSuite getTaskSuite(String taskSuiteId) {
        return taskSuiteMap.get(taskSuiteId);
    }

    @Override
    public Task getTask(String taskId) {
        return tasksMap.get(taskId);
    }

    public Map<String, Task> getAllTasks() {
        return tasksMap;
    }

    public void addAssignment(Assignment assignment) {
        assignmentsByTaskSuite.put(assignment.getTaskSuiteId(), assignment);
    }

    public String setTaskFinished(String taskSuiteId, Map<String, Object> outputValues, String workerId) {
        TaskSuite taskSuite = taskSuiteMap.get(taskSuiteId);
        if (taskSuite == null) {
            throw new IllegalArgumentException("Task suite " + taskSuiteId + " not found");
        }
        Pool pool = poolMap.get(taskSuite.getPoolId());
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found");
        }
        Assignment assignment = new Assignment();
        ReflectionTestUtils.setField(assignment, "poolId", taskSuite.getPoolId());
        ReflectionTestUtils.setField(assignment, "taskSuiteId", taskSuite.getId());

        List<Task> tasks = new ArrayList<>();
        List<Solution> solutions = new ArrayList<>();
        taskSuite.getTasks().forEach(it -> {
            Task someTask = new Task(taskSuite.getPoolId(), Map.of());
            ReflectionTestUtils.setField(someTask, "id", it.getId());
            tasks.add(someTask);
            Solution solution = new Solution();
            ReflectionTestUtils.setField(solution, "outputValues", outputValues);
            solutions.add(solution);
        });
        ReflectionTestUtils.setField(assignment, "tasks", tasks);
        ReflectionTestUtils.setField(assignment, "solutions", solutions);
        ReflectionTestUtils.setField(assignment, "userId", workerId);
        ReflectionTestUtils.setField(assignment, "id", generateId());
        ReflectionTestUtils.setField(assignment, "status", AssignmentStatus.SUBMITTED);

        addAssignment(assignment);
        return assignment.getId();
    }

    public void addActiveAssignment(String taskSuiteId) {
        TaskSuite taskSuite = taskSuiteMap.get(taskSuiteId);
        if (taskSuite == null) {
            throw new IllegalArgumentException("Task suite " + taskSuiteId + " not found");
        }
        Pool pool = poolMap.get(taskSuite.getPoolId());
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found");
        }
        Assignment assignment = new Assignment();
        ReflectionTestUtils.setField(assignment, "poolId", taskSuite.getPoolId());
        ReflectionTestUtils.setField(assignment, "taskSuiteId", taskSuite.getId());

        ReflectionTestUtils.setField(assignment, "id", generateId());
        ReflectionTestUtils.setField(assignment, "status", AssignmentStatus.ACTIVE);
        addAssignment(assignment);
    }

    @Override
    public SearchResult<Trait> findTraits(TraitSearchRequest request) {
        TraitSearchResult traitSearchResult = new TraitSearchResult();
        ReflectionTestUtils.setField(traitSearchResult, "items", Collections.emptyList());
        return traitSearchResult;
    }

    @Override
    public ModificationResult<Skill> createSkill(Skill skill) {
        setId(skill);
        return new ModificationResult(skill, true);
    }

    @Override
    public PoolOpenOperation openPool(String poolId) {
        Pool pool = poolMap.get(poolId);
        ReflectionTestUtils.setField(pool, "status", PoolStatus.OPEN);

        String operationId = generateId();
        PoolOpenOperation operation = Mockito.mock(PoolOpenOperation.class);
        Mockito.when(operation.getId()).thenReturn(operationId);
        Mockito.when(operation.getStatus()).thenReturn(OperationStatus.SUCCESS);
        operationMap.put(operationId, operation);
        return operation;
    }

    private <T> void setId(T val) {
        ReflectionTestUtils.setField(val, "id", generateId());
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    public TolokaClientMock setShouldThrowOnce(boolean shouldThrowOnce) {
        this.shouldThrowOnce = shouldThrowOnce;
        return this;
    }

    public TolokaClientMock setShouldCreateOnThrowOnce(boolean shouldCreateOnThrowOnce) {
        this.shouldCreateOnThrowOnce = shouldCreateOnThrowOnce;
        return this;
    }

    public void addTaskAggregatedResult(String taskId, Map<String, Object> output) {
        taskAggregatedResults.put(taskId, output);
    }

    public void resetAllTasks() {
        tasksMap.clear();
    }
}

class TraitSearchResult extends SearchResult<Trait> {
    public TraitSearchResult() {
    }
}
