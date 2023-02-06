package ru.yandex.direct.core.testing.steps;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.NewMcBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewMcBanners.fullMcBanner;

@ParametersAreNonnullByDefault
public class McBannerSteps {

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

    public NewMcBannerInfo createMcBanner() {
        return createMcBanner(new NewMcBannerInfo());
    }

    public NewMcBannerInfo createMcBanner(NewMcBannerInfo bannerInfo) {
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
                        .withAdGroup(activeMcBannerAdGroup(null));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        if (bannerInfo.getImageFormat() == null) {
            BannerImageFormat imageAdImageFormat = steps.bannerSteps().createBannerImageFormat(
                    bannerInfo.getClientInfo(),
                    TestBanners.defaultMcBannerImageFormat(null));
            bannerInfo.withImageFormat(imageAdImageFormat);
        }

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullMcBanner(bannerInfo.getCampaignId(), bannerInfo.getAdGroupId(),
                    bannerInfo.getImageFormat().getImageHash()));
        }

        // дозаполняем поля баннера перед сохранением
        McBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withImageHash(bannerInfo.getImageFormat().getImageHash());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewMcBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof McBannerAdGroup, "adGroupType must be McBannerAdGroup");
        }
        if (banner != null) {
            checkState(banner instanceof McBanner, "banner must be NewMcBanner");
        }
    }

}
