package ru.yandex.direct.web.entity.inventori.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestModerateBannerPages.defaultModerateBannerPage;

@DirectWebTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class CampaignInfoCollectorPageIdsTest {
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignInfoCollector collector;
    @Autowired
    private TestAdGroupRepository adGroupRepository;

    private ClientInfo clientInfo;
    private AdGroupInfo adGroupInfo;
    private CpmOutdoorBannerInfo bannerInfo;
    private long campaignId;
    private long adGroupId;
    private long bannerId;
    private long pageId;
    private int shard;
    private List<PageBlock> pageBlocks;

    @Before
    public void setUp() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmOutdoorAdGroup();
        campaignId = adGroupInfo.getCampaignId();
        adGroupId = adGroupInfo.getAdGroupId();
        shard = adGroupInfo.getShard();

        clientInfo = adGroupInfo.getClientInfo();
        Long creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(campaignId, adGroupId, creativeId);
        bannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroupInfo);
        bannerId = bannerInfo.getBannerId();
        CpmOutdoorAdGroup adGroup = (CpmOutdoorAdGroup) adGroupInfo.getAdGroup();
        pageBlocks = adGroup.getPageBlocks();
        pageId = pageBlocks.get(0).getPageId();
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithOneBanner_BannerPageModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId).get(0).getPageId()).isEqualTo(pageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(1);
        assertThat(moderatedBannerIds).contains(bannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithOneBanner_BannerPageRejected() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.NO);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).isEmpty();
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).isEmpty();
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithOneBanner_TwoPages_BothRejected() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.NO);

        var anotherPlacementBlocks = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock().getBlocks();
        var anotherPageId = anotherPlacementBlocks.get(0).getPageId();
        var anotherPageBlock = new PageBlock()
                .withPageId(anotherPageId)
                .withImpId(1L);
        adGroupRepository.addOutdoorPageTargets(shard, adGroupId, List.of(pageBlocks.get(0), anotherPageBlock));
        moderateBannerPage(bannerInfo, anotherPageId, StatusModerateBannerPage.NO);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).isEmpty();
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).isEmpty();
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithOneBanner_TwoPages_OnlyOneModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        var anotherPlacementBlocks = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock().getBlocks();
        var anotherPageId = anotherPlacementBlocks.get(0).getPageId();
        var anotherPageBlock = new PageBlock()
                .withPageId(anotherPageId)
                .withImpId(1L);
        adGroupRepository.addOutdoorPageTargets(shard, adGroupId, List.of(pageBlocks.get(0), anotherPageBlock));
        moderateBannerPage(bannerInfo, anotherPageId, StatusModerateBannerPage.NO);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId).get(0).getPageId()).isEqualTo(pageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(1);
        assertThat(moderatedBannerIds).contains(bannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithOneBanner_TwoPages_BothModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        var anotherPlacementBlocks = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock().getBlocks();
        var anotherPageId = anotherPlacementBlocks.get(0).getPageId();
        var anotherPageBlock = new PageBlock()
                .withPageId(anotherPageId)
                .withImpId(1L);
        adGroupRepository.addOutdoorPageTargets(shard, adGroupId, List.of(pageBlocks.get(0), anotherPageBlock));
        moderateBannerPage(bannerInfo, anotherPageId, StatusModerateBannerPage.YES);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(2);
        assertThat(StreamEx.of(pageTargetsByAdGroupIds.get(adGroupId)).map(PageBlock::getPageId).toSet())
                .containsExactlyInAnyOrder(pageId, anotherPageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(1);
        assertThat(moderatedBannerIds).contains(bannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithTwoBanners_SamePage_OnlyOneModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        Long creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(campaignId, adGroupId, creativeId);
        CpmOutdoorBannerInfo anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroupInfo);
        Long anotherBannerId = anotherBannerInfo.getBannerId();
        moderateBannerPage(anotherBannerInfo, pageId, StatusModerateBannerPage.NO);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId, anotherBannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId).get(0).getPageId()).isEqualTo(pageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(1);
        assertThat(moderatedBannerIds).contains(bannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithTwoBanners_SamePage_BothModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        Long creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(campaignId, adGroupId, creativeId);
        CpmOutdoorBannerInfo anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroupInfo);
        Long anotherBannerId = anotherBannerInfo.getBannerId();
        moderateBannerPage(anotherBannerInfo, pageId, StatusModerateBannerPage.YES);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId, anotherBannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId).get(0).getPageId()).isEqualTo(pageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(2);
        assertThat(moderatedBannerIds).contains(bannerId, anotherBannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithTwoBanners_TwoPages_OnlyOneModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        Long creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(campaignId, adGroupId, creativeId);
        CpmOutdoorBannerInfo anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroupInfo);
        Long anotherBannerId = anotherBannerInfo.getBannerId();
        Long anotherPageId = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock()
                .getBlocks().get(0).getPageId();
        moderateBannerPage(anotherBannerInfo, anotherPageId, StatusModerateBannerPage.NO);

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId, anotherBannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId).get(0).getPageId()).isEqualTo(pageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(1);
        assertThat(moderatedBannerIds).contains(bannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithTwoBanners_TwoPages_OneModeratedForEachBanner() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        Long creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        var banner = activeCpmOutdoorBanner(campaignId, adGroupId, creativeId);
        var anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroupInfo);
        var anotherBannerId = anotherBannerInfo.getBannerId();
        var anotherPlacementBlocks = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock().getBlocks();
        var anotherPageId = anotherPlacementBlocks.get(0).getPageId();
        var anotherPageBlock = new PageBlock()
                .withPageId(anotherPageId)
                .withImpId(1L);
        moderateBannerPage(anotherBannerInfo, anotherPageId, StatusModerateBannerPage.YES);
        adGroupRepository.addOutdoorPageTargets(shard, adGroupId, List.of(pageBlocks.get(0), anotherPageBlock));

        var result = collector.getModeratedPagesAndBanners(shard, Map.of(adGroupId, Set.of(bannerId, anotherBannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(1);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId);
        List<PageBlock> actualPageBlocks = pageTargetsByAdGroupIds.get(adGroupId);
        assertThat(actualPageBlocks).hasSize(2);
        assertThat(StreamEx.of(actualPageBlocks).map(PageBlock::getPageId).toSet())
                .containsExactlyInAnyOrder(pageId, anotherPageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(2);
        assertThat(moderatedBannerIds).contains(bannerId, anotherBannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_TwoAdGroups_BothModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        var anotherAdGroupInfo = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(adGroupInfo.getCampaignInfo());
        var anotherCampaignId = anotherAdGroupInfo.getCampaignId();
        var anotherAdGroupId = anotherAdGroupInfo.getAdGroupId();
        var creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        var banner = activeCpmOutdoorBanner(anotherCampaignId, anotherAdGroupId, creativeId);
        var anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, anotherAdGroupInfo);
        var anotherBannerId = anotherBannerInfo.getBannerId();
        var adGroup = (CpmOutdoorAdGroup) anotherAdGroupInfo.getAdGroup();
        var anotherPageId = adGroup.getPageBlocks().get(0).getPageId();
        moderateBannerPage(anotherBannerInfo, anotherPageId, StatusModerateBannerPage.YES);

        var result = collector.getModeratedPagesAndBanners(shard,
                Map.of(adGroupId, Set.of(bannerId), anotherAdGroupId, Set.of(anotherBannerId)));
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(2);
        assertThat(pageTargetsByAdGroupIds).containsKeys(adGroupId, anotherAdGroupId);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(adGroupId).get(0).getPageId()).isEqualTo(pageId);
        assertThat(pageTargetsByAdGroupIds.get(anotherAdGroupId)).hasSize(1);
        assertThat(pageTargetsByAdGroupIds.get(anotherAdGroupId).get(0).getPageId()).isEqualTo(anotherPageId);
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(2);
        assertThat(moderatedBannerIds).contains(bannerId, anotherBannerId);
    }

    @Test
    public void getModeratedPagesAndBanners_1000AdGroups_AllModerated() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.YES);

        Map<Long, Long> pageIdByAdGroupId = new HashMap<>();
        Map<Long, Set<Long>> bannerIdByAdGroupId = new HashMap<>();

        IntStream.range(0, 1000).forEach(i -> {
            var anotherAdGroupInfo = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(adGroupInfo.getCampaignInfo());
            var anotherCampaignId = anotherAdGroupInfo.getCampaignId();
            var anotherAdGroupId = anotherAdGroupInfo.getAdGroupId();
            var creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
            var banner = activeCpmOutdoorBanner(anotherCampaignId, anotherAdGroupId, creativeId);
            var anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, anotherAdGroupInfo);
            var anotherBannerId = anotherBannerInfo.getBannerId();
            var adGroup = (CpmOutdoorAdGroup) anotherAdGroupInfo.getAdGroup();
            var anotherPageId = adGroup.getPageBlocks().get(0).getPageId();
            moderateBannerPage(anotherBannerInfo, anotherPageId, StatusModerateBannerPage.YES);

            pageIdByAdGroupId.put(anotherAdGroupId, anotherPageId);
            bannerIdByAdGroupId.put(anotherAdGroupId, Set.of(anotherBannerId));
        });

        var result = collector.getModeratedPagesAndBanners(shard, bannerIdByAdGroupId);
        var pageTargetsByAdGroupIds = result.getLeft();
        assertThat(pageTargetsByAdGroupIds).hasSize(pageIdByAdGroupId.size());
        assertThat(pageTargetsByAdGroupIds).containsKeys(pageIdByAdGroupId.keySet().toArray(new Long[0]));

        for (var insertedAdGroupId : pageIdByAdGroupId.keySet()) {
            assertThat(pageTargetsByAdGroupIds.get(insertedAdGroupId)).hasSize(1);
            assertThat(pageTargetsByAdGroupIds.get(insertedAdGroupId).get(0).getPageId())
                    .isEqualTo(pageIdByAdGroupId.get(insertedAdGroupId));
        }
        var moderatedBannerIds = result.getRight();
        assertThat(moderatedBannerIds).hasSize(bannerIdByAdGroupId.size());
        assertThat(moderatedBannerIds).isEqualTo(
                StreamEx.of(bannerIdByAdGroupId.values()).flatMap(StreamEx::of).toSet());
    }

    @Test
    public void getModeratedPagesAndBanners_AdGroupWithTwoBanners_SamePage_OneNotModeratedAndOneNotActive() {
        moderateBannerPage(bannerInfo, pageId, StatusModerateBannerPage.NO);

        Long creativeId = steps.creativeSteps().createCreative(clientInfo).getCreativeId();
        OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(campaignId, adGroupId, creativeId).withStatusShow(false);
        CpmOutdoorBannerInfo anotherBannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroupInfo);
        moderateBannerPage(anotherBannerInfo, pageId, StatusModerateBannerPage.YES);

        var result = collector.filterActiveAdGroups(shard,
                List.of(adGroupInfo.getAdGroup()), false, Set.of());
        var pageTargetsByAdGroupIds = result.getPageBlocksByAdGroupIdFiltered();
        assertThat(pageTargetsByAdGroupIds).isEmpty();
        var moderatedBannerIds = result.getModeratedBannerIdsByActiveAdGroupIds();
        assertThat(moderatedBannerIds).isEmpty();
    }

    private void moderateBannerPage(CpmOutdoorBannerInfo bannerInfo, long pageId, StatusModerateBannerPage status) {
        ModerateBannerPage moderateBannerPage = defaultModerateBannerPage()
                .withStatusModerate(status)
                .withPageId(pageId);
        steps.moderateBannerPageSteps().createModerateBannerPage(bannerInfo, moderateBannerPage);
    }
}
