package ru.yandex.market.markup3.yang;

import java.util.Iterator;
import java.util.List;

import ru.yandex.toloka.client.staging.trait.Trait;
import ru.yandex.toloka.client.staging.trait.TraitClient;
import ru.yandex.toloka.client.staging.trait.TraitSearchRequest;
import ru.yandex.toloka.client.staging.usertrait.UserTrait;
import ru.yandex.toloka.client.staging.usertrait.UserTraitClient;
import ru.yandex.toloka.client.staging.usertrait.UserTraitPatch;
import ru.yandex.toloka.client.staging.usertrait.UserTraitPatchForm;
import ru.yandex.toloka.client.staging.usertrait.UserTraitPut;
import ru.yandex.toloka.client.staging.usertrait.UserTraitSearchRequest;
import ru.yandex.toloka.client.v1.BatchCreateResult;
import ru.yandex.toloka.client.v1.ModificationResult;
import ru.yandex.toloka.client.v1.SearchResult;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolution;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionClient;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionOperation;
import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionSearchRequest;
import ru.yandex.toloka.client.v1.aggregatedsolutions.PoolAggregatedSolutionRequest;
import ru.yandex.toloka.client.v1.aggregatedsolutions.TaskAggregatedSolutionRequest;
import ru.yandex.toloka.client.v1.assignment.Assignment;
import ru.yandex.toloka.client.v1.assignment.AssignmentClient;
import ru.yandex.toloka.client.v1.assignment.AssignmentPatch;
import ru.yandex.toloka.client.v1.assignment.AssignmentSearchRequest;
import ru.yandex.toloka.client.v1.operation.Operation;
import ru.yandex.toloka.client.v1.operation.OperationClient;
import ru.yandex.toloka.client.v1.operation.OperationLogItem;
import ru.yandex.toloka.client.v1.operation.OperationSearchRequest;
import ru.yandex.toloka.client.v1.pool.Pool;
import ru.yandex.toloka.client.v1.pool.PoolArchiveOperation;
import ru.yandex.toloka.client.v1.pool.PoolClient;
import ru.yandex.toloka.client.v1.pool.PoolCloneOperation;
import ru.yandex.toloka.client.v1.pool.PoolCloseOperation;
import ru.yandex.toloka.client.v1.pool.PoolOpenOperation;
import ru.yandex.toloka.client.v1.pool.PoolPatchRequest;
import ru.yandex.toloka.client.v1.pool.PoolSearchRequest;
import ru.yandex.toloka.client.v1.skill.Skill;
import ru.yandex.toloka.client.v1.skill.SkillClient;
import ru.yandex.toloka.client.v1.skill.SkillSearchRequest;
import ru.yandex.toloka.client.v1.task.Task;
import ru.yandex.toloka.client.v1.task.TaskClient;
import ru.yandex.toloka.client.v1.task.TaskCreateBatchOperation;
import ru.yandex.toloka.client.v1.task.TaskCreateRequestParameters;
import ru.yandex.toloka.client.v1.task.TaskOverlapPatch;
import ru.yandex.toloka.client.v1.task.TaskPatch;
import ru.yandex.toloka.client.v1.task.TaskPatchRequestParameters;
import ru.yandex.toloka.client.v1.task.TaskSearchRequest;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuite;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteClient;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteCreateBatchOperation;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteCreateRequestParameters;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteOverlapPatch;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuitePatch;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuitePatchRequestParameters;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteSearchRequest;

