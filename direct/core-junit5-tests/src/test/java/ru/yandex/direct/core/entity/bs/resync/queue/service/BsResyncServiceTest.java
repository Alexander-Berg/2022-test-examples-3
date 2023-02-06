package ru.yandex.direct.core.entity.bs.resync.queue.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncQueueInfo;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.utils.FunctionalUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@CoreTest
@ExtendWith(SpringExtension.class)
class BsResyncServiceTest {

    private Long campaignId;
    private Long emptyCampaignId;
    private TextBannerInfo bannerInfo;

    @Autowired
    private BsResyncService bsResyncService;

    @Autowired
    private BsResyncQueueRepository bsResyncQueueRepository;

    @Autowired
    private Steps steps;

    @BeforeEach
    void createTestData() {
        CampaignInfo campaignInfo = new CampaignInfo()
                .withCampaign(activeTextCampaign(null, null));
        campaignInfo.getClientInfo().withShard(2);  //чтобы в тесте были кампании из разных шардов
        steps.campaignSteps().createCampaign(campaignInfo);
        AdGroupInfo defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignId, defaultAdGroup.getAdGroupId()), defaultAdGroup);
        steps.bannerSteps().createBanner(activeTextBanner(campaignId, defaultAdGroup.getAdGroupId()), defaultAdGroup);

        campaignId = campaignInfo.getCampaignId();
        emptyCampaignId = steps.campaignSteps().createDefaultCampaign().getCampaignId();
    }

    @Test
    void checkAddWholeCampaignsToResync() {
        Long notExistCampaignId = Long.MAX_VALUE;

        Map<Long, Long> result = bsResyncService.addWholeCampaignsToResync(
                ImmutableList.of(campaignId, notExistCampaignId, emptyCampaignId),
                BsResyncPriority.INTERNAL_REPORTS_LAZY_RESYNC
        );

        Map<Long, Long> expectedResult = ImmutableMap.of(
                campaignId, 3L,
                emptyCampaignId, 1L,
                notExistCampaignId, 0L
        );
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void checkAddObjectsToResync() {
        Long notExistCampaignId = Long.MAX_VALUE;
        List<BsResyncItem> bsResyncItems = Arrays.asList(
                new BsResyncItem(BsResyncPriority.DEFAULT, notExistCampaignId),
                new BsResyncItem(BsResyncPriority.DEFAULT, emptyCampaignId),
                new BsResyncItem(BsResyncPriority.DEFAULT, campaignId),
                new BsResyncItem(BsResyncPriority.INTERNAL_REPORTS_FAST_RESYNC.value(), campaignId,
                        bannerInfo.getAdGroupId(), null),
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2.value(), campaignId,
                        bannerInfo.getAdGroupId(), bannerInfo.getBannerId())
        );

        long result = bsResyncService.addObjectsToResync(bsResyncItems);

        long expectedResult = bsResyncItems.size() - 1; //минус notExistCampaignId
        assertThat(result).isEqualTo(expectedResult);

        //Проверяем, что записи появились в базе
        List<BsResyncQueueInfo> bsResyncQueueInfoList = bsResyncQueueRepository
                .getCampaignItemsFromQueue(bannerInfo.getShard(), Collections.singletonList(campaignId));
        List<BsResyncItem> campaignItemsFromQueue = FunctionalUtils.mapList(bsResyncQueueInfoList, BsResyncItem::new);

        Comparator<BsResyncItem> comparator = Comparator.comparingLong(BsResyncItem::getPriority);
        campaignItemsFromQueue.sort(comparator);
        List<BsResyncItem> expectedItemsFromQueue = bsResyncItems.stream()
                .filter(item -> item.getCampaignId().equals(campaignId))
                .collect(Collectors.toList());

        assertThat(campaignItemsFromQueue).usingFieldByFieldElementComparator().isEqualTo(expectedItemsFromQueue);
    }
}
