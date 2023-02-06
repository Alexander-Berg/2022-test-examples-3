package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
public class GetAdGroupIdsBySelectionCriteriaWithAdGroupOrCampaignIdsTest {

    private int shard;

    private Long textCampaignId;
    private Long mobileAppCampaignId;
    private Long dynamicCampaignId;
    private Long cpmBannerCampaignId;
    private Long cpmDealsCampaignId;
    private Long cpmYndxFrontPageCampaignId;
    private Long contentPromotionCampaignId;
    private Long internalFreeCampaignId;
    private Long internalDistribCampaignId;
    private Long internalAutobudgetCampaignId;
    private Long mcBannerCampaignId;
    private Long textAdGroupId;
    private Long mobileAppAdGroupId;
    private Long dynamicAdGroupId;
    private Long cpmBannerAdGroupId;
    private Long cpmBannerInDealsAdGroupId;
    private Long cpmGeoproductAdGroupId;
    private Long cpmVideoAdGroupId;
    private Long cpmOutdoorAdGroupId;
    private Long cpmIndoorAdGroupId;
    private Long cpmYndxFrontPageAdGroupId;
    private Long contentPromotionVideoAdGroupId;
    private Long contentPromotionAdGroupId;
    private Long internalFreeAdGroupId;
    private Long internalDistribAdGroupId;
    private Long internalAutobudgetAdGroupId;
    private Long mcBannerAdGroupId;

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
        textCampaignId = textCampaign.getCampaignId();
        textAdGroupId = adGroupSteps.createAdGroup(activeTextAdGroup(textCampaignId), textCampaign).getAdGroupId();

        // mobile app
        CampaignInfo mobileAppCampaign = campaignSteps.createActiveMobileAppCampaign(clientInfo);
        mobileAppCampaignId = mobileAppCampaign.getCampaignId();
        mobileAppAdGroupId = adGroupSteps.createAdGroup(activeMobileAppAdGroup(mobileAppCampaignId), mobileAppCampaign)
                .getAdGroupId();

        // dynamic
        CampaignInfo dynamicCampaign = campaignSteps.createActiveDynamicCampaign(clientInfo);
        dynamicCampaignId = dynamicCampaign.getCampaignId();
        dynamicAdGroupId = adGroupSteps.createAdGroup(activeDynamicTextAdGroup(dynamicCampaignId), dynamicCampaign)
                .getAdGroupId();

        // cpm_banner
        CampaignInfo cpmBannerCampaign = campaignSteps.createActiveCpmBannerCampaign(clientInfo);
        cpmBannerCampaignId = cpmBannerCampaign.getCampaignId();
        cpmBannerAdGroupId = adGroupSteps.createAdGroup(activeCpmBannerAdGroup(cpmBannerCampaignId), cpmBannerCampaign)
                .getAdGroupId();

        // cpm_deals
        CampaignInfo cpmDealsCampaign = campaignSteps.createActiveCpmDealsCampaign(clientInfo);
        cpmDealsCampaignId = cpmDealsCampaign.getCampaignId();
        cpmBannerInDealsAdGroupId =
                adGroupSteps.createAdGroup(activeCpmBannerAdGroup(cpmDealsCampaignId), cpmDealsCampaign)
                        .getAdGroupId();

        // cpm_geoproduct
        cpmGeoproductAdGroupId =
                adGroupSteps.createAdGroup(activeCpmGeoproductAdGroup(cpmBannerCampaignId), cpmBannerCampaign)
                        .getAdGroupId();

        // cpm_video
        cpmVideoAdGroupId = adGroupSteps.createAdGroup(activeCpmVideoAdGroup(cpmBannerCampaignId), cpmBannerCampaign)
                .getAdGroupId();

        // cpm_outdoor
        cpmOutdoorAdGroupId = adGroupSteps.createActiveCpmOutdoorAdGroup(cpmBannerCampaign).getAdGroupId();

        //cpm_indoor
        cpmIndoorAdGroupId = adGroupSteps.createActiveCpmIndoorAdGroup(cpmBannerCampaign).getAdGroupId();

        // yndx_frontpage
        CampaignInfo frontpageCampaign = campaignSteps.createActiveCpmYndxFrontpageCampaign(clientInfo);
        cpmYndxFrontPageCampaignId = frontpageCampaign.getCampaignId();
        cpmYndxFrontPageAdGroupId =
                adGroupSteps.createAdGroup(activeCpmYndxFrontpageAdGroup(cpmYndxFrontPageCampaignId), frontpageCampaign)
                        .getAdGroupId();

