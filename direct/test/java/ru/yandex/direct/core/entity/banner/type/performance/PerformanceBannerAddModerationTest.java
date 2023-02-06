package ru.yandex.direct.core.entity.banner.type.performance;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.EntryStream;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupMappings;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.regions.Region;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaignWithStrategy;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.clientPerformanceBanner;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.regions.Region.SIMFEROPOL_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@CoreTest
@RunWith(SpringRunner.class)
public class PerformanceBannerAddModerationTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final List<Long> EXPECTED_CREATIVE_GEO = asList(RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID);

    private static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_NEW =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.NEW;
    private static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_READY =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.READY;
    private static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_YES =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.YES;
    private static final ru.yandex.direct.core.entity.creative.model.StatusModerate CREATIVE_MODERATE_ERROR =
            ru.yandex.direct.core.entity.creative.model.StatusModerate.ERROR;

    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUP_MODERATE_NEW =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusModerate ADGROUP_MODERATE_YES =
            ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES;

    private static final ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate ADGROUP_POST_MODERATE_YES =
            ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.YES;
    private static final ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate ADGROUP_POST_MODERATE_NO =
            ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate.NO;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private TestCreativeRepository testCreativeRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Test
    public void addBanners_updateBannerStatuses() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long creativeId = createCreativeWithStatusModerate(adGroupInfo.getClientInfo(), CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        Long id = prepareAndApplyValid(banner);

        PerformanceBanner actualBanner = getBanner(id);

        PerformanceBanner expectedBanner = new PerformanceBanner()
                .withStatusModerate(BannerStatusModerate.YES)
                .withStatusPostModerate(BannerStatusPostModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES);

        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addBanners_sendCreativesToModeration() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long creativeId = createCreativeWithStatusModerate(adGroupInfo.getClientInfo(), CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        prepareAndApplyValid(banner, false);

        List<Creative> creatives = creativeRepository.getCreatives(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                singletonList(creativeId));
        MatcherAssert.assertThat(creatives.get(0).getStatusModerate(), CoreMatchers.equalTo(CREATIVE_MODERATE_READY));
    }

    @Test
    public void addBanners_dontSendCreativesToModeration_whenSaveDraft() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long creativeId = createCreativeWithStatusModerate(adGroupInfo.getClientInfo(), CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        prepareAndApplyValid(banner, true);

        List<Creative> creatives = creativeRepository.getCreatives(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                singletonList(creativeId));
        MatcherAssert.assertThat(creatives.get(0).getStatusModerate(), CoreMatchers.equalTo(CREATIVE_MODERATE_NEW));
    }

    @Test
    public void addBanners_sendCreativesToModeration_when3Banners() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        var adGroupId = adGroupInfo.getAdGroupId();

        Long creativeId1 = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_NEW);
        Long creativeId2 = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_ERROR);
        Long creativeId3 = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_YES);

        PerformanceBanner banner1 = clientPerformanceBanner(adGroupId, creativeId1);
        PerformanceBanner banner2 = clientPerformanceBanner(adGroupId, creativeId2);
        PerformanceBanner banner3 = clientPerformanceBanner(adGroupId, creativeId3);

        prepareAndApplyValid(List.of(banner1, banner2, banner3), false);

        List<Creative> creatives = creativeRepository.getCreatives(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                List.of(creativeId1, creativeId2, creativeId3));
        Map<Long, ru.yandex.direct.core.entity.creative.model.StatusModerate> actualStatuses =
                listToMap(creatives, Creative::getId, Creative::getStatusModerate);

        Map<Long, ru.yandex.direct.core.entity.creative.model.StatusModerate> expectedStatuses = ImmutableMap.of(
                creativeId1, CREATIVE_MODERATE_READY,
                creativeId2, CREATIVE_MODERATE_READY,
                creativeId3, CREATIVE_MODERATE_YES
        );
        assertThat(actualStatuses, equalTo(expectedStatuses));
    }

    @Test
    public void addBanners_sendCreativesToModeration_whenTwoCampaigns() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        var adGroupId1 = adGroupInfo.getAdGroupId();

        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(
                activePerformanceCampaignWithStrategy(clientInfo.getClientId(), null)
                        .withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NEW),
                clientInfo);
        var adGroupInfo2 = steps.adGroupSteps().addPerformanceAdGroup(new PerformanceAdGroupInfo()
                .withCampaignInfo(campaignInfo2)
                .withClientInfo(clientInfo)
        );
        var adGroupId2 = adGroupInfo2.getAdGroupId();

        Long creativeId1 = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_NEW);
        Long creativeId2 = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_NEW);
        Long creativeId3 = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_NEW);

        PerformanceBanner banner1 = clientPerformanceBanner(adGroupId1, creativeId1);
        PerformanceBanner banner2 = clientPerformanceBanner(adGroupId1, creativeId2);
        PerformanceBanner banner3 = clientPerformanceBanner(adGroupId2, creativeId2);
        PerformanceBanner banner4 = clientPerformanceBanner(adGroupId2, creativeId3);
        prepareAndApplyValid(List.of(banner1, banner2, banner3, banner4));

        List<Creative> creatives = creativeRepository.getCreatives(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                List.of(creativeId1, creativeId2, creativeId3));
        Map<Long, StatusModerate> actualStatuses =
                listToMap(creatives, Creative::getId, Creative::getStatusModerate);
        Map<Long, ru.yandex.direct.core.entity.creative.model.StatusModerate> expectedStatuses =
                ImmutableMap.of(
                        creativeId1, CREATIVE_MODERATE_READY,
                        creativeId2, CREATIVE_MODERATE_READY,
                        creativeId3, CREATIVE_MODERATE_NEW
                );

        assertThat(actualStatuses, equalTo(expectedStatuses));
    }

    @Test
    public void addBanners_updateCreativesGeo() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        List<Long> adGroupGeo = asList(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, KAZAKHSTAN_REGION_ID);
        adGroupInfo = createAdGroupWithGeo(adGroupGeo, campaignInfo, feedId);
        Long creativeId = createCreativeWithStatusModerate(adGroupInfo.getClientInfo(), CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        prepareAndApplyValid(banner, false);

        Map<Long, List<Long>> geoByCreativeId = getCreativeGeoByCreativeId(adGroupInfo.getShard(),
                singletonList(creativeId));
        assertThat(geoByCreativeId.get(creativeId), containsInAnyOrder(EXPECTED_CREATIVE_GEO.toArray()));
    }

    @Test
    public void addBanners_updateCreativesGeo_whenSaveDraft() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        List<Long> adGroupGeo = asList(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, KAZAKHSTAN_REGION_ID);
        adGroupInfo = createAdGroupWithGeo(adGroupGeo, campaignInfo, feedId);
        Long creativeId = createCreativeWithStatusModerate(adGroupInfo.getClientInfo(), CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        prepareAndApplyValid(banner, true);

        Map<Long, List<Long>> geoByCreativeId = getCreativeGeoByCreativeId(adGroupInfo.getShard(),
                singletonList(creativeId));
        assertThat(geoByCreativeId.get(creativeId), containsInAnyOrder(EXPECTED_CREATIVE_GEO.toArray()));
    }

    @Test
    public void addBanners_updateCreativesGeo_whenAdGroupContainsTheCrimeaAndClientFromRussia() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        assumeThat(campaignInfo.getClientInfo().getClient().getCountryRegionId(), is(RUSSIA_REGION_ID));
        testClientRepository.setClientRegionId(campaignInfo.getShard(), campaignInfo.getClientId(),
                RUSSIA_REGION_ID);
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        adGroupInfo = createAdGroupWithGeo(singletonList(SIMFEROPOL_REGION_ID), campaignInfo, feedId);

        Long creativeId = createCreativeWithStatusModerate(campaignInfo.getClientInfo(), CREATIVE_MODERATE_NEW);
        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        prepareAndApplyValid(banner);

        Map<Long, List<Long>> geoByCreativeId = getCreativeGeoByCreativeId(campaignInfo.getShard(),
                singletonList(creativeId));
        List<Long> actualGeo = geoByCreativeId.get(creativeId);
        Assertions.assertThat(actualGeo).containsOnly(RUSSIA_REGION_ID);
    }

    @Test
    public void addBanners_updateCreativesGeo_whenAdGroupContainsTheCrimeaAndClientFromUkraine() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        testClientRepository.setClientRegionId(campaignInfo.getShard(), campaignInfo.getClientId(),
                Region.UKRAINE_REGION_ID);
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        adGroupInfo = createAdGroupWithGeo(singletonList(SIMFEROPOL_REGION_ID), campaignInfo, feedId);

        Long creativeId = createCreativeWithStatusModerate(campaignInfo.getClientInfo(), CREATIVE_MODERATE_NEW);
        PerformanceBanner banner = clientPerformanceBanner(adGroupInfo.getAdGroupId(), creativeId);
        prepareAndApplyValid(banner);

        Map<Long, List<Long>> geoByCreativeId = getCreativeGeoByCreativeId(campaignInfo.getShard(),
                singletonList(creativeId));
        List<Long> actualGeo = geoByCreativeId.get(creativeId);
        Assertions.assertThat(actualGeo).containsOnly(Region.UKRAINE_REGION_ID);
    }

    @Test
    public void addBanners_updateCreativesGeo_whenTwoAdGroups() {
        var campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        adGroupInfo = createAdGroupWithGeo(List.of(MOSCOW_REGION_ID, KAZAKHSTAN_REGION_ID), campaignInfo, feedId);
        AdGroupInfo adGroupInfo2 = createAdGroupWithGeo(singletonList(BY_REGION_ID), campaignInfo, feedId);

        Long creativeId1 = createCreativeWithStatusModerate(campaignInfo.getClientInfo(), CREATIVE_MODERATE_NEW);
        Long creativeId2 = createCreativeWithStatusModerate(campaignInfo.getClientInfo(), CREATIVE_MODERATE_NEW);
        Long creativeId3 = createCreativeWithStatusModerate(campaignInfo.getClientInfo(), CREATIVE_MODERATE_ERROR);

        Long adGroupId1 = adGroupInfo.getAdGroupId();
        Long adGroupId2 = adGroupInfo2.getAdGroupId();
        PerformanceBanner banner1 = clientPerformanceBanner(adGroupId1, creativeId1);
        PerformanceBanner banner2 = clientPerformanceBanner(adGroupId1, creativeId2);
        PerformanceBanner banner3 = clientPerformanceBanner(adGroupId2, creativeId2);
        PerformanceBanner banner4 = clientPerformanceBanner(adGroupId2, creativeId3);
        prepareAndApplyValid(List.of(banner1, banner2, banner3, banner4));

        Map<Long, List<Long>> geoByCreativeId = getCreativeGeoByCreativeId(campaignInfo.getShard(),
                asList(creativeId1, creativeId2, creativeId3));

        assertSoftly(softly -> {
            softly.assertThat(geoByCreativeId.get(creativeId1)).is(matchedBy(containsInAnyOrder(
                    RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID)));
            softly.assertThat(geoByCreativeId.get(creativeId2)).is(matchedBy(containsInAnyOrder(
                    RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID, BY_REGION_ID)));
            softly.assertThat(geoByCreativeId.get(creativeId3)).is(matchedBy(containsInAnyOrder(
                    BY_REGION_ID)));
        });
    }

    @Test
    public void addBanners_updateAdGroupStatuses() {
        // в обычной ситуации мы делаем campaignRepository.sendRejectedCampaignsToModerate
        // но для performance банера такого быть не должно, поэтому мы создаём именно rejected кампанию
        // чтобы проверить, что она останется rejected, а не станет ready
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(activePerformanceCampaign(null, null)
                .withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NO));
        Long campaignId = campaignInfo.getCampaignId();
        ClientInfo clientInfo = campaignInfo.getClientInfo();
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();

        PerformanceAdGroup adGroup = activePerformanceAdGroup(campaignId, feedId)
                .withStatusModerate(ADGROUP_MODERATE_NEW)
                .withStatusPostModerate(ADGROUP_POST_MODERATE_NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);
        adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();

        Long creativeId = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupId, creativeId);
        prepareAndApplyValid(banner, false);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(adGroupInfo.getShard(), singletonList(adGroupId));
        PerformanceAdGroup actualAdGroup = (PerformanceAdGroup) adGroups.get(0);

        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withStatusModerate(ADGROUP_MODERATE_YES)
                .withStatusPostModerate(ADGROUP_POST_MODERATE_YES)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
                .withStatusBsSynced(StatusBsSynced.NO);

        assertThat(actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));

        Campaign actualCampaign = campaignRepository.getCampaigns(campaignInfo.getShard(),
                singletonList(campaignId)).get(0);
        assertThat(actualCampaign.getStatusModerate(), equalTo(CampaignStatusModerate.NO));
    }

    @Test
    public void addBanners_dontUpdateAdGroupStatuses_whenSaveDraft() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign();
        ClientInfo clientInfo = campaignInfo.getClientInfo();
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();

        PerformanceAdGroup adGroup = activePerformanceAdGroup(null, feedId)
                .withStatusModerate(ADGROUP_MODERATE_NEW)
                .withStatusPostModerate(ADGROUP_POST_MODERATE_NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);
        adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        Long adGroupId = adGroupInfo.getAdGroupId();

        Long creativeId = createCreativeWithStatusModerate(clientInfo, CREATIVE_MODERATE_NEW);

        PerformanceBanner banner = clientPerformanceBanner(adGroupId, creativeId);
        prepareAndApplyValid(banner, true);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(adGroupInfo.getShard(), singletonList(adGroupId));
        PerformanceAdGroup actualAdGroup = (PerformanceAdGroup) adGroups.get(0);

        PerformanceAdGroup expectedAdGroup = new PerformanceAdGroup()
                .withStatusModerate(ADGROUP_MODERATE_NEW)
                .withStatusPostModerate(ADGROUP_POST_MODERATE_NO)
                .withStatusBLGenerated(StatusBLGenerated.NO);

        assertThat(actualAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));
    }

    private Long createCreativeWithStatusModerate(ClientInfo clientInfo,
                                                  ru.yandex.direct.core.entity.creative.model.StatusModerate statusModerate) {
        Creative creative = defaultPerformanceCreative(null, null).withStatusModerate(statusModerate);
        return steps.creativeSteps().createCreative(creative, clientInfo).getCreativeId();
    }

    private AdGroupInfo createAdGroupWithGeo(List<Long> geo, CampaignInfo campaignInfo, Long feedId) {
        return steps.adGroupSteps().createAdGroup(
                activePerformanceAdGroup(null, feedId).withGeo(geo),
                campaignInfo);
    }

    private Map<Long, List<Long>> getCreativeGeoByCreativeId(int shard, Collection<Long> creativeIds) {
        Map<Long, String> geoByCreativeId = testCreativeRepository.getCreativesGeo(shard, creativeIds);
        return EntryStream.of(geoByCreativeId)
                .filterValues(StringUtils::isNotEmpty)
                .mapValues(AdGroupMappings::geoFromDb)
                .toMap();
    }

}
