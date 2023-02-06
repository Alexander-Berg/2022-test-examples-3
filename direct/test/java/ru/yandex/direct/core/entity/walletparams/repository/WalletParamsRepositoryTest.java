package ru.yandex.direct.core.entity.walletparams.repository;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.AggregatingSumStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignsMulticurrencySumsRepository;
import ru.yandex.direct.core.entity.wallet.model.WalletParamsModel;
import ru.yandex.direct.core.entity.walletparams.container.WalletParams;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestWalletCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMulticurrencySums;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class WalletParamsRepositoryTest {

    private static final DefaultCompareStrategy WALLET_PARAMS_COMPARE_STRATEGY = allFields()
            .forFields(
                    newPath("totalChipsCost"),
                    newPath("totalSum")
            ).useDiffer(new BigDecimalDiffer());

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignsMulticurrencySumsRepository campaignsMulticurrencySumsRepository;

    @Autowired
    private WalletParamsRepository walletParamsRepository;

    @Autowired
    private TestWalletCampaignRepository testWalletCampaignRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private CampaignInfo walletInfo;
    private ClientInfo clientInfo;
    private long walletId;
    private int shard;

    private WalletParamsModel walletParams;

    @Before
    public void before() {
        walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        clientInfo = walletInfo.getClientInfo();
        walletId = walletInfo.getCampaignId();
        shard = walletInfo.getShard();

        walletParamsRepository.addWalletParams(shard, new WalletParams()
                .withWalletId(walletId)
                .withTotalBalanceTid(123L)
                .withTotalSum(BigDecimal.valueOf(42)));

        walletParams = walletParamsRepository.get(shard, singletonList(walletId)).get(0);
    }

    @Test
    public void get_ByNotExistId_EmptyList() {
        List<WalletParamsModel> actualList = walletParamsRepository.get(shard, singletonList(0L));
        assertThat(actualList, empty());
    }

    @Test
    public void get_WalletParamsById_Expected() {
        List<WalletParamsModel> actualList = walletParamsRepository.get(shard, singletonList(walletId));
        assertThat(actualList, hasSize(1));

        WalletParamsModel expected = new WalletParamsModel()
                .withId(walletId)
                .withTotalSum(walletParams.getTotalSum())
                .withTotalChipsCost(walletParams.getTotalChipsCost())
                .withTotalBalanceTid(walletParams.getTotalBalanceTid())
                .withAggregateMigrateStatus(AggregatingSumStatus.NO);

        assertThat(actualList.get(0), beanDiffer(expected).useCompareStrategy(WALLET_PARAMS_COMPARE_STRATEGY));
    }

    @Test
    public void getChipsCostForUpdate_WithoutCampaignUnderWallet_ZeroChipsCost() {
        DSLContext context = dslContextProvider.ppc(shard);
        BigDecimal actual = walletParamsRepository.getChipsCostForUpdate(context, walletParams.getId());

        assertThat(actual, Matchers.comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void getChipsCostForUpdate_WithCampaignUnderWallet_TenChipsCost() {
        BigDecimal chipsCost = BigDecimal.TEN;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCost);

        DSLContext context = dslContextProvider.ppc(shard);
        BigDecimal actual = walletParamsRepository.getChipsCostForUpdate(context, walletParams.getId());

        assertThat(actual, Matchers.comparesEqualTo(chipsCost));
    }

    @Test
    public void getChipsCostForUpdate_CampaignWithWallet_SumChipsCost() {
        BigDecimal walletChipsCost = BigDecimal.valueOf(3);
        campaignsMulticurrencySumsRepository.insertCampaignsMulticurrencySums(clientInfo.getShard(),
                defaultMulticurrencySums(walletInfo.getCampaignId()).withChipsCost(walletChipsCost));

        BigDecimal campaignChipsCost = BigDecimal.valueOf(2);
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, campaignChipsCost);

        DSLContext context = dslContextProvider.ppc(shard);
        BigDecimal actual = walletParamsRepository.getChipsCostForUpdate(context, walletParams.getId());

        assertThat(actual, Matchers.comparesEqualTo(campaignChipsCost.add(walletChipsCost)));
    }

    @Test
    public void getChipsCostForUpdate_WithCampaignsUnderWallet_SumOfCampaignsChipsCost() {
        BigDecimal chipsCost1 = BigDecimal.TEN;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCost1);

        BigDecimal chipsCost2 = BigDecimal.TEN;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCost2);

        DSLContext context = dslContextProvider.ppc(shard);
        BigDecimal actual = walletParamsRepository.getChipsCostForUpdate(context, walletParams.getId());

        assertThat(actual, Matchers.comparesEqualTo(chipsCost1.add(chipsCost2)));
    }

    @Test
    public void update_EmptyCampaignsUnderWallet_ZeroTotalChipsCost() {
        boolean updated = updateInTransaction(shard, walletParams);
        assertFalse(updated);

        BigDecimal actual = testWalletCampaignRepository.getTotalChipsCosts(shard, walletId);
        assertThat(actual, Matchers.comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    public void update_OneCampaignUnderWallet_TotalChipsCostEqualsToChipsCostInCampaign() {
        BigDecimal chipsCosts = BigDecimal.TEN;

        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCosts);

        boolean updated = updateInTransaction(shard, walletParams);
        assertTrue(updated);

        BigDecimal totalChipsCost = testWalletCampaignRepository.getTotalChipsCosts(shard, walletId);
        assertThat(totalChipsCost, Matchers.comparesEqualTo(chipsCosts));
    }

    @Test
    public void update_TwoCampaignUnderWallet_TotalChipsCostEqualsSumChipsCostInCampaigns() {
        BigDecimal chipsCost1 = BigDecimal.TEN;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCost1);

        BigDecimal chipsCost2 = BigDecimal.ONE;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCost2);

        boolean updated = updateInTransaction(shard, walletParams);
        assertTrue(updated);

        BigDecimal totalChipsCost = testWalletCampaignRepository.getTotalChipsCosts(shard, walletId);
        assertThat(totalChipsCost, Matchers.comparesEqualTo(chipsCost1.add(chipsCost2)));
    }

    @Test
    public void update_TotalChipsCostNotChanged_ResultNotUpdated() {
        BigDecimal chipsCost1 = BigDecimal.TEN;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, chipsCost1);

        updateInTransaction(shard, walletParams);

        boolean updated = updateInTransaction(shard, walletParams);
        assertFalse(updated);
    }

    @Test
    public void update_ChangeAggregateSumStatus_StatusUpdated() {
        walletParamsRepository.update(dslContextProvider.ppc(shard),
                singletonList(applyStatus(walletParams, AggregatingSumStatus.YES)));

        WalletParamsModel walletParams = walletParamsRepository.get(shard, singletonList(walletId)).get(0);
        assertThat(walletParams.getAggregateMigrateStatus(), is(AggregatingSumStatus.YES));
    }


    private AppliedChanges<WalletParamsModel> applyTotalChipsCost(WalletParamsModel walletParams,
                                                                  BigDecimal totalChipsCost) {
        ModelChanges<WalletParamsModel> changes = new ModelChanges<>(walletParams.getId(), WalletParamsModel.class);
        changes.process(totalChipsCost, WalletParamsModel.TOTAL_CHIPS_COST);
        return changes.applyTo(walletParams);
    }

    private AppliedChanges<WalletParamsModel> applyStatus(WalletParamsModel walletParams, AggregatingSumStatus status) {
        ModelChanges<WalletParamsModel> changes = new ModelChanges<>(walletParams.getId(), WalletParamsModel.class);
        changes.process(status, WalletParamsModel.AGGREGATE_MIGRATE_STATUS);
        return changes.applyTo(walletParams);
    }


    private boolean updateInTransaction(int shard, WalletParamsModel walletParams) {
        return dslContextProvider.ppc(shard).transactionResult(t -> {
            DSLContext context = t.dsl();

            BigDecimal chipsCost = walletParamsRepository.getChipsCostForUpdate(context, walletParams.getId());
            return walletParamsRepository.update(context,
                    singletonList(applyTotalChipsCost(walletParams, chipsCost)));
        });
    }

}
