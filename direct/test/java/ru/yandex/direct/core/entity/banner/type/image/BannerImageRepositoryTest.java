package ru.yandex.direct.core.entity.banner.type.image;

import java.util.List;

import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerImageRepositoryTest {
    private static final Long TEXT_BANNER_ID = 555L;
    private static final Long DYNAMIC_BANNER_ID = 545L;
    private static final Long MOBILE_CONTEXT_BANNER_ID = 525L;
    private static final Long IMAGE_AD_BANNER_ID = 505L;

    @Autowired
    private BannerImageRepository bannerImageRepository;

    @Autowired
    private TestBannerImageRepository testBannerImageRepository;

    @Autowired
    private Steps steps;

    private int shard;
    private Long textBid;
    private Long textImageId;
    private Long dynamicBid;
    private Long dynamicImageId;
    private Long mobileContentBid;
    private Long mobileContentImageId;
    private Long imageAdBid;

    @Before
    public void before() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        var clientInfo = campaignInfo.getClientInfo();
        shard = campaignInfo.getShard();

        var bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo);
        var textBannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), campaignInfo);
        textBid = textBannerInfo.getBannerId();
        var textBannerImage = defaultBannerImage(textBid, bannerImageFormat.getImageHash())
                .withBsBannerId(TEXT_BANNER_ID);

        var dynamicBannerInfo = steps.bannerSteps().createBanner(activeDynamicBanner(), campaignInfo);
        dynamicBid = dynamicBannerInfo.getBannerId();
        var dynamicBannerImage = defaultBannerImage(dynamicBid, bannerImageFormat.getImageHash())
                .withBsBannerId(DYNAMIC_BANNER_ID);

        var mobileContentBannerInfo = steps.bannerSteps().createBanner(activeMobileAppBanner(), campaignInfo);
        mobileContentBid = mobileContentBannerInfo.getBannerId();
        var mobileContentBannerImage = defaultBannerImage(mobileContentBid, bannerImageFormat.getImageHash())
                .withBsBannerId(MOBILE_CONTEXT_BANNER_ID);

        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        var imageAdBannerInfo = steps.bannerSteps().createBanner(
                activeImageHashBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId()), campaignInfo);
        imageAdBid = imageAdBannerInfo.getBannerId();
        var imageAdBannerImage = defaultBannerImage(imageAdBid, bannerImageFormat.getImageHash())
                .withBsBannerId(IMAGE_AD_BANNER_ID);

        testBannerImageRepository.delete(shard, List.of(textBid, dynamicBid, mobileContentBid, imageAdBid));
        testBannerImageRepository.addBannerImages(shard,
                List.of(textBannerImage, dynamicBannerImage, mobileContentBannerImage, imageAdBannerImage));
        textImageId = textBannerImage.getId();
        dynamicImageId = dynamicBannerImage.getId();
        mobileContentImageId = mobileContentBannerImage.getId();
    }

    @Test
    public void getBannerImageIdsFromBids_CheckAllowedBannerTypesTest() {
        var bidToBannerImageIdBsData = bannerImageRepository.getBannerImageIdsFromBids(shard,
                List.of(textBid, dynamicBid, mobileContentBid, imageAdBid));

        var bidToBannerImageIdAndBannerId = EntryStream.of(bidToBannerImageIdBsData)
                .mapValues(data -> Pair.of(data.getImageId(), data.getBsImageBannerId()))
                .toMap();

        assertThat(bidToBannerImageIdAndBannerId.keySet())
                .containsExactlyInAnyOrder(textBid, dynamicBid, mobileContentBid);
        assertThat(bidToBannerImageIdAndBannerId.values())
                .containsExactlyInAnyOrder(
                        Pair.of(textImageId, TEXT_BANNER_ID),
                        Pair.of(dynamicImageId, DYNAMIC_BANNER_ID),
                        Pair.of(mobileContentImageId, MOBILE_CONTEXT_BANNER_ID));
    }
}
