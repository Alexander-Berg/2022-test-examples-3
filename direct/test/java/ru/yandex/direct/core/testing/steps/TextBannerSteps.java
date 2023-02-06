package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.utils.CommonUtils.nvl;

public class TextBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private VcardSteps vcardSteps;

    @Autowired
    private SitelinkSetSteps sitelinkSetSteps;

    @Autowired
    private BannerImageFormatSteps bannerImageFormatSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    public NewTextBannerInfo createDefaultTextBanner() {
        return createBanner(new NewTextBannerInfo());
    }

    public NewTextBannerInfo createBanner(NewTextBannerInfo bannerInfo) {
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

        // если баннер не задан, устанавливаем дефолтный
        if (bannerInfo.getBanner() == null) {
            bannerInfo.withBanner(fullTextBanner());
        }

        //создаем необязательные связанные сущности, такие как визитка, сайтлинк
        createRelatedEntities(bannerInfo);

        // дозаполняем поля баннера перед сохранением
        TextBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private void createRelatedEntities(NewTextBannerInfo bannerInfo) {
        createVcard(bannerInfo);
        createSitelinkSet(bannerInfo);
        createBannerImageFormat(bannerInfo);
    }

    private void createVcard(NewTextBannerInfo bannerInfo) {
        if (bannerInfo.getVcardInfo() == null) {
            return;
        }
        var vcardInfo = bannerInfo.getVcardInfo()
                .withCampaignInfo(bannerInfo.getCampaignInfo())
                .withClientInfo(bannerInfo.getClientInfo());
        if (bannerInfo.getVcardInfo().getVcardId() == null) {
            vcardSteps.createVcard(vcardInfo);
        }
        ((TextBanner) bannerInfo.getBanner()).withVcardId(vcardInfo.getVcardId());
    }

    private void createSitelinkSet(NewTextBannerInfo bannerInfo) {
        if (bannerInfo.getSitelinkSetInfo() == null) {
            return;
        }
        var sitelinkSetInfo = bannerInfo.getSitelinkSetInfo()
                .withClientInfo(bannerInfo.getClientInfo());
        if (bannerInfo.getSitelinkSetInfo().getSitelinkSetId() == null) {
            sitelinkSetSteps.createSitelinkSet(sitelinkSetInfo);
        }
        ((TextBanner) bannerInfo.getBanner()).withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId());
    }

    private void createBannerImageFormat(NewTextBannerInfo bannerInfo) {
        if (bannerInfo.getBannerImageFormatInfo() == null) {
            return;
        }
        var bannerImageFormatInfo = bannerInfo.getBannerImageFormatInfo()
                .withClientInfo(bannerInfo.getClientInfo());
        if (bannerInfo.getBannerImageFormatInfo() != null) {
            bannerImageFormatSteps.createBannerImageFormat(bannerImageFormatInfo);
            TextBanner banner = bannerInfo.getBanner();
            banner.setImageStatusShow(nvl(banner.getImageStatusShow(), true));
            banner.setImageStatusModerate(nvl(banner.getImageStatusModerate(), StatusBannerImageModerate.YES));
            banner.setImageDateAdded(nvl(banner.getImageDateAdded(), LocalDateTime.now()));
            banner.setImageBsBannerId(nvl(banner.getImageBsBannerId(), 37L));
        }
        ((TextBanner) bannerInfo.getBanner()).withImageHash(bannerImageFormatInfo.getImageHash());
    }

    private static void checkBannerInfoConsistency(NewTextBannerInfo bannerInfo) {
        var adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        var banner = bannerInfo.getBanner();
        var sitelinkSetInfo = bannerInfo.getSitelinkSetInfo();
        var vcardInfo = bannerInfo.getVcardInfo();

        if (adGroup != null) {
            checkState(adGroup instanceof TextAdGroup, "adGroupType must be TEXT");
        }
        if (banner != null) {
            checkState(banner instanceof TextBanner,
                    "banner must be NewTextBanner");
        }
        if (sitelinkSetInfo != null) {
            checkState(sitelinkSetInfo.getClientInfo() == null
                            || sitelinkSetInfo.getClientInfo() == bannerInfo.getClientInfo(),
                    "sitelinkSetInfo clientInfo must be null or the same as bannerInfo");
        }
        if (vcardInfo != null) {
            checkState(vcardInfo.getClientInfo() == null
                            || vcardInfo.getClientInfo() == bannerInfo.getClientInfo(),
                    "vcardInfo clientInfo must be null or the same as bannerInfo");
            checkState(vcardInfo.getCampaignInfo() == null
                            || vcardInfo.getCampaignInfo() == bannerInfo.getCampaignInfo(),
                    "vcardInfo getCampaignInfo must be null or the same as bannerInfo");
        }
    }
}
