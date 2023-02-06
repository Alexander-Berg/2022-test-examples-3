package ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.core.stubs.persisters.KvStoragePersisterStub;
import ru.yandex.market.markup2.dao.YangAssignmentPersister;
import ru.yandex.market.markup2.dao.YangAssignmentResultsPersister;
import ru.yandex.market.markup2.dao.YangResultsPoolStatusPersister;
import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.yang.YangAssignmentInfo;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;
import ru.yandex.market.markup2.processors.task.DataItems;
import ru.yandex.market.markup2.processors.task.ProgressStatus;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.Comment;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataIdentity;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataItemPayload;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.TasksProcessTestsBase;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.priority.YangPrioritiesProcessor;
import ru.yandex.market.markup2.utils.mboc.MbocOfferStatus;
import ru.yandex.market.markup2.utils.mboc.TicketPriorityInfo;
import ru.yandex.market.markup2.utils.offer.OfferFreezingService;
import ru.yandex.market.markup2.utils.traits.TraitsAndSkillsService;
import ru.yandex.market.markup2.workflow.TaskProcessManager;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueCache;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.YangResultsDownloader;
import ru.yandex.market.toloka.model.TaskSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author galaev
 * @since 2019-08-22
 */
@SuppressWarnings("checkstyle:MagicNumber")
@Ignore("m3")
public class MappingModerationTest extends TasksProcessTestsBase {

    @Override
    public YangLogStorage.YangTaskType getYangTaskType() {
        return YangLogStorage.YangTaskType.MAPPING_MODERATION;
    }

    @Override
    protected MboCategory.GetOffersPrioritiesRequest getBaseOffersRequest() {
        return MboCategory.GetOffersPrioritiesRequest.newBuilder()
                .setStatusFilter(MboCategory.GetOffersPrioritiesRequest.StatusFilter.IN_MODERATION)
                .setSuggestSkuType(SupplierOffer.SkuType.TYPE_MARKET)
                .build();
    }

    @Override
    protected boolean oldPipeSupported() {
        return false;
    }

