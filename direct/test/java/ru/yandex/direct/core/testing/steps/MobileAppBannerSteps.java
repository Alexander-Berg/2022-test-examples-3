package ru.yandex.direct.core.testing.steps;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileAppBanner;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@ParametersAreNonnullByDefault
public class MobileAppBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    public NewMobileAppBannerInfo createDefaultMobileAppBanner() {
        return createMobileAppBanner(new NewMobileAppBannerInfo());
    }

    public NewMobileAppBannerInfo createDefaultMobileAppBanner(ClientInfo clientInfo, AdGroupInfo adGroupInfo) {
        return createMobileAppBanner(new NewMobileAppBannerInfo()
                .withClientInfo(clientInfo)
                .withAdGroupInfo(adGroupInfo));
    }

    public NewMobileAppBannerInfo createMobileAppBanner(NewMobileAppBannerInfo bannerInfo) {
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
                        .withAdGroup(activeMobileAppAdGroup(null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // по умолчанию создаём без картинки, не заполняем imageFormat

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullMobileAppBanner(bannerInfo.getCampaignId(),
                    bannerInfo.getAdGroupId()));
        }

        // дозаполняем поля баннера перед сохранением
        MobileAppBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withImageHash(ifNotNull(bannerInfo.getImageFormat(), BannerImageFormat::getImageHash));

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewMobileAppBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof MobileContentAdGroup, "adGroupType must be PERFORMANCE");
        }
        if (banner != null) {
            checkState(banner instanceof MobileAppBanner, "banner must be NewMobileAppBanner");
        }
    }

}
