package ru.yandex.market.markup2.tasks.supplier_sku_mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.YangUtils;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.yang.YangAssignmentInfo;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionDataIdentity;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionDataPayload;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.inspection.MappingInspectionResponse;
import ru.yandex.market.markup2.utils.mboc.MbocOfferStatus;
import ru.yandex.market.markup2.utils.mboc.TicketPriorityInfo;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.YangResultsDownloader;
import ru.yandex.market.toloka.model.Pool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shadoff
 */
@SuppressWarnings("checkstyle:MagicNumber")
@Ignore("m3")
public class SupplierSkuMappingTest extends TasksProcessTestsBase {
    private static final Logger log = LogManager.getLogger();

    @Override
    public YangLogStorage.YangTaskType getYangTaskType() {
        return YangLogStorage.YangTaskType.BLUE_LOGS;
    }

    @Override
    protected boolean oldPipeSupported() {
        return false;
    }

    @Test
    public void testSupplierUseSpecificYangPoolId() throws Exception {
        mockNeedInspectionCall(false);
        tolokaApi.createPool(new Pool().setId(113621));

        aliasMakerService.clearOffers();
        addOffers(CATEGORY_ID, offer(10316738L), offer(10316738L));
        runGeneration();

        taskProcessManager.processAll();

        Set<TaskConfigInfo> cat1configInfoSet = tasksCache.getConfigInfosByCategoryIdTypeId(CATEGORY_ID, getTaskType());
        Assert.assertEquals(1, cat1configInfoSet.size());
        TaskConfigInfo switchedConfig = cat1configInfoSet.iterator().next();
        Assert.assertEquals(switchedConfig.getYangPoolId(), 113621);
    }

