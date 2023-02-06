package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.CpmAudioBannerInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CpmGeoPinBannerInfo;
import ru.yandex.direct.core.testing.info.CpmIndoorBannerInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.ImageCreativeBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;

import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmAudioBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoPinAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;

public class BannerCreativeSteps {

    @Autowired
    private Steps steps;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private TestBannerCreativeRepository testBannerCreativeRepository;

    public BannerCreativeInfo<OldTextBanner> createDefaultTextBannerCreative() {
        return createTextBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldTextBanner> createTextBannerCreative(ClientInfo clientInfo) {
        return createTextBannerCreative(new BannerCreativeInfo<OldTextBanner>()
                .withBannerInfo(new TextBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldImageCreativeBanner> createDefaultImageBannerCreative() {
        return createImageBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldImageCreativeBanner> createImageBannerCreative(ClientInfo clientInfo) {
        return createImageBannerCreative(new BannerCreativeInfo<OldImageCreativeBanner>()
                .withBannerInfo(new ImageCreativeBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldImageCreativeBanner> createImageBannerHtml5Creative(ClientInfo clientInfo) {
        return createImageBannerHtml5Creative(new BannerCreativeInfo<OldImageCreativeBanner>()
                .withBannerInfo(new ImageCreativeBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpmBanner> createDefaultCpmBannerCreative() {
        return createCpmBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldCpmBanner> createCpmBannerCreative(ClientInfo clientInfo) {
        return createCpmBannerCreative(new BannerCreativeInfo<OldCpmBanner>()
                .withBannerInfo(new CpmBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpmBanner> createCpmVideoCreative(ClientInfo clientInfo) {
        return createCpmBannerCreative(new BannerCreativeInfo<OldCpmBanner>()
                .withBannerInfo(new CpmBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpmOutdoorBanner> createDefaultCpmOutdoorBannerCreative() {
        return createCpmOutdoorBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldCpmOutdoorBanner> createDefaultDraftCpmOutdoorBannerCreative() {
        BannerCreativeInfo<OldCpmOutdoorBanner> bannerCreativeInfo = new BannerCreativeInfo<>();
        CpmOutdoorBannerInfo bannerInfo = new CpmOutdoorBannerInfo();
        bannerCreativeInfo.withBannerInfo(bannerInfo);
        CreativeInfo creativeInfo = new CreativeInfo();
        bannerCreativeInfo.withCreativeInfo(creativeInfo);

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(null, null, null).withStatusModerate(OldBannerStatusModerate.NEW);
        bannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, clientInfo);

        Creative creative = defaultCpmOutdoorVideoAddition(clientInfo.getClientId(), null);
        creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        return link(bannerInfo, creativeInfo);
    }

    public BannerCreativeInfo<OldCpmIndoorBanner> createDefaultCpmIndoorBannerCreative() {
        return createCpmIndoorBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldCpmAudioBanner> createDefaultCpmAudioBannerCreative() {
        return createCpmAudioBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldCpmAudioBanner> createCpmAudioBannerCreative(ClientInfo clientInfo) {
        return createCpmAudioBannerCreative(new BannerCreativeInfo<OldCpmAudioBanner>()
                .withBannerInfo(new CpmAudioBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpmOutdoorBanner> createCpmOutdoorBannerCreative(ClientInfo clientInfo) {
        return createCpmOutdoorBannerCreative(new BannerCreativeInfo<OldCpmOutdoorBanner>()
                .withBannerInfo(new CpmOutdoorBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpmIndoorBanner> createCpmIndoorBannerCreative(ClientInfo clientInfo) {
        return createCpmIndoorBannerCreative(new BannerCreativeInfo<OldCpmIndoorBanner>()
                .withBannerInfo(new CpmIndoorBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpmGeoPinBanner> createDefaultCpmGeoPinBannerCreative() {
        return createCpmGeoPinBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldCpcVideoBanner> createDefaultCpcVideoBannerCreative() {
        return createCpcVideoBannerCreative(new BannerCreativeInfo<>());
    }

    public BannerCreativeInfo<OldCpcVideoBanner> createCpcVideoBannerCreative(ClientInfo clientInfo) {
        return createCpcVideoBannerCreative(new BannerCreativeInfo<OldCpcVideoBanner>()
                .withBannerInfo(new CpcVideoBannerInfo()
                        .withClientInfo(clientInfo)));
    }

    public BannerCreativeInfo<OldCpcVideoBanner> createCpcVideoBannerCreative(Creative creative, ClientInfo clientInfo) {
        return createCpcVideoBannerCreative(new BannerCreativeInfo<OldCpcVideoBanner>()
                .withBannerInfo(new CpcVideoBannerInfo()
                        .withClientInfo(clientInfo))
                .withCreativeInfo(new CreativeInfo().withCreative(creative)));
    }

    public BannerCreativeInfo<OldCpcVideoBanner> createCpcVideoBannerCreative(
            Creative creative,
            AbstractBannerInfo<OldCpcVideoBanner> bannerInfo) {
        return createCpcVideoBannerCreative(new BannerCreativeInfo<OldCpcVideoBanner>()
                .withBannerInfo(bannerInfo)
                .withCreativeInfo(new CreativeInfo().withCreative(creative)));
    }

    /**
     * Используется для создания баннера со связанным видеокреативом. Валидным типом баннера является BannerType.TEXT.
     */
    public BannerCreativeInfo<OldTextBanner> createTextBannerCreative(BannerCreativeInfo<OldTextBanner> bannerCreativeInfo) {
        TextBannerInfo bannerInfo = (TextBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new TextBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldTextBanner banner = activeTextBanner(null, null);
            bannerInfo = steps.bannerSteps().createBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultVideoAddition(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldImageCreativeBanner> createImageBannerCreative(
            BannerCreativeInfo<OldImageCreativeBanner> bannerCreativeInfo) {
        ImageCreativeBannerInfo bannerInfo = (ImageCreativeBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new ImageCreativeBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldImageCreativeBanner banner = activeImageCreativeBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveImageCreativeBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCanvas(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldImageCreativeBanner> createImageBannerHtml5Creative(
            BannerCreativeInfo<OldImageCreativeBanner> bannerCreativeInfo) {
        ImageCreativeBannerInfo bannerInfo = (ImageCreativeBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new ImageCreativeBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldImageCreativeBanner banner = activeImageCreativeBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveImageCreativeBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultHtml5(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldCpmBanner> createCpmBannerCreative(BannerCreativeInfo<OldCpmBanner> bannerCreativeInfo) {
        CpmBannerInfo bannerInfo = (CpmBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new CpmBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldCpmBanner banner = activeCpmBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCanvas(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldCpcVideoBanner> createCpcVideoBannerCreative(
            BannerCreativeInfo<OldCpcVideoBanner> bannerCreativeInfo) {
        CpcVideoBannerInfo bannerInfo = (CpcVideoBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new CpcVideoBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldCpcVideoBanner banner = activeCpcVideoBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCpcVideoForCpcVideoBanner(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldCpmOutdoorBanner> createCpmOutdoorBannerCreative(
            BannerCreativeInfo<OldCpmOutdoorBanner> bannerCreativeInfo) {
        CpmOutdoorBannerInfo bannerInfo = (CpmOutdoorBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new CpmOutdoorBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldCpmOutdoorBanner banner = activeCpmOutdoorBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCpmOutdoorVideoAddition(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldCpmIndoorBanner> createCpmIndoorBannerCreative(
            BannerCreativeInfo<OldCpmIndoorBanner> bannerCreativeInfo) {
        CpmIndoorBannerInfo bannerInfo = (CpmIndoorBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new CpmIndoorBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldCpmIndoorBanner banner = activeCpmIndoorBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveCpmIndoorBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCpmIndoorVideoAddition(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldCpmAudioBanner> createCpmAudioBannerCreative(
            BannerCreativeInfo<OldCpmAudioBanner> bannerCreativeInfo) {
        CpmAudioBannerInfo bannerInfo = (CpmAudioBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new CpmAudioBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldCpmAudioBanner banner = activeCpmAudioBanner(null, null, null);
            bannerInfo = steps.bannerSteps().createActiveCpmAudioBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCpmAudioAddition(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private BannerCreativeInfo<OldCpmGeoPinBanner> createCpmGeoPinBannerCreative(
            BannerCreativeInfo<OldCpmGeoPinBanner> bannerCreativeInfo) {
        CpmGeoPinBannerInfo bannerInfo = (CpmGeoPinBannerInfo) bannerCreativeInfo.getBannerInfo();
        if (bannerInfo == null) {
            bannerInfo = new CpmGeoPinBannerInfo();
            bannerCreativeInfo.withBannerInfo(bannerInfo);
        }

        CreativeInfo creativeInfo = bannerCreativeInfo.getCreativeInfo();
        if (creativeInfo == null) {
            creativeInfo = new CreativeInfo();
            bannerCreativeInfo.withCreativeInfo(creativeInfo);
        }

        ClientInfo clientInfo = getOrCreateClientInfo(bannerCreativeInfo, bannerInfo);

        if (bannerInfo.getBanner() == null) {
            OldCpmGeoPinBanner banner = activeCpmGeoPinBanner(null, null, null, null);
            bannerInfo = steps.bannerSteps().createActiveCpmGeoPinBanner(banner, clientInfo);
        }
        if (creativeInfo.getCreative() == null) {
            Creative creative = defaultCpmGeoPinAddition(clientInfo.getClientId(), null);
            creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        }

        return link(bannerInfo, creativeInfo);
    }

    private ClientInfo getOrCreateClientInfo(BannerCreativeInfo<?> bannerCreativeInfo,
                                             AbstractBannerInfo<?> bannerInfo) {
        if (bannerCreativeInfo.getClientInfo() != null) {
            return bannerCreativeInfo.getClientInfo();
        } else if (bannerInfo.getClientInfo() != null) {
            return bannerInfo.getClientInfo();
        } else {
            return clientSteps.createDefaultClient();
        }
    }

    private <B extends OldBannerWithCreative> BannerCreativeInfo<B> link(AbstractBannerInfo<B> bannerInfo,
                                                                         CreativeInfo creativeInfo) {
        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creativeInfo.getCreativeId());

        return new BannerCreativeInfo<B>()
                .withBannerInfo(bannerInfo)
                .withCreativeInfo(creativeInfo);
    }

    public BannerCreativeInfo<OldPerformanceBanner> createPerformanceBannerCreative(PerformanceAdGroupInfo adGroupInfo) {
        Creative creative = defaultPerformanceCreative(adGroupInfo.getClientId(), null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());
        OldPerformanceBanner banner = activePerformanceBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                creativeInfo.getCreativeId());
        steps.bannerSteps().createBanner(new BannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo)
                .withCampaignInfo(adGroupInfo.getCampaignInfo())
                .withClientInfo(adGroupInfo.getClientInfo()));
        PerformanceBannerInfo performanceBannerInfo = new PerformanceBannerInfo()
                .withCampaignInfo(adGroupInfo.getCampaignInfo())
                .withClientInfo(adGroupInfo.getClientInfo())
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner);
        return new BannerCreativeInfo<OldPerformanceBanner>()
                .withBannerInfo(performanceBannerInfo)
                .withCreativeInfo(creativeInfo);
    }
}
