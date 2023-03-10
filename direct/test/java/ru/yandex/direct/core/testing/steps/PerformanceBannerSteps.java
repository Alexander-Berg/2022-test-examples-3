package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;

public class PerformanceBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private FeedSteps feedSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewPerformanceBannerInfo createDefaultPerformanceBanner() {
        return createPerformanceBanner(new NewPerformanceBannerInfo());
    }

    public NewPerformanceBannerInfo createBanner(PerformanceBanner banner, AdGroupInfo adGroupInfo) {
        return createPerformanceBanner(new NewPerformanceBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public NewPerformanceBannerInfo createPerformanceBanner(AdGroupInfo adGroupInfo) {
        return createPerformanceBanner(new NewPerformanceBannerInfo()
                .withAdGroupInfo(adGroupInfo));
    }

    public NewPerformanceBannerInfo createPerformanceBanner(NewPerformanceBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // ???????? ???????????? ???? ????????????, ?????????????? ??????
        if (bannerInfo.getClientId() == null) {
            if (bannerInfo.getClientInfo().getClient() == null) {
                bannerInfo.getClientInfo()
                        .withClient(defaultClient());
            }
            clientSteps.createClient(bannerInfo.getClientInfo());
        }

        // ???????? ???????????? ???? ??????????????, ?????????????? ????
        // (???????? ???????????????? ???????????? ?????????????? ???????????????? ?? ?????????????? ?????? ??????????????????????????)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                FeedInfo feedInfo = feedSteps.createDefaultFeed(bannerInfo.getClientInfo());
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activePerformanceAdGroup(null, feedInfo.getFeedId()));
                if (bannerInfo.getAdGroupInfo() instanceof PerformanceAdGroupInfo) {
                    ((PerformanceAdGroupInfo) bannerInfo.getAdGroupInfo()).withFeedInfo(feedInfo);
                }
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // ???????? ???????????? ???? ??????????, ?????????????????????????? ??????????????????
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullPerformanceBanner(bannerInfo.getCampaignId(),
                    bannerInfo.getAdGroupId(), null));
        }

        PerformanceBanner banner = bannerInfo.getBanner();

        if (banner.getCreativeId() == null) {
            var creativeInfo = bannerInfo.getCreativeInfo();
            creativeInfo.withClientInfo(bannerInfo.getClientInfo());

            // ???????? ?????????????? ???? ??????????, ???????????? ??????
            if (creativeInfo.getCreative() == null) {
                creativeInfo.withCreative(defaultPerformanceCreative(null, null));
            }

            // ???????? ?????????????? ??????????, ???? ???? ?????????????? ?? ????????, ???? ???????????????????? ?????? (?? ???????? ?????????????????????????? id)
            if (creativeInfo.getCreative().getId() == null) {
                creativeSteps.createCreative(creativeInfo);
            }

            //?????????????????????? ?????????????? ?? ??????????????
            banner.withCreativeId(bannerInfo.getCreative().getId());
        }

        // ?????????????????????? ???????? ?????????????? ?????????? ??????????????????????
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewPerformanceBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        var creativeInfo = bannerInfo.getCreativeInfo();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof PerformanceAdGroup || adGroup instanceof TextAdGroup, "adGroupType must be PERFORMANCE or TEXT");
        }
        if (creativeInfo != null && creativeInfo.getCreative() != null) {
            checkState(bannerInfo.getCreative().getType().equals(CreativeType.PERFORMANCE),
                    "creativeType must be PERFORMANCE");
        }
        if (banner != null) {
            checkState(banner instanceof PerformanceBanner, "banner must be NewPerformanceBanner");
        }
    }
}
