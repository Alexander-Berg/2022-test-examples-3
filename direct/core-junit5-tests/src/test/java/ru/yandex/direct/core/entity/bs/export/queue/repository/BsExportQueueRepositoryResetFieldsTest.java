package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.core.entity.bs.export.model.CampaignIdWithSyncValue;
import ru.yandex.direct.core.entity.bs.export.model.WorkerPurpose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithoutStat;

class BsExportQueueRepositoryResetFieldsTest extends BsExportQueueRepositoryBase {

    /**
     * Тест проверяет, что если у кампании изменился syncVal, то для нее не сбросятся поля
     */
    @Test
    void resetFields_SyncValueChangedTest() {
        var campWithAnotherSyncValue = createCampaign();
        long syncValBeforeChanged = 1;
        long syncValAfterChanged = 2;
        int parId = 15;
        var recordWithAnotherSyncValue = recordWithFullStat(campWithAnotherSyncValue);
        recordWithAnotherSyncValue
                .withSynchronizeValue(syncValAfterChanged)
                .withCampaignsCount(1L)
                .withLockedBy(parId);

        queueRepository
                .insertRecord(testShardContext, recordWithAnotherSyncValue);
        List<CampaignIdWithSyncValue> campaignIdsWithSyncVal = List.of(
                new CampaignIdWithSyncValueImpl(campWithAnotherSyncValue, syncValBeforeChanged));

        queueRepository.resetHandledFields(TEST_SHARD, campaignIdsWithSyncVal, parId, WorkerPurpose.ONLY_CAMPAIGNS);
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campWithAnotherSyncValue))
                .isEqualToComparingFieldByField(recordWithAnotherSyncValue);
    }

    /**
     * Тест проверяет, что если у кампании не изменился syncVal, то для нее сбросятся поля
     */
    @Test
    void resetFields_SyncValueNotChangedTest() {
        var campWithSameSyncValue = createCampaign();
        long syncVal = 0;
        int parId = 15;
        var recordWithSameSyncValue = recordWithFullStat(campWithSameSyncValue);
        recordWithSameSyncValue
                .withSynchronizeValue(syncVal)
                .withCampaignsCount(1L)
                .withLockedBy(parId);
        queueRepository
                .insertRecord(testShardContext, recordWithSameSyncValue);

        List<CampaignIdWithSyncValue> campaignIdsWithSyncVal = List.of(
                new CampaignIdWithSyncValueImpl(campWithSameSyncValue, syncVal));
        queueRepository.resetHandledFields(TEST_SHARD, campaignIdsWithSyncVal, parId, WorkerPurpose.ONLY_CAMPAIGNS);

        var recordWithSameSyncValGot = queueRepository.getBsExportQueueInfo(TEST_SHARD, campWithSameSyncValue);
        assertThat(recordWithSameSyncValGot).isEqualToIgnoringGivenFields(recordWithSameSyncValue,
                "campaignsCount");
        assertThat(recordWithSameSyncValGot.getCampaignsCount()).isEqualTo(0L);
    }

    /**
     * Тест проверяет, что если у кампании изменился parId, то для нее не сбросятся поля
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = 16)
    void resetFields_ParIdChangedTest(Integer anotherParId) {
        var campWithAnotherParId = createCampaign();
        long syncVal = 0;
        int ourParId = 15;
        var recordWithAnotherParId = recordWithFullStat(campWithAnotherParId);
        recordWithAnotherParId
                .withSynchronizeValue(syncVal)
                .withCampaignsCount(1L)
                .withLockedBy(anotherParId);

        queueRepository
                .insertRecord(testShardContext, recordWithAnotherParId);
        List<CampaignIdWithSyncValue> campaignIdsWithSyncVal = List.of(
                new CampaignIdWithSyncValueImpl(campWithAnotherParId, syncVal));

        queueRepository.resetHandledFields(TEST_SHARD, campaignIdsWithSyncVal, ourParId, WorkerPurpose.ONLY_CAMPAIGNS);
        assertThat(queueRepository.getBsExportQueueInfo(TEST_SHARD, campWithAnotherParId))
                .isEqualToComparingFieldByField(recordWithAnotherParId);
    }

    static Stream<Arguments> params() {
        return Stream.of(
                arguments(WorkerPurpose.ONLY_CAMPAIGNS, List.of("campaignsCount")),
                arguments(WorkerPurpose.ONLY_PRICES, List.of("pricesCount")),
                arguments(WorkerPurpose.CONTEXTS_AND_BANNERS,
                        List.of("bannersCount", "contextsCount", "keywordsCount")),
                arguments(WorkerPurpose.DATA_AND_PRICES,
                        List.of("campaignsCount", "bannersCount", "contextsCount", "keywordsCount", "pricesCount")),
                arguments(WorkerPurpose.DATA,
                        List.of("campaignsCount", "bannersCount", "contextsCount", "keywordsCount")),
                arguments(WorkerPurpose.FULL_EXPORT, List.of("needFullExport")));
    }

    @ParameterizedTest(name = "workerPurpose = {0}, fields to be reseted = {1}")
    @MethodSource("params")
    void resetFields_WorkerPurposeTest(WorkerPurpose workerPurpose, List<String> fieldsToReset) {
        var camp = createCampaign();
        long syncVal = 0;
        int parId = 15;

        var queueInfo = recordWithFullStat(camp);
        queueInfo
                .withSynchronizeValue(syncVal)
                .withCampaignsCount(1L)
                .withBannersCount(30L)
                .withContextsCount(40L)
                .withPricesCount(10L)
                .withNeedFullExport(true)
                .withKeywordsCount(50L)
                .withLockedBy(parId);
        queueRepository
                .insertRecord(testShardContext, queueInfo);

        List<CampaignIdWithSyncValue> campaignIdsWithSyncVal = List.of(new CampaignIdWithSyncValueImpl(camp, syncVal));
        queueRepository.resetHandledFields(TEST_SHARD, campaignIdsWithSyncVal, parId, workerPurpose);

        var queueInfoGot = queueRepository.getBsExportQueueInfo(TEST_SHARD, camp);
        assertThat(queueInfoGot).isEqualToIgnoringGivenFields(queueInfo, fieldsToReset.toArray(String[]::new));
        var zeroStat = recordWithoutStat(camp);
        assertThat(queueInfoGot).isEqualToComparingOnlyGivenFields(zeroStat, fieldsToReset.toArray(String[]::new));
    }
}
