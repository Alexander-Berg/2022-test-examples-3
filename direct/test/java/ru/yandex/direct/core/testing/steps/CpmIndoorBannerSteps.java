package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.info.NewCpmIndoorBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmIndoorBanners.fullCpmIndoorBanner;

public class CpmIndoorBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewCpmIndoorBannerInfo createDefaultCpmIndoorBanner() {
        return createCpmIndoorBanner(new NewCpmIndoorBannerInfo());
    }

    public NewCpmIndoorBannerInfo createCpmIndoorBanner(NewCpmIndoorBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeCpmIndoorAdGroup(null, null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если креатив не задан, задаем его
        if (bannerInfo.getCreative() == null) {
            bannerInfo.withCreative(defaultCpmIndoorVideoAddition(null, null));
        }

        // если креатив задан, но не записан в базу, то записываем его (в него проставляется id)
        if (bannerInfo.getCreative() != null && bannerInfo.getCreative().getId() == null) {
            creativeSteps.createCreative(bannerInfo.getCreative(), bannerInfo.getClientInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullCpmIndoorBanner(bannerInfo.getCreative().getId()));
        }

        // дозаполняем поля баннера перед сохранением
        CpmIndoorBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withCreativeId(bannerInfo.getCreative().getId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewCpmIndoorBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Creative creative = bannerInfo.getCreative();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof CpmIndoorAdGroup, "adGroupType must be CPM_INDOOR");
        }
        if (creative != null) {
            checkState(creative.getType().equals(CreativeType.CPM_INDOOR_CREATIVE),
                    "creativeType must be CPM_INDOOR_CREATIVE");
        }
        if (banner != null) {
            checkState(banner instanceof CpmIndoorBanner,
                    "banner must be NewCpmIndoorBanner");
        }
    }
}
