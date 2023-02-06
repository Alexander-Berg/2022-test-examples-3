package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.fullPerformanceMainBanner;

public class PerformanceMainBannerSteps {
    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private FeedSteps feedSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewPerformanceMainBannerInfo createDefaultPerformanceMainBanner() {
        return createPerformanceMainBanner(new NewPerformanceMainBannerInfo());
    }

    public NewPerformanceMainBannerInfo createPerformanceMainBanner(AdGroupInfo adGroupInfo) {
        return createPerformanceMainBanner(new NewPerformanceMainBannerInfo()
                .withAdGroupInfo(adGroupInfo));
    }

    public NewPerformanceMainBannerInfo createPerformanceMainBanner(NewPerformanceMainBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
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

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullPerformanceMainBanner());
        }

        // дозаполняем поля баннера перед сохранением
        PerformanceBannerMain banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewPerformanceMainBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof PerformanceAdGroup || adGroup instanceof TextAdGroup, "adGroup must be PerformanceAdGroup or TextAdGroup");
        }
        if (banner != null) {
            checkState(banner instanceof PerformanceBannerMain, "banner must be PerformanceBannerMain");
        }
    }
}
