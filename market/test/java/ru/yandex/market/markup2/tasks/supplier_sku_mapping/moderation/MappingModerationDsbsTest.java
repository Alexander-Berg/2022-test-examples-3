package ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.config.ConfigParameterType;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;
import ru.yandex.market.markup2.utils.mboc.TicketPriorityInfo;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.toloka.YangResultsDownloader;

@Ignore("m3")
public class MappingModerationDsbsTest extends MappingModerationTest {

    @Override
    protected void runGeneration(int offersInTask) throws Exception {
        runGeneration(offersInTask, false);
    }

    protected void runGeneration(int offersInTask, boolean toPsku) throws Exception {
        mboCategoryService.getTicketPriorities()
                .forEach(op -> op.setSkuType(
                        toPsku ? SupplierOffer.SkuType.TYPE_PARTNER: SupplierOffer.SkuType.TYPE_MARKET));
        runGeneration(offersInTask, builder ->
                builder.addParameter(ParameterType.FOR_MODERATION_TO_DSBS, true));
    }

    @Test
    public void testDsbsNotToPsku() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID), offer(SUPPLIER_ID));
        runGeneration(2, false);

        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo configInfo = configInfoSet.get(0);
        TaskInfo taskInfo = configInfo.getCurrentTasks().get(0);
        Assertions.assertThat(configInfo.getParameterValue(ConfigParameterType.DSBS)).isTrue();
        Assertions.assertThat(SupplierMappingModerationTaskPropertiesGetter.isToPsku(taskInfo)).isFalse();

        taskProcessManager.processAll();
        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        YangTaskToDataItems items = yangTaskToDataItemsPersister.getByMarkupTaskId(taskInfo.getId()).get(0);

        tolokaApi.addYangTaskResults(items.getTaskId(), TEST_WORKER_ID,
                tolokaApi.convertModeration(generateModerationResults(taskInfo)));
        allBeans.get(YangResultsDownloader.class).stopSkippingTask(taskInfo.getId());
        processAllTasksWithUnlock(taskProcessManager);

        checkResult(taskInfo, 2, SWITCHED_CATEGORY_ID);
    }

    @Test
    public void testDsbsToPskuGeneration() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID), offer(SUPPLIER_ID));
        runGeneration(2, true);

        List<TaskConfigInfo> configInfoSet = new ArrayList<>(
                tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true));
        Assert.assertEquals(1, configInfoSet.size());

        TaskConfigInfo configInfo = configInfoSet.get(0);
        TaskInfo taskInfo = configInfo.getCurrentTasks().get(0);
        Assertions.assertThat(configInfo.getParameterValue(ConfigParameterType.DSBS)).isTrue();
        Assertions.assertThat(SupplierMappingModerationTaskPropertiesGetter.isToPsku(taskInfo)).isTrue();
    }

    @Test
    public void testPrioritiesWithNormalTasks() throws Exception {
        aliasMakerService.clearOffers();
        addOffers(SWITCHED_CATEGORY_ID, offer(SUPPLIER_ID), offer(SUPPLIER_ID));
        runGeneration(2, false);
        TaskConfigInfo dsbsConfigInfo = tasksCache.getConfigInfosByTypeId(getTaskType(), (x) -> true).iterator()
                .next();

        addOffers(CATEGORY_ID, offer(SUPPLIER_ID_2), offer(SUPPLIER_ID_2));
        runGeneration(true, false, false, 2);

        TaskConfigInfo normalConfigInfo = tasksCache.getConfigInfosByTypeId(
                getTaskType(), (x) -> x != dsbsConfigInfo).iterator().next();

        Assertions.assertThat(normalConfigInfo.getParameterValue(ConfigParameterType.DSBS)).isNull();

        processAllTasksWithUnlock(taskProcessManager);

        List<TicketPriorityInfo> priorityInfos = mboCategoryService.getTicketPriorities();

        List<TicketPriorityInfo> dsbsPriorities = priorityInfos.stream().filter(
                p -> p.getCategories().contains((long) SWITCHED_CATEGORY_ID)).collect(Collectors.toList());
        List<TicketPriorityInfo> normalPriorities = priorityInfos.stream().filter(
                p -> p.getCategories().contains((long) CATEGORY_ID)).collect(Collectors.toList());

        yangPrioritiesProcessor.updatePrioritiesInModeration(dsbsPriorities, true);
        yangPrioritiesProcessor.updatePrioritiesInModeration(normalPriorities, false);

        YangTaskToDataItemsPersister yangTaskToDataItemsPersister = allBeans.get(YangTaskToDataItemsPersister.class);
        YangTaskToDataItems dsbsItems = yangTaskToDataItemsPersister.getByMarkupTaskId(
                dsbsConfigInfo.getCurrentTasks().get(0).getId()).get(0);

        YangTaskToDataItems normalItems = yangTaskToDataItemsPersister.getByMarkupTaskId(
                normalConfigInfo.getCurrentTasks().get(0).getId()).get(0);

        Assertions.assertThat(normalItems.getIssuingOrderOverride()).isGreaterThan(
                dsbsItems.getIssuingOrderOverride()
        );
    }

    @Override
    public YangLogStorage.YangTaskType getYangTaskType() {
        return YangLogStorage.YangTaskType.MAPPING_MODERATION_DSBS;
    }
}
