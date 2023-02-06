package ru.yandex.direct.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;
import ru.yandex.direct.core.entity.campaign.container.WalletsWithCampaigns;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignForBlockedMoneyCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignForNotifyFinishedByDate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithTypeAndWalletId;
import ru.yandex.direct.core.entity.campaign.model.WalletCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.ImageCreativeBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignTypeOld;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CampaignRepositoryTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final int DB_SCALE = 6;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    private static BalanceInfo getBalanceInfoWithRandomSum() {
        return activeBalanceInfo(CurrencyCode.RUB)
                .withSum(RandomNumberUtils.nextPositiveBigDecimal().setScale(DB_SCALE, RoundingMode.HALF_DOWN))
                .withSumToPay(RandomNumberUtils.nextPositiveBigDecimal().setScale(DB_SCALE, RoundingMode.HALF_DOWN));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void testGetCampaignsUnderWalletsForBlockedMoneyCheck(CampaignType campaignType) {
        List<Long> userIds = new ArrayList<>();
        List<Long> walletIds = new ArrayList<>();

        CampaignInfo walletCampaign = steps.campaignSteps().createCampaign(
                TestCampaigns.activeWalletCampaign(null, null).withBalanceInfo(getBalanceInfoWithRandomSum()));
        int shard = walletCampaign.getShard();
        userIds.add(walletCampaign.getUid());
        walletIds.add(walletCampaign.getCampaignId());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                TestCampaigns.activeCampaignByCampaignType(campaignType, walletCampaign.getClientId(), walletCampaign.getUid())
                        .withAgencyUid(RandomNumberUtils.nextPositiveLong())
                        .withBalanceInfo(getBalanceInfoWithRandomSum()
                                .withWalletCid(walletCampaign.getCampaignId())),
                walletCampaign.getClientInfo()
        );

        long managerUid = RandomNumberUtils.nextPositiveLong();
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.MANAGER_UID, managerUid)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();

        List<CampaignForBlockedMoneyCheck> campaigns =
                campaignRepository.getCampaignsUnderWalletsForBlockedMoneyCheck(shard, userIds, walletIds);

        assertThat("Получили один результат", campaigns.size(), equalTo(1));

        CampaignForBlockedMoneyCheck expectedCampaignParam = new Campaign()
                .withId(campaignInfo.getCampaignId())
                .withWalletId(walletCampaign.getCampaignId())
                .withManagerUserId(managerUid)
                .withAgencyUserId(campaignInfo.getCampaign().getAgencyUid())
                .withUserId(walletCampaign.getUid())
                .withType(ru.yandex.direct.core.entity.campaign.model.CampaignType.valueOf(campaignType.name()))
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.YES)
                .withSum(campaignInfo.getCampaign().getBalanceInfo().getSum()
                        .add(walletCampaign.getCampaign().getBalanceInfo().getSum()))
                .withSumToPay(campaignInfo.getCampaign().getBalanceInfo().getSumToPay()
                        .add(walletCampaign.getCampaign().getBalanceInfo().getSumToPay()));
        assertThat(campaigns.get(0), beanDiffer(expectedCampaignParam));
    }

    @Test
    public void testGetWalletIdsByCampaingIds() {
        var campaignWithWalletId = TestCampaigns.activeWalletCampaign(null, null);
        campaignWithWalletId.getBalanceInfo().withWalletCid(10L);
        CampaignInfo campaign = steps.campaignSteps().createCampaign(campaignWithWalletId);

        var campaignWithWalletId2 = TestCampaigns.activeWalletCampaign(null, null);
        campaignWithWalletId2.getBalanceInfo().withWalletCid(20L);
        CampaignInfo campaign2 = steps.campaignSteps().createCampaign(campaignWithWalletId2);

        var campaignWithoutWalletId = TestCampaigns.activeWalletCampaign(null, null);
        CampaignInfo campaign3 = steps.campaignSteps().createCampaign(campaignWithoutWalletId);

        int shard = campaign.getShard();
        Map<Long, Long> res = campaignRepository.getWalletIdsByCampaingIds(shard,
                Arrays.asList(campaign.getCampaignId(), campaign2.getCampaignId(), campaign3.getCampaignId()));

        var expected = Map.of(campaign.getCampaignId(), 10L, campaign2.getCampaignId(), 20L,
                campaign3.getCampaignId(), 0L);
        softAssertions.assertThat(res).containsAllEntriesOf(expected);
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getCampaignsWithTypeAndWalletId_success(CampaignType campaignType) {
        CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(TestCampaigns.activeWalletCampaign(null, null));

        var campaign1 = activeCampaignByCampaignType(campaignType, null, null);
        campaign1.getBalanceInfo().withWalletCid(walletCampaign.getCampaignId());
        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaign(campaign1, walletCampaign.getClientInfo());

        CampaignInfo campaignInfo2 = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);

        var actual = campaignRepository.getCampaignsWithTypeAndWalletId(walletCampaign.getShard(),
                List.of(walletCampaign.getCampaignId(), campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()));


        CampaignWithTypeAndWalletId wallet = new Campaign()
                .withId(walletCampaign.getCampaignId())
                .withType(ru.yandex.direct.core.entity.campaign.model.CampaignType.WALLET)
                .withWalletId(0L);
        CampaignWithTypeAndWalletId campaignUnderWallet = new Campaign()
                .withId(campaignInfo1.getCampaignId())
                .withType(ru.yandex.direct.core.entity.campaign.model.CampaignType.valueOf(campaignType.name()))
                .withWalletId(walletCampaign.getCampaignId());
        CampaignWithTypeAndWalletId campaignWithoutWallet = new Campaign()
                .withId(campaignInfo2.getCampaignId())
                .withType(ru.yandex.direct.core.entity.campaign.model.CampaignType.valueOf(campaignType.name()))
                .withWalletId(0L);

        Assertions.assertThat(actual)
                .usingElementComparatorOnFields("id", "walletId", "type")
                .containsExactlyInAnyOrder(wallet,  campaignUnderWallet, campaignWithoutWallet);
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getWalletCampaigns_success(CampaignType campaignType) {
        CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(TestCampaigns.activeWalletCampaign(null, null));

        int shard = walletCampaign.getShard();
        Long walletCampaignId = walletCampaign.getCampaignId();
        ClientId clientId = walletCampaign.getClientId();

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign newCampaign =
                activeCampaignByCampaignType(campaignType, null, null);
        newCampaign.getBalanceInfo().withWalletCid(walletCampaignId);
        CampaignInfo campaign =
                steps.campaignSteps().createCampaign(
                        newCampaign,
                        walletCampaign.getClientInfo());

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singletonList(campaign.getCampaignId()));

        WalletsWithCampaigns walletCampaigns = campaignRepository.getWalletAllCampaigns(shard, clientId, campaigns);
        Assertions.assertThat(walletCampaigns.getAllWallets()).hasSize(1);

        WalletCampaign wallet = walletCampaigns.getAllWallets().iterator().next();
        Assertions.assertThat(wallet.getId()).isEqualTo(walletCampaignId);

        Assertions.assertThat(walletCampaigns.getCampaignsBoundTo(wallet)).hasSize(1);
        WalletCampaign actualCampaign = walletCampaigns.getCampaignsBoundTo(wallet).iterator().next();

        Assertions.assertThat(actualCampaign.getWalletId()).isEqualTo(walletCampaignId);
    }

    @Test
    public void getWalletCampaigns_campaignWithNoWallet() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        int shard = campaignInfo.getShard();
        ClientId clientId = campaignInfo.getClientId();
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singletonList(campaignInfo.getCampaignId()));

        WalletsWithCampaigns walletCampaigns = campaignRepository.getWalletAllCampaigns(shard, clientId, campaigns);

        Assertions.assertThat(walletCampaigns.getAllWallets()).isEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getFinishedCampaignsShouldNotAllowEmptyArgument(CampaignType campaignType) {
        final ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaign =
                TestCampaigns.activeCampaignByCampaignType(campaignType, null, null)
                        .withFinishTime(LocalDate.now().plusMonths(1));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        campaignRepository.getActiveCampaignsFinishedByDate(campaignInfo.getShard(), Collections.emptyList());
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void campaignsFinishedZeroDaysAgoShouldIncludeCampaignsFinishedYesterday(CampaignType campaignType) {
        final ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaign =
                TestCampaigns.activeCampaignByCampaignType(campaignType, null, null)
                        .withFinishTime(yesterday());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        final List<CampaignForNotifyFinishedByDate> campaigns =
                campaignRepository.getActiveCampaignsFinishedByDate(campaignInfo.getShard(), zeroDaysAgo());
        assertThat(campaigns, hasItem(hasProperty("id", is(campaignInfo.getCampaignId()))));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void campaignsWithZeroBalanceShouldBeIncluded(CampaignType campaignType) {
        final BalanceInfo balanceInfo = TestCampaigns.activeBalanceInfo(CurrencyCode.RUB);
        final ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaign =
                TestCampaigns.activeCampaignByCampaignType(campaignType, null, null)
                        .withFinishTime(yesterday())
                        .withBalanceInfo(balanceInfo.withSumSpent(balanceInfo.getSum()));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        final List<CampaignForNotifyFinishedByDate> campaigns =
                campaignRepository.getActiveCampaignsFinishedByDate(campaignInfo.getShard(), zeroDaysAgo());
        assertThat(campaigns, hasItem(hasProperty("id", is(campaignInfo.getCampaignId()))));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void campaignsWithZeroBalanceOnWalletShouldBeIncluded(CampaignType campaignType) {
        final BalanceInfo zeroBalanceInfo = TestCampaigns.activeBalanceInfo(CurrencyCode.RUB);
        zeroBalanceInfo.withSumSpent(zeroBalanceInfo.getSum());

        final CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeWalletCampaign(null, null).withBalanceInfo(zeroBalanceInfo));

        final ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaign = TestCampaigns
                .activeCampaignByCampaignType(campaignType, walletCampaign.getClientId(), walletCampaign.getUid())
                .withFinishTime(yesterday())
                .withBalanceInfo(zeroBalanceInfo);

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        final List<CampaignForNotifyFinishedByDate> campaigns =
                campaignRepository.getActiveCampaignsFinishedByDate(campaignInfo.getShard(), zeroDaysAgo());
        assertThat(campaigns, hasItem(hasProperty("id", is(campaignInfo.getCampaignId()))));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void campaignsWithPositiveBalanceOnWalletShouldBeIncluded(CampaignType campaignType) {
        final CampaignInfo walletCampaign =
                steps.campaignSteps().createCampaign(
                        TestCampaigns.activeWalletCampaign(null, null));

        final BalanceInfo zeroBalanceInfo = TestCampaigns.activeBalanceInfo(CurrencyCode.RUB)
                .withWalletCid(walletCampaign.getCampaignId());
        zeroBalanceInfo.withSumSpent(zeroBalanceInfo.getSum());

        final ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaign = TestCampaigns
                .activeCampaignByCampaignType(campaignType, walletCampaign.getClientId(), walletCampaign.getUid())
                .withFinishTime(yesterday())
                .withBalanceInfo(zeroBalanceInfo);

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, walletCampaign.getClientInfo());
        final List<CampaignForNotifyFinishedByDate> campaigns =
                campaignRepository.getActiveCampaignsFinishedByDate(campaignInfo.getShard(), zeroDaysAgo());
        assertThat(campaigns, hasItem(hasProperty("id", is(campaignInfo.getCampaignId()))));
        assertThat(campaigns, not(hasItem(hasProperty("id", is(walletCampaign.getCampaignId())))));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getArchivedCampaignIds_ArchivedCampaign(CampaignType campaignType) {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
        dslContextProvider.ppc(campaignInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();
        Set<Long> campIds = campaignRepository.getArchivedCampaigns(
                campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId()));
        Assert.assertThat(campIds, hasSize(1));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getArchivedCampaignIds_NotArchivedCampaign(CampaignType campaignType) {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
        Set<Long> campIds = campaignRepository.getArchivedCampaigns(
                campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId()));
        Assert.assertThat(campIds, hasSize(0));
    }

    @Test
    public void getCampaignsWithActiveBanners_textBanner_oneCampaignReturned() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        Set<Long> campaignIds = campaignRepository.getCampaignsWithActiveBanners(bannerInfo.getShard(),
                singletonList(bannerInfo.getCampaignId()));
        assertThat(campaignIds, equalTo(singleton(bannerInfo.getCampaignId())));
    }

    @Test
    public void getCampaignsWithActiveBanners_dynamicBanner_oneCampaignReturned() {
        DynamicBannerInfo bannerInfo = steps.bannerSteps().createActiveDynamicBanner();
        Set<Long> campaignIds = campaignRepository.getCampaignsWithActiveBanners(bannerInfo.getShard(),
                singletonList(bannerInfo.getCampaignId()));
        assertThat(campaignIds, equalTo(singleton(bannerInfo.getCampaignId())));
    }

    @Test
    public void getCampaignsWithActiveBanners_imageBanner_oneCampaignReturned() {
        ImageCreativeBannerInfo bannerInfo = steps.bannerSteps().createActiveImageCreativeBanner();
        Set<Long> campaignIds = campaignRepository.getCampaignsWithActiveBanners(bannerInfo.getShard(),
                singletonList(bannerInfo.getCampaignId()));
        assertThat(campaignIds, equalTo(singleton(bannerInfo.getCampaignId())));
    }

    @Test
    public void getCampaignsWithActiveBanners_nonActiveNonModeratedBanner_zeroCampaignsReturned() {
        AdGroupInfo adGroupInfo = new AdGroupInfo().withAdGroup(activeTextAdGroup());
        OldImageCreativeBanner banner = activeImageCreativeBanner(null, null, null)
                .withStatusActive(false)
                .withStatusPostModerate(OldBannerStatusPostModerate.NO);
        AbstractBannerInfo bannerInfo = steps.bannerSteps().createImageCreativeBanner(banner, adGroupInfo);
        int shard = bannerInfo.getShard();
        Set<Long> campaignIds = campaignRepository.getCampaignsWithActiveBanners(shard,
                singletonList(adGroupInfo.getCampaignId()));
        assertThat(campaignIds, empty());
    }

    @Test
    public void getCampaignsWithActiveBanners_nonShownBanner_zeroCampaignsReturned() {
        AdGroupInfo adGroupInfo = new AdGroupInfo().withAdGroup(activeTextAdGroup());
        OldImageCreativeBanner banner = activeImageCreativeBanner(null, null, null)
                .withStatusShow(false);
        AbstractBannerInfo bannerInfo = steps.bannerSteps().createImageCreativeBanner(banner, adGroupInfo);
        int shard = bannerInfo.getShard();
        Set<Long> campaignIds = campaignRepository.getCampaignsWithActiveBanners(shard,
                singletonList(adGroupInfo.getCampaignId()));
        assertThat(campaignIds, empty());
    }

    @Test
    public void campaignExists_nonExists_returnsFalse() {
        assertFalse(campaignRepository.campaignExists(1, -1));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void campaignExists_existsEmpty_returnsFalse(CampaignType campaignType) {
        CampaignInfo campaignInfo =
                steps.campaignSteps().createCampaign(emptyCampaignByCampaignType(campaignType, null, null));
        assertFalse(campaignRepository.campaignExists(campaignInfo.getShard(), campaignInfo.getCampaignId()));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void campaignExists_existsNotEmpty_returnsTrue(CampaignType campaignType) {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaignByCampaignType(campaignType);
        assertTrue(campaignRepository.campaignExists(campaignInfo.getShard(), campaignInfo.getCampaignId()));
    }

    @Test
    public void getSumFromCampaignsUnderWallet_NoCampaignUnderWallet_ZeroSum() {
        CampaignInfo walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        DSLContext context = dslContextProvider.ppc(walletInfo.getShard());

        BigDecimal sum = campaignRepository.getSumFromCampaignsUnderWallet(context, walletInfo.getCampaignId());
        assertThat(sum, comparesEqualTo(BigDecimal.ZERO));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getSumFromCampaignsUnderWallet_OneCampaignUnderWallet_EqualsSum(CampaignType campaignType) {
        CampaignInfo walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        ClientInfo clientInfo = walletInfo.getClientInfo();

        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaignUnderWalletByCampaignType(campaignType,
                clientInfo, walletInfo.getCampaignId(), BigDecimal.ZERO);
        BigDecimal sum1 = campaignInfo1.getCampaign().getBalanceInfo().getSum();
        DSLContext context = dslContextProvider.ppc(clientInfo.getShard());

        BigDecimal sum = campaignRepository.getSumFromCampaignsUnderWallet(context, walletInfo.getCampaignId());
        assertThat(sum, comparesEqualTo(sum1));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void getSumFromCampaignsUnderWallet_TwoCampaignUnderWallet_SumOfTwoCampaigns(CampaignType campaignType) {
        CampaignInfo walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        ClientInfo clientInfo = walletInfo.getClientInfo();

        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaignUnderWalletByCampaignType(campaignType,
                clientInfo, walletInfo.getCampaignId(), BigDecimal.ZERO);
        BigDecimal sum1 = campaignInfo1.getCampaign().getBalanceInfo().getSum();

        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaignUnderWalletByCampaignType(campaignType,
                clientInfo, walletInfo.getCampaignId(), BigDecimal.ZERO);
        BigDecimal sum2 = campaignInfo2.getCampaign().getBalanceInfo().getSum();

        DSLContext context = dslContextProvider.ppc(clientInfo.getShard());
        BigDecimal sum = campaignRepository.getSumFromCampaignsUnderWallet(context, walletInfo.getCampaignId());
        assertThat(sum, comparesEqualTo(sum1.add(sum2)));
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void setManagerForAllClientCampaigns_whenNotAgencyCampaign_success(CampaignType campaignType) {
        var managerClientInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER);
        Long managerUid = managerClientInfo.getUid();
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        ClientId clientId = clientInfo.getClientId();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo);
        Integer shard = clientInfo.getShard();
        Campaign startCampaign = campaignRepository.getCampaigns(shard, List.of(campaignInfo.getCampaignId())).get(0);
        assumeThat(startCampaign.getManagerUserId(), not(equalTo(managerUid)));
        assumeThat(startCampaign.getStatusBsSynced(), not(equalTo(StatusBsSynced.NO)));

        campaignRepository.setManagerForAllClientCampaigns(shard, clientId, managerUid);

        Campaign actualCampaign = campaignRepository.getCampaigns(shard, List.of(campaignInfo.getCampaignId())).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualCampaign.getManagerUserId()).isEqualTo(managerUid);
            soft.assertThat(actualCampaign.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    @Parameters(method = "parametrizedCampaignTypes")
    @TestCaseName("{0}")
    public void setManagerForAllClientCampaigns_whenAgencyCampaign_doNothing(CampaignType campaignType) {
        var managerClientInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER);
        Long managerUid = managerClientInfo.getUid();
        ClientInfo agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        ClientId clientId = clientInfo.getClientId();
        Integer shard = clientInfo.getShard();
        Long clientUid = clientInfo.getUid();
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaign =
                newCampaignByCampaignTypeOld(campaignType, clientId, clientUid)
                        .withAgencyId(agencyClientInfo.getClientId().asLong())
                        .withAgencyUid(agencyClientInfo.getUid())
                        .withStatusBsSynced(StatusBsSynced.YES);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        Campaign startCampaign = campaignRepository.getCampaigns(shard, List.of(campaignInfo.getCampaignId())).get(0);
        assumeThat(startCampaign.getManagerUserId(), nullValue());
        assumeThat(startCampaign.getAgencyUserId(), notNullValue());
        assumeThat(startCampaign.getStatusBsSynced(), equalTo(StatusBsSynced.YES));

        campaignRepository.setManagerForAllClientCampaigns(shard, clientId, managerUid);

        Campaign actualCampaign = campaignRepository.getCampaigns(shard, List.of(campaignInfo.getCampaignId())).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualCampaign.getManagerUserId()).isNull();
            soft.assertThat(actualCampaign.getStatusBsSynced()).isEqualTo(StatusBsSynced.YES);
        });
    }

    @Test
    public void getBrandSurveyIdsForCampaignsTest() {
        String brandSurveyId0 = "id0";
        String brandSurveyId1 = "id1";
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaign0 = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo,
                brandSurveyId0);
        CampaignInfo campaign1 = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo,
                brandSurveyId1);
        int shard = clientInfo.getShard();

        Map<Long, String> campaignIdToBrandSurveyId = campaignRepository.getBrandSurveyIdsForCampaigns(shard,
                asList(campaign0.getCampaignId(), campaign1.getCampaignId()));

        assertThat(campaignIdToBrandSurveyId.keySet(), hasSize(2));

        assertThat(campaignIdToBrandSurveyId.get(campaign0.getCampaignId()), equalTo(brandSurveyId0));

        assertThat(campaignIdToBrandSurveyId.get(campaign1.getCampaignId()), equalTo(brandSurveyId1));
    }

    @Test
    public void deleteBrandSurveyIdTest() {
        String brandSurveyId0 = "id0";
        String brandSurveyId1 = "id1";
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaign0 = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, brandSurveyId0);
        var campaign1 = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, brandSurveyId1);
        int shard = clientInfo.getShard();

        campaignRepository.deleteBrandSurveyId(shard, List.of(campaign0.getCampaignId()));

        var cids = List.of(campaign0.getCampaignId(), campaign1.getCampaignId());
        var brandSurveyIdsForCampaigns = campaignRepository.getBrandSurveyIdsForCampaigns(shard, cids);

        assertThat(brandSurveyIdsForCampaigns.keySet(), hasSize(2));
        assertNull(brandSurveyIdsForCampaigns.get(campaign0.getCampaignId()), null);
        assertEquals(brandSurveyIdsForCampaigns.get(campaign1.getCampaignId()), brandSurveyId1);
    }

    @Test
    public void getCampaignIdsByFeedId() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaignInfo1 = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        var campaignInfo2 = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);
        var campaignInfo3 = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        var feedInfo1 = steps.feedSteps().createDefaultFeed(clientInfo);
        var feedInfo2 = steps.feedSteps().createDefaultFeed(clientInfo);
        var feedInfo3 = steps.feedSteps().createDefaultFeed(clientInfo);

        var feedId1 = feedInfo1.getFeedId();
        var feedId2 = feedInfo2.getFeedId();
        var feedId3 = feedInfo3.getFeedId();

        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo1, feedId1);
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo1, feedId2);
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo2, feedId1);
        steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo3, feedId3);

        var feedIds = List.of(feedId1, feedId2);
        int shard = clientInfo.getShard();
        Map<Long, List<Long>> actual = campaignRepository.getCampaignIdsByFeedId(shard, feedIds);
        Map<Long, List<Long>> expected = Map.of(
                feedId1, List.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()),
                feedId2, List.of(campaignInfo1.getCampaignId())
        );
        Assert.assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void testToGetCampaignIdsWithStartDateAndCampaignType() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        int shard = clientInfo.getShard();
        var campaignFirst = steps.campaignSteps().createActiveCampaignByCampaignType(CampaignType.CPM_YNDX_FRONTPAGE, clientInfo);
        var campaignSecond = steps.campaignSteps().createActiveCampaignByCampaignType(CampaignType.CPM_YNDX_FRONTPAGE, clientInfo);
        var campaignThird = steps.campaignSteps().createActiveCampaignByCampaignType(CampaignType.CPM_YNDX_FRONTPAGE, clientInfo);
        campaignRepository.updateStatusBsSynced(shard,
                List.of(campaignFirst.getCampaignId(),
                        campaignSecond.getCampaignId()),
                StatusBsSynced.YES);
        campaignRepository.updateStatusBsSynced(shard,
                List.of(campaignThird.getCampaignId()),
                StatusBsSynced.NO);
        var ids = campaignRepository.getCampaignIdsForResendToBSToAvoidGenocide(shard, LocalDate.now());
        var campaigns = campaignRepository.getCampaigns(shard, ids);
        assertEquals(campaigns.stream().filter(campaign ->
                Objects.equals(campaign.getStartTime(), LocalDate.now())
                        && campaign.getType()  == CampaignType.CPM_YNDX_FRONTPAGE
                        && campaign.getStatusBsSynced() == StatusBsSynced.YES).count(), campaigns.size());
    }

    private LocalDate yesterday() {
        return LocalDate.now().minusDays(1);
    }

    private List<Integer> zeroDaysAgo() {
        return Collections.singletonList(0);
    }

    private static Object[] parametrizedCampaignTypes() {
        return new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.DYNAMIC},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
        };
    }
}
