package ru.yandex.direct.grid.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.BigDecimalComparator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStatusBsSynced;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@GridCoreTest
@RunWith(SpringRunner.class)
public class GridCampaignRepositoryDailyTest {

    @Autowired
    private Steps steps;

    @Autowired
    private GridCampaignRepository gridCampaignRepository;

    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void checkGetMasterCampaign() {
        CampaignInfo masterCampaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        CampaignInfo subCampaignInfo = steps.campaignSteps()
                .createActiveSubCampaign(clientInfo, masterCampaignInfo.getCampaign().getId());

        List<GdiCampaign> gridCampaigns = getAllCampaigns();

        GdiCampaign expectedMaster = getCommonCampaign(masterCampaignInfo.getCampaign());
        GdiCampaign expectedSub = getCommonCampaign(subCampaignInfo.getCampaign());

        assertThat(gridCampaigns, hasSize(2));
        Assertions.assertThat(gridCampaigns.get(0)).isEqualToIgnoringNullFields(expectedMaster);
        Assertions.assertThat(gridCampaigns.get(1)).isEqualToIgnoringNullFields(expectedSub);
    }

    @Test
    public void checkGetDynamicCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);

        List<GdiCampaign> gridCampaigns = getAllCampaigns();

        GdiCampaign expected = getExpectedDynamicCampaign(campaignInfo.getCampaign());

        assertThat(gridCampaigns, hasSize(1));
        Assertions.assertThat(gridCampaigns.get(0)).isEqualToIgnoringNullFields(expected);
    }

    @Test
    public void checkGetSmartCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        List<GdiCampaign> gridCampaigns = getAllCampaigns();

        GdiCampaign expected = getExpectedSmartCampaign(campaignInfo.getCampaign());

        assertThat(gridCampaigns, hasSize(1));
        Assertions.assertThat(gridCampaigns.get(0)).isEqualToIgnoringNullFields(expected);
    }

    @Test
    public void checkGetMobileContentCampaign() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        List<GdiCampaign> gridCampaigns = getAllCampaigns();

        GdiCampaign expected = getExpectedMobileContentCampaign(campaignInfo.getCampaign());

        assertThat(gridCampaigns, hasSize(1));
        Assertions.assertThat(gridCampaigns.get(0)).isEqualToIgnoringNullFields(expected);
    }

    @Test
    public void checkGetMcBannerCampaign() {
        TypedCampaignInfo campaignInfo =
                steps.typedCampaignSteps().createDefaultMcBannerCampaign(clientInfo.getChiefUserInfo(), clientInfo);

        List<GdiCampaign> gridCampaigns = getAllCampaigns();

        GdiCampaign expected = getExpectedMcBannerCampaign((McBannerCampaign) campaignInfo.getCampaign());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(gridCampaigns).as("кампании клиента")
                    .hasSize(1);
            soft.assertThat(gridCampaigns.get(0)).as("кампания mcbanner")
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                    .ignoringExpectedNullFields()
                    .isEqualTo(expected);
        });
    }

    @Test
    public void checkGetAllCampaignsAndWallets() {
        CampaignInfo wallet = steps.campaignSteps().createWalletCampaign(clientInfo);
        CampaignInfo campaignInfo1 =
                steps.campaignSteps().createCampaignUnderWallet(clientInfo, wallet.getCampaignId(), BigDecimal.ZERO);
        CampaignInfo campaignInfo2 =
                steps.campaignSteps().createCampaignUnderWallet(clientInfo, wallet.getCampaignId(), BigDecimal.ZERO);

        List<GdiCampaign> allCampaignsAndWallets = getAllCampaigns();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(allCampaignsAndWallets).hasSize(3);
            Set<Long> allCampaignIdsAndWalletIds = listToSet(allCampaignsAndWallets, GdiCampaign::getId);
            soft.assertThat(allCampaignIdsAndWalletIds).isEqualTo(
                    Set.of(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId(), wallet.getCampaignId()));
        });
    }

    @Test
    public void checkGetFilteredCampaignsAndWallets() {
        CampaignInfo wallet = steps.campaignSteps().createWalletCampaign(clientInfo);
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, wallet.getCampaignId(), BigDecimal.ZERO);
        CampaignInfo campaignInfo =
                steps.campaignSteps().createCampaignUnderWallet(clientInfo, wallet.getCampaignId(), BigDecimal.ZERO);

        List<GdiCampaign> filteredCampaignsAndWallets = gridCampaignRepository.getCampaignsAndAllWallets(
                UidClientIdShard.of(clientInfo.getUid(), clientInfo.getClientId(), shard),
                List.of(campaignInfo.getCampaignId()));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(filteredCampaignsAndWallets).hasSize(2);
            Set<Long> campaignIdsAndWalletIds = listToSet(filteredCampaignsAndWallets, GdiCampaign::getId);
            soft.assertThat(campaignIdsAndWalletIds).isEqualTo(
                    Set.of(campaignInfo.getCampaignId(), wallet.getCampaignId()));
        });
    }

    private List<GdiCampaign> getAllCampaigns() {
        return gridCampaignRepository.getAllCampaigns(
                UidClientIdShard.of(clientInfo.getUid(), clientInfo.getClientId(), shard));
    }

    private GdiCampaign getExpectedDynamicCampaign(Campaign campaign) {
        return getCommonCampaign(campaign)
                .withAllowedPageIds(campaign.getAllowedPageIds())
                .withDefaultPermalinkId(campaign.getDefaultPermalink())
                .withType(CampaignType.DYNAMIC);
    }

    private GdiCampaign getExpectedSmartCampaign(Campaign campaign) {
        return getCommonCampaign(campaign)
                .withType(CampaignType.PERFORMANCE);
    }

    private GdiCampaign getExpectedMobileContentCampaign(Campaign campaign) {
        return getCommonCampaign(campaign)
                .withAllowedPageIds(campaign.getAllowedPageIds());
    }

    private GdiCampaign getCommonCampaign(Campaign campaign) {
        return new GdiCampaign()
                .withId(campaign.getId())
                .withClientId(campaign.getClientId())
                .withWalletId(campaign.getWalletId())
                .withUserId(campaign.getUid())
                .withName(campaign.getName())
                .withEmpty(campaign.getStatusEmpty())
                .withStatusModerate(CampaignStatusModerate.valueOf(campaign.getStatusModerate().name()))
                .withAgencyId(campaign.getAgencyId())
                .withOrderId(campaign.getOrderId())
                .withEnableCompanyInfo(campaign.getEnableCompanyInfo())
                .withShowing(campaign.getStatusShow())
                .withActive(campaign.getStatusActive())
                .withStatusBsSynced(GdiCampaignStatusBsSynced.valueOf(campaign.getStatusBsSynced().name()))
                .withStatusPostModerate(CampaignStatusPostmoderate.valueOf(campaign.getStatusPostModerate().name()))
                .withStartDate(campaign.getStartTime())
                .withEmail(campaign.getEmail())
                .withSum(campaign.getBalanceInfo().getSum())
                .withSumLast(campaign.getBalanceInfo().getSumLast())
                .withSumSpent(campaign.getBalanceInfo().getSumSpent())
                .withTimezoneId(campaign.getTimezoneId())
                .withMinusKeywords(campaign.getMinusKeywords());
    }

    private GdiCampaign getExpectedMcBannerCampaign(McBannerCampaign campaign) {
        return getTypedCommonCampaign(campaign)
                .withMinusKeywords(campaign.getMinusKeywords())
                .withType(CampaignType.MCBANNER);
    }

    private GdiCampaign getTypedCommonCampaign(CommonCampaign campaign) {
        return new GdiCampaign()
                .withId(campaign.getId())
                .withClientId(campaign.getClientId())
                .withWalletId(campaign.getWalletId())
                .withUserId(campaign.getUid())
                .withName(campaign.getName())
                .withEmpty(campaign.getStatusEmpty())
                .withStatusModerate(CampaignStatusModerate.valueOf(campaign.getStatusModerate().name()))
                .withAgencyId(campaign.getAgencyId())
                .withOrderId(campaign.getOrderId())
                .withEnableCompanyInfo(campaign.getEnableCompanyInfo())
                .withShowing(campaign.getStatusShow())
                .withActive(campaign.getStatusActive())
                .withStatusBsSynced(GdiCampaignStatusBsSynced.valueOf(campaign.getStatusBsSynced().name()))
                .withStatusPostModerate(CampaignStatusPostmoderate.valueOf(campaign.getStatusPostModerate().name()))
                .withStartDate(campaign.getStartDate())
                .withEmail(campaign.getEmail())
                .withSum(campaign.getSum())
                .withSumLast(campaign.getSumLast())
                .withSumSpent(campaign.getSumSpent())
                .withTimezoneId(campaign.getTimeZoneId());
    }
}
