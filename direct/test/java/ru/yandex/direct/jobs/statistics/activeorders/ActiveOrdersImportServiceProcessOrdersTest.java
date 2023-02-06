package ru.yandex.direct.jobs.statistics.activeorders;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.log.service.LogActiveOrdersService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.WhenMoneyOnCampWasRepository;
import ru.yandex.direct.core.entity.campoperationqueue.CampOperationQueueRepository;
import ru.yandex.direct.core.entity.statistics.container.ProceededActiveOrder;
import ru.yandex.direct.core.entity.statistics.model.ActiveOrderChanges;
import ru.yandex.direct.core.entity.statistics.repository.ActiveOrdersRepository;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_DISTRIB;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_FREE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;

class ActiveOrdersImportServiceProcessOrdersTest {

    private ActiveOrdersImportService activeOrdersImportService;
    private ActiveOrdersMetrics activeOrdersMetrics;

    @BeforeEach
    void before() {
        var campaignRepository = mock(CampaignRepository.class);
        var ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        var whenMoneyOnCampWasRepository = mock(WhenMoneyOnCampWasRepository.class);
        var campOperationQueueRepository = mock(CampOperationQueueRepository.class);
        var activeOrdersRepository = mock(ActiveOrdersRepository.class);
        var logActiveOrdersService = mock(LogActiveOrdersService.class);
        var walletMoneyCalculator = mock(WalletMoneyCalculator.class);
        activeOrdersImportService = new ActiveOrdersImportService(campaignRepository, ppcPropertiesSupport,
                whenMoneyOnCampWasRepository, campOperationQueueRepository, walletMoneyCalculator,
                activeOrdersRepository, logActiveOrdersService);
        activeOrdersMetrics = new ActiveOrdersMetrics(1);
    }

    @AfterEach
    void after() {
        activeOrdersMetrics.clearMetrics();
    }

    @Test
    void rollbackedCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withNewSumSpent(1_000_000)
                .withOldSumSpent(1_100_000)
                .withArchived(CampaignsArchived.No.getLiteral())
                .withSum(10_000_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededOrder(1_000_000);
        expectedProcessedOrder.setRollbacked(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 1, 0, 0, 0, 0);
    }

