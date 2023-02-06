package ru.yandex.direct.bsexport.iteration.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bs.export.model.WorkerSpec;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueStat;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.BANNERS_COUNT;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.CAMPAIGNS_COUNT;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.CAMPAIGN_ID;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.CONTEXTS_COUNT;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.KEYWORDS_COUNT;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.NEED_FULL_EXPORT;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.PRICES_COUNT;
import static ru.yandex.direct.core.entity.bs.export.queue.model.BsExportCandidateInfo.STATUS_BS_SYNCED;
import static ru.yandex.direct.dbschema.ppc.Tables.BS_EXPORT_QUEUE;

class CandidatesRepositoryGetCandidatesForExportTest extends CandidatesRepositoryTestBase {

    /**
     * 1.
     * В очереди bs_export_queue нет данных, а кампании в campaigns есть
     * -> ничего не выбирается
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STD_2", "STDPRICE_1", "STDPRICE_2", "CAMP_1"})
    void queueIsEmpty_returnsNothing(WorkerSpec workerSpec) {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campaignId = prepareCampaignText(dsl, null);
            bsExportQueueRepository.delete(dsl, Collections.singleton(campaignId));

            selectionCriteria.setWorkerSpec(workerSpec);

            var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build(), dsl);
            assertThat(exportCandidatesInfo).isEmpty();
        });
    }

    /**
     * 2.
     * В очереди bs_export_queue есть 5 незалоченных кампаний, вызов метода с lockNewCampaigns = false
     * -> ничего не выбралось
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STD_2", "STDPRICE_1", "STDPRICE_2", "CAMP_1"})
    void fiveUnlockedCampaignsInQueue_callWithoutLockNewCampaigns_returnsNothing(WorkerSpec workerSpec) {
        selectionCriteria.setWorkerSpec(workerSpec)
                .setLockNewCampaigns(false);

        runWithEmptyBsExportQueueTable(dsl -> {
            Stream.generate(() -> prepareCampaignText(dsl, null))
                    .limit(5)
                    .forEach(this::doNothingButCampaignsCreated);

            var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build(), dsl);
            assertThat(exportCandidatesInfo).isEmpty();
        });
    }

    /**
     * 3.
     * В очереди bs_export_queue 5 незалоченных кампаний, вызов метода с lockNewCampaigns = true
     * -> выбираются все 5 кампаний, c id данных кампаний
     */
    @Test
    void fiveUnlockedCampaignsInQueue_callWithLockNewCampaigns_returnsAllOfThem() {
        selectionCriteria.setLockNewCampaigns(true);

        runWithEmptyBsExportQueueTable(dsl -> {
            List<BsExportCandidateInfo> expected = Stream.generate(() -> prepareCampaignText(dsl, null))
                    .limit(5)
                    .map(this::makeCandidateInfo)
                    .collect(Collectors.toList());

            var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build(), dsl);
            assertThat(exportCandidatesInfo)
                    .usingElementComparatorOnFields(CAMPAIGN_ID.name())
                    .containsOnlyElementsOf(expected);
        });
    }

    /**
     * 4, 5.
     * В очереди bs_export_queue 5 залоченных кампаний, вызов метода с lockNewCampaigns = false или true
     * -> выбираются все 5 кампаний, c id данных кампаний
     */
    @ParameterizedTest(name = "lockNewCampaigns = {0}")
    @ValueSource(booleans = {false, true})
    void fiveLockedByUsCampaigns_callWithLockNewCampaignsValue_returnsAllOfThem(boolean lockNewCampaigns) {
        selectionCriteria.setLockNewCampaigns(lockNewCampaigns)
                .setWorkerSpec(LOCKED_BY);

        runWithEmptyBsExportQueueTable(dsl -> {
            List<BsExportCandidateInfo> expected = Stream.generate(() -> prepareCampaignText(dsl, LOCKED_BY))
                    .limit(5)
                    .map(this::makeCandidateInfo)
                    .collect(Collectors.toList());

            var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build(), dsl);
            assertThat(exportCandidatesInfo)
                    .usingElementComparatorOnFields(CAMPAIGN_ID.name())
                    .containsOnlyElementsOf(expected);
        });
    }

    /**
     * 6.
     * В очереди bs_export_queue есть 5 залоченных кампаний, вызов метода с lockNewCampaigns = false, limit = 3
     * -> выбираются только 3 кампании
     */
    @Test
    void fiveLockedByUsCampaigns_callWithoutLockNewCampaignsAndLimit3_returnsOnlyThree() {
        int expectSize = 3;

        Stream.generate(() -> prepareCampaignText(testShardContext, WorkerSpec.STD_9))
                .limit(expectSize + 2)
                .forEach(this::doNothingButCampaignsCreated);

        selectionCriteria.setLockNewCampaigns(false)
                .setWorkerSpec(WorkerSpec.STD_9)
                .setLimit(expectSize);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .hasSize(expectSize);
    }

    private void callWithCampaignIds(int expectSize, WorkerSpec lockedBy, boolean lockNewCampaigns) {
        List<Long> campaignIds = Stream.generate(() -> prepareCampaignText(testShardContext, lockedBy))
                .limit(expectSize + 2)
                .collect(Collectors.toList());
        Collections.shuffle(campaignIds);
        List<Long> expectCampaignIds = campaignIds.subList(0, expectSize);

        selectionCriteria.setLockNewCampaigns(lockNewCampaigns)
                .setWorkerSpec(LOCKED_BY)
                .setOnlyCampaignIds(expectCampaignIds);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsOnlyElementsOf(expectCampaignIds);
    }

    /**
     * 7.
     * В очереди bs_export_queue есть 5 залоченных кампаний
     * Вызов метода с lockNewCampaigns = false и списком campaignIds из трех id кампаний из очереди
     * -> выбираются только 3 кампании, c id переданных кампаний из списка campaignIds
     */
    @Test
    void fiveLockedByUsCampaigns_callWithCampaignIds_returnsSpecified() {
        callWithCampaignIds(3, LOCKED_BY, false);
    }

    /**
     * 8.
     * В очереди bs_export_queue есть 5 незалоченных кампаний
     * Вызов метода с limit = 3 и lockNewCampaigns = true
     * -> выбираются только 3 кампании
     */
    @Test
    void fiveUnlockedCampaigns_callWithLockNewCampaignsAndLimit3_returnsOnlyThree() {
        int expectSize = 3;

        Stream.generate(() -> prepareCampaignText(testShardContext, null))
                .limit(expectSize + 2)
                .forEach(this::doNothingButCampaignsCreated);

        selectionCriteria.setLockNewCampaigns(true)
                .setLimit(expectSize);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).hasSize(expectSize);
    }

    /**
     * 9.
     * В очереди bs_export_queue есть 5 незалоченных кампаний
     * Вызов метода с lockNewCampaigns = true и списком campaignIds из трех id кампаний из очереди
     * -> выбираются только 3 кампании, c id переданных кампаний из списка campaignIds
     */
    @Test
    void fiveUnlockedCampaigns_callWithCampaignIds_returnsSpecified() {
        callWithCampaignIds(3, null, true);
    }

    /**
     * 10.
     * В очереди bs_export_queue три кампании (общий залоченый с STD_2 счет и две не залоченые кампании под ним).
     * Вызов метода со списком campaignIds из id этих кампаний
     * -> ничего не выбирается
     */
    @Test
    void lockedWalletAndUnlockedCampaigns_returnsNothing() {
        Long campWalletId = prepareCampaignWallet(testShardContext, WorkerSpec.STD_2);
        Campaign campaign1 = prepareCampaignWithWallet(testShardContext, campWalletId, null);
        Campaign campaign2 = prepareCampaignWithWallet(testShardContext, campWalletId, null);

        selectionCriteria.setOnlyCampaignIds(List.of(campWalletId, campaign1.getId(), campaign2.getId()))
                .setWorkerSpec(WorkerSpec.STD_1);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).isEmpty();
    }

    /**
     * 11.
     * В очереди bs_export_queue три кампании (не залоченный общий счет, две кампании под ним - одна залочена STD_2,
     * другая нет)
     * Вызов метода со списком campaignIds из id этих кампаний
     * -> ничего не выбирается
     */
    @Test
    void lockedCampaignAndUnlockedWallet_returnsNothing() {
        Long campWalletId = prepareCampaignWallet(testShardContext, null);
        Campaign campaign1 = prepareCampaignWithWallet(testShardContext, campWalletId, WorkerSpec.STD_2);
        Campaign campaign2 = prepareCampaignWithWallet(testShardContext, campWalletId, null);

        selectionCriteria.setOnlyCampaignIds(List.of(campWalletId, campaign1.getId(), campaign2.getId()))
                .setWorkerSpec(WorkerSpec.STD_1);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).isEmpty();
    }

    /**
     * 12.
     * В очереди bs_export_queue три кампании (общий залоченый с STD_2 счет и две не залоченные кампании под ним).
     * Вызов метода со списком campaignIds из id этих кампаний и skipLockedWallets = fasle
     * -> выбираются только 2 кампании под кошельком
     */
    @Test
    void lockedWalletAndUnlockedCampaigns_callWithSkipLockedWallets_returnsCampaigns() {
        Long campWalletId = prepareCampaignWallet(testShardContext, WorkerSpec.STD_2);
        Campaign campaign1 = prepareCampaignWithWallet(testShardContext, campWalletId, null);
        Campaign campaign2 = prepareCampaignWithWallet(testShardContext, campWalletId, null);

        selectionCriteria.setOnlyCampaignIds(List.of(campWalletId, campaign1.getId(), campaign2.getId()))
                .setSkipLockedWallets(false)
                .setWorkerSpec(WorkerSpec.STD_1);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsExactlyInAnyOrder(campaign1.getId(), campaign2.getId());
    }

    /**
     * 13.
     * В очереди bs_export_queue три кампании (общий не залоченный счет и две кампании под ним - одна залочена STD_2,
     * другая нет)
     * Вызов метода со списком campaignIds из id этих кампаний и skipLockedWallets = false
     * -> выбираются только 2 кампании (не залоченная и общий счет)
     */
    @Test
    void lockedCampaignAndUnlockedWallet_callWithSkipLockedWallets_returnsUnlockedCampaignAndWallet() {
        Long campWalletId = prepareCampaignWallet(testShardContext, null);
        Campaign campaign1 = prepareCampaignWithWallet(testShardContext, campWalletId, WorkerSpec.STD_2);
        Campaign campaign2 = prepareCampaignWithWallet(testShardContext, campWalletId, null);

        selectionCriteria.setOnlyCampaignIds(List.of(campWalletId, campaign1.getId(), campaign2.getId()))
                .setSkipLockedWallets(false)
                .setWorkerSpec(WorkerSpec.STD_1);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsExactlyInAnyOrder(campWalletId, campaign2.getId());
    }

    /**
     * 14.
     * В очереди bs_export_queue две кампании, одна из них залочена
     * Вызов метода со списком campaignIds из id этих кампаний
     * -> выбираются все кампании из списка campaignIds
     */
    @Test
    void lockedByUsCampaignAndUnlockedCampaign_returnsBoth() {
        Long campaignId1 = prepareCampaignText(testShardContext, null);
        Long campaignId2 = prepareCampaignText(testShardContext, LOCKED_BY);
        List<Long> expectCampaignIds = List.of(campaignId1, campaignId2);

        selectionCriteria.setOnlyCampaignIds(expectCampaignIds);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportQueueStat::getCampaignId)
                .containsOnlyElementsOf(expectCampaignIds);
    }

    /**
     * 15.
     * В очереди bs_export_queue есть одна залоченная кампания
     * Вызов метода со списком campaignIds из id этой кампании
     * -> выбирается данная кампания с теми же значениями статистики, которые передавали в очередь
     */
    @Test
    void lockedByUsCampaign_returnedWithQueueStat() {
        Long campaignId = prepareCampaignText(testShardContext, LOCKED_BY);

        selectionCriteria.setOnlyCampaignIds(List.of(campaignId));

        BsExportQueueInfo expected = bsExportQueueRepository.getBsExportQueueInfo(TEST_SHARD, campaignId);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .hasSize(1)
                .first()
                .isEqualToComparingOnlyGivenFields(expected, CAMPAIGN_ID.name(),
                        CAMPAIGNS_COUNT.name(),
                        CONTEXTS_COUNT.name(),
                        BANNERS_COUNT.name(),
                        KEYWORDS_COUNT.name(),
                        PRICES_COUNT.name(),
                        NEED_FULL_EXPORT.name());
    }

    /**
     * 16.
     * В очереди bs_export_queue есть залоченная кампания, у которой есть заданное значение statusBsSynced
     * Вызов метода со списком campaignIds c id этой кампании
     * -> выбирается кампания из списка campaignIds и у нее переданное значение statusBsSynced
     */
    @ParameterizedTest(name = "campaigns.statusBsSynced = {0}")
    @EnumSource(value = StatusBsSynced.class)
    void unlockedCampaign_returnedWithStatusBsSynced(StatusBsSynced statusBsSynced) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, statusBsSynced);

        selectionCriteria.setOnlyCampaignIds(singleton(campaignId));

        var expected = new BsExportCandidateInfo()
                .withCampaignId(campaignId)
                .withStatusBsSynced(statusBsSynced);

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .isEqualToComparingOnlyGivenFields(expected, CAMPAIGN_ID.name(), STATUS_BS_SYNCED.name());
    }

    /**
     * 17.
     * Std-воркер не берет кампании ни с каким назначенным queueType.
     */
    @ParameterizedTest(name = "queueType = {0}")
    @EnumSource(value = QueueType.class)
    void campaignWithQueueTypeValue_callAsStdWorker_returnsNothing(QueueType queueType) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setQueueType(campaignId, queueType);

        selectionCriteria.setOnlyCampaignIds(singleton(campaignId))
                .setWorkerSpec(WorkerSpec.STD_1);

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 18.
     * Никакие воркеры не берут nosend-кампании.
     * <p>
     * В очереди bs_export_queue есть не залоченная кампания, у которой есть заданное значение в таблице
     * bs_export_specials.par_type = nosend
     * Вызов метода с разным значением WorkSpec, со списком campaignIds c id этой кампании
     * -> ничего не выбирается
     */
    @Test
    void unlockedCampaignWithNosendQueueType_callAsAnyWorker_returnsNothing() {
        SoftAssertions softAssertions = new SoftAssertions();

        StreamEx.of(WorkerSpec.values())
                .remove(WorkerSpec.MASTER::equals)
                .remove(WorkerSpec.NOSEND_FOR_DROP_SANDBOX_CLIENT::equals)
                .forEach(workerSpec -> {
                    Long campaignId = prepareCampaignText(testShardContext, null);
                    setQueueType(campaignId, QueueType.NOSEND);

                    selectionCriteria.setWorkerSpec(workerSpec)
                            .setOnlyCampaignIds(singleton(campaignId));

                    var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
                    softAssertions.assertThat(campaignIdsForExport)
                            .withFailMessage("No candidates expected for workerSpec %s", workerSpec)
                            .isEmpty();
                });

        softAssertions.assertAll();
    }

    /**
     * 19.
     * Никакой воркер не берет кампанию, залоченную другим.
     * <p>
     * В очереди bs_export_queue есть залоченная кампания заданным WorkSpec
     * Вызов метода с разным значением WorkSpec (не равным MASTER или NOSEND_FOR_DROP_SANDBOX_CLIENT), отличающимся от
     * значения WorkSpec кампании, со списком campaignIds c id этой кампании
     * -> ничего не выбирается
     */
    @Test
    void workerDoesNotGetCampaignLockedByOther() {
        SoftAssertions softAssertions = new SoftAssertions();

        WorkerSpec[] sample = new WorkerSpec[]{
                WorkerSpec.STD_1, WorkerSpec.STD_2, WorkerSpec.STD_10, WorkerSpec.STD_18,
                WorkerSpec.STDPRICE_1, WorkerSpec.STDPRICE_2, WorkerSpec.STDPRICE_10,
                WorkerSpec.HEAVY_1, WorkerSpec.HEAVY_2, WorkerSpec.HEAVY_10, WorkerSpec.HEAVY_18,
                WorkerSpec.CAMP_1,
                WorkerSpec.CAMPS_ONLY_1,
                WorkerSpec.FAST_1,
                WorkerSpec.DEV1_1, WorkerSpec.DEVPRICE1_1,
                WorkerSpec.DEV2_1, WorkerSpec.DEVPRICE2_1,
                WorkerSpec.PREPROD_1,
                WorkerSpec.BUGGY_1, WorkerSpec.BUGGY_2,
                WorkerSpec.INTERNAL_ADS_1,
                WorkerSpec.FULL_LB_EXPORT_1, WorkerSpec.FULL_LB_EXPORT_2, WorkerSpec.FULL_LB_EXPORT_10,
        };

        StreamEx.of(sample)
                .cross(sample)
                .filterKeyValue((lockedBy, workerSpec) -> lockedBy != workerSpec)
                .forKeyValue((lockedBy, workerSpec) -> {
                    Long campaignId = prepareCampaignText(testShardContext, lockedBy);

                    selectionCriteria.setWorkerSpec(workerSpec)
                            .setOnlyCampaignIds(singleton(campaignId));

                    var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
                    assertThat(campaignIdsForExport)
                            .withFailMessage("No candidates expected for workerSpec {} when campaign locked by %s",
                                    workerSpec, lockedBy)
                            .isEmpty();
                });

        softAssertions.assertAll();
    }

    /**
     * 20.
     * Специальные вокеры не берут кампании без специализации
     * <p>
     * В очереди bs_export_queue есть не залоченная кампания
     * Вызов метода с разным значением WorkSpec, со списком campaignIds c id этой кампании
     * -> ничего не выбирается
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"DEV1_1", "DEVPRICE1_1", "DEV2_1", "DEVPRICE2_1", "HEAVY_1",
            "FAST_1", "CAMPS_ONLY_1", "BUGGY_1", "PREPROD_1"})
    void campaignWithoutQueueType_callAsSpecialWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркеров цен

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    private Long prepareDataForStdprice1Test(StatusBsSynced statusBsSynced) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, statusBsSynced);

        selectionCriteria.setOnlyCampaignIds(List.of(campaignId))
                .setWorkerSpec(WorkerSpec.STDPRICE_1);

        return campaignId;
    }

    /**
     * 21, 22.
     * Есть кампания с statusBsSynced = No / Sending
     * Вызов метода c WorkSpec.STDPRICE_1, со списком campaignIds из id этой кампании
     * -> ничего не выбирается
     */
    @ParameterizedTest(name = "campaigns.statusBsSynced = {0}")
    @EnumSource(value = StatusBsSynced.class, names = {"NO", "SENDING"})
    void unsyncedCampaign_callAsStdpriceWorker_returnsNothing(StatusBsSynced statusBsSynced) {
        prepareDataForStdprice1Test(statusBsSynced);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).isEmpty();
    }

    /**
     * 23.
     * Есть кампания с statusBsSynced = Yes
     * Вызов метода c WorkSpec.STDPRICE_1, со списком campaignIds из id этой кампании
     * -> выбирается кампания из списка campaignIds
     */
    @Test
    void syncedCampaign_callAsStdpriceWorker_returnsCampaign() {
        Long campaignId = prepareDataForStdprice1Test(StatusBsSynced.YES);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .usingElementComparatorOnFields(CAMPAIGN_ID.name())
                .contains(makeCandidateInfo(campaignId));
    }

    /**
     * 24
     * Проверяем что потоки для limtest1 успешно берут кампании dev1 очереди
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, mode = EnumSource.Mode.MATCH_ALL, names = {"^DEV(PRICE)?1_\\d+$"})
    void campaignWithQueueTypeDev1_callAsDev1Worker_returnsCampaign(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.DEV1);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .hasFieldOrPropertyWithValue(CAMPAIGN_ID.name(), campaignId);
    }

    /**
     * 25
     * Проверяем обычные кампании из dev1 очереди не берут ни обычные, ни воркеры предназначенные для limtest2.
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STDPRICE_1", "HEAVY_1", "FAST_1", "BUGGY_1", "PREPROD_1",
            "DEV2_1", "DEVPRICE2_1"})
    void campaignWithQueueTypeDev1_callAsUnrelatedWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.DEV1);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 26
     * Проверяем что потоки для limtest2 успешно берут кампании dev2 очереди
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, mode = EnumSource.Mode.MATCH_ALL, names = {"^DEV(PRICE)?2_\\d+$"})
    void campaignWithQueueTypeDev2_callAsDev2Worker_returnsCampaign(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.DEV2);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .hasFieldOrPropertyWithValue(CAMPAIGN_ID.name(), campaignId);
    }

    /**
     * 27
     * Проверяем обычные кампании из dev2 очереди не берут ни обычные, ни воркеры предназначенные для limtest1.
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STDPRICE_1", "HEAVY_1", "FAST_1", "BUGGY_1", "PREPROD_1",
            "DEV1_1", "DEVPRICE1_1"})
    void campaignWithQueueTypeDev2_callAsUnrelatedWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.DEV2);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 28
     * Проверяем что кампании с назначенной очередью "buggy" успешно берут buggy-воркеры
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"BUGGY_1", "BUGGY_2"})
    void campaignWithQueueTypeBuggy_callAsBuggyWorker_returnsCampaign(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setQueueType(campaignId, QueueType.BUGGY);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .hasFieldOrPropertyWithValue(CAMPAIGN_ID.name(), campaignId);
    }

    /**
     * 29
     * Камапнии с назначенной очередью "buggy" не берут никакие воркеры (обычные или специальные), кроме buggy.
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STDPRICE_1", "HEAVY_1", "DEV1_1", "DEVPRICE1_1", "FAST_1",
            "DEV2_1", "DEVPRICE2_1", "PREPROD_1", "CAMP_1", "CAMPS_ONLY_1"})
    void campaignWithQueueTypeBuggy_callAsUnrelatedWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.BUGGY);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 30
     * Проверяем что кампании с назначенной очередью "heavy" успешно берут heavy-воркеры
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"HEAVY_1", "HEAVY_2", "HEAVY_10", "HEAVY_18"})
    void campaignWithQueueTypeHeavy_callAsHeavyWorker_returnsCampaign(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setQueueType(campaignId, QueueType.HEAVY);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .hasFieldOrPropertyWithValue(CAMPAIGN_ID.name(), campaignId);
    }

    /**
     * 31
     * Камапнии с назначенной очередью "heavy" не берут никакие воркеры (обычные или специальные), кроме heavy.
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STDPRICE_1", "DEV1_1", "DEVPRICE1_1", "FAST_1", "CAMP_1",
            "DEV2_1", "DEVPRICE2_1", "PREPROD_1", "BUGGY_1", "CAMPS_ONLY_1"})
    void campaignWithQueueTypeHeavy_callAsUnrelatedWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.HEAVY);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 32
     * Проверяем что кампании с назначенной очередью "preprod" успешно берут preprod-воркеры
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, mode = EnumSource.Mode.MATCH_ALL, names = "^PREPROD_\\d+$")
    void campaignWithQueueTypePreprod_callAsPreprodWorker_returnsCampaign(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setQueueType(campaignId, QueueType.PREPROD);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .hasFieldOrPropertyWithValue(CAMPAIGN_ID.name(), campaignId);
    }

    /**
     * 33
     * Камапнии с назначенной очередью "preprod" не берут никакие воркеры (обычные или специальные), кроме preprod.
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STDPRICE_1", "DEV1_1", "DEVPRICE1_1", "FAST_1", "CAMP_1",
            "DEV2_1", "DEVPRICE2_1", "HEAVY_1", "BUGGY_1", "CAMPS_ONLY_1"})
    void campaignWithQueueTypePreprod_callAsUnrelatedWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES); // для воркера цен
        setQueueType(campaignId, QueueType.PREPROD);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 34.
     * Кампании с типом INTERNAL_FREE не берут никакие воркеры кроме INTERNAL_ADS* и ре-экспорта
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "DEV1_1", "FAST_1", "PREPROD_1", "CAMP_1", "BUGGY_1",
            "CAMPS_ONLY_1", "HEAVY_1", "STDPRICE_1"})
    void internalFreeCampaign_callAsUnrelatedWorker_returnsNoting(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaign(testShardContext, CampaignType.INTERNAL_FREE, null);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    private void internalFreeCampaignSuccessTest(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaign(testShardContext, CampaignType.INTERNAL_FREE, null);

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .usingElementComparatorOnFields(CAMPAIGN_ID.name())
                .contains(makeCandidateInfo(campaignId));
    }

    /**
     * 35.
     * Есть кампания c типом INTERNAL_FREE
     * Вызов метода c WorkSpec.INTERNAL_ADS_1, со списком campaignIds c id этой кампании
     * -> выбирается кампания из списка campaignIds
     */
    @Test
    void internalFreeCampaign_callAsInternalAdsWorker_returnsCampaign() {
        internalFreeCampaignSuccessTest(WorkerSpec.INTERNAL_ADS_1);
    }

    /**
     * 36.
     * Есть кампания c типом INTERNAL_FREE
     * Вызов метода c WorkSpec.FULL_LB_EXPORT_1, со списком campaignIds c id этой кампании
     * -> выбирается кампания из списка campaignIds
     */
    @Test
    void internalFreeCampaign_callAsFullLbExportWorker_returnsCampaign() {
        internalFreeCampaignSuccessTest(WorkerSpec.FULL_LB_EXPORT_1);
    }

    /**
     * Кейс 37: FullLbExport отправляет кампании из некоторых специальных очередей
     */
    @ParameterizedTest(name = "queueType = {0}")
    @NullSource
    @EnumSource(value = QueueType.class, names = {"HEAVY", "FAST", "CAMPS_ONLY", "PREPROD"})
    void campaignWithQueueTypeValue_callAsFullLbExportWorker_returnsCampaign(QueueType queueType) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setQueueType(campaignId, queueType);

        selectionCriteria.setWorkerSpec(WorkerSpec.FULL_LB_EXPORT_1)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport)
                .first()
                .hasFieldOrPropertyWithValue(CAMPAIGN_ID.name(), campaignId);
    }

    /**
     * Кейс 38: FullLbExport воркер не берет кампании из особенных очередей, так как у них могут значительно отличаться
     * данные: лимтесты - by design, buggy - "на всякий случай"
     */
    @ParameterizedTest(name = "queueType = {0}")
    @EnumSource(value = QueueType.class, names = {"BUGGY", "DEV1", "DEV2"})
    void campaignWithVerySpecialQueueTypeValue_callAsFullLbExportWorker_returnsNothing(QueueType queueType) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        setQueueType(campaignId, queueType);

        selectionCriteria.setWorkerSpec(WorkerSpec.FULL_LB_EXPORT_1)
                .setOnlyCampaignIds(singleton(campaignId));

        var campaignIdsForExport = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(campaignIdsForExport).isEmpty();
    }

    /**
     * 39 FULL_LB_EXPORT воркеры не берут кампании с is_full_export = 0
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"FULL_LB_EXPORT_1", "FULL_LB_EXPORT_2", "FULL_LB_EXPORT_10"})
    void campaignWithoutFullExportFlag_callAsFullLbExportWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.IS_FULL_EXPORT, 0L)
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId))
                .execute();

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).isEmpty();
    }

    /**
     * 40 STDPRICE воркеры не берут кампании с prices_num = 0
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STDPRICE_1", "STDPRICE_2", "STDPRICE_10"})
    void campaignWithoutPricesNum_callAsStdpriceWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        // для успешной отправки цен кампания должна быь синхронной
        setCampaignStatusBsSynced(testShardContext, campaignId, StatusBsSynced.YES);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.PRICES_NUM, 0L)
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId))
                .execute();

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).isEmpty();
    }

    /**
     * 41.
     * В очереди bs_export_queue есть кампания с banners_num = 0 ,camps_num = 0 ,contexts_num = 0 ,bids_num = 0
     * Вызов метода со списком campaignIds c id этой кампании
     * -> ничего не выбирается
     */
    @ParameterizedTest(name = "workerSpec = {0}")
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STD_2", "STD_10", "STD_18"})
    void campaignWithoutStat_callAsStdWorker_returnsNothing(WorkerSpec workerSpec) {
        Long campaignId = prepareCampaignText(testShardContext, null);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.BANNERS_NUM, 0L)
                .set(BS_EXPORT_QUEUE.CAMPS_NUM, 0L)
                .set(BS_EXPORT_QUEUE.CONTEXTS_NUM, 0L)
                .set(BS_EXPORT_QUEUE.BIDS_NUM, 0L)
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId))
                .execute();

        selectionCriteria.setWorkerSpec(workerSpec)
                .setOnlyCampaignIds(singleton(campaignId));

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo).isEmpty();
    }

    /**
     * 42.
     * В очереди bs_export_queue есть три кампании с одинаковыми queue_time и seq_time
     * Вызов метода со списком campaignIds c id этих кампаний и lockNewCampaigns = true
     * -> выбираются все кампании из списка campaignIds в порядке возрастания cid
     */
    @Test
    void campaignsWithSameQueueAndSeqTime_returnsSortedByCampaignId() {
        LocalDateTime time = LocalDateTime.now();
        List<Long> campaignIds = Stream.generate(() -> prepareCampaignText(testShardContext, null))
                .limit(3)
                .collect(Collectors.toList());
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.QUEUE_TIME, time)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, time)
                .where(BS_EXPORT_QUEUE.CID.in(campaignIds))
                .execute();

        selectionCriteria.setOnlyCampaignIds(campaignIds);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(CAMPAIGN_ID.name())
                .isSorted();
    }

    /**
     * 43.
     * В очереди bs_export_queue есть три кампании с одинаковыми queue_time и seq_time, при этом вторая кампания
     * залочена
     * Вызов метода со списком campaignIds c id этих кампаний
     * -> выбираются все кампании из списка campaignIds, при этом в начале списка будет залоченная, остальные в
     * порядке возрастания cid
     */
    @Test
    void lockedAndUnlockedCampaigns_returnsLockedFirstAndOtherSortedByCampaignId() {
        LocalDateTime time = LocalDateTime.now();
        // неявно полагаемся, что каждая новая созданная кампнания имеет номер, больший предыдущего
        Long campaignId1 = prepareCampaignText(testShardContext, null);
        Long campaignId2 = prepareCampaignText(testShardContext, LOCKED_BY);
        Long campaignId3 = prepareCampaignText(testShardContext, null);
        List<Long> campaignIds = List.of(campaignId1, campaignId2, campaignId3);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.QUEUE_TIME, time)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, time)
                .where(BS_EXPORT_QUEUE.CID.in(campaignIds))
                .execute();

        selectionCriteria.setOnlyCampaignIds(campaignIds);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsExactly(campaignId2, campaignId1, campaignId3);
    }

    /**
     * 44.
     * В очереди bs_export_queue есть три кампании с разными значениями seq_time
     * Вызов метода со списком campaignIds c id этих кампаний
     * -> выбираются все кампании из списка campaignIds в порядке возрастания seq_time
     */
    @Test
    void campaignsWithSameQueueTime_returnsSortedBySeqTime() {
        LocalDateTime now = LocalDateTime.now();
        Long campaignId1 = prepareCampaignText(testShardContext, null);
        Long campaignId2 = prepareCampaignText(testShardContext, null);
        Long campaignId3 = prepareCampaignText(testShardContext, null);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now.minusWeeks(1))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId2))
                .execute();
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now.minusMonths(1))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId3))
                .execute();

        selectionCriteria.setOnlyCampaignIds(List.of(campaignId1, campaignId2, campaignId3));

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsExactly(campaignId3, campaignId2, campaignId1);
    }

    /**
     * 45.
     * В очереди bs_export_queue есть три кампании с разными значениями seq_time и queue_time
     * Вызов метода со списком campaignIds c id этих кампаний
     * -> выбираются все кампании из списка campaignIds в порядке возрастания seq_time и queue_time (для случаев с
     * одинаковым seq_time)
     */
    @Test
    void campaignsWithDifferentQueueAndSeqTimes_returnsSortedBySeqTimeAndQueueTime() {
        LocalDateTime now = LocalDateTime.now();
        Long campaignId1 = prepareCampaignText(testShardContext, null);
        Long campaignId2 = prepareCampaignText(testShardContext, null);
        Long campaignId3 = prepareCampaignText(testShardContext, null);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now)
                .where(BS_EXPORT_QUEUE.CID.in(List.of(campaignId1, campaignId2)))
                .execute();
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now.plusHours(1))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId3))
                .execute();
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.QUEUE_TIME, now.minusHours(2))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId1))
                .execute();

        selectionCriteria.setOnlyCampaignIds(List.of(campaignId1, campaignId2, campaignId3));

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsExactly(campaignId1, campaignId2, campaignId3);
    }

    /**
     * 46.
     * В очереди bs_export_queue есть три кампании с разными значениями seq_time и full_export_seq_time
     * Вызов метода c WorkerSpec.FULL_LB_EXPORT_1, со списком campaignIds c id этих кампаний
     * -> выбираются все кампании из списка campaignIds в порядке возрастания full_export_seq_time
     */
    @Test
    void campaignsTimes_callAsFullLbExportWorker_returnsSortedByFullExportSeqTime() {
        LocalDateTime now = LocalDateTime.now();
        Long campaignId1 = prepareCampaignText(testShardContext, null);
        Long campaignId2 = prepareCampaignText(testShardContext, null);
        Long campaignId3 = prepareCampaignText(testShardContext, null);
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now.plusHours(3))
                .set(BS_EXPORT_QUEUE.FULL_EXPORT_SEQ_TIME, now.plusHours(1))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId1))
                .execute();
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now)
                .set(BS_EXPORT_QUEUE.FULL_EXPORT_SEQ_TIME, now.plusHours(3))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId2))
                .execute();
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.SEQ_TIME, now.plusHours(1))
                .set(BS_EXPORT_QUEUE.FULL_EXPORT_SEQ_TIME, now)
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId3))
                .execute();
        testShardContext.update(BS_EXPORT_QUEUE)
                .set(BS_EXPORT_QUEUE.QUEUE_TIME, now.minusHours(2))
                .where(BS_EXPORT_QUEUE.CID.eq(campaignId1))
                .execute();

        selectionCriteria.setOnlyCampaignIds(List.of(campaignId1, campaignId2, campaignId3))
                .setWorkerSpec(WorkerSpec.FULL_LB_EXPORT_1);

        var exportCandidatesInfo = queueRepository.getCandidatesForExport(selectionCriteria.build());
        assertThat(exportCandidatesInfo)
                .extracting(BsExportCandidateInfo::getCampaignId)
                .containsExactly(campaignId3, campaignId1, campaignId2);
    }

    private void doNothingButCampaignsCreated(Long ignored) {
    }

    private BsExportCandidateInfo makeCandidateInfo(Long campaignId) {
        return new BsExportCandidateInfo().withCampaignId(campaignId);
    }
}
