package ru.yandex.market.markup2.tasks.supplier_sku_mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.core.stubs.persisters.KvStoragePersisterStub;
import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;
import ru.yandex.market.markup2.exception.CommonException;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.priority.YangPrioritiesProcessor;
import ru.yandex.market.markup2.utils.Mocks;
import ru.yandex.market.markup2.utils.mboc.TicketPriorityInfo;
import ru.yandex.market.markup2.utils.traits.TraitsAndSkillsService;
import ru.yandex.market.markup2.workflow.generation.RequestsGenerator;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueCache;
import ru.yandex.market.markup2.workflow.taskType.processor.SinglePoolSwitchChecker;
import ru.yandex.market.markup2.workflow.taskType.processor.SwitchedCategories;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.users.MboUsers;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.PoolCloseReason;
import ru.yandex.market.toloka.model.PoolStatus;
import ru.yandex.market.toloka.model.TaskSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author shadoff
 * created on 6/16/20
 */
@Ignore("m3")
public abstract class TasksProcessTestsBase extends SkuMappingTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockNeedInspectionCall(false);
    }

    protected abstract void runGeneration() throws Exception;
    protected abstract void runGeneration(int offersInTask) throws Exception;
    public abstract void generateResult(YangTaskToDataItems items, TaskInfo taskInfo);
    public abstract void checkResult(TaskInfo taskInfo, int batchSize, long categoryId);
    public abstract int getTaskType();
    public abstract void processTaskInYang(TaskInfo task);
    public abstract YangLogStorage.YangTaskType getYangTaskType();
    protected MboCategory.GetOffersPrioritiesRequest getBaseOffersRequest() {
        return null;
    };
    protected abstract boolean oldPipeSupported();

    @Test
    public void testSwitchedToSingleGenerationSkipSuppliers() throws Exception {
        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );

        runGeneration();
        Set<TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).hasSize(1);
        TaskConfigInfo taskConfigInfo = configs.iterator().next();
        assertThat(taskConfigInfo.getCurrentTasks()).hasSize(1);
        TaskInfo task = taskConfigInfo.getSingleCurrentTask();
        lockStep(task.getId(), RequestsGenerator.class);

        //adding new offers
        addOffers(SWITCHED_CATEGORY_ID,
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        addOffers(CATEGORY_ID,
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration();
        configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).hasSize(1); //no more configs for category generated

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(task.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);

        runGeneration();
        configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).hasSize(2); //one more config
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testPrioritiesProcessingNewPipe() throws Exception {
        //spying
        tolokaApi = Mockito.spy(tolokaApi);
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);

        YangPrioritiesProcessor yangPrioritiesProcessor = initPrioritiesProcessor();

        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_2),
                offer(SUPPLIER_ID_2)
        );
        mboCategoryService.getTicketPriorities()
                .forEach(op -> {
                    if (op.getSupplierId() == SUPPLIER_ID_2) {
                        op.setTicketDeadline(1l);
                    } else {
                        op.setTicketDeadline(0l);
                    }
                });

        runGeneration();
        List<TaskConfigInfo> newPipeConfigInfos = new ArrayList<>(
                tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true));
        assertThat(newPipeConfigInfos).hasSize(2);

        TaskConfigInfo configInfo1 = newPipeConfigInfos.stream()
                .filter(c -> c.getGroupInfo().getParameterValue(ParameterType.SUPPLIER_ID) == SUPPLIER_ID_1)
                .findFirst().get();
        assertThat(configInfo1.getCurrentTasks()).hasSize(1);
        TaskInfo taskInfo1 = configInfo1.getCurrentTasks().get(0);

        TaskConfigInfo configInfo2 = newPipeConfigInfos.stream().filter(c -> c != configInfo1).findFirst().get();
        TaskInfo taskInfo2 = configInfo2.getCurrentTasks().get(0);

        taskProcessManager.processAll();
        assertThat(taskInfo1.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(taskInfo2.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        YangTaskToDataItems items1 = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo1.getId()).get(0);
        YangTaskToDataItems items2 = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo2.getId()).get(0);

        boolean dsbs = configInfo1.getParameterValueOrDefault(ConfigParameterType.DSBS, false);
        List<TicketPriorityInfo> priorityInfos = mboCategoryService.getTicketPriorities();
        yangPrioritiesProcessor.updatePrioritiesInClassification(priorityInfos);
        yangPrioritiesProcessor.updatePrioritiesInModeration(priorityInfos, dsbs);
        yangPrioritiesProcessor.updatePrioritiesInProcess(priorityInfos);

        verify(tolokaApi, times(0)).changeTaskSuiteOrder(any(), anyDouble());
        assertThat(tolokaApi.getTaskSuiteOrder(items1.getTaskSuiteId())).isNotNull();
        assertThat(tolokaApi.getTaskSuiteOrder(items2.getTaskSuiteId())).isNotNull();
        //first task has higher priority
        assertThat(tolokaApi.getTaskSuiteOrder(items1.getTaskSuiteId()))
                .isGreaterThan(tolokaApi.getTaskSuiteOrder(items2.getTaskSuiteId()));
        assertThat(items1.getIssuingOrderOverride()).isGreaterThan(items2.getIssuingOrderOverride());

        //nothing changed - no interations
        yangPrioritiesProcessor.updatePrioritiesInClassification(priorityInfos);
        yangPrioritiesProcessor.updatePrioritiesInModeration(priorityInfos, dsbs);
        yangPrioritiesProcessor.updatePrioritiesInProcess(priorityInfos);
        verify(tolokaApi, times(0)).changeTaskSuiteOrder(any(), anyDouble());

        //increasing priority - should be updated
        mboCategoryService.getTicketPriorities()
                .forEach(op -> {
                    if (op.getSupplierId() == SUPPLIER_ID_1 || op.getSupplierId() == SUPPLIER_ID) {
                        op.setTicketDeadline(2L);
                    }
                });
        yangPrioritiesProcessor.updatePrioritiesInClassification(priorityInfos);
        yangPrioritiesProcessor.updatePrioritiesInModeration(priorityInfos, dsbs);
        yangPrioritiesProcessor.updatePrioritiesInProcess(priorityInfos);
        verify(tolokaApi, times(1)).changeTaskSuiteOrder(any(), anyDouble());
        assertThat(tolokaApi.getTaskSuiteOrder(items1.getTaskSuiteId()))
                .isLessThan(tolokaApi.getTaskSuiteOrder(items2.getTaskSuiteId()));
        assertThat(items1.getIssuingOrderOverride()).isLessThan(items2.getIssuingOrderOverride());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testPrioritiesTicketGone() throws Exception {
        //spying
        tolokaApi = Mockito.spy(tolokaApi);
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);

        YangPrioritiesProcessor yangPrioritiesProcessor = initPrioritiesProcessor();

        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration();

        Map<Long, TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true)
                .stream()
                .collect(Collectors.toMap(
                        tc -> tc.getGroupInfo().getParameterValue(ParameterType.SUPPLIER_ID), x -> x));
        TaskConfigInfo supplierConfig = configs.get(SUPPLIER_ID);
        taskProcessManager.processAll();

        assertThat(supplierConfig.getSingleCurrentTask().getTaskStatus().getSendingStatus())
                .isEqualTo(ProgressStatus.FINISHED);

        boolean dsbs = supplierConfig.getParameterValueOrDefault(ConfigParameterType.DSBS, false);

        yangPrioritiesProcessor.updatePrioritiesInProcess(Collections.emptyList());
        yangPrioritiesProcessor.updatePrioritiesInClassification(Collections.emptyList());
        yangPrioritiesProcessor.updatePrioritiesInModeration(Collections.emptyList(), dsbs);

        YangTaskToDataItems items = yangTaskToDataItemsPersister.getByMarkupTaskId(
                supplierConfig.getSingleCurrentTask().getId()).get(0);
        assertThat(items.isCancelled()).isTrue();
    }


    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testPrioritiesTicketGoneNotYetGenerated() throws Exception {
        //spying
        tolokaApi = Mockito.spy(tolokaApi);
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);

        YangPrioritiesProcessor yangPrioritiesProcessor = initPrioritiesProcessor();

        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration();

        Map<Long, TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true)
                .stream()
                .collect(Collectors.toMap(
                        tc -> tc.getGroupInfo().getParameterValue(ParameterType.SUPPLIER_ID), x -> x));
        TaskConfigInfo supplierConfig = configs.get(SUPPLIER_ID);
        lockStep(supplierConfig.getSingleCurrentTask().getId(), RequestsSender.class);

        taskProcessManager.processAll();

        assertThat(supplierConfig.getSingleCurrentTask().getTaskStatus().getSendingStatus())
                .isEqualTo(ProgressStatus.NOT_STARTED);

        boolean dsbs = supplierConfig.getParameterValueOrDefault(ConfigParameterType.DSBS, false);

        yangPrioritiesProcessor.updatePrioritiesInProcess(Collections.emptyList());
        yangPrioritiesProcessor.updatePrioritiesInClassification(Collections.emptyList());
        yangPrioritiesProcessor.updatePrioritiesInModeration(Collections.emptyList(), dsbs);
        processAllTasksWithUnlock(taskProcessManager);

        assertThat(supplierConfig.getState()).isEqualTo(TaskConfigState.FORCE_FINISHED);
        assertThat(supplierConfig.getSingleCurrentTask().getState()).isEqualTo(TaskState.FORCE_FINISHED);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testPrioritiesFrozenOffers() throws Exception {
        //spying
        tolokaApi = Mockito.spy(tolokaApi);
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);

        YangPrioritiesProcessor yangPrioritiesProcessor = initPrioritiesProcessor();

        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID_2),
                offer(SUPPLIER_ID_2)
        );
        runGeneration();

        Map<Long, TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true)
                .stream()
                .collect(Collectors.toMap(
                        tc -> tc.getGroupInfo().getParameterValue(ParameterType.SUPPLIER_ID), x -> x));
        TaskConfigInfo supplierConfig = configs.get(SUPPLIER_ID);
        TaskConfigInfo supplier2Config = configs.get(SUPPLIER_ID_2);
        taskProcessManager.processAll();

        assertThat(supplierConfig.getSingleCurrentTask().getTaskStatus().getSendingStatus())
                .isEqualTo(ProgressStatus.FINISHED);
        assertThat(supplier2Config.getSingleCurrentTask().getTaskStatus().getSendingStatus())
                .isEqualTo(ProgressStatus.FINISHED);

        boolean dsbs = supplierConfig.getParameterValueOrDefault(ConfigParameterType.DSBS, false);
        createFrozenOffers(System.currentTimeMillis() - 1, System.currentTimeMillis() + 10000, SUPPLIER_ID);

        List<TicketPriorityInfo> priorityInfos = mboCategoryService.getTicketPriorities();
        yangPrioritiesProcessor.updatePrioritiesInProcess(priorityInfos);
        yangPrioritiesProcessor.updatePrioritiesInClassification(priorityInfos);
        yangPrioritiesProcessor.updatePrioritiesInModeration(priorityInfos, dsbs);

        YangTaskToDataItems items1 = yangTaskToDataItemsPersister.getByMarkupTaskId(
                supplierConfig.getSingleCurrentTask().getId()).get(0);
        YangTaskToDataItems items2 = yangTaskToDataItemsPersister.getByMarkupTaskId(
                supplier2Config.getSingleCurrentTask().getId()).get(0);

        assertThat(items1.isCancelled()).isTrue();
        assertThat(items2.isCancelled()).isFalse();
    }

    private YangPrioritiesProcessor initPrioritiesProcessor() {
        return new YangPrioritiesProcessor(
                tolokaApi,
                tasksCache,
                markupManager,
                allBeans.get(TaskDataUniqueCache.class),
                allBeans.get(YangTaskToDataItemsPersister.class),
                yangResultsDownloader,
                new TraitsAndSkillsService(),
                offerFreezingService,
                new KvStoragePersisterStub(),
                taskProcessManager,
                ImmutableMap.of(Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE, -1,
                        Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE, 100,
                        Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, -1)
        ) {
            @Override
            protected ExecutorService getAsyncRequestsExecutorService() {
                return Mocks.instantExecutorService();
            }
        };
    }

    @Test
    public void testFrozenOffersSSMGenerationSkip() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID_2),
                offer(SUPPLIER_ID_2)
        );
        createFrozenOffers(System.currentTimeMillis() - 1, System.currentTimeMillis() + 10000, SUPPLIER_ID);
        runGeneration();
        Set<TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).extracting(tc -> tc.getGroupInfo()
                .getParameterValue(ParameterType.SUPPLIER_ID))
                .containsExactly(SUPPLIER_ID_2);
    }

    @Test
    public void testFrozenOffersNoTicketSSMGenerationSkip() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID_2),
                offer(SUPPLIER_ID_2)
        );
        createFrozenOffers(System.currentTimeMillis() - 1, System.currentTimeMillis() + 10000, SUPPLIER_ID,
                null);
        runGeneration();
        Set<TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).extracting(tc -> tc.getGroupInfo()
                .getParameterValue(ParameterType.SUPPLIER_ID))
                .containsExactly(SUPPLIER_ID_2);
    }

    @Test
    public void testOldFrozenOffersSSMGeneration() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID,  getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        createFrozenOffers(System.currentTimeMillis() - 1000, System.currentTimeMillis() - 1, SUPPLIER_ID);
        runGeneration();
        Set<TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).hasSize(1);
    }

    @Test
    public void testFutureFrozenOffersSSMGeneration() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        createFrozenOffers(System.currentTimeMillis() + 1000, System.currentTimeMillis() + 2000,
                SUPPLIER_ID);
        runGeneration();
        Set<TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(configs).hasSize(1);
    }

    @Test
    public void testFreezingOffersForceFinishing() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration();
        Set<TaskConfigInfo> configs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        taskProcessManager.processAll();
        TaskInfo taskInfo = configs.iterator().next().getSingleCurrentTask();
        assertThat(taskInfo.getState()).isEqualTo(TaskState.RUNNING);

        createFrozenOffers(System.currentTimeMillis() - 1, System.currentTimeMillis() + 10000,
                SUPPLIER_ID_2); //different supplier

        assertThat(taskInfo.getState()).isEqualTo(TaskState.RUNNING);

        createFrozenOffers(System.currentTimeMillis() - 1, System.currentTimeMillis() + 10000,
                SUPPLIER_ID);

        assertThat(taskInfo.getState()).isEqualTo(TaskState.FORCE_FINISHING);
    }

    @Test
    public void testSkipPreviouslyGenerated() throws Exception {
        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );

        runGeneration();
        Set<TaskConfigInfo> newPipeConfigs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(newPipeConfigs).hasSize(1);
        TaskConfigInfo configInfo = newPipeConfigs.iterator().next();
        assertThat(configInfo.getSimultaneousTasksCount()).isEqualTo(1);
        processAllTasksWithUnlock(taskProcessManager);
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        assertThat(taskInfo.getTaskStatus().getGenerationStatus()).isEqualTo(ProgressStatus.FINISHED);

        // offers for same supplier but different ticket
        addOffers(SWITCHED_CATEGORY_ID, (s) -> "MCP-1" + s, getBaseOffersRequest(), offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration();
        newPipeConfigs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        assertThat(newPipeConfigs).hasSize(2);
        newPipeConfigs.remove(configInfo);
        TaskConfigInfo newConfig = newPipeConfigs.iterator().next();
        assertThat(newConfig.getSimultaneousTasksCount()).isEqualTo(1);
    }

    @Test
    public void successfulTaskOnePool() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        addOffers(SWITCHED_CATEGORY_ID_1, offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1)
        );
        runGeneration();
        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        Assert.assertEquals(2, configInfoSet.size());

        TaskConfigInfo configInfo1 = configInfoSet.get(0);
        assertThat(configInfo1.getCurrentTasks()).hasSize(1);
        TaskInfo taskInfo1 = configInfo1.getCurrentTasks().get(0);

        TaskConfigInfo configInfo2 = configInfoSet.get(1);
        TaskInfo taskInfo2 = configInfo2.getCurrentTasks().get(0);

        taskProcessManager.processAll();
        assertThat(taskInfo1.getTaskStatus().getGeneratedCount()).isEqualTo(2);
        assertThat(taskInfo2.getTaskStatus().getGeneratedCount()).isEqualTo(2);
        assertThat(taskInfo1.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(taskInfo2.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        List<TaskInfo> taskInfos = Arrays.asList(taskInfo1, taskInfo2);
        Collections.sort(taskInfos, Comparator.comparing(TaskInfo::getCategoryId));

        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        YangTaskToDataItems first = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfos.get(0).getId()).get(0);
        YangTaskToDataItems second = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfos.get(1).getId()).get(0);
        assertSkills(first.getPoolId(), TraitsAndSkillsService.getOperatorSkillName(getYangTaskType()),
                TraitsAndSkillsService.getSuperOperatorSkillName(getYangTaskType()));
        TaskSuite taskSuite = tolokaApi.getTaskSuite(first.getTaskSuiteId());
        MboUsers.ProjectType projectType = TraitsAndSkillsService.getProjectType(getYangTaskType());
        String suffix = TraitsAndSkillsService.getProjectSuffix(projectType);
        assertThat(taskSuite.getTraitsAnyOf()).containsExactlyInAnyOrder(
                TraitsAndSkillsService.OPERATOR_PREFIX + taskInfos.get(0).getCategoryId() + suffix,
                TraitsAndSkillsService.OPERATOR_PREFIX + taskInfos.get(0).getCategoryId() + TraitsAndSkillsService.ALL,
                TraitsAndSkillsService.SUPER_PREFIX + taskInfos.get(0).getCategoryId() + suffix,
                TraitsAndSkillsService.SUPER_PREFIX + taskInfos.get(0).getCategoryId() + TraitsAndSkillsService.ALL);

        TaskInfo firstTask = taskInfos.get(0);

        //check same pool
        assertThat(first.getPoolId()).isEqualTo(second.getPoolId());

        generateResult(first, firstTask);

        processAllTasksWithUnlock(taskProcessManager);

        checkResult(firstTask,2, SWITCHED_CATEGORY_ID);
    }

    @Test
    public void testAllSwitched() throws Exception {
        taskTypesContainers.getTaskTypeContainers()
                .get(getTaskType())
                .setSwitchedCagories(SwitchedCategories.SWITCH_ALL);
        addOffers(CATEGORY_ID, offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration();
        SinglePoolSwitchChecker switchChecker = new SinglePoolSwitchChecker();

        Set<TaskConfigInfo> configInfos = tasksCache.getConfigInfosByTypeId(
                getTaskType(),
                switchChecker::isSwitched);
        assertThat(configInfos).hasSize(2);
    }

    @Test
    public void testProcessingExpiredTask() throws CommonException {
        if (!oldPipeSupported()) {
            return;
        }
        TaskInfo task = runTask(getTaskType());
        int taskId = task.getId();
        YangPoolInfo poolInfo = YangPoolsCache.getInstance().getYangPools(task.getId(), false).get(0);
        Pool pool = tolokaApi.getPoolInfo(poolInfo.getPoolId());

        pool.setStatus(PoolStatus.CLOSED);
        pool.setLastCloseReason(PoolCloseReason.EXPIRED);
        tolokaApi.updatePool(pool);

        clearStepLocks();
        taskProcessManager.processTask(taskId);

        assertThat(task.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getReceivedCount()).isEqualTo(0);
        assertThat(task.getTaskStatus().getLostCount()).isEqualTo(BATCH_SIZE);

        assertThat(task.getTaskStatus().getFinalizationStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(task.getTaskStatus().getProcessingStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    protected void generateResult(TaskInfo taskInfo) {
        generateResult(yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0), taskInfo);
    }

    protected void createFrozenOffers(long from, long to, long supplierId) {
        createFrozenOffers(from, to, supplierId, getTicketForSupplier(supplierId));
    }

    protected void createFrozenOffers(long from, long to, long supplierId, @Nullable String ticket) {
        SupplierOffer.Offer.InternalProcessingStatus processingStatus = null;
        switch (getYangTaskType()) {
            case BLUE_LOGS:
                processingStatus = SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS;
                break;
            case MAPPING_MODERATION:
            case MAPPING_MODERATION_DSBS:
            case MAPPING_MODERATION_TO_PSKU:
                processingStatus = SupplierOffer.Offer.InternalProcessingStatus.IN_MODERATION;
                break;
            default:
        }
        Markup.FrozenGroup.Builder builder = Markup.FrozenGroup.newBuilder()
                .setProcessingStatus(processingStatus)
                .setSupplierId(supplierId)
                .setFreezeStartTime(from)
                .setFreezeFinishTime(to);

        if (ticket != null) {
            builder.setTicket(ticket)
                    .setProcessingTicketId(getProcessingTicketId(ticket));
        }
        offerFreezingService.updateFrozenOffers(
                Collections.singletonList(
                        builder.build()
                )
        );
    }
}
