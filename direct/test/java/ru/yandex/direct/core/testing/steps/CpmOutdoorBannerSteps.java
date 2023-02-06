package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.info.NewCpmOutdoorBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmOutdoorBanners.fullCpmOutdoorBanner;

public class CpmOutdoorBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewCpmOutdoorBannerInfo createDefaultCpmOutdoorBanner() {
        return createCpmOutdoorBanner(new NewCpmOutdoorBannerInfo());
    }

    public NewCpmOutdoorBannerInfo createCpmOutdoorBanner(NewCpmOutdoorBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeCpmOutdoorAdGroup(null, null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если креатив не задан, задаем его
        if (bannerInfo.getCreative() == null) {
            bannerInfo.withCreative(defaultCpmOutdoorVideoAddition(null, null));
        }

        // если креатив задан, но не записан в базу, то записываем его (в него проставляется id)
        if (bannerInfo.getCreative() != null && bannerInfo.getCreative().getId() == null) {
            creativeSteps.createCreative(bannerInfo.getCreative(), bannerInfo.getClientInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullCpmOutdoorBanner(bannerInfo.getCreative().getId()));
        }

        // дозаполняем поля баннера перед сохранением
        CpmOutdoorBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withCreativeId(bannerInfo.getCreative().getId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewCpmOutdoorBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Creative creative = bannerInfo.getCreative();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof CpmOutdoorAdGroup, "adGroupType must be CPM_OUTDOOR");
        }
        if (creative != null) {
            checkState(creative.getType().equals(CreativeType.CPM_OUTDOOR_CREATIVE),
                    "creativeType must be CPM_OUTDOOR_CREATIVE");
        }
        if (banner != null) {
            checkState(banner instanceof CpmOutdoorBanner,
                    "banner must be NewCpmOutdoorBanner");
        }
    }
}
