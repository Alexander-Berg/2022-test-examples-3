package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoPinAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.info.NewCpmGeoPinBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoPinAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmGeoPinBanners.fullCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;

public class CpmGeoPinBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private OrganizationsSteps organizationsSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewCpmGeoPinBannerInfo createDefaultCpmGeoPinBanner() {
        return createCpmGeoPinBanner(new NewCpmGeoPinBannerInfo());
    }

    public NewCpmGeoPinBannerInfo createCpmGeoPinBanner(NewCpmGeoPinBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeCpmGeoPinAdGroup(null));
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

        // если организация не задана, задаем ее
        if (bannerInfo.getOrganization() == null) {
            bannerInfo.withOrganization(defaultOrganization(bannerInfo.getClientInfo().getClientId()));

            organizationsSteps.createClientOrganization(
                    bannerInfo.getClientInfo().getClientId(), bannerInfo.getOrganization().getPermalinkId());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullCpmGeoPinBanner(
                    bannerInfo.getCreative().getId(),
                    bannerInfo.getOrganization().getPermalinkId()));
        }

        // дозаполняем поля баннера перед сохранением
        CpmGeoPinBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withCreativeId(bannerInfo.getCreative().getId())
                .withPermalinkId(bannerInfo.getOrganization().getPermalinkId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewCpmGeoPinBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Creative creative = bannerInfo.getCreative();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof CpmGeoPinAdGroup, "adGroupType must be CpmGeoPinAdGroup");
        }
        if (creative != null) {
            checkState(creative.getType().equals(CreativeType.CANVAS),
                    "creativeType must be CANVAS");
        }
        if (banner != null) {
            checkState(banner instanceof CpmGeoPinBanner,
                    "banner must be NewCpmGeoPinBanner");
        }
    }
}