    @Override
    public void processTaskInYang(TaskInfo moderationTask) {
        // check sending finished
        assertThat(moderationTask.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        YangTaskToDataItems yangTaskToDataItems = yangTaskToDataItemsPersister
                .getByMarkupTaskId(moderationTask.getId()).get(0);

        tolokaApi.addYangTaskResults(yangTaskToDataItems.getTaskId(), TEST_WORKER_ID,
                tolokaApi.convertModeration(generateModerationResults(moderationTask)));

        allBeans.get(YangResultsDownloader.class).stopSkippingTask(moderationTask.getId());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testPrioritiesProcessingOneOfferChanged() throws Exception {
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        //spying
        tolokaApi = Mockito.spy(tolokaApi);
        YangPrioritiesProcessor yangPrioritiesProcessor = new YangPrioritiesProcessor(
                tolokaApi,
                tasksCache,
                markupManager,
                allBeans.get(TaskDataUniqueCache.class),
                yangTaskToDataItemsPersister,
                yangResultsDownloader,
                new TraitsAndSkillsService(),
                Mockito.mock(OfferFreezingService.class),
                new KvStoragePersisterStub(),
                taskProcessManager,
                ImmutableMap.of(Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE, -1,
                        Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE, 100,
                        Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, -1)
        );

        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(), offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1)
        );

        mboCategoryService.getTicketPriorities()
                .forEach(op -> op.setTicketDeadline(100));
        runGeneration(5);

        TaskConfigInfo configInfo = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true)
                .iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        Random random = new Random(1);
        for (long i = 10; i > 0; i--) {
            List<TicketPriorityInfo> priorityInfos = mboCategoryService.getTicketPriorities();

            priorityInfos.get(0).setTicketDeadline(i);
            yangPrioritiesProcessor.updatePrioritiesInModeration(priorityInfos,
                    taskInfo.getConfig().getParameterValueOrDefault(ConfigParameterType.DSBS, false));

            assertThat(yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0).getRawPriority())
                    .isEqualTo(i);
        }
    }

    @Test
    public void testPrioritiesProcessingWithPriority() throws Exception {
        final int mbocPriority  = 1_000_000_000;
        final double priority = YangPrioritiesProcessor.convertIntToBigDecimalPriority(mbocPriority).doubleValue();
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        //spying
        tolokaApi = Mockito.spy(tolokaApi);

        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(), offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1)
        );

        mboCategoryService.getTicketPriorities()
                .forEach(op -> op.setPriority(mbocPriority));
        runGeneration(5);

        TaskConfigInfo configInfo = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true)
                .iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        String taskSuiteId = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0).getTaskSuiteId();
        TaskSuite taskSuite = tolokaApi.getTaskSuite(taskSuiteId);
        assertThat(taskSuite.getIssuingOrderOverride()).isEqualTo(priority);

    }

    @Test
    public void testYangDownloaderSinglePool() throws Exception {
        aliasMakerService.clearOffers();

        TaskProcessManager spyingTaskProcessManager = Mockito.spy(taskProcessManager);
        yangResultsDownloader.setTaskProcessManager(spyingTaskProcessManager);
        yangResultsDownloader.setYangAssignmentResultsPersister(allBeans.get(YangAssignmentResultsPersister.class));
        yangResultsDownloader.setTransactionTemplate(allBeans.get(TransactionTemplate.class));
        yangResultsDownloader.setYangResultsPoolStatusPersister(allBeans.get(YangResultsPoolStatusPersister.class));
        yangResultsDownloader.setYangTaskToDataItemsPersister(yangTaskToDataItemsPersister);

        AppContext appContext = allBeans.get(AppContext.class);
        appContext.setYangResultsDownloader(yangResultsDownloader);

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(), offer(SUPPLIER_ID_1),
            offer(SUPPLIER_ID_1)
        );
        runGeneration();
        Set<TaskConfigInfo> сonfigs = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true);
        TaskConfigInfo config = сonfigs.iterator().next();
        TaskInfo taskInfo = config.getSingleCurrentTask();

        yangResultsDownloader.stopSkippingTask(taskInfo.getId());

        taskProcessManager.processAll();
        assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);

        YangTaskToDataItems yangTaskToDataItems = yangTaskToDataItemsPersister
            .getByMarkupTaskId(taskInfo.getId()).get(0);

        tolokaApi.addYangTaskResults(yangTaskToDataItems.getTaskId(), TEST_WORKER_ID,
            tolokaApi.convertModeration(generateModerationResults(taskInfo)));

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(taskInfo.getTaskStatus().getReceivingStatus()).isNotEqualTo(ProgressStatus.FINISHED);

        yangResultsDownloader.downloadResultsAllPools();
        verify(spyingTaskProcessManager).addTask(eq(taskInfo.getId()));

        processAllTasksWithUnlock(taskProcessManager);
        assertThat(taskInfo.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
    }

    @Test
    public void testOffersChangesCorrectlySendToMboCategoryService() throws Exception {
        aliasMakerService.clearOffers();

        AliasMaker.Offer offer = offer(SUPPLIER_ID);
        Integer offerId = Integer.valueOf(offer.getOfferId());
        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(), offer);
        runGeneration();

        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo configInfo = configInfoSet.get(0);
        TaskInfo taskInfo = configInfo.getCurrentTasks().get(0);

        taskProcessManager.processAll();
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        YangTaskToDataItems items = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0);

        tolokaApi.addYangTaskResults(items.getTaskId(), TEST_WORKER_ID,
                tolokaApi.convertModeration(generateModerationResults(taskInfo)));
        allBeans.get(YangResultsDownloader.class).stopSkippingTask(taskInfo.getId());

        mboCategoryService.getAndClearHistory();

        processAllTasksWithUnlock(taskProcessManager);
        List<List<MbocOfferStatus>> history = mboCategoryService.getAndClearHistory();

        YangAssignmentPersister yangAssignmentPersister = allBeans.get(YangAssignmentPersister.class);
        List<YangAssignmentInfo> byPoolId = yangAssignmentPersister.getByPoolId(items.getPoolId());
        YangAssignmentInfo yangAssignmentInfo = byPoolId.get(0);

        String yangUrl = String.format("https://yang.yandex.ru/task/%d/%s", yangAssignmentInfo.getPoolId(),
                yangAssignmentInfo.getAssignmentId());

        Assert.assertEquals(2, history.size());
        Assertions.assertThat(history.get(0)).containsExactlyInAnyOrder(
                new MbocOfferStatus(offerId, TaskDataItemState.SUCCESSFUL_RESPONSE).setYangUrl(yangUrl));
        Assertions.assertThat(history.get(1)).containsExactlyInAnyOrder(
                new MbocOfferStatus(offerId, TaskDataItemState.SUCCESSFULLY_PROCEEDED));
    }

    @Test
    public void testCancelTasksInPrio() throws Exception {
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        //spying
        tolokaApi = Mockito.spy(tolokaApi);
        YangPrioritiesProcessor yangPrioritiesProcessor = new YangPrioritiesProcessor(
                tolokaApi,
                tasksCache,
                markupManager,
                allBeans.get(TaskDataUniqueCache.class),
                yangTaskToDataItemsPersister,
                yangResultsDownloader,
                new TraitsAndSkillsService(),
                Mockito.mock(OfferFreezingService.class),
                new KvStoragePersisterStub(),
                taskProcessManager,
                ImmutableMap.of(Markup.TaskType.SUPPLIER_SKU_MAPPING_NEW_PIPE_VALUE, -1,
                        Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE, 100,
                        Markup.TaskType.SUPPLIER_OFFER_CLASSIFICATION_VALUE, -1)
        );

        aliasMakerService.clearOffers();

        addOffers(SWITCHED_CATEGORY_ID, getBaseOffersRequest(), offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1),
                offer(SUPPLIER_ID_1)
        );
        runGeneration(2);

        TaskConfigInfo configInfo = tasksCache.getConfigInfosByCategoryId(SWITCHED_CATEGORY_ID, (x) -> true)
                .iterator().next();
        List<TaskInfo> taskInfos = configInfo.getCurrentTasks();
        assertThat(taskInfos).hasSize(3);
        lockStep(taskInfos.get(2).getId(), RequestsSender.class);
        taskProcessManager.processAll();
        taskInfos.forEach(
            taskInfo -> {
                if (taskInfos.indexOf(taskInfo) != 2) {
                    assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.FINISHED);
                } else {
                    assertThat(taskInfo.getTaskStatus().getSendingStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
                }
            }
        );

        TaskInfo taskInfo = taskInfos.get(0);
        YangTaskToDataItems items = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0);
        tolokaApi.addYangTaskResults(items.getTaskId(), TEST_WORKER_ID,
                tolokaApi.convertModeration(generateModerationResults(taskInfo)));

        Set<Long> offerIds = yangPrioritiesProcessor.cancelTasksForOffers(taskInfos, t -> false,
                offers -> true, __ -> {});
        assertThat(offerIds).hasSize(2);
        Set<Long> cur = taskInfo.getProgress().getTaskDataItemsByStates(TaskDataItemState.values())
                .stream()
                .map(tdi -> (SupplierOfferDataIdentity)tdi.getInputData().getDataIdentifier())
                .map(i -> Long.valueOf(i.getOfferId()))
                .collect(Collectors.toSet());

        assertThat(offerIds).isEqualTo(cur);
    }

    protected List<SupplierMappingModerationResponse> generateModerationResults(TaskInfo moderationTask) {
        List<SupplierMappingModerationResponse> responses = new ArrayList<>();
        DataItems<SupplierOfferDataIdentity, SupplierOfferDataItemPayload, SupplierMappingModerationResponse> items =
            moderationTask.getProgress().getDataItemsByState(TaskDataItemState.SENT);

        List<String> statuses = Arrays.asList("ACCEPTED", "REJECTED", "NEED_INFO");
        AtomicInteger i = new AtomicInteger();
        items.getItems().forEach(item -> {
            int idx = i.getAndIncrement();
            long skuId = getMskuIdByIndex(idx);
            SupplierMappingModerationResponse resp = new SupplierMappingModerationResponse(item.getId(),
                    item.getInputData().getDataIdentifier().getOfferId(),
                    skuId,
                    TEST_WORKER_ID,
                    statuses.get(idx % statuses.size()),
                    ImmutableList.of(new Comment("Не хватает параметров", ImmutableList.of("цвет", "ширина"))),
                    isBadCard(skuId),
                    ImmutableList.of(new Comment("Карточка некрасивая", ImmutableList.of("вообще", "совсем"))),
                    0L);
            responses.add(resp);
        });

        return responses;
    }

    protected Boolean isBadCard(long skuId) {
        return true; //always return true to check that it is not sent to mbo
    }

    protected Long getMskuIdByIndex(int i) {
        return TEST_MSKU;
    }

    @Override
    protected void runGeneration() throws Exception {
        runGeneration(2);
    }

    @Override
    protected void runGeneration(int offersInTask) throws Exception {
        mboCategoryService.getTicketPriorities()
                .forEach(op -> op.setSkuType(SupplierOffer.SkuType.TYPE_MARKET));
        runGeneration(true, false, false, offersInTask);
    }

    @Override
    public void generateResult(YangTaskToDataItems items, TaskInfo taskInfo) {
        tolokaApi.addYangTaskResults(items.getTaskId(), TEST_WORKER_ID,
                tolokaApi.convertModeration(generateModerationResults(taskInfo)));
        allBeans.get(YangResultsDownloader.class).stopSkippingTask(items.getMarkupTaskId());
    }

    @Override
    public void checkResult(TaskInfo taskInfo, int batchSize, long categoryId) {
        assertThat(taskInfo.getTaskStatus().getReceivingStatus()).isEqualTo(ProgressStatus.FINISHED);
        assertThat(taskInfo.getTaskStatus().getReceivedCount()).isEqualTo(batchSize);
        assertThat(taskInfo.getTaskStatus().getProcessedCount()).isEqualTo(batchSize);
        assertThat(taskInfo.getConfig().getState().isFinalState()).isEqualTo(true);

        List<SupplierOffer.MappingModerationTaskResult> results = mboCategoryService.getModerationResults();
        assertThat(results).hasSize(batchSize);

        YangLogStorage.YangLogStoreRequest request = getLogStoreRequest(taskInfo.getId());

        assertThat(request.getId()).isEqualTo(String.valueOf(taskInfo.getId()));
        assertThat(request.getHitmanId()).isEqualTo(taskInfo.getId());
        assertThat(request.getTaskType()).isEqualTo(getYangTaskType());
        assertThat(request.getCategoryId()).isEqualTo(categoryId);
        assertThat(request.getContractorInfo().getUid()).isEqualTo(TEST_UID);
        assertThat(request.getMappingModerationStatisticCount()).isEqualTo(batchSize);

        assertThat(request.getMappingModerationStatistic(0).getMappingModerationStatus()).isEqualTo(
                YangLogStorage.MappingModerationStatus.ACCEPTED
        );
        if (batchSize >= 2) {
            assertThat(request.getMappingModerationStatistic(1).getMappingModerationStatus()).isEqualTo(
                    YangLogStorage.MappingModerationStatus.REJECTED
            );
        }
        if (batchSize >= 3) {
            assertThat(request.getMappingModerationStatistic(2).getMappingModerationStatus()).isEqualTo(
                    YangLogStorage.MappingModerationStatus.NEED_INFO
            );
        }

        AtomicInteger i = new AtomicInteger();
        request.getMappingModerationStatisticList().stream().forEach(statistic -> {
            long mskuId = getMskuIdByIndex(i.getAndIncrement());
            assertThat(statistic.getUid()).isEqualTo(TEST_UID);
            assertThat(statistic.getMarketSkuId()).isEqualTo(mskuId);
            if (!SupplierMappingModerationTaskPropertiesGetter.isToPsku(taskInfo)) {
                assertThat(statistic.hasBadCard()).isEqualTo(false);
            } else {
                assertThat(statistic.getBadCard()).isEqualTo(isBadCard(mskuId));
            }
        });
    }

    @Override
    protected void assertSkills(int poolId, String... skillnames) {
        super.assertSkills(poolId,
                TraitsAndSkillsService.getOperatorSkillName(YangLogStorage.YangTaskType.MAPPING_MODERATION),
                TraitsAndSkillsService.getSuperOperatorSkillName(YangLogStorage.YangTaskType.MAPPING_MODERATION),
                TraitsAndSkillsService.getOperatorSkillName(YangLogStorage.YangTaskType.MAPPING_MODERATION_DSBS),
                TraitsAndSkillsService.getSuperOperatorSkillName(YangLogStorage.YangTaskType.MAPPING_MODERATION_DSBS)
        );
    }

    @Override
    public int getTaskType() {
        return Markup.TaskType.SUPPLIER_MAPPING_APPROVING_VALUE;
    }

}
