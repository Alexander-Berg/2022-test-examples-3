package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.fullDynamicBanner;

public class DynamicBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewDynamicBannerInfo createDefaultDynamicBanner() {
        return createDynamicBanner(new NewDynamicBannerInfo());
    }

    public NewDynamicBannerInfo createDynamicBanner(AdGroupInfo adGroupInfo) {
        return createDynamicBanner(new NewDynamicBannerInfo()
                .withAdGroupInfo(adGroupInfo));
    }

    public NewDynamicBannerInfo createDynamicBanner(NewDynamicBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeDynamicTextAdGroup(null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullDynamicBanner());
        }

        // дозаполняем поля баннера перед сохранением
        DynamicBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewDynamicBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof DynamicAdGroup || adGroup instanceof TextAdGroup,
                    "adGroupType must be DYNAMIC or TEXT");
        }
        if (banner != null) {
            checkState(banner instanceof DynamicBanner, "banner must be NewDynamicBanner");
        }
    }
}