    @Test(timeout = TASK_TIMEOUT_MS)
    public void testOffersChangesCorrectlySendToMboCategoryService() throws Exception {
        mockNeedInspectionCall(false);

        aliasMakerService.clearOffers();
        addOffers(CATEGORY_ID, offer(1), offer(1), offer(1));

        List<AliasMaker.Offer> allOffers = new ArrayList(aliasMakerService.getAllOffers());
        long offerId1 = Long.parseLong(allOffers.get(0).getOfferId());
        long offerId2 = Long.parseLong(allOffers.get(1).getOfferId());
        long offerId3 = Long.parseLong(allOffers.get(2).getOfferId());

        runGeneration();

        Set<TaskConfigInfo> configInfoSet = tasksCache.getConfigInfosByCategoryId(1, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());
        Assert.assertEquals(2, configInfoSet.iterator().next().getSimultaneousTasksCount());

        processAllTasksWithUnlock(taskProcessManager);
        TaskConfigInfo configInfo = configInfoSet.iterator().next();
        configInfo.getCurrentTasks()
                .forEach(t -> {
                    Assert.assertTrue(t.getTaskStatus().isGenerationFinished());
                    Assert.assertTrue(t.getTaskStatus().isSendingFinished());
                });

        List<List<MbocOfferStatus>> history = mboCategoryService.getAndClearHistory();

        Assert.assertEquals(4, history.size());
        assertOfferStatuses(history.stream().flatMap(List::stream).collect(Collectors.toList()),
                new MbocOfferStatus(offerId1, TaskDataItemState.GENERATED),
                new MbocOfferStatus(offerId2, TaskDataItemState.GENERATED),
                new MbocOfferStatus(offerId3, TaskDataItemState.GENERATED),
                new MbocOfferStatus(offerId1, TaskDataItemState.SENT),
                new MbocOfferStatus(offerId2, TaskDataItemState.SENT),
                new MbocOfferStatus(offerId3, TaskDataItemState.SENT));

        assertLastOfferState(offerId1, TaskDataItemState.SENT, null);
        assertLastOfferState(offerId2, TaskDataItemState.SENT, null);
        assertLastOfferState(offerId3, TaskDataItemState.SENT, null);

        configInfoSet = tasksCache.getConfigInfosByCategoryId(1, (x) -> true);
        Assert.assertEquals(1, configInfoSet.size());

        List<TaskInfo> sent = configInfoSet.stream()
                .findFirst().get().getCurrentTasks().stream()
                .filter(t -> t.getTaskStatus().isSendingFinished())
                .collect(Collectors.toList());
        Assert.assertEquals(2, sent.size());

        processTaskInYang(sent.get(0));
        processTaskInYang(sent.get(1));

        processAllTasksWithUnlock(taskProcessManager);

        history = mboCategoryService.getAndClearHistory();
        Assert.assertEquals(4, history.size());

        Map<String, String> urlByOffer = new HashMap<>();
        sent.forEach(task -> {
            Map<Long, YangAssignmentInfo> mp = YangUtils.getAssignmentInfoMap(yangTaskToDataItemsPersister,
                        yangAssignmentPersister,
                        task.getId());
            task.getProgress().getTaskDataItemsByStates(TaskDataItemState.values()).forEach(tdi -> {
                YangAssignmentInfo info = mp.get(tdi.getId());
                String offerId = ((SupplierOfferDataIdentity) tdi.getInputData().getDataIdentifier()).getOfferId();
                String url = String.format("https://yang.yandex.ru/task/%d/%s", info.getPoolId(),
                        info.getAssignmentId());
                urlByOffer.put(offerId, url);
            });
        });
        String url1 = urlByOffer.get(String.valueOf(offerId1));
        String url2 = urlByOffer.get(String.valueOf(offerId2));
        String url3 = urlByOffer.get(String.valueOf(offerId3));

        assertOfferStatuses(history.stream().flatMap(List::stream).collect(Collectors.toList()),
                new MbocOfferStatus(offerId1, TaskDataItemState.SUCCESSFUL_RESPONSE).setYangUrl(url1),
                new MbocOfferStatus(offerId2, TaskDataItemState.SUCCESSFUL_RESPONSE).setYangUrl(url2),
                new MbocOfferStatus(offerId3, TaskDataItemState.SUCCESSFUL_RESPONSE).setYangUrl(url3),
                new MbocOfferStatus(offerId1, TaskDataItemState.SUCCESSFULLY_PROCEEDED),
                new MbocOfferStatus(offerId2, TaskDataItemState.SUCCESSFULLY_PROCEEDED),
                new MbocOfferStatus(offerId3, TaskDataItemState.SUCCESSFULLY_PROCEEDED));

        assertLastOfferState(offerId1, TaskDataItemState.SUCCESSFULLY_PROCEEDED, null);
        assertLastOfferState(offerId2, TaskDataItemState.SUCCESSFULLY_PROCEEDED, null);
        assertLastOfferState(offerId3, TaskDataItemState.SUCCESSFULLY_PROCEEDED, null);

        Assert.assertEquals(sent.get(0).getState(), TaskState.COMPLETED);
        Assert.assertEquals(sent.get(1).getState(), TaskState.COMPLETED);
    }

