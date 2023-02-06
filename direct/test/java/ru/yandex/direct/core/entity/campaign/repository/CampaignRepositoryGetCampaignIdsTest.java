package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmDealsCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileAppCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignTypeOld;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newMcbCampaign;
import static ru.yandex.direct.dbutil.SqlUtils.ID_NOT_SET;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignRepositoryGetCampaignIdsTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static class CampaignIdsGetterParams {
        private int shard;
        private DSLContext context;
        private long lastProcessedCampaignId;
        private int limit;

        CampaignIdsGetterParams(int shard, long lastProcessedCampaignId, int limit) {
            this.shard = shard;
            this.lastProcessedCampaignId = lastProcessedCampaignId;
            this.limit = limit;
        }

        CampaignIdsGetterParams(DSLContext context, long lastProcessedCampaignId, int limit) {
            this.context = context;
            this.lastProcessedCampaignId = lastProcessedCampaignId;
            this.limit = limit;
        }

        public int getShard() {
            return shard;
        }

        public DSLContext getContext() {
            return context;
        }

        public long getLastProcessedCampaignId() {
            return lastProcessedCampaignId;
        }

        public int getLimit() {
            return limit;
        }
    }

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository campaignRepository0;

    @Autowired
    private Steps steps;

    private final SoftAssertions softly = new SoftAssertions();
    private ClientInfo clientInfo;
    private ClientId clientId;
    private Long uid;
    private int shard;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void createClientInShard() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
        shard = clientInfo.getShard();
    }

    @Test
    public void fullExportSmokeTest() {
        smokeTest(p -> campaignRepository.getCampaignIdsForFullExport(p.getShard(), p.getLastProcessedCampaignId(),
                p.getLimit()), false);
    }

    @Test
    public void statusRecountSmokeTest() {
        smokeTest(p -> campaignRepository.getCampaignIdsForStatusRecalculation(p.getShard(),
                p.getLastProcessedCampaignId(), p.getLimit()), true);
    }

    private void smokeTest(Function<CampaignIdsGetterParams, List<Long>> campaignIdsGetter,
                           boolean getterReturnsNoWalletCampaigns) {
        // эти две создаем заранее, потому что нужны их cid'ы
        Campaign activeWalletCampaign = steps.campaignSteps()
                .createCampaign(activeWalletCampaign(clientId, uid))
                .getCampaign();
        Campaign walletWithoutOrderId = steps.campaignSteps()
                .createCampaign(activeWalletCampaign(clientId, uid).withOrderId(ID_NOT_SET))
                .getCampaign();

        BalanceInfo balanceInfo1 =
                emptyBalanceInfo(activeWalletCampaign.getBalanceInfo().getCurrency()).withWalletCid(activeWalletCampaign.getId());
        Campaign activeCampaignUnderActiveWallet =
                activeCampaignByCampaignType(campaignType, clientId, uid).withBalanceInfo(balanceInfo1);

        BalanceInfo balanceInfo2 =
                emptyBalanceInfo(walletWithoutOrderId.getBalanceInfo().getCurrency()).withWalletCid(walletWithoutOrderId.getId());
        Campaign campaignUnderWalletWithoutOrderId =
                newCampaignByCampaignTypeOld(campaignType, clientId, uid).withBalanceInfo(balanceInfo2);

        // остальные создаем сразу пачкой
        Campaign activeTextCampaign = activeTextCampaign(clientId, uid);
        Campaign activeMobileAppCampaign = activeMobileAppCampaign(clientId, uid);
        Campaign activeDynamicCampaign = activeDynamicCampaign(clientId, uid);
        Campaign activePerformanceCampaign = activePerformanceCampaign(clientId, uid);
        Campaign activeCpmBannerCampaign = activeCpmBannerCampaign(clientId, uid);
        Campaign activeCpmDealsCampaign = activeCpmDealsCampaign(clientId, uid);
        Campaign mcbCampaign = newMcbCampaign(clientId, uid);
        Campaign emptyCampaign = emptyCampaignByCampaignType(campaignType, clientId, uid);

        campaignRepository0.addCampaigns(shard, clientId, asList(activeCpmBannerCampaign, activeCpmDealsCampaign,
                activeTextCampaign, activeMobileAppCampaign, activeDynamicCampaign, activePerformanceCampaign,
                campaignUnderWalletWithoutOrderId, activeCampaignUnderActiveWallet, emptyCampaign,
                mcbCampaign));

        var idList = campaignIdsGetter.apply(new CampaignIdsGetterParams(shard, Long.MAX_VALUE, Integer.MAX_VALUE));

        softly.assertThat(idList).isSortedAccordingTo(Collections.reverseOrder());

        softly.assertThat(idList).contains(activeWalletCampaign.getId(), walletWithoutOrderId.getId(),
                activeTextCampaign.getId(), activeCampaignUnderActiveWallet.getId(),
                activeMobileAppCampaign.getId(), activeDynamicCampaign.getId(), activePerformanceCampaign.getId(),
                activeCpmBannerCampaign.getId(), activeCpmDealsCampaign.getId());

        softly.assertThat(idList).doesNotContain(emptyCampaign.getId());
        if (getterReturnsNoWalletCampaigns) {
            softly.assertThat(idList).contains(mcbCampaign.getId(), campaignUnderWalletWithoutOrderId.getId());
        } else {
            softly.assertThat(idList).doesNotContain(mcbCampaign.getId(),
                    campaignUnderWalletWithoutOrderId.getId());
        }

        softly.assertAll();
    }

    @Test
    public void fullExportLimitTest() {
        limitTest(p -> campaignRepository.getCampaignIdsForFullExport(p.getContext(), p.getLastProcessedCampaignId(),
                p.getLimit()));
    }

    @Test
    public void statusRecountLimitTest() {
        limitTest(p -> campaignRepository.getCampaignIdsForStatusRecalculation(p.getContext(),
                p.getLastProcessedCampaignId(), p.getLimit()));
    }

    public void limitTest(Function<CampaignIdsGetterParams, List<Long>> campaignIdsGetter) {
        int limit = 3;
        Campaign campaign1 = activeCampaignByCampaignType(campaignType, clientId, uid);
        Campaign campaign2 = activeCampaignByCampaignType(campaignType, clientId, uid);
        Campaign campaign3 = activeCampaignByCampaignType(campaignType, clientId, uid);
        Campaign campaign4 = activeCampaignByCampaignType(campaignType, clientId, uid);

        steps.campaignSteps().runWithEmptyCampaignsTables(shard, dsl -> {
            campaignRepository0.addCampaigns(dsl, clientId, asList(campaign1, campaign2, campaign3, campaign4));

            var idList = campaignIdsGetter.apply(new CampaignIdsGetterParams(dsl, Long.MAX_VALUE, limit));
            softly.assertThat(idList).hasSize(limit);
            softly.assertThat(idList).containsOnly(campaign2.getId(), campaign3.getId(), campaign4.getId());
            softly.assertThat(idList).doesNotContain(campaign1.getId());
            softly.assertAll();
        });
    }

    @Test
    public void fullExportDoesNotContainLastProcessedCampaignIdTest() {
        doesNotContainsLastProcessedCampaignIdTest(p -> campaignRepository.getCampaignIdsForFullExport(p.getShard(),
                p.getLastProcessedCampaignId(), p.getLimit()));
    }

    @Test
    public void statusRecountDoesNotContainLastProcessedCampaignIdTest() {
        doesNotContainsLastProcessedCampaignIdTest(p -> campaignRepository.getCampaignIdsForStatusRecalculation(
                p.getShard(), p.getLastProcessedCampaignId(), p.getLimit()));
    }

    public void doesNotContainsLastProcessedCampaignIdTest(
            Function<CampaignIdsGetterParams, List<Long>> campaignIdsGetter) {
        Long campaignId = steps.campaignSteps()
                .createActiveCampaignByCampaignType(campaignType, clientInfo)
                .getCampaignId();

        var idList = campaignIdsGetter.apply(new CampaignIdsGetterParams(shard, campaignId, Integer.MAX_VALUE));
        softly.assertThat(idList).doesNotContain(campaignId);
        softly.assertAll();
    }

    @Test
    public void fullExportContainsPreviousToLastProcessedCampaignIdTest() {
        containsPreviousToLastProcessedCampaignIdTest(p -> campaignRepository.getCampaignIdsForFullExport(p.getShard(),
                p.getLastProcessedCampaignId(), p.getLimit()));
    }

    @Test
    public void statusRecountContainsPreviousToLastProcessedCampaignIdTest() {
        containsPreviousToLastProcessedCampaignIdTest(p -> campaignRepository.getCampaignIdsForStatusRecalculation(
                p.getShard(), p.getLastProcessedCampaignId(), p.getLimit()));
    }

    public void containsPreviousToLastProcessedCampaignIdTest(
            Function<CampaignIdsGetterParams, List<Long>> campaignIdsGetter) {
        Long campaignId = steps.campaignSteps()
                .createActiveCampaignByCampaignType(campaignType, clientInfo)
                .getCampaignId();

        var idList = campaignIdsGetter.apply(new CampaignIdsGetterParams(shard, campaignId + 1, Integer.MAX_VALUE));
        softly.assertThat(idList).contains(campaignId);
        softly.assertAll();
    }
}
