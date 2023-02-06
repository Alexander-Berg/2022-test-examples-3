package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupIdsBySelectionCriteriaWithTypesTest {

    private int shard;

    private Set<Long> campaignIds = new HashSet<>();
    private Set<Long> internalFreeCampaignIds = new HashSet<>();
    private Set<Long> internalDistribCampaignIds = new HashSet<>();
    private Set<Long> internalAutobudgetCampaignIds = new HashSet<>();

    private Long textAdGroupId1;
    private Long textAdGroupId2;
    private Long mobileAppAdGroupId1;
    private Long mobileAppAdGroupId2;
    private Long dynamicAdGroupId1;
    private Long dynamicAdGroupId2;
    private Long cpmBannerAdGroupId1;
    private Long cpmBannerAdGroupId2;
    private Long cpmGeoproductAdGroupId1;
    private Long cpmGeoproductAdGroupId2;
    private Long cpmVideoAdGroupId1;
    private Long cpmVideoAdGroupId2;
    private Long cpmOutdoorAdGroupId1;
    private Long cpmOutdoorAdGroupId2;
    private Long cpmIndoorAdGroupId1;
    private Long cpmIndoorAdGroupId2;
    private Long cpmYndxFrontpageAdGroupId1;
    private Long cpmYndxFrontpageAdGroupId2;
    private Long contentPromotionVideoAdGroupId1;
    private Long contentPromotionVideoAdGroupId2;
    private Long contentPromotionAdGroupId1;
    private Long contentPromotionAdGroupId2;
    private Long internalAdGroupId1;
    private Long internalAdGroupId2;
    private Long internalAdGroupId3;
    private Long internalAdGroupId4;
    private Long internalAdGroupId5;
    private Long internalAdGroupId6;
    private Long mcBannerAdGroupId1;
    private Long mcBannerAdGroupId2;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository repository;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo());

        shard = clientInfo.getShard();

        CampaignSteps campaignSteps = steps.campaignSteps();
        AdGroupSteps adGroupSteps = steps.adGroupSteps();

        // text
        CampaignInfo textCampaign = campaignSteps.createActiveTextCampaign(clientInfo);
        campaignIds.add(textCampaign.getCampaignId());

        textAdGroupId1 =
                adGroupSteps.createAdGroup(activeTextAdGroup(textCampaign.getCampaignId()), textCampaign)
                        .getAdGroupId();
        textAdGroupId2 =
                adGroupSteps.createAdGroup(activeTextAdGroup(textCampaign.getCampaignId()), textCampaign)
                        .getAdGroupId();

        // mobile app
        CampaignInfo mobileAppCampaign = campaignSteps.createActiveMobileAppCampaign(clientInfo);
        campaignIds.add(mobileAppCampaign.getCampaignId());

        mobileAppAdGroupId1 =
                adGroupSteps.createAdGroup(activeMobileAppAdGroup(mobileAppCampaign.getCampaignId()), mobileAppCampaign)
                        .getAdGroupId();
        mobileAppAdGroupId2 =
                adGroupSteps.createAdGroup(activeMobileAppAdGroup(mobileAppCampaign.getCampaignId()), mobileAppCampaign)
                        .getAdGroupId();

        // dynamic
        CampaignInfo dynamicCampaign = campaignSteps.createActiveDynamicCampaign(clientInfo);
        campaignIds.add(dynamicCampaign.getCampaignId());

        dynamicAdGroupId1 =
                adGroupSteps.createAdGroup(
                        activeDynamicTextAdGroup(dynamicCampaign.getCampaignId()),
                        dynamicCampaign)
                        .getAdGroupId();
        dynamicAdGroupId2 =
                adGroupSteps.createAdGroup(
                        activeDynamicTextAdGroup(dynamicCampaign.getCampaignId()),
                        dynamicCampaign)
                        .getAdGroupId();

        // cpm_banner
        CampaignInfo cpmBannerCampaign = campaignSteps.createActiveCpmBannerCampaign(clientInfo);
        campaignIds.add(cpmBannerCampaign.getCampaignId());

        cpmBannerAdGroupId1 =
                adGroupSteps.createAdGroup(activeCpmBannerAdGroup(cpmBannerCampaign.getCampaignId()), cpmBannerCampaign)
                        .getAdGroupId();

        CampaignInfo cpmDealsCampaign = campaignSteps.createActiveCpmDealsCampaign(clientInfo);
        campaignIds.add(cpmDealsCampaign.getCampaignId());

        cpmBannerAdGroupId2 =
                adGroupSteps.createAdGroup(activeCpmBannerAdGroup(cpmDealsCampaign.getCampaignId()), cpmDealsCampaign)
                        .getAdGroupId();

        // cpm_geoproduct
        cpmGeoproductAdGroupId1 = adGroupSteps
                .createAdGroup(activeCpmGeoproductAdGroup(cpmBannerCampaign.getCampaignId()), cpmBannerCampaign)
                .getAdGroupId();

        cpmGeoproductAdGroupId2 = adGroupSteps
                .createAdGroup(activeCpmGeoproductAdGroup(cpmDealsCampaign.getCampaignId()), cpmBannerCampaign)
                .getAdGroupId();

        // cpm_video
        cpmVideoAdGroupId1 =
                adGroupSteps.createAdGroup(activeCpmVideoAdGroup(cpmBannerCampaign.getCampaignId()), cpmBannerCampaign)
                        .getAdGroupId();

        cpmVideoAdGroupId2 =
                adGroupSteps.createAdGroup(activeCpmVideoAdGroup(cpmBannerCampaign.getCampaignId()), cpmBannerCampaign)
                        .getAdGroupId();

        // cpm_outdoor
        cpmOutdoorAdGroupId1 = adGroupSteps.createActiveCpmOutdoorAdGroup(cpmBannerCampaign).getAdGroupId();

        cpmOutdoorAdGroupId2 = adGroupSteps.createActiveCpmOutdoorAdGroup(cpmBannerCampaign).getAdGroupId();

        // cpm_indoor
        cpmIndoorAdGroupId1 = adGroupSteps.createActiveCpmIndoorAdGroup(cpmBannerCampaign).getAdGroupId();

        cpmIndoorAdGroupId2 = adGroupSteps.createActiveCpmIndoorAdGroup(cpmBannerCampaign).getAdGroupId();

        // yndx_frontpage
        CampaignInfo cpmYndxFrontpageCampaign = campaignSteps.createActiveCpmYndxFrontpageCampaign(clientInfo);
        campaignIds.add(cpmYndxFrontpageCampaign.getCampaignId());

        cpmYndxFrontpageAdGroupId1 = adGroupSteps
                .createAdGroup(activeCpmYndxFrontpageAdGroup(cpmYndxFrontpageCampaign.getCampaignId()),
                        cpmYndxFrontpageCampaign)
                .getAdGroupId();

        cpmYndxFrontpageAdGroupId2 = adGroupSteps
                .createAdGroup(activeCpmYndxFrontpageAdGroup(cpmYndxFrontpageCampaign.getCampaignId()),
                        cpmYndxFrontpageCampaign)
                .getAdGroupId();

        // content_promotion_video
        var contentPromotionCampaign = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        campaignIds.add(contentPromotionCampaign.getCampaignId());
        contentPromotionVideoAdGroupId1 = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaign, fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO))
                .getAdGroupId();
        contentPromotionVideoAdGroupId2 = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaign, fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO))
                .getAdGroupId();

        //content_promotion
        contentPromotionAdGroupId1 = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaign,
                        fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION))
                .getAdGroupId();
        contentPromotionAdGroupId2 = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaign,
                        fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION))
                .getAdGroupId();

        // internal for internal_free
        CampaignInfo internalFreeCampaign = campaignSteps.createActiveInternalFreeCampaign(clientInfo);
        campaignIds.add(internalFreeCampaign.getCampaignId());
        internalFreeCampaignIds.add(internalFreeCampaign.getCampaignId());

        internalAdGroupId1 =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalFreeCampaign.getCampaignId()),
                        internalFreeCampaign)
                        .getAdGroupId();
        internalAdGroupId2 =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalFreeCampaign.getCampaignId()),
                        internalFreeCampaign)
                        .getAdGroupId();

        // internal for internal_distrib
        CampaignInfo internalDistribCampaign = campaignSteps.createActiveInternalDistribCampaign(clientInfo);
        campaignIds.add(internalDistribCampaign.getCampaignId());
        internalDistribCampaignIds.add(internalDistribCampaign.getCampaignId());

        internalAdGroupId3 =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalDistribCampaign.getCampaignId()),
                        internalDistribCampaign)
                        .getAdGroupId();
        internalAdGroupId4 =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalDistribCampaign.getCampaignId()),
                        internalDistribCampaign)
                        .getAdGroupId();

        // internal for internal_autobudget
        CampaignInfo internalAutobudgetCampaign = campaignSteps.createActiveInternalAutobudgetCampaign(clientInfo);
        campaignIds.add(internalAutobudgetCampaign.getCampaignId());
        internalAutobudgetCampaignIds.add(internalAutobudgetCampaign.getCampaignId());

        internalAdGroupId5 =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalAutobudgetCampaign.getCampaignId()),
                        internalAutobudgetCampaign)
                        .getAdGroupId();
        internalAdGroupId6 =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalAutobudgetCampaign.getCampaignId()),
                        internalAutobudgetCampaign)
                        .getAdGroupId();

        // mc_banner
        CampaignInfo mcBannerCampaign = campaignSteps.createActiveMcBannerCampaign(clientInfo);
        campaignIds.add(mcBannerCampaign.getCampaignId());

        mcBannerAdGroupId1 =
                adGroupSteps.createAdGroup(activeMcBannerAdGroup(mcBannerCampaign.getCampaignId()), mcBannerCampaign)
                        .getAdGroupId();

        mcBannerAdGroupId2 =
                adGroupSteps.createAdGroup(activeMcBannerAdGroup(mcBannerCampaign.getCampaignId()), mcBannerCampaign)
                        .getAdGroupId();
    }

    @Test
    public void getTextAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.BASE),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(textAdGroupId1, textAdGroupId2));
    }

    @Test
    public void getMobileAppAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupTypes(AdGroupType.MOBILE_CONTENT), maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(mobileAppAdGroupId1, mobileAppAdGroupId2));
    }

    @Test
    public void getDynamicAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.DYNAMIC),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(dynamicAdGroupId1, dynamicAdGroupId2));
    }

    @Test
    public void getCpmBannerAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.CPM_BANNER),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(cpmBannerAdGroupId1, cpmBannerAdGroupId2));
    }

    @Test
    public void getCpmGeoproductAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupTypes(AdGroupType.CPM_GEOPRODUCT),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(cpmGeoproductAdGroupId1, cpmGeoproductAdGroupId2));
    }

    @Test
    public void getCpmVideoAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.CPM_VIDEO),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(cpmVideoAdGroupId1, cpmVideoAdGroupId2));
    }

    @Test
    public void getCpmOutdoorAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.CPM_OUTDOOR),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(cpmOutdoorAdGroupId1, cpmOutdoorAdGroupId2));
    }

    @Test
    public void getCpmIndoorAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.CPM_INDOOR),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(cpmIndoorAdGroupId1, cpmIndoorAdGroupId2));
    }

    @Test
    public void getCpmYndxFrontpageAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupTypes(AdGroupType.CPM_YNDX_FRONTPAGE),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(cpmYndxFrontpageAdGroupId1, cpmYndxFrontpageAdGroupId2));
    }

    @Test
    public void getContentPromotionVideoAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupTypes(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.VIDEO)),
                maxLimited());
        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(contentPromotionVideoAdGroupId1, contentPromotionVideoAdGroupId2));
    }

    @Test
    public void getContentPromotionAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds)
                        .withAdGroupTypes(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionAdgroupTypes(Set.of(ContentPromotionAdgroupType.COLLECTION)),
                maxLimited());
        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(contentPromotionAdGroupId1, contentPromotionAdGroupId2));
    }

    @Test
    public void getInternalAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.INTERNAL),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(internalAdGroupId1, internalAdGroupId2, internalAdGroupId3, internalAdGroupId4,
                        internalAdGroupId5, internalAdGroupId6));
    }

    @Test
    public void getInternalFreeAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(internalFreeCampaignIds),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(internalAdGroupId1, internalAdGroupId2));
    }

    @Test
    public void getInternalDistribAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(internalDistribCampaignIds),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(internalAdGroupId3, internalAdGroupId4));
    }

    @Test
    public void getInternalAutobudgetAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(internalAutobudgetCampaignIds),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(internalAdGroupId5, internalAdGroupId6));
    }

    @Test
    public void getMcBannerAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes(AdGroupType.MCBANNER),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(mcBannerAdGroupId1, mcBannerAdGroupId2));
    }

    @Test
    public void getAdGroupsWithAllTypes() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria()
                        .withCampaignIds(campaignIds)
                        .withAdGroupTypes(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT, AdGroupType.DYNAMIC,
                                AdGroupType.CPM_BANNER, AdGroupType.CPM_VIDEO, AdGroupType.CPM_OUTDOOR,
                                AdGroupType.CPM_INDOOR, AdGroupType.CPM_YNDX_FRONTPAGE, AdGroupType.CPM_GEOPRODUCT,
                                AdGroupType.CONTENT_PROMOTION_VIDEO, AdGroupType.CONTENT_PROMOTION,
                                AdGroupType.INTERNAL, AdGroupType.MCBANNER),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(textAdGroupId1, textAdGroupId2, mobileAppAdGroupId1, mobileAppAdGroupId2, dynamicAdGroupId1,
                        dynamicAdGroupId2, cpmBannerAdGroupId1, cpmBannerAdGroupId2,
                        cpmGeoproductAdGroupId1, cpmGeoproductAdGroupId2, cpmVideoAdGroupId1,
                        cpmVideoAdGroupId2, cpmOutdoorAdGroupId1, cpmOutdoorAdGroupId2, cpmIndoorAdGroupId1,
                        cpmIndoorAdGroupId2, cpmYndxFrontpageAdGroupId1, cpmYndxFrontpageAdGroupId2,
                        contentPromotionVideoAdGroupId1, contentPromotionVideoAdGroupId2,
                        contentPromotionAdGroupId1, contentPromotionAdGroupId2, internalAdGroupId1,
                        internalAdGroupId2, internalAdGroupId3, internalAdGroupId4, internalAdGroupId5,
                        internalAdGroupId6, mcBannerAdGroupId1, mcBannerAdGroupId2));
    }

    @Test
    public void getAdGroupsWithNullAsType() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(campaignIds).withAdGroupTypes((Set<AdGroupType>) null),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(textAdGroupId1, textAdGroupId2, mobileAppAdGroupId1, mobileAppAdGroupId2, dynamicAdGroupId1,
                        dynamicAdGroupId2, cpmBannerAdGroupId1, cpmBannerAdGroupId2,
                        cpmGeoproductAdGroupId1, cpmGeoproductAdGroupId2, cpmVideoAdGroupId1,
                        cpmVideoAdGroupId2, cpmOutdoorAdGroupId1, cpmOutdoorAdGroupId2, cpmIndoorAdGroupId1,
                        cpmIndoorAdGroupId2, cpmYndxFrontpageAdGroupId1, cpmYndxFrontpageAdGroupId2,
                        contentPromotionVideoAdGroupId1, contentPromotionVideoAdGroupId2,
                        contentPromotionAdGroupId1, contentPromotionAdGroupId2, internalAdGroupId1,
                        internalAdGroupId2, internalAdGroupId3, internalAdGroupId4, internalAdGroupId5,
                        internalAdGroupId6, mcBannerAdGroupId1, mcBannerAdGroupId2));
    }
}
