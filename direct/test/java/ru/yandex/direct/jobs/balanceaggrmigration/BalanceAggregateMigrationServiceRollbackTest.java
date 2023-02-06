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
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
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
import ru.yandex.direct.currency.CurrencyCode;
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
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@JobsTest
@ExtendWith(SpringExtension.class)
class BalanceAggregateMigrationServiceRollbackTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BalanceAggregateMigrationService migrationService;

    @Autowired
    private BsExportSpecialsRepository bsExportSpecialsRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private WalletParamsRepository walletParamsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo clientInfo;
    private int shard;

    private Campaign wallet;
    private WalletParamsModel walletParams;
    private Campaign campaign1;
    private Campaign campaign2;

    @BeforeEach
    void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        BigDecimal walletSum = BigDecimal.valueOf(35);
        CampaignInfo walletInfo = steps.campaignSteps()
                .createCampaign(activeWalletCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withBalanceInfo(activeBalanceInfo(CurrencyCode.RUB)
                                .withSum(walletSum)
                                .withSumBalance(BigDecimal.valueOf(20))));
        long walletId = walletInfo.getCampaignId();

        wallet = campaignRepository.getCampaigns(shard, singleton(walletId)).get(0);

        CampaignInfo campaign1Info = steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, activeBalanceInfo(CurrencyCode.RUB)
                        .withSum(BigDecimal.ZERO)
                        .withSumBalance(BigDecimal.valueOf(7)));
        long campaign1Id = campaign1Info.getCampaignId();

        CampaignInfo campaign2Info = steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, activeBalanceInfo(CurrencyCode.RUB)
                        .withSum(BigDecimal.ZERO)
                        .withSumBalance(BigDecimal.valueOf(8)));

        long campaign2Id = campaign2Info.getCampaignId();

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, asList(campaign1Id, campaign2Id));
        campaign1 = campaigns.get(0);
        campaign2 = campaigns.get(1);


        walletParamsRepository.addWalletParams(shard, new WalletParams()
                .withTotalBalanceTid(0L)
                .withWalletId(walletId)
                .withTotalSum(walletSum)
        );
        walletParams = walletParamsRepository.get(shard, singletonList(walletId)).get(0);
        setWalletStatusMigrated();
    }

    private void setWalletStatusMigrated() {
        ModelChanges<WalletParamsModel> changes = new ModelChanges<>(walletParams.getId(), WalletParamsModel.class);
        changes.process(AggregatingSumStatus.YES, WalletParamsModel.AGGREGATE_MIGRATE_STATUS);

        walletParamsRepository.update(dslContextProvider.ppc(shard), singleton(changes.applyTo(walletParams)));
    }

    private void setWalletTotalChipsCost(int value) {
        ModelChanges<WalletParamsModel> changes = new ModelChanges<>(walletParams.getId(), WalletParamsModel.class);
        changes.process(BigDecimal.valueOf(value), WalletParamsModel.TOTAL_CHIPS_COST);

        walletParamsRepository.update(dslContextProvider.ppc(shard), singleton(changes.applyTo(walletParams)));
    }

    @Test
    void rollbackWalletInTransaction_WalletSumUpdated() {
        BigDecimal expectedWalletSum = wallet.getSumBalance();
        BigDecimal expectedWalletSumBalance = BigDecimal.ZERO;

        migrationService.rollbackWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(wallet.getId()));
        Campaign actualWallet = campaigns.get(0);

        assertThat(actualWallet.getSum(), comparesEqualTo(expectedWalletSum));
        assertThat(actualWallet.getSumBalance(), comparesEqualTo(expectedWalletSumBalance));
    }

    @Test
    void rollbackWalletInTransaction_CampaignsSumsUpdated() {
        BigDecimal expectedCampaign1Sum = campaign1.getSumBalance();
        BigDecimal expectedCampaign2Sum = campaign2.getSumBalance();

        migrationService.rollbackWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        List<Campaign> campaigns =
                campaignRepository.getCampaigns(shard, asList(campaign1.getId(), campaign2.getId()));

        assertThat(campaigns.get(0).getSum(), comparesEqualTo(expectedCampaign1Sum));
        assertThat(campaigns.get(0).getSumBalance(), comparesEqualTo(BigDecimal.ZERO));

        assertThat(campaigns.get(1).getSum(), comparesEqualTo(expectedCampaign2Sum));
        assertThat(campaigns.get(1).getSumBalance(), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    void rollbackWalletInTransaction_WalletParamsUpdated() {
        setWalletTotalChipsCost(13);
        migrationService.rollbackWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), emptyMap()
        );

        WalletParamsModel expectedWalletParams = new WalletParamsModel()
                .withAggregateMigrateStatus(AggregatingSumStatus.NO)
                .withTotalChipsCost(BigDecimal.ZERO);

        WalletParamsModel actualWalletParams = walletParamsRepository.get(shard, singletonList(wallet.getId())).get(0);
        assertThat(actualWalletParams, beanDiffer(expectedWalletParams).useCompareStrategy(onlyExpectedFields()
                .forFields(newPath(WalletParamsModel.TOTAL_CHIPS_COST.name())).useDiffer(new BigDecimalDiffer())));
    }


    @Test
    void rollbackWalletInTransaction_bsExportSpecial_ContainsWalletAndCampaignIds() {
        migrationService.rollbackWalletInTransaction(
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

    @Test
    public void rollbackWalletInTransaction_bsExportSpecial_DoesntContainCampsWithChipsOnly() {
        var chipsCost = Map.of(campaign1.getId(), campaign1.getSumBalance().setScale(3, RoundingMode.DOWN));
        migrationService.rollbackWalletInTransaction(
                shard, wallet, walletParams, asList(campaign1, campaign2), chipsCost
        );

        List<BsExportSpecials> bsExportSpecials = bsExportSpecialsRepository.getByCampaignIds(shard,
                asList(wallet.getId(), campaign1.getId(), campaign2.getId()));

        assertThat(bsExportSpecials, containsInAnyOrder(
                bsExportCampsOnly(wallet.getId()),
                bsExportCampsOnly(campaign2.getId())
        ));
    }


    @Test
    void rollbackWalletInTransaction_bsExportSpecial_NotContainsCampaignsWithNotChangedSum() {
        campaignRepository.updateCampaigns(shard, singletonList(applyChangesSumBalance(campaign1, BigDecimal.ZERO)));

        migrationService.rollbackWalletInTransaction(
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
    void rollbackWalletInTransaction_LastChangeNotChanged() {
        migrationService.rollbackWalletInTransaction(
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
    void rollbackWalletInTransaction_ResetStatusBsSynced() {
        migrationService.rollbackWalletInTransaction(
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

    private AppliedChanges<Campaign> applyChangesSumBalance(Campaign campaign, BigDecimal sum) {
        ModelChanges<Campaign> changes = new ModelChanges<>(campaign.getId(), Campaign.class);
        changes.process(sum, Campaign.SUM_BALANCE);
        return changes.applyTo(campaign);
    }

}