public abstract class AbstractTolokaClientMock implements PoolClient, OperationClient, TaskSuiteClient,
    AssignmentClient, SkillClient, TraitClient, UserTraitClient, TaskClient, AggregatedSolutionClient {

    @Override
    public ModificationResult<UserTraitPut> putUserTraits(UserTraitPut userTraitPut) {
        return null;
    }

    @Override
    public ModificationResult<UserTraitPatch> patchUserTraits(UserTraitPatchForm userTraitPatchForm) {
        return null;
    }

    @Override
    public SearchResult<UserTrait> findUserTraits(UserTraitSearchRequest userTraitSearchRequest) {
        return null;
    }

    @Override
    public ModificationResult<Trait> createTrait(Trait trait) {
        return new ModificationResult(trait, true);
    }

    @Override
    public BatchCreateResult<Trait> createTraits(List<Trait> traits) {
        return null;
    }

    @Override
    public SearchResult<Trait> findTraits(TraitSearchRequest request) {
        return null;
    }

    @Override
    public Trait getTrait(String name) {
        return new Trait(name);
    }

    @Override
    public SearchResult<Assignment> findAssignments(AssignmentSearchRequest request) {
        return null;
    }

    @Override
    public Assignment getAssignment(String assignmentId) {
        return null;
    }

    @Override
    public ModificationResult<Assignment> patchAssignment(String assignmentId, AssignmentPatch patch) {
        return null;
    }

    @Override
    public ModificationResult<Assignment> acceptAssignment(String assignmentId, String publicComment) {
        return null;
    }

    @Override
    public ModificationResult<Assignment> rejectAssignment(String assignmentId, String publicComment) {
        return null;
    }

    @Override
    public SearchResult<Operation<?, ?>> findOperations(OperationSearchRequest request) {
        return null;
    }

    @Override
    public <T extends Operation<?, ?>> T getOperation(String operationId) {
        return null;
    }

    @Override
    public <T extends Operation<?, ?>> T getOperation(String operationId, Class<T> c) {
        return null;
    }

    @Override
    public Iterator<OperationLogItem> getOperationLog(String operationId) {
        return null;
    }

    @Override
    public SearchResult<Pool> findPools(PoolSearchRequest poolSearchRequest) {
        return null;
    }

    @Override
    public Pool getPool(String s) {
        return null;
    }

    @Override
    public ModificationResult<Pool> createPool(Pool pool) {
        return null;
    }

    @Override
    public ModificationResult<Pool> updatePool(String s, Pool pool) {
        return null;
    }

    @Override
    public ModificationResult<Pool> patchPool(String s, PoolPatchRequest poolPatchRequest) {
        return null;
    }

    @Override
    public PoolOpenOperation openPool(String s) {
        return null;
    }

    @Override
    public PoolCloseOperation closePool(String s) {
        return null;
    }

    @Override
    public PoolCloseOperation closePoolForUpdate(String s) {
        return null;
    }

    @Override
    public PoolArchiveOperation archivePool(String s) {
        return null;
    }

    @Override
    public PoolCloneOperation clonePool(String s) {
        return null;
    }

    @Override
    public ModificationResult<Skill> createSkill(Skill skill) {
        return null;
    }

    @Override
    public SearchResult<Skill> findSkills(SkillSearchRequest request) {
        return null;
    }

    @Override
    public Skill getSkill(String skillId) {
        return null;
    }

    @Override
    public ModificationResult<Skill> updateSkill(String skillId, Skill skill) {
        return null;
    }

    @Override
    public ModificationResult<TaskSuite> createTaskSuite(TaskSuite taskSuite) {
        return null;
    }

    @Override
    public ModificationResult<TaskSuite> createTaskSuite(TaskSuite taskSuite,
                                                         TaskSuiteCreateRequestParameters params) {
        return null;
    }

    @Override
    public BatchCreateResult<TaskSuite> createTaskSuites(List<TaskSuite> list) {
        return null;
    }

    @Override
    public BatchCreateResult<TaskSuite> createTaskSuites(List<TaskSuite> list,
                                                         TaskSuiteCreateRequestParameters params) {
        return null;
    }

    @Override
    public TaskSuiteCreateBatchOperation createTaskSuitesAsync(Iterator<TaskSuite> iterator) {
        return null;
    }

    @Override
    public TaskSuiteCreateBatchOperation createTaskSuitesAsync(Iterator<TaskSuite> iterator,
                                                               TaskSuiteCreateRequestParameters params) {
        return null;
    }

    @Override
    public SearchResult<TaskSuite> findTaskSuites(TaskSuiteSearchRequest taskSuiteSearchRequest) {
        return null;
    }

    @Override
    public TaskSuite getTaskSuite(String s) {
        return null;
    }

    @Override
    public ModificationResult<TaskSuite> patchTaskSuite(String s, TaskSuitePatch taskSuitePatch) {
        return null;
    }

    @Override
    public ModificationResult<TaskSuite> patchTaskSuite(String s, TaskSuitePatch taskSuitePatch,
                                                        TaskSuitePatchRequestParameters params) {
        return null;
    }

    @Override
    public ModificationResult<TaskSuite> setOverlapOrMin(String s, TaskSuiteOverlapPatch taskSuiteOverlapPatch) {
        return null;
    }

    @Override
    public AggregatedSolutionOperation aggregateSolutionsByPool(PoolAggregatedSolutionRequest request) {
        return null;
    }

    @Override
    public ModificationResult<AggregatedSolution> aggregateSolutionsByTask(TaskAggregatedSolutionRequest request) {
        return null;
    }

    @Override
    public SearchResult<AggregatedSolution> findAggregatedSolutions(String operationId,
                                                                    AggregatedSolutionSearchRequest request) {
        return null;
    }

    @Override
    public ModificationResult<Task> createTask(Task task) {
        return null;
    }

    @Override
    public ModificationResult<Task> createTask(Task task, TaskCreateRequestParameters parameters) {
        return null;
    }

    @Override
    public BatchCreateResult<Task> createTasks(List<Task> tasks) {
        return null;
    }

    @Override
    public BatchCreateResult<Task> createTasks(List<Task> tasks, TaskCreateRequestParameters parameters) {
        return null;
    }

    @Override
    public TaskCreateBatchOperation createTasksAsync(Iterator<Task> tasks) {
        return null;
    }

    @Override
    public TaskCreateBatchOperation createTasksAsync(Iterator<Task> tasks, TaskCreateRequestParameters parameters) {
        return null;
    }

    @Override
    public SearchResult<Task> findTasks(TaskSearchRequest request) {
        return null;
    }

    @Override
    public Task getTask(String taskId) {
        return null;
    }

    @Override
    public ModificationResult<Task> patchTask(String taskId, TaskPatch patch) {
        return null;
    }

    @Override
    public ModificationResult<Task> patchTask(String taskId, TaskPatch patch, TaskPatchRequestParameters parameters) {
        return null;
    }

    @Override
    public ModificationResult<Task> setOverlapOrMin(String taskId, TaskOverlapPatch overlapPatch) {
        return null;
    }
}
