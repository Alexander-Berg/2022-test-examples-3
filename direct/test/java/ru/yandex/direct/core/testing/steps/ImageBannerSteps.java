package ru.yandex.direct.core.testing.steps;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.NewImageBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.fullImageBannerWithCreative;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.fullImageBannerWithImage;

@ParametersAreNonnullByDefault
public class ImageBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewImageBannerInfo createImageBannerWithImage() {
        return createImageBanner(new NewImageBannerInfo());
    }

    public NewImageBannerInfo createImageBannerWithCreative() {
        return createImageBanner(new NewImageBannerInfo()
                .withCreative(defaultCanvas(null, null)));
    }

    public NewImageBannerInfo createImageBanner(ImageBanner banner, AdGroupInfo adGroupInfo) {
        return createImageBanner(new NewImageBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo)
                .withClientInfo(adGroupInfo.getClientInfo())
                .withCampaignInfo(adGroupInfo.getCampaignInfo()));
    }

    public NewImageBannerInfo createImageBanner(NewImageBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // если клиент не создан, создаем его
        if (bannerInfo.getClientId() == null) {
            if (bannerInfo.getClientInfo().getClient() == null) {
                bannerInfo.getClientInfo()
                        .withClient(defaultClient());
            }
            clientSteps.createClient(bannerInfo.getClientInfo());
        }

        // если группа не создана, создаем её
        // (степ создания группы создаст кампанию и клиента при необходимости)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                bannerInfo.getAdGroupInfo()
                        .withAdGroup(activeTextAdGroup());
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // если креатив задан, но не записан в базу, то записываем его (в него проставляется id)
        if (bannerInfo.getCreative() != null
                && bannerInfo.getCreative().getId() == null) {
            creativeSteps.createCreative(bannerInfo.getCreative(), bannerInfo.getClientInfo());
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            // по дефолтну создаём картинку если не заданы ни картинка ни креатив
            if (bannerInfo.getCreative() == null) {
                if (bannerInfo.getImageFormat() == null) {
                    BannerImageFormat imageAdImageFormat = steps.bannerSteps().createImageAdImageFormat(
                            bannerInfo.getClientInfo());
                    bannerInfo.withImageFormat(imageAdImageFormat);
                }
                bannerInfo.withBanner(
                        fullImageBannerWithImage(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId(),
                                bannerInfo.getImageFormat().getImageHash()));
            } else {
                bannerInfo.withBanner(
                        fullImageBannerWithCreative(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId(),
                                bannerInfo.getCreative().getId()));
            }
        }

        // дозаполняем поля баннера перед сохранением
        ImageBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId());
        if (bannerInfo.getCreative() != null) {
            banner.withCreativeId(bannerInfo.getCreative().getId());
        } else if (bannerInfo.getImageFormat() != null) {
            banner.withImageHash(bannerInfo.getImageFormat().getImageHash());
            if (banner.getImageStatusModerate() == null) {
                banner.withImageStatusModerate(NewStatusImageModerate.YES);
            }
        }

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private void checkBannerInfoConsistency(NewImageBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Creative creative = bannerInfo.getCreative();
        BannerImageFormat imageFormat = bannerInfo.getImageFormat();

        Set<AdGroupType> supportedAdGroupTypes = Set.of(AdGroupType.BASE, AdGroupType.MOBILE_CONTENT);
        if (adGroup != null) {
            checkState(supportedAdGroupTypes.contains(adGroup.getType()),
                    "adGroupType must be TextAdGroup or MobileContentAdGroup");
        }
        if (bannerInfo.getBanner() != null) {
            Banner banner = bannerInfo.getBanner();
            checkState(banner instanceof ImageBanner, "banner must be NewImageBanner");
            ImageBanner imageBanner = (ImageBanner) banner;
            if (imageBanner.getCreativeId() != null) {
                checkState(creative != null,
                        "banner.creativeId must be null or creative must be set");
                checkState(imageBanner.getCreativeId().equals(creative.getId()),
                        "banner.creativeId must be null or equal to creative.id");
                checkState(imageBanner.getImageHash() == null,
                        "Either banner imageHash or banner creativeId must be null.");
            }
        }
        if (creative != null) {
            checkState(imageFormat == null, "Either creative or imageFormat must be null.");
        }
    }

}
