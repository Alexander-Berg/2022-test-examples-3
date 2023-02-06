package ru.yandex.direct.intapi.entity.balanceclient.repository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyOrderRepositoryFetchCampaignItemsForBsResyncTest {

    private int shard;
    private Long campaignId;
    private AdGroupInfo adGroupInfo;

    @Autowired
    private NotifyOrderRepository notifyOrderRepository;

    @Autowired
    public Steps steps;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Before
    public void prepareData() {
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        campaignId = defaultCampaign.getCampaignId();
        shard = defaultCampaign.getShard();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(defaultCampaign);
    }


    @Test
    public void fetchEmptyList_WhenCampaignWithoutBanner() {
        List<BsResyncItem> bsResyncItems = notifyOrderRepository
                .fetchCampaignItemsForBsResync(shard, campaignId, BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2);
        assertThat(bsResyncItems).isEmpty();
    }

    @Test
    public void fetchEmptyList_WhenGroupAndBannerStatusBsSyncedIsNo() {
        steps.bannerSteps().createBanner(activeTextBanner(campaignId, adGroupInfo.getAdGroupId()), adGroupInfo);
        bannerCommonRepository.updateStatusBsSyncedByCampaignIds(shard, singletonList(campaignId),
                StatusBsSynced.NO);
        adGroupRepository.updateStatusBsSynced(shard, singletonList(adGroupInfo.getAdGroupId()), StatusBsSynced.NO);

        List<BsResyncItem> bsResyncItems = notifyOrderRepository
                .fetchCampaignItemsForBsResync(shard, campaignId, BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2);
        assertThat(bsResyncItems).isEmpty();
    }

    @Test
    public void fetchNotEmptyList_WhenGroupStatusBsSyncedIsYes() {
        steps.bannerSteps().createBanner(activeTextBanner(campaignId, adGroupInfo.getAdGroupId()), adGroupInfo);
        bannerCommonRepository.updateStatusBsSyncedByCampaignIds(shard, singletonList(campaignId),
                StatusBsSynced.NO);
        adGroupRepository.updateStatusBsSynced(shard, singletonList(adGroupInfo.getAdGroupId()), StatusBsSynced.YES);

        List<BsResyncItem> bsResyncItems = notifyOrderRepository
                .fetchCampaignItemsForBsResync(shard, campaignId, BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2);
        assertThat(bsResyncItems).isNotEmpty();
    }

    @Test
    public void fetchNotEmptyList_WhenBannerStatusBsSyncedIsYes() {
        steps.bannerSteps().createBanner(activeTextBanner(campaignId, adGroupInfo.getAdGroupId()), adGroupInfo);
        bannerCommonRepository.updateStatusBsSyncedByCampaignIds(shard, singletonList(campaignId),
                StatusBsSynced.YES);
        adGroupRepository.updateStatusBsSynced(shard, singletonList(adGroupInfo.getAdGroupId()), StatusBsSynced.NO);

        List<BsResyncItem> bsResyncItems = notifyOrderRepository
                .fetchCampaignItemsForBsResync(shard, campaignId, BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2);
        assertThat(bsResyncItems).isNotEmpty();
    }

    @Test
    public void checkFetchedBsResyncItemParams() {
        TextBannerInfo banner = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignId, adGroupInfo.getAdGroupId()), adGroupInfo);

        List<BsResyncItem> bsResyncItems = notifyOrderRepository
                .fetchCampaignItemsForBsResync(shard, campaignId, BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2);

        BsResyncItem expectedItem =
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2.value(), campaignId,
                        banner.getBannerId(), banner.getAdGroupId());
        assertThat(bsResyncItems).is(matchedBy(beanDiffer(singletonList(expectedItem))));
    }

    @Test
    public void checkFetchedBsResyncItemParams_ForTwoBanners() {
        TextBannerInfo banner = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignId, adGroupInfo.getAdGroupId()), adGroupInfo);
        TextBannerInfo banner2 = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignId, adGroupInfo.getAdGroupId()), adGroupInfo);

        List<BsResyncItem> bsResyncItems = notifyOrderRepository
                .fetchCampaignItemsForBsResync(shard, campaignId, BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2);

        bsResyncItems.sort(Comparator.comparing(BsResyncItem::getBannerId));
        List<BsResyncItem> expectedBsResyncItems = Arrays.asList(
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2.value(), campaignId,
                        banner.getBannerId(), adGroupInfo.getAdGroupId()),
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2.value(), campaignId,
                        banner2.getBannerId(), adGroupInfo.getAdGroupId())
        );
        assertThat(bsResyncItems).is(matchedBy(beanDiffer(expectedBsResyncItems)));
    }
}
