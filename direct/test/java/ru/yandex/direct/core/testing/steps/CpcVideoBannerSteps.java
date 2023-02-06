package ru.yandex.direct.core.testing.steps;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.testing.data.TestNewBanners;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

public class CpcVideoBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    public NewCpcVideoBannerInfo createDefaultCpcVideoBanner() {
        return createBanner(new NewCpcVideoBannerInfo());
    }

    public NewCpcVideoBannerInfo createBanner(NewCpcVideoBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeTextAdGroup(null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если креатив не задан, задаем его
        if (bannerInfo.getCreative() == null) {
            bannerInfo.withCreative(defaultCpcVideoForCpcVideoBanner(null, null));
        }

        // если креатив задан, но не записан в базу, то записываем его (в него проставляется id)
        if (bannerInfo.getCreative() != null && bannerInfo.getCreative().getId() == null) {
            creativeSteps.createCreative(bannerInfo.getCreative(), bannerInfo.getClientInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullCpcVideoBanner(bannerInfo.getCreative().getId()));
        }

        // дозаполняем поля баннера перед сохранением
        CpcVideoBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withCreativeId(bannerInfo.getCreative().getId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());
        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewCpcVideoBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Creative creative = bannerInfo.getCreative();
        Banner banner = bannerInfo.getBanner();

        Set<AdGroupType> supportedAdGroupTypes = Set.of(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT);
        if (adGroup != null) {
            checkState(supportedAdGroupTypes.contains(adGroup.getType()),
                    "adGroupType must be TextAdGroup or MobileContentAdGroup");
        }
        if (creative != null) {
            checkState(creative.getType().equals(CreativeType.CPC_VIDEO_CREATIVE),
                    "creativeType must be CPC_VIDEO_CREATIVE");
        }
        if (banner != null) {
            checkState(banner instanceof CpcVideoBanner,
                    "banner must be NewCpcVideoBanner");
        }
    }

    public CpcVideoBanner fullCpcVideoBanner(Long creativeId) {
        CpcVideoBanner banner = new CpcVideoBanner();
        fillCpcVideoBannerClientFields(banner, creativeId);
        fillCpcVideoBannerSystemFields(banner);
        return banner;
    }

    private static void fillCpcVideoBannerClientFields(
            CpcVideoBanner banner, Long creativeId) {
        banner.withHref("https://www.yandex.ru")
                .withCreativeId(creativeId);
    }

    private void fillCpcVideoBannerSystemFields(CpcVideoBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(banner.getHref()))
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN)
                .withGeoFlag(false);
    }
}