    @Test
    public void testOnePoolWithInspection() throws Exception {
        mockNeedInspectionCall(true);
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

        YangTaskToDataItems first = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfos.get(0).getId()).get(0);
        YangTaskToDataItems second = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfos.get(1).getId()).get(0);
        TaskInfo firstTask = taskInfos.get(0);
        TaskInfo secondTask = taskInfos.get(1);

        //check same pool
        assertThat(first.getPoolId()).isEqualTo(second.getPoolId());

        generateResult(first, firstTask);
        generateResult(second, secondTask);

        processAllTasksWithUnlock(taskProcessManager);

        List<TaskConfigInfo> inspectionConfigInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(Markup.TaskType.SKU_MAPPING_INSPECTION_VALUE, (x) -> true));

        Assert.assertEquals(2, configInfoSet.size());

        TaskConfigInfo inspectionConfigInfo1 = inspectionConfigInfoSet.get(0);
        assertThat(inspectionConfigInfo1.getCurrentTasks()).hasSize(1);
        TaskInfo inspectionTaskInfo1 = inspectionConfigInfo1.getCurrentTasks().get(0);

        TaskConfigInfo inspectionConfigInfo2 = inspectionConfigInfoSet.get(1);
        TaskInfo inspectionTaskInfo2 = inspectionConfigInfo2.getCurrentTasks().get(0);

        taskProcessManager.processAll();
        assertThat(inspectionTaskInfo1.getTaskStatus().getGeneratedCount()).isEqualTo(1);
        assertThat(inspectionTaskInfo2.getTaskStatus().getGeneratedCount()).isEqualTo(1);
        assertThat(inspectionTaskInfo1.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(inspectionTaskInfo2.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        List<TaskInfo> inspectionTaskInfos = Arrays.asList(inspectionTaskInfo1, inspectionTaskInfo2);
        Collections.sort(inspectionTaskInfos, Comparator.comparing(TaskInfo::getCategoryId));

        YangTaskToDataItems inspectionFirst = yangTaskToDataItemsPersister.getByMarkupTaskId(inspectionTaskInfos.get(0).getId()).get(0);
        YangTaskToDataItems inspectionSecond = yangTaskToDataItemsPersister.getByMarkupTaskId(inspectionTaskInfos.get(1).getId()).get(0);

        //check inspection has the same pool
        assertThat(inspectionFirst.getPoolId()).isEqualTo(first.getPoolId());

        TaskInfo inspectionFirstTask = inspectionTaskInfos.get(0);
        TaskInfo inspectionSecondTask = inspectionTaskInfos.get(1);

        assertThat(inspectionFirst.getPoolId()).isEqualTo(inspectionSecond.getPoolId());

        generateInspectionResult(inspectionFirst, inspectionFirstTask, firstTask);
        generateInspectionResult(inspectionSecond, inspectionSecondTask, secondTask);
        processAllTasksWithUnlock(taskProcessManager);

        assertThat(inspectionFirstTask.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(inspectionFirstTask.getTaskStatus().getReceivedCount()).isEqualTo(1);
        assertThat(inspectionFirstTask.getTaskStatus().getProcessedCount()).isEqualTo(1);

        assertThat(inspectionFirstTask.getState()).isEqualTo(TaskState.COMPLETED);

        List<SupplierOffer.ContentTaskResult> mappingResults = mboCategoryService.getMappingResults();
        assertThat(mappingResults).hasSize(4);

        YangLogStorage.YangLogStoreRequest request = getLogStoreRequest(firstTask.getId(), 2);

        assertThat(request.getId()).isEqualTo(String.valueOf(firstTask.getId()));
        assertThat(request.getHitmanId()).isEqualTo(firstTask.getId());
        assertThat(request.getTaskType()).isEqualTo(YangLogStorage.YangTaskType.BLUE_LOGS);
        assertThat(request.getContractorInfo().getUid()).isEqualTo(TEST_UID);
        assertThat(request.getMappingStatisticCount()).isEqualTo(3);

        assertThat(request.getMappingStatistic(0).getOfferMappingStatus()).isEqualTo(
                YangLogStorage.MappingStatus.MAPPED
        );
    }

    @Test
    public void testPrioritiesSinglePoolWithInspection() throws Exception {
        mockNeedInspectionCall(true);
        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID_1, offer(SUPPLIER_ID_1, "MCP-1"),
                offer(SUPPLIER_ID_1, "MCP-1")
        );
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID, "MCP-2"),
                offer(SUPPLIER_ID, "MCP-2")
        );
        addOffers(SWITCHED_CATEGORY_ID_1, offer(SUPPLIER_ID, "MCP-3"),
                offer(SUPPLIER_ID, "MCP-3")
        );
        runGeneration();
        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        assertThat(configInfoSet).hasSize(3);
        TaskInfo firstTask = configInfoSet.get(0)
                    .getSingleCurrentTask();
        TaskInfo secondTask = configInfoSet.get(1)
                .getSingleCurrentTask();
        TaskInfo thirdTask = configInfoSet.get(2)
                .getSingleCurrentTask();

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(firstTask.getTaskStatus().isSendingFinished());
        generateResult(firstTask);
        processAllTasksWithUnlock(taskProcessManager);
        TaskInfo inspectionTask = tasksCache.getDependTask(firstTask);
        assertThat(inspectionTask.getTaskStatus().isSendingFinished());

        mboCategoryService.getTicketPriorities()
                .forEach(op -> {
                    if (op.getSupplierId() ==
                            secondTask.getConfig().getGroupInfo().getParameterValue(ParameterType.SUPPLIER_ID) &&
                            op.getCategories().contains((long) secondTask.getCategoryId())) {
                        op.setTicketDeadline(2);
                    } else {
                        op.setTicketDeadline(10);
                    }
                });
        yangPrioritiesProcessor.updatePrioritiesInProcess(mboCategoryService.getTicketPriorities());

        assertThat(yangTaskToDataItemsPersister.getAllValues().stream()
                .filter(y -> y.getIssuingOrderOverride() != null)
                .sorted(Comparator.comparing(YangTaskToDataItems::getIssuingOrderOverride).reversed())
                .map(YangTaskToDataItems::getMarkupTaskId)
                .collect(Collectors.toList())
        ).containsExactly(secondTask.getId(), inspectionTask.getId(), thirdTask.getId(), firstTask.getId());

    }

    @Test
    public void testPrioritiesSinglePoolWithInspectionAbsentOffers() throws Exception {
        mockNeedInspectionCall(true);
        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID_1, offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1)
        );

        runGeneration();
        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        assertThat(configInfoSet).hasSize(1);
        TaskInfo mainTask = configInfoSet.get(0)
                .getSingleCurrentTask();

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(mainTask.getTaskStatus().isSendingFinished());
        generateResult(mainTask);

        yangPrioritiesProcessor.updatePrioritiesInProcess(mboCategoryService.getTicketPriorities());

        mboCategoryService.clearOffers();

        processAllTasksWithUnlock(taskProcessManager);
        TaskInfo inspectionTask = tasksCache.getDependTask(mainTask);
        assertThat(inspectionTask.getTaskStatus().isSendingFinished());

        yangPrioritiesProcessor.updatePrioritiesInProcess(mboCategoryService.getTicketPriorities());

        assertThat(yangTaskToDataItemsPersister.getAllValues().stream()
                .filter(y -> y.getIssuingOrderOverride() != null)
                .map(YangTaskToDataItems::getMarkupTaskId)
                .collect(Collectors.toList())
        ).containsExactlyInAnyOrder(inspectionTask.getId(), mainTask.getId());

        // inspection issuing order is copied from main task
        Assert.assertEquals(
                yangTaskToDataItemsPersister.getByMarkupTaskId(mainTask.getId()).get(0).getIssuingOrderOverride(),
                yangTaskToDataItemsPersister.getByMarkupTaskId(inspectionTask.getId()).get(0).getIssuingOrderOverride());
    }

    @Test
    public void testPrioritiesSinglePoolMatchedToModelFirst() throws Exception {
        mockNeedInspectionCall(true);
        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID_1, offer(SUPPLIER_ID_1, "MCP-1"),
                offer(SUPPLIER_ID_1, "MCP-1")
        );
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID, "MCP-2"),
                offer(SUPPLIER_ID, "MCP-2")
        );
        addOffers(SWITCHED_CATEGORY_ID_1, offer(SUPPLIER_ID, "MCP-3"),
                offer(SUPPLIER_ID, "MCP-3")
        );
        runGeneration();
        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        assertThat(configInfoSet).hasSize(3);
        TaskInfo matchedToModel = configInfoSet.get(0)
                .getSingleCurrentTask();
        TaskInfo secondTask = configInfoSet.get(1)
                .getSingleCurrentTask();
        TaskInfo thirdTask = configInfoSet.get(2)
                .getSingleCurrentTask();
        processAllTasksWithUnlock(taskProcessManager);

        List<TicketPriorityInfo> ticketPriorityInfos = mboCategoryService.getTicketPriorities();

        ticketPriorityInfos.forEach(op -> {
            if (op.getSupplierId() ==
                    secondTask.getConfig().getGroupInfo().getParameterValue(ParameterType.SUPPLIER_ID) &&
                    op.getCategories().contains((long) secondTask.getCategoryId())) {
                op.setTicketDeadline(2);
            } else {
                op.setTicketDeadline(10);
            }
        });
        ticketPriorityInfos.forEach(op -> {
            if (op.getSupplierId() == matchedToModel.getConfig().getGroupInfo()
                    .getParameterValue(ParameterType.SUPPLIER_ID) &&
                    op.getCategories().contains((long) matchedToModel.getCategoryId())) {
                op.setMatchedToModel(true);
            }
        });
        yangPrioritiesProcessor.updatePrioritiesInProcess(ticketPriorityInfos);

        assertThat(yangTaskToDataItemsPersister.getAllValues().stream()
                .filter(y -> y.getIssuingOrderOverride() != null)
                .sorted(Comparator.comparing(YangTaskToDataItems::getIssuingOrderOverride).reversed())
                .map(YangTaskToDataItems::getMarkupTaskId)
                .collect(Collectors.toList())).containsExactly(secondTask.getId(), matchedToModel.getId(), thirdTask.getId());
    }

    private void assertOfferStatuses(List<MbocOfferStatus> actual,
                                     MbocOfferStatus... expected) {

        Assertions.assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    private void assertLastOfferState(long offerId, TaskDataItemState taskDataItemState, String url) {
        MbocOfferStatus expectedState = new MbocOfferStatus(offerId, taskDataItemState).setYangUrl(url);
        Assertions.assertThat(mboCategoryService.getLastState(offerId)).isEqualTo(expectedState);
    }

    @Override
    protected void runGeneration() throws Exception {
        runGeneration(false, true, false, 2);
    }

    @Override
    protected void runGeneration(int offersInTask) throws Exception {
        runGeneration(false, true, false, offersInTask);
    }

    @Override
    public void generateResult(YangTaskToDataItems items, TaskInfo taskInfo) {
        tolokaApi.addYangTaskResults(items.getTaskId(), TEST_WORKER_ID, tolokaApi.convertBlueLogs(generateMappingResults(taskInfo)));
        allBeans.get(YangResultsDownloader.class).stopSkippingTask(items.getMarkupTaskId());
    }

    public void generateInspectionResult(YangTaskToDataItems items, TaskInfo inspectionTaskInfo, TaskInfo mappingTaskInfo) {
        TaskDataItem<MappingInspectionDataPayload, MappingInspectionResponse> inspectionItem =
                inspectionTaskInfo.getProgress().getDataItem(new MappingInspectionDataIdentity(mappingTaskInfo.getId()));
        tolokaApi.addYangTaskResults(items.getTaskId(), TEST_WORKER_ID, tolokaApi.convertBlueLogsInspection(
                generateInspectionResults(mappingTaskInfo, inspectionItem, TEST_WORKER_ID)));
        allBeans.get(YangResultsDownloader.class).stopSkippingTask(inspectionTaskInfo.getId());
    }

    @Override
    public void checkResult(TaskInfo taskInfo,
                            int batchSize,
                            long categoryId) {
        assertThat(taskInfo.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(taskInfo.getTaskStatus().getReceivedCount()).isEqualTo(batchSize);
        assertThat(taskInfo.getTaskStatus().getProcessedCount()).isEqualTo(batchSize);

        assertThat(taskInfo.getState()).isEqualTo(TaskState.COMPLETED);

        List<SupplierOffer.ContentTaskResult> mappingResults = mboCategoryService.getMappingResults();
        assertThat(mappingResults).hasSize(batchSize);

        YangLogStorage.YangLogStoreRequest request = getLogStoreRequest(taskInfo.getId());

        assertThat(request.getId()).isEqualTo(String.valueOf(taskInfo.getId()));
        assertThat(request.getHitmanId()).isEqualTo(taskInfo.getId());
        assertThat(request.getTaskType()).isEqualTo(YangLogStorage.YangTaskType.BLUE_LOGS);
        assertThat(request.getCategoryId()).isEqualTo(categoryId);
        assertThat(request.getContractorInfo().getUid()).isEqualTo(TEST_UID);
        assertThat(request.getMappingStatisticCount()).isEqualTo(batchSize);

        assertThat(request.getMappingStatistic(0).getOfferMappingStatus()).isEqualTo(
                YangLogStorage.MappingStatus.MAPPED
        );

        request.getMappingStatisticList().forEach(statistic -> {
            assertThat(statistic.getUid()).isEqualTo(TEST_UID);
            assertThat(statistic.getMarketSkuId()).isEqualTo(TEST_MSKU);
        });
    }

    @Override
    public int getTaskType() {
        return Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE;
    }

    @Override
    public void processTaskInYang(TaskInfo task) {
        assertThat(task.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        YangTaskToDataItems yangTaskToDataItems = yangTaskToDataItemsPersister
                .getByMarkupTaskId(task.getId()).get(0);

        generateResult(yangTaskToDataItems, task);
    }
}