        // content_promotion_video
        var contentPromotionCampaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        contentPromotionCampaignId = contentPromotionCampaignInfo.getCampaignId();
        contentPromotionVideoAdGroupId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaignInfo,
                        fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO))
                .getAdGroupId();

        //content_promotion
        contentPromotionAdGroupId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaignInfo,
                        fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION))
                .getAdGroupId();

        // internal for internal_free
        CampaignInfo internalFreeCampaign = campaignSteps.createActiveInternalFreeCampaign(clientInfo);
        internalFreeCampaignId = internalFreeCampaign.getCampaignId();
        internalFreeAdGroupId =
                adGroupSteps.createAdGroup(activeInternalAdGroup(internalFreeCampaignId), internalFreeCampaign)
                        .getAdGroupId();

        // internal for internal_distrib
        CampaignInfo internalDistribCampaign = campaignSteps.createActiveInternalDistribCampaign(clientInfo);
        internalDistribCampaignId = internalDistribCampaign.getCampaignId();
        internalDistribAdGroupId =
                adGroupSteps
                        .createAdGroup(activeInternalAdGroup(internalDistribCampaignId), internalDistribCampaign)
                        .getAdGroupId();

        // internal for internal_autobudget
        CampaignInfo internalAutobudgetCampaign = campaignSteps.createActiveInternalAutobudgetCampaign(clientInfo);
        internalAutobudgetCampaignId = internalAutobudgetCampaign.getCampaignId();
        internalAutobudgetAdGroupId =
                adGroupSteps
                        .createAdGroup(activeInternalAdGroup(internalAutobudgetCampaignId), internalAutobudgetCampaign)
                        .getAdGroupId();

        // mcbanner
        CampaignInfo mcBannerCampaign = campaignSteps.createActiveMcBannerCampaign(clientInfo);
        mcBannerCampaignId = mcBannerCampaign.getCampaignId();
        mcBannerAdGroupId = adGroupSteps.createAdGroup(activeMcBannerAdGroup(mcBannerCampaignId), mcBannerCampaign)
                .getAdGroupId();
    }

    @Test
    public void getAdGroupIdsWithoutCampaignOrAdGroupIds_exceptionThrown() {
        assertThatThrownBy(() -> {
            repository.getAdGroupIdsBySelectionCriteria(shard, new AdGroupsSelectionCriteria(), maxLimited());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AdGroupIds or CampaignIds must be specified!");
    }

    @Test
    public void getAdGroupIdsByCampaignIds() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignId,
                        cpmBannerCampaignId, cpmDealsCampaignId, cpmYndxFrontPageCampaignId,
                        contentPromotionCampaignId, internalFreeCampaignId, internalDistribCampaignId,
                        internalAutobudgetCampaignId, mcBannerCampaignId),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(textAdGroupId, mobileAppAdGroupId, dynamicAdGroupId, cpmBannerAdGroupId,
                        cpmBannerInDealsAdGroupId, cpmGeoproductAdGroupId, cpmVideoAdGroupId,
                        cpmOutdoorAdGroupId, cpmIndoorAdGroupId,
                        cpmYndxFrontPageAdGroupId, contentPromotionVideoAdGroupId, contentPromotionAdGroupId,
                        internalFreeAdGroupId, internalDistribAdGroupId, internalAutobudgetAdGroupId,
                        mcBannerAdGroupId));
    }

    @Test
    public void getAdGroupIdsByAdGroupIds() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withAdGroupIds(textAdGroupId, mobileAppAdGroupId, dynamicAdGroupId,
                        cpmBannerAdGroupId, cpmBannerInDealsAdGroupId, cpmVideoAdGroupId, cpmOutdoorAdGroupId,
                        cpmIndoorAdGroupId, cpmYndxFrontPageAdGroupId, contentPromotionVideoAdGroupId,
                        cpmGeoproductAdGroupId, internalFreeAdGroupId, internalDistribAdGroupId,
                        internalAutobudgetAdGroupId, mcBannerAdGroupId),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(textAdGroupId, mobileAppAdGroupId, dynamicAdGroupId, cpmBannerAdGroupId,
                        cpmBannerInDealsAdGroupId, cpmGeoproductAdGroupId, cpmVideoAdGroupId, cpmOutdoorAdGroupId,
                        cpmIndoorAdGroupId, cpmYndxFrontPageAdGroupId, contentPromotionVideoAdGroupId,
                        internalFreeAdGroupId, internalDistribAdGroupId, internalAutobudgetAdGroupId,
                        mcBannerAdGroupId));
    }

    @Test
    public void getAdGroupIdsByCampaignAndAdGroupIds() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria()
                        .withAdGroupIds(textAdGroupId, mobileAppAdGroupId, dynamicAdGroupId, cpmBannerAdGroupId,
                                cpmBannerInDealsAdGroupId, cpmVideoAdGroupId, cpmOutdoorAdGroupId,
                                cpmYndxFrontPageAdGroupId, contentPromotionVideoAdGroupId,
                                internalFreeAdGroupId, cpmGeoproductAdGroupId, internalDistribAdGroupId,
                                internalAutobudgetAdGroupId, mcBannerAdGroupId)
                        .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignId, cpmBannerCampaignId,
                                cpmDealsCampaignId, cpmYndxFrontPageCampaignId,
                                contentPromotionCampaignId, internalFreeCampaignId, internalDistribCampaignId,
                                internalAutobudgetCampaignId, mcBannerCampaignId),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(textAdGroupId, mobileAppAdGroupId, dynamicAdGroupId, cpmBannerAdGroupId,
                        cpmBannerInDealsAdGroupId, cpmGeoproductAdGroupId, cpmVideoAdGroupId, cpmOutdoorAdGroupId,
                        cpmYndxFrontPageAdGroupId, contentPromotionVideoAdGroupId, internalFreeAdGroupId,
                        internalDistribAdGroupId, internalAutobudgetAdGroupId, mcBannerAdGroupId));
    }

    @Test
    public void getAdGroupIdsWhenAmountOfCampaignIdsIsGreaterThanAmountOfAdGroupIds() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria()
                        .withAdGroupIds(textAdGroupId, mobileAppAdGroupId)
                        .withCampaignIds(textCampaignId, mobileAppCampaignId, dynamicCampaignId),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(textAdGroupId, mobileAppAdGroupId));
    }

    @Test
    public void getAdGroupIdsWhenAmountOfCampaignIdsIsLessThanAmountOfAdGroupIds() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria()
                        .withAdGroupIds(textAdGroupId, mobileAppAdGroupId, dynamicAdGroupId)
                        .withCampaignIds(mobileAppCampaignId, dynamicCampaignId),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds, contains(mobileAppAdGroupId, dynamicAdGroupId));
    }

    @Test
    public void getOnlyInternalAdGroupsByCampaignIds() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard, new AdGroupsSelectionCriteria()
                        .withCampaignIds(internalFreeCampaignId, internalDistribCampaignId,
                                internalAutobudgetCampaignId),
                maxLimited());

        assertThat("вернулись id ожидаемых групп", adGroupIds,
                contains(internalFreeAdGroupId, internalDistribAdGroupId, internalAutobudgetAdGroupId));
    }

    @Test
    public void getOnlyInternalFreeAdGroupByCampaignId() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard, new AdGroupsSelectionCriteria()
                        .withCampaignIds(internalFreeCampaignId),
                maxLimited());

        assertThat("вернулась одна група", adGroupIds.size(), is(1));

        Long adGroupId = adGroupIds.get(0);

        assertThat("вернулся id ожидаемой группы", adGroupId, equalTo(internalFreeAdGroupId));
    }

    @Test
    public void getOnlyInternalDistribAdGroupByCampaignId() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard, new AdGroupsSelectionCriteria()
                        .withCampaignIds(internalDistribCampaignId),
                maxLimited());

        assertThat("вернулась одна група", adGroupIds.size(), is(1));

        Long adGroupId = adGroupIds.get(0);

        assertThat("вернулся id ожидаемой группы", adGroupId, is(internalDistribAdGroupId));
    }

    @Test
    public void getOnlyInternalAutobudgetAdGroupByCampaignId() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard, new AdGroupsSelectionCriteria()
                        .withCampaignIds(internalAutobudgetCampaignId),
                maxLimited());

        assertThat("вернулась одна група", adGroupIds.size(), is(1));

        Long adGroupId = adGroupIds.get(0);

        assertThat("вернулся id ожидаемой группы", adGroupId, is(internalAutobudgetAdGroupId));
    }

    @Test
    public void getContentPromotionAndContentPromotionVideoAdGroups() {
        List<Long> adGroupIds = repository.getAdGroupIdsBySelectionCriteria(shard,
                new AdGroupsSelectionCriteria().withCampaignIds(contentPromotionCampaignId),
                maxLimited());
        assertThat("вернулись id ожидаемых групп", adGroupIds,
                containsInAnyOrder(contentPromotionAdGroupId, contentPromotionVideoAdGroupId));
    }

}