    @Test
    void notRollbackedCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withNewSumSpent(1_100_000)
                .withOldSumSpent(1_000_000)
                .withArchived(CampaignsArchived.No.getLiteral())
                .withSum(10_000_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);
        var expectedProcessedOrder = proceededOrder(1_100_000);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 0, 0, 0, 0);
    }

    @Test
    void rollbackedAndUnarcCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withNewSumSpent(1_000_000)
                .withOldSumSpent(1_100_000)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withSum(10_000_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededOrder(1_000_000);
        expectedProcessedOrder.setRollbacked(true);
        expectedProcessedOrder.setUnarchived(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 1, 0, 0, 0, 1);
    }

    @Test
    void rollbackedInternalFreeCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withType(INTERNAL_FREE)
                .withNewSumSpentUnits(1_000_000)
                .withOldSumSpentUnits(1_100_000)
                .withArchived(CampaignsArchived.No.getLiteral())
                .withUnits(1_050_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededInternalFreeOrder(1_050_000, 1_000_000);
        expectedProcessedOrder.setRollbacked(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 1, 0, 0, 0, 0);
    }

    @Test
    void rollbackedAndUnarcInternalFreeCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withType(INTERNAL_FREE)
                .withNewSumSpentUnits(1_000_000)
                .withOldSumSpentUnits(1_100_000)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withUnits(1_050_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededInternalFreeOrder(1_050_000, 1_000_000);
        expectedProcessedOrder.setRollbacked(true);
        expectedProcessedOrder.setUnarchived(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 1, 0, 0, 0, 1);
    }

    @Test
    void finishedCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withNewSumSpent(1_000_000)
                .withOldSumSpent(999_800)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withSum(1_000_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededOrder(1_000_000);
        expectedProcessedOrder.setFinished(true);
        expectedProcessedOrder.setMoneyEnd(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 1, 1, 0, 0);
    }

    @Test
    void finishedInternalFreeCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withType(INTERNAL_FREE)
                .withUnits(100)
                .withNewSumSpentUnits(100)
                .withOldSumSpentUnits(10)
                .withArchived(CampaignsArchived.No.getLiteral())
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededInternalFreeOrder(100, 100);
        expectedProcessedOrder.setFinished(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 1, 0, 0, 0);
    }

    @Test
    void finishedInternalDistribCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withType(INTERNAL_DISTRIB)
                .withNewSumSpent(1_200_000)
                .withOldSumSpent(1_100_000)
                .withSum(1_000_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = new ProceededActiveOrder(1, INTERNAL_DISTRIB, 1000, 10,
                BigDecimal.valueOf(1_000_000, 6),
                0, 0);
        expectedProcessedOrder.setFinished(false);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 0, 0, 0, 0);
    }

    @Test
    void finishedCampaignAndNonZeroWalletTest() {
        var activeOrderChanges = changesBuilder()
                .withNewSumSpent(1_200_000)
                .withOldSumSpent(999_800)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withSum(1_000_000)
                .withWalletCid(2)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = proceededOrder(1_200_000);
        expectedProcessedOrder.setFinished(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 1, 0, 0, 0);
    }

    @Test
    void alreadyFinishedCampaignTest() {
        var activeOrderChanges = changesBuilder()
                .withNewSumSpent(1_400_000)
                .withOldSumSpent(1_200_000)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withSum(1_200_000)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);
        var expectedProcessedOrder = proceededOrder(1_400_000);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 0, 0, 0, 0);
    }

    @Test
    void campaignHasNewShowsTest() {
        var activeOrderChanges = changesBuilder()
                .withNewShows(1003)
                .withNewSumSpent(1_100_000)
                .withOldSumSpent(1_000_000)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withSum(10_000_000)
                .withWalletCid(2)
                .build();

        var gotProcessedOrder = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = new ProceededActiveOrder(1, TEXT, 1003, 10,
                BigDecimal.valueOf(1_100_000, 6),
                0, 0);
        expectedProcessedOrder.setNewShows(true);

        assertState(gotProcessedOrder, expectedProcessedOrder, 0, 0, 0, 1, 0);
    }

    @Test
    void campaignHasDecreasedShowsTest() {
        var activeOrderChanges = changesBuilder()
                .withOldShows(1003)
                .withNewSumSpent(1_000_000)
                .withOldSumSpent(1_000_000)
                .withArchived(CampaignsArchived.Yes.getLiteral())
                .withSum(1_000_000)
                .withWalletCid(2)
                .build();

        var gotOrderProcessed = activeOrdersImportService.processOrder(activeOrderChanges, activeOrdersMetrics);

        var expectedProcessedOrder = new ProceededActiveOrder(1, TEXT, 1000, 10,
                BigDecimal.valueOf(1_000_000, 6),
                0, 0);

        assertState(gotOrderProcessed, expectedProcessedOrder, 0, 0, 0, 0, 0);
    }

    private void assertState(ProceededActiveOrder gotProcessedOrder, ProceededActiveOrder expectedProcessedOrder,
                             long rollbacked, long finished, long moneyEnd, long newShows, long unarchived) {
        assertThat(gotProcessedOrder, equalTo(expectedProcessedOrder));
        assertThat(activeOrdersMetrics.getRollbackedCamps(), equalTo(rollbacked));
        assertThat(activeOrdersMetrics.getFinishedCamps(), equalTo(finished));
        assertThat(activeOrdersMetrics.getMoneyEndCamps(), equalTo(moneyEnd));
        assertThat(activeOrdersMetrics.getNewShowsCamps(), equalTo(newShows));
        assertThat(activeOrdersMetrics.getUnarchCamps(), equalTo(unarchived));
    }

    private static ActiveOrderChanges.Builder changesBuilder() {
        return new ActiveOrderChanges.Builder()
                .withCid(1)
                .withType(TEXT)
                .withNewShows(1000)
                .withOldShows(1000)
                .withNewClicks(10)
                .withWalletCid(0);
    }

    private static ProceededActiveOrder proceededOrder(long sumSpent) {
        return new ProceededActiveOrder(1, TEXT, 1000, 10,
                BigDecimal.valueOf(sumSpent, 6), 0, 0);
    }

    private static ProceededActiveOrder proceededInternalFreeOrder(long units, long newSpentUnits) {
        return new ProceededActiveOrder(1, INTERNAL_FREE, 1000, 10,
                BigDecimal.valueOf(0, 6), units, newSpentUnits);
    }
}
