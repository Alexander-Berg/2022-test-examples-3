package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;

public class CpmBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewCpmBannerInfo createDefaultCpmBanner() {
        return createCpmBanner(new NewCpmBannerInfo());
    }

    public NewCpmBannerInfo createCpmBanner(NewCpmBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeCpmBannerAdGroup(null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если креатив не задан, задаем его
        if (bannerInfo.getCreative() == null) {
            bannerInfo.withCreative(defaultCanvas(null, null));
        }

        // если креатив задан, но не записан в базу, то записываем его (в него проставляется id)
        if (bannerInfo.getCreative() != null && bannerInfo.getCreative().getId() == null) {
            creativeSteps.createCreative(bannerInfo.getCreative(), bannerInfo.getClientInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullCpmBanner(bannerInfo.getCreative().getId()));
        }

        // дозаполняем поля баннера перед сохранением
        CpmBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withCreativeId(bannerInfo.getCreative().getId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    public NewCpmBannerInfo createCpmGeoproductBanner(NewCpmBannerInfo bannerInfo) {
        AdGroupInfo adGroupInfo = bannerInfo.getAdGroupInfo();
        AdGroup adGroup = adGroupInfo.getAdGroup();
        if (adGroup != null) {
            checkState(adGroup instanceof CpmGeoproductAdGroup, "adGroupType must be CpmGeoproductAdGroup");
        }

        if (bannerInfo.getAdGroupId() == null) {
            if (adGroupInfo.getAdGroup() == null) {
                adGroupInfo.withAdGroup(activeCpmGeoproductAdGroup(bannerInfo.getCampaignId()));
            }
            adGroupSteps.createAdGroup(adGroupInfo);
        }

        return createCpmBanner(bannerInfo);
    }

    private static void checkBannerInfoConsistency(NewCpmBannerInfo bannerInfo) {
        Creative creative = bannerInfo.getCreative();
        Banner banner = bannerInfo.getBanner();

        if (creative != null) {
            checkState(creative.getType().equals(CreativeType.CANVAS)
                            || creative.getType().equals(CreativeType.HTML5_CREATIVE),
                    "creativeType must be CANVAS or HTML5_CREATIVE");
        }
        if (banner != null) {
            checkState(banner instanceof CpmBanner,
                    "banner must be NewCpmBanner");
        }
    }
}
