package ru.yandex.direct.jobs.balanceaggrmigration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportSpecialsRepository;
import ru.yandex.direct.core.entity.campaign.model.AggregatingSumStatus;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.wallet.model.WalletParamsModel;
import ru.yandex.direct.core.entity.walletparams.container.WalletParams;
import ru.yandex.direct.core.entity.walletparams.repository.WalletParamsRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.yesterdayRecordWithoutStat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@JobsTest
@ExtendWith(SpringExtension.class)
class BalanceAggregateMigrationServiceMigrateTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BalanceAggregateMigrationService migrationService;

    @Autowired
    private BsExportSpecialsRepository bsExportSpecialsRepository;

    @Autowired
    private BsExportQueueRepository queueRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private WalletParamsRepository walletParamsRepository;

    private ClientInfo clientInfo;
    private int shard;

    private Campaign wallet;
    private WalletParamsModel walletParams;
    private Campaign campaign1;
    private Campaign campaign2;

    private static final BigDecimal CAMPAIGN_1_CHIPS_COST = BigDecimal.valueOf(15);
    private static final BigDecimal CAMPAIGN_2_CHIPS_COST = BigDecimal.valueOf(20);

    @BeforeEach
    void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        CampaignInfo walletInfo = steps.campaignSteps()
                .createCampaign(activeWalletCampaign(clientInfo.getClientId(), clientInfo.getUid()));
        long walletId = walletInfo.getCampaignId();

        wallet = campaignRepository.getCampaigns(shard, singleton(walletId)).get(0);

        CampaignInfo campaign1Info = steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, walletId, CAMPAIGN_1_CHIPS_COST);
        long campaign1Id = campaign1Info.getCampaignId();

        CampaignInfo campaign2Info = steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, walletId, CAMPAIGN_2_CHIPS_COST);
        long campaign2Id = campaign2Info.getCampaignId();

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, asList(campaign1Id, campaign2Id));
        campaign1 = campaigns.get(0);
        campaign2 = campaigns.get(1);


        walletParamsRepository.addWalletParams(shard, new WalletParams()
                .withTotalBalanceTid(0L)
                .withWalletId(walletId)
                .withTotalSum(wallet.getSum().add(campaign1.getSum()).add(campaign2.getSum()))
        );
        walletParams = walletParamsRepository.get(shard, singletonList(walletId)).get(0);
    }

    @Test
    void migrateWalletInTransaction_WalletSumUpdated() {
        BigDecimal expectedWalletSum = walletParams.getTotalSum();
        BigDecimal expectedWalletSumBalance = wallet.getSum();

        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(wallet.getId()));
        Campaign actualWallet = campaigns.get(0);

        assertThat(actualWallet.getSum(), comparesEqualTo(expectedWalletSum));
        assertThat(actualWallet.getSumBalance(), comparesEqualTo(expectedWalletSumBalance));
    }

    @Test
    void migrateWalletInTransaction_CampaignsSumsUpdated() {
        BigDecimal expectedCampaign1SumBalance = campaign1.getSum();
        BigDecimal expectedCampaign2SumBalance = campaign2.getSum();

        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<Campaign> campaigns =
                campaignRepository.getCampaigns(shard, asList(campaign1.getId(), campaign2.getId()));

        assertThat(campaigns.get(0).getSumBalance(), comparesEqualTo(expectedCampaign1SumBalance));
        assertThat(campaigns.get(0).getSum(), comparesEqualTo(BigDecimal.ZERO));

        assertThat(campaigns.get(1).getSumBalance(), comparesEqualTo(expectedCampaign2SumBalance));
        assertThat(campaigns.get(1).getSum(), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    void migrateWalletInTransaction_WalletParamsUpdated() {
        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        WalletParamsModel expectedWalletParams = new WalletParamsModel()
                .withAggregateMigrateStatus(AggregatingSumStatus.YES)
                .withTotalChipsCost(CAMPAIGN_1_CHIPS_COST.add(CAMPAIGN_2_CHIPS_COST));

        WalletParamsModel actualWalletParams = walletParamsRepository.get(shard, singletonList(wallet.getId())).get(0);
        assertThat(actualWalletParams, beanDiffer(expectedWalletParams).useCompareStrategy(onlyExpectedFields()
                .forFields(newPath(WalletParamsModel.TOTAL_CHIPS_COST.name())).useDiffer(new BigDecimalDiffer())));
    }

    @Test
    void migrateWalletInTransaction_bsExportSpecial_ContainsWalletAndCampaignIds() {
        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<BsExportSpecials> bsExportSpecials = bsExportSpecialsRepository.getByCampaignIds(shard,
                asList(wallet.getId(), campaign1.getId(), campaign2.getId()));

        assertThat(bsExportSpecials, containsInAnyOrder(
                bsExportCampsOnly(wallet.getId()),
                bsExportCampsOnly(campaign1.getId()),
                bsExportCampsOnly(campaign2.getId())
        ));
    }

    // если у кампании все зачисления были потрачены в фишках, такую не нужно перепосылать в БК,
    // и поэтому не нужно класть в спец очередь
    @Test
    void migrateWalletInTransaction_bsExportSpecial_DoesntContainCampsWithChipsOnly() {
        var chipsCost = Map.of(campaign1.getId(), campaign1.getSum().setScale(3, RoundingMode.DOWN));
        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), chipsCost
        );

        List<BsExportSpecials> bsExportSpecials = bsExportSpecialsRepository.getByCampaignIds(shard,
                asList(wallet.getId(), campaign1.getId(), campaign2.getId()));

        assertThat(bsExportSpecials, containsInAnyOrder(
                bsExportCampsOnly(wallet.getId()),
                bsExportCampsOnly(campaign2.getId())
        ));
    }

    // проверяем, что кампания кладётся в очередь, если у неё не все зачисления были потрачены в фишках
    @Test
    void migrateWalletInTransaction_bsExportSpecial_ContainsCampsWithSomeChips() {
        var chipsCost = Map.of(campaign1.getId(), campaign1.getSum().subtract(BigDecimal.ONE));
        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), chipsCost
        );

        List<BsExportSpecials> bsExportSpecials = bsExportSpecialsRepository.getByCampaignIds(shard,
                asList(wallet.getId(), campaign1.getId(), campaign2.getId()));

        assertThat(bsExportSpecials, containsInAnyOrder(
                bsExportCampsOnly(wallet.getId()),
                bsExportCampsOnly(campaign1.getId()),
                bsExportCampsOnly(campaign2.getId())
        ));
    }

    @Test
    void migrateWalletInTransaction_bsExportSpecial_NotContainsCampaignsWithNotChangedSum() {
        campaignRepository.updateCampaigns(shard, singletonList(applyChangesSum(campaign1, BigDecimal.ZERO)));

        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<BsExportSpecials> bsExportSpecials = bsExportSpecialsRepository.getByCampaignIds(shard,
                asList(wallet.getId(), campaign1.getId(), campaign2.getId()));

        assertThat(bsExportSpecials, containsInAnyOrder(
                bsExportCampsOnly(wallet.getId()),
                bsExportCampsOnly(campaign2.getId())
        ));
    }

    @Test
    void migrateWalletInTransaction_bsExportQueue_queueTimeResetWhenChangedSum() {
        BsExportQueueInfo queueRecord1 = yesterdayRecordWithoutStat(campaign1.getId());
        queueRepository.insertRecord(dslContextProvider.ppc(shard), queueRecord1);

        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(shard, campaign1.getId());
        assertThat(actual.getQueueTime(), is(not(queueRecord1.getQueueTime())));
    }

    @Test
    void migrateWalletInTransaction_bsExportQueue_queueTimeNotResetWhenSumNotChanged() {
        BsExportQueueInfo queueRecord1 = yesterdayRecordWithoutStat(campaign1.getId());
        queueRepository.insertRecord(dslContextProvider.ppc(shard), queueRecord1);
        campaignRepository.updateCampaigns(shard, singletonList(applyChangesSum(campaign1, BigDecimal.ZERO)));

        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        BsExportQueueInfo actual = queueRepository.getBsExportQueueInfo(shard, campaign1.getId());
        assertThat(actual.getQueueTime(), is(queueRecord1.getQueueTime()));
    }

    @Test
    void migrateWalletInTransaction_LastChangeNotChanged() {
        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<Campaign> campaigns =
                campaignRepository.getCampaigns(shard, asList(wallet.getId(), campaign1.getId(), campaign2.getId()));
        assumeThat(campaigns, hasSize(3));

        DefaultCompareStrategy lastChangeFieldStrategy =
                onlyFields(newPath(Campaign.LAST_CHANGE.name()));

        assertThat(campaigns.get(0), beanDiffer(wallet).useCompareStrategy(lastChangeFieldStrategy));
        assertThat(campaigns.get(1), beanDiffer(campaign1).useCompareStrategy(lastChangeFieldStrategy));
        assertThat(campaigns.get(2), beanDiffer(campaign2).useCompareStrategy(lastChangeFieldStrategy));
    }

    @Test
    void migrateWalletInTransaction_ResetStatusBsSynced() {
        migrationService.migrateWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<Campaign> campaigns =
                campaignRepository.getCampaigns(shard, asList(wallet.getId(), campaign1.getId(), campaign2.getId()));
        assumeThat(campaigns, hasSize(3));

        assertThat(campaigns.get(0).getStatusBsSynced(), is(StatusBsSynced.NO));
        assertThat(campaigns.get(1).getStatusBsSynced(), is(StatusBsSynced.NO));
        assertThat(campaigns.get(2).getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    private BsExportSpecials bsExportCampsOnly(Long campaignId) {
        return new BsExportSpecials()
                .withCampaignId(campaignId)
                .withType(QueueType.CAMPS_ONLY);
    }

    private AppliedChanges<Campaign> applyChangesSum(Campaign campaign, BigDecimal sum) {
        ModelChanges<Campaign> changes = new ModelChanges<>(campaign.getId(), Campaign.class);
        changes.process(sum, Campaign.SUM);
        return changes.applyTo(campaign);
    }
}
