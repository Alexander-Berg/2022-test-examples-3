package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithLanguage;
import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.old.OldContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldContentPromotionVideoBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldInternalBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldMcBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldMobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.image.model.BannerImageFromPool;
import ru.yandex.direct.core.entity.image.model.BannerImageSource;
import ru.yandex.direct.core.entity.image.repository.BannerImagePoolRepository;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionVideoBannerInfo;
import ru.yandex.direct.core.testing.info.CpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.CpmAudioBannerInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CpmGeoPinBannerInfo;
import ru.yandex.direct.core.testing.info.CpmIndoorBannerInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.ImageCreativeBannerInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.ImageInfo;
import ru.yandex.direct.core.testing.info.InternalBannerInfo;
import ru.yandex.direct.core.testing.info.McBannerInfo;
import ru.yandex.direct.core.testing.info.MobileAppBannerInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestImageRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.repository.TestTurboLandingRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerCollectionType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerEdaType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerServiceType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerVideoType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeInternalBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMcBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.fillBannerImageDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillCommonDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillCpcVideoDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillCpmAudioBannerDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillCpmBannerDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillCpmGeoPinBannerDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillDynamicDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillImageCreativeDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestBanners.fillTextDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeContentPromotionVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmAudioAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoPinAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestImages.defaultImage;
import static ru.yandex.direct.core.testing.data.TestImages.fillImageDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;

public class BannerSteps {
    public static final String DEFAULT_IMAGE_NAME_TEMPLATE = "image_name_%s.jpeg";

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private TestBannerImageRepository testBannerImageRepository;

    @Autowired
    private TestImageRepository testImageRepository;

    @Autowired
    private BannerImagePoolRepository bannerImagePoolRepository;

    @Autowired
    private TestBannerImageFormatRepository testBannerImageFormatRepository;

    @Autowired
    private TestTurboLandingRepository testTurboLandingRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    public AbstractBannerInfo createActiveBannerByCampaignType(CampaignType campaignType, CampaignInfo campaignInfo) {
        switch (campaignType) {
            case TEXT:
                return createActiveTextBanner(campaignInfo);
            case DYNAMIC:
                return createActiveDynamicBanner(campaignInfo);
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании");
        }
    }

    public TextBannerInfo createActiveTextBanner() {
        OldTextBanner banner = activeTextBanner(null, null);
        return createActiveTextBanner(banner);
    }

    public TextBannerInfo createActiveTextBanner(CampaignInfo campaignInfo) {
        OldTextBanner banner = activeTextBanner(null, null);
        fillTextDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withBanner(banner)
                .withCampaignInfo(campaignInfo));
    }

    public TextBannerInfo createActiveTextBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = activeTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        fillTextDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner)
                .withCampaignInfo(adGroupInfo.getCampaignInfo()));
    }

    public TextBannerInfo createActiveTextBanner(AdGroupInfo adGroupInfo, String title, String titleExtension, String body, String href) {
        OldTextBanner banner = activeTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        fillTextDefaultSystemFields(banner);
        if (title != null) {
            banner.setTitle(title);
        }
        if (titleExtension != null) {
            banner.setTitleExtension(titleExtension);
        }
        if (body != null) {
            banner.setBody(body);
        }
        if (href != null) {
            banner.setHref(href);
        }
        return createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner)
                .withCampaignInfo(adGroupInfo.getCampaignInfo()));
    }

    public TextBannerInfo createActiveTextBanner(OldTextBanner banner) {
        var activeBanner = activeTextBanner(banner);
        fillTextDefaultSystemFields(activeBanner);
        return createBannerInActiveTextAdGroup(new TextBannerInfo().withBanner(activeBanner));
    }

    public DynamicBannerInfo createActiveDynamicBanner() {
        return createActiveDynamicBanner(new AdGroupInfo()
                .withAdGroup(activeDynamicTextAdGroup(null))
                .withCampaignInfo(new CampaignInfo().withCampaign(activeDynamicCampaign(null, null))));
    }

    public DynamicBannerInfo createActiveDynamicBanner(CampaignInfo campaignInfo) {
        OldDynamicBanner banner = activeDynamicBanner(null, null);
        fillDynamicDefaultSystemFields(banner);
        DynamicBannerInfo bannerInfo = createBannerInActiveDynamicAdGroup(new DynamicBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withAdGroup(activeDynamicTextAdGroup(null))
                        .withCampaignInfo(campaignInfo)));
        createBannerImage(bannerInfo);
        return bannerInfo;
    }

    public DynamicBannerInfo createActiveDynamicBanner(AdGroupInfo adGroupInfo) {
        OldDynamicBanner banner = activeDynamicBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        fillDynamicDefaultSystemFields(banner);
        DynamicBannerInfo bannerInfo = createBanner(new DynamicBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
        createBannerImage(bannerInfo);
        return bannerInfo;
    }

    public DynamicBannerInfo createActiveDynamicBanner(OldDynamicBanner banner, AdGroupInfo adGroupInfo) {
        fillDynamicDefaultSystemFields(banner);
        DynamicBannerInfo bannerInfo = createBanner(new DynamicBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
        createBannerImage(bannerInfo);
        return bannerInfo;
    }

    /**
     * Картинка для баннера не создаётся. При необходимости стоит позвать {@link #createImage(AbstractBannerInfo)}
     */
    public ImageHashBannerInfo createActiveImageHashBanner(AdGroupInfo adGroupInfo) {
        return createActiveImageHashBanner(
                activeImageHashBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()),
                adGroupInfo);
    }

    /**
     * Картинка для баннера не создаётся. При необходимости стоит позвать {@link #createImage(AbstractBannerInfo)}
     */
    public ImageHashBannerInfo createActiveImageHashBanner(OldImageHashBanner banner, AdGroupInfo adGroupInfo) {
        return createBannerInActiveTextAdGroup(new ImageHashBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withCampaignInfo(adGroupInfo.getCampaignInfo())
                .withBanner(banner));
    }

    public ImageCreativeBannerInfo createActiveImageCreativeBanner() {
        OldImageCreativeBanner banner = activeImageCreativeBanner(null, null, null);
        fillImageCreativeDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new ImageCreativeBannerInfo()
                .withBanner(banner));
    }

    public ImageCreativeBannerInfo createActiveImageCreativeBanner(long creativeId) {
        OldImageCreativeBanner banner = activeImageCreativeBanner(null, null, creativeId);
        fillImageCreativeDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new ImageCreativeBannerInfo()
                .withBanner(banner));
    }

    public ImageCreativeBannerInfo createActiveImageCreativeBanner(OldImageCreativeBanner banner, ClientInfo clientInfo) {
        fillImageCreativeDefaultSystemFields(banner);
        return createBanner(new ImageCreativeBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeTextCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeTextAdGroup(null))));
    }

    public ImageCreativeBannerInfo createActiveImageCreativeBanner(OldImageCreativeBanner banner,
                                                                   AdGroupInfo adGroupInfo) {
        fillImageCreativeDefaultSystemFields(banner);
        return createBanner(new ImageCreativeBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public MobileAppBannerInfo createActiveMobileAppBanner(AdGroupInfo adGroup) {
        OldMobileAppBanner banner = activeMobileAppBanner(adGroup.getCampaignId(), adGroup.getAdGroupId());
        return createActiveMobileAppBanner(banner, adGroup);
    }

    public MobileAppBannerInfo createActiveMobileAppBanner(OldMobileAppBanner banner) {
        return createBanner(new MobileAppBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo().withAdGroup(activeMobileAppAdGroup(null))));
    }

    public MobileAppBannerInfo createActiveMobileAppBanner(OldMobileAppBanner banner, AdGroupInfo adGroup) {
        return createBanner(new MobileAppBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroup));
    }

    public CpmBannerInfo createActiveCpmBanner() {
        OldCpmBanner banner = activeCpmBanner(null, null, null);
        fillCpmBannerDefaultSystemFields(banner);
        return createBannerInActiveCpmAdGroup(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo().withAdGroup(activeCpmBannerAdGroup(null))));
    }

    public CpmBannerInfo createActiveCpmBanner(Long creativeId, CampaignInfo campaignInfo) {
        OldCpmBanner banner = activeCpmBanner(null, null, creativeId);
        fillCpmBannerDefaultSystemFields(banner);
        return createBannerInActiveCpmAdGroup(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo().withAdGroup(activeCpmBannerAdGroup(null)))
                .withCampaignInfo(campaignInfo)
        );
    }

    public CpmBannerInfo createActiveCpmBanner(AdGroupInfo adGroupInfo) {
        OldCpmBanner banner = activeCpmBanner(null, adGroupInfo.getAdGroupId(), null);
        fillCpmBannerDefaultSystemFields(banner);
        return createBannerInActiveCpmAdGroup(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public CpmBannerInfo createActiveCpmBanner(OldCpmBanner banner, ClientInfo clientInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmBannerAdGroup(null))));
    }

    public CpmBannerInfo createActiveCpmBanner(OldCpmBanner banner, AdGroupInfo adGroupInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public void createActiveCpmBannerRaw(Integer shard, OldCpmBanner banner, AdGroup adGroup) {
        createActiveCpmBannerRaw(shard, banner, adGroup.getCampaignId(), adGroup.getId());
    }

    public void createActiveCpmBannerRaw(Integer shard, OldCpmBanner banner, Long campaignId, Long adgroupId) {
        fillCpmBannerDefaultSystemFields(banner);
        createBannerRow(shard, banner, campaignId, adgroupId);
    }

    public CpmBannerInfo createActiveCpmVideoBanner(ClientInfo clientInfo) {
        OldCpmBanner banner = activeCpmVideoBanner(null, null, null);
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo()
                                .withCampaign(activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmVideoAdGroup(null))));
    }

    public CpmAudioBannerInfo createActiveCpmAudioBanner(OldCpmAudioBanner banner, AdGroupInfo adGroupInfo) {
        fillCpmAudioBannerDefaultSystemFields(banner);
        return createBanner(new CpmAudioBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public CpmBannerInfo createActiveCpmVideoBanner(OldCpmBanner banner, ClientInfo clientInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmVideoAdGroup(null))));
    }

    public CpmBannerInfo createActiveCpmGeoproductBanner(OldCpmBanner banner, ClientInfo clientInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmGeoproductAdGroup(null))));
    }

    public CpmBannerInfo createActiveCpmYndxFrontpageBanner(OldCpmBanner banner, ClientInfo clientInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmYndxFrontpageCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmYndxFrontpageAdGroup(null))));
    }

    public CpmAudioBannerInfo createActiveCpmAudioBanner(OldCpmAudioBanner banner, ClientInfo clientInfo) {
        fillCpmAudioBannerDefaultSystemFields(banner);
        return createBanner(new CpmAudioBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmAudioAdGroup(null))));
    }

    public CpmBannerInfo createActiveCpmVideoBanner(OldCpmBanner banner, CampaignInfo campaignInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(campaignInfo)
                        .withAdGroup(activeCpmVideoAdGroup(campaignInfo.getCampaignId()))));
    }

    public CpmBannerInfo createActiveCpmVideoBanner(OldCpmBanner banner, AdGroupInfo adGroupInfo) {
        fillCpmBannerDefaultSystemFields(banner);
        return createBanner(new CpmBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public CpmGeoPinBannerInfo createActiveCpmGeoPinBanner(OldCpmGeoPinBanner banner, ClientInfo clientInfo) {
        fillCpmGeoPinBannerDefaultSystemFields(banner);
        return createBanner(new CpmGeoPinBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeCpmGeoPinAdGroup(null))));
    }

    public CpmGeoPinBannerInfo createActiveCpmGeoPinBanner(OldCpmGeoPinBanner banner, AdGroupInfo adGroupInfo) {
        fillCpmGeoPinBannerDefaultSystemFields(banner);
        return createBanner(new CpmGeoPinBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public CpcVideoBannerInfo createActiveCpcVideoBanner(OldCpcVideoBanner banner, AdGroupInfo adGroupInfo) {
        fillCpcVideoDefaultSystemFields(banner);
        return createBanner(new CpcVideoBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public CpcVideoBannerInfo createActiveCpcVideoBanner(OldCpcVideoBanner banner, CampaignInfo campaignInfo) {
        fillCpcVideoDefaultSystemFields(banner);
        return createBanner(new CpcVideoBannerInfo()
                .withBanner(banner)
                .withCampaignInfo(campaignInfo));
    }

    public CpcVideoBannerInfo createActiveCpcVideoBanner(OldCpcVideoBanner banner, ClientInfo clientInfo) {
        fillCpcVideoDefaultSystemFields(banner);
        return createBanner(new CpcVideoBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeTextCampaign(null, null)))
                        .withClientInfo(clientInfo)
                        .withAdGroup(activeTextAdGroup(null))));
    }

    public CpcVideoBannerInfo createDefaultCpcVideoBanner(AdGroupInfo adGroupInfo, Long creativeId) {
        OldCpcVideoBanner banner = TestBanners.activeCpcVideoBanner(null, adGroupInfo.getAdGroupId(), creativeId);
        fillCpcVideoDefaultSystemFields(banner);
        return createBanner(new CpcVideoBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
    }

    public ImageCreativeBannerInfo createDefaultImageCreativeBanner(AdGroupInfo adGroupInfo, long creativeId) {
        OldImageCreativeBanner banner = activeImageCreativeBanner(null, adGroupInfo.getAdGroupId(), creativeId);
        fillImageCreativeDefaultSystemFields(banner);
        return createBanner(new ImageCreativeBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
    }

    public CpmOutdoorBannerInfo createActiveCpmOutdoorBanner(OldCpmOutdoorBanner banner, ClientInfo clientInfo) {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();
        return createBanner(new CpmOutdoorBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo().withAdGroup(activeCpmOutdoorAdGroup(null, placement)))
                .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                .withClientInfo(clientInfo));
    }

    public CpmIndoorBannerInfo createActiveCpmIndoorBanner(OldCpmIndoorBanner banner, ClientInfo clientInfo) {
        IndoorPlacement placement = placementSteps.addDefaultIndoorPlacementWithOneBlock();
        return createBanner(new CpmIndoorBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo().withAdGroup(activeCpmIndoorAdGroup(null, placement)))
                .withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)))
                .withClientInfo(clientInfo));
    }

    public CpmOutdoorBannerInfo createActiveCpmOutdoorBanner(OldCpmOutdoorBanner banner, AdGroupInfo adGroupInfo) {
        return createBanner(new CpmOutdoorBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public CpmIndoorBannerInfo createActiveCpmIndoorBanner(OldCpmIndoorBanner banner, AdGroupInfo adGroupInfo) {
        return createBanner(new CpmIndoorBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public InternalBannerInfo createActiveInternalBanner() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveInternalAdGroup();
        return createActiveInternalBanner(adGroupInfo);
    }

    public InternalBannerInfo createStoppedInternalBanner(AdGroupInfo adGroupInfo) {
        var stoppedBanner = activeInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withStatusShow(false)
                .withStatusActive(false);
        return createActiveInternalBanner(adGroupInfo, stoppedBanner);
    }

    public InternalBannerInfo createActiveInternalBanner(AdGroupInfo adGroupInfo) {
        return createActiveInternalBanner(adGroupInfo,
                activeInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()));
    }

    public InternalBannerInfo createActiveInternalBannerForModeratedPlace(AdGroupInfo adGroupInfo) {
        return createActiveInternalBanner(adGroupInfo,
                activeInternalBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(),
                        TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1,
                        TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE));
    }

    public InternalBannerInfo createActiveInternalBanner(AdGroupInfo adGroupInfo, OldInternalBanner internalBanner) {
        return createBanner(new InternalBannerInfo()
                .withBanner(internalBanner)
                .withAdGroupInfo(adGroupInfo));
    }

    public McBannerInfo createActiveMcBanner(AdGroupInfo adGroupInfo) {
        return createBanner(new McBannerInfo()
                .withBanner(activeMcBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()))
                .withAdGroupInfo(adGroupInfo));
    }

    public McBannerInfo createActiveMcBanner(OldMcBanner mcBanner, AdGroupInfo adGroupInfo) {
        return createBanner(new McBannerInfo()
                .withBanner(mcBanner)
                .withAdGroupInfo(adGroupInfo));
    }

    // content promotion video (старая версия)

    public ContentPromotionVideoBannerInfo createActiveContentPromotionVideoBanner(AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionVideoBannerInfo()
                .withBanner(activeContentPromotionVideoBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()))
                .withAdGroupInfo(adGroupInfo));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionVideoBannerInfo createActiveContentPromotionVideoBanner(OldContentPromotionVideoBanner banner,
                                                                                   AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionVideoBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionVideoBannerInfo createActiveContentPromotionVideoBanner(OldContentPromotionVideoBanner banner) {
        return createBanner(new ContentPromotionVideoBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withAdGroup(activeContentPromotionVideoAdGroup(null))
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeContentPromotionCampaign(null, null))
                        )));
    }

    // content promotion collection

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBannerCollectionType(OldContentPromotionBanner banner,
                                                                                       AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public ContentPromotionBannerInfo createActiveContentPromotionBannerCollectionType(
            AdGroupInfo contentCollectionAdGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(activeContentPromotionBannerCollectionType(
                        contentCollectionAdGroupInfo.getCampaignId(), contentCollectionAdGroupInfo.getAdGroupId()))
                .withAdGroupInfo(contentCollectionAdGroupInfo));
    }

    public ContentPromotionBannerInfo createActiveContentPromotionBannerCollectionType() {
        return createActiveContentPromotionBannerCollectionType(new AdGroupInfo()
                .withAdGroup(fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION))
                .withCampaignInfo(new CampaignInfo().withCampaign(activeContentPromotionCampaign(null, null))));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBannerCollectionType(OldContentPromotionBanner banner) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withAdGroup(fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION))
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeContentPromotionCampaign(null, null))
                        )));
    }

    // content promotion video (новая версия)

    public ContentPromotionBannerInfo createActiveContentPromotionBannerVideoType(
            AdGroupInfo contentCollectionAdGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(activeContentPromotionBannerVideoType(
                        contentCollectionAdGroupInfo.getCampaignId(), contentCollectionAdGroupInfo.getAdGroupId()))
                .withAdGroupInfo(contentCollectionAdGroupInfo));
    }

    public ContentPromotionBannerInfo createActiveContentPromotionBannerVideoType() {
        return createActiveContentPromotionBannerVideoType(new AdGroupInfo()
                .withAdGroup(fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO))
                .withCampaignInfo(new CampaignInfo().withCampaign(activeContentPromotionCampaign(null, null))));
    }

    // content promotion service
    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBannerServiceType(
            OldContentPromotionBanner banner, AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBannerServiceType(AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(activeContentPromotionBannerServiceType(
                        adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()))
                .withAdGroupInfo(adGroupInfo));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBannerServiceType() {
        return createActiveContentPromotionBannerServiceType(new AdGroupInfo()
                .withAdGroup(fullContentPromotionAdGroup(ContentPromotionAdgroupType.SERVICE))
                .withCampaignInfo(new CampaignInfo().withCampaign(activeContentPromotionCampaign(null, null))));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBannerEdaType(AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(activeContentPromotionBannerEdaType(
                        adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()))
                .withAdGroupInfo(adGroupInfo));
    }

    // content promotion

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBanner(OldContentPromotionBanner banner,
                                                                         AdGroupInfo adGroupInfo) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    @Deprecated //use ContentPromotionBannerSteps
    public ContentPromotionBannerInfo createActiveContentPromotionBanner(OldContentPromotionBanner banner,
                                                                         ContentPromotionAdgroupType
                                                                                 contentPromotionAdgroupType) {
        return createBanner(new ContentPromotionBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(new AdGroupInfo()
                        .withAdGroup(fullContentPromotionAdGroup(contentPromotionAdgroupType))
                        .withCampaignInfo(new CampaignInfo().withCampaign(activeContentPromotionCampaign(null, null))
                        )));
    }

    public TextBannerInfo createDefaultBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = activeTextBanner();
        fillTextDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
    }

    public TextBannerInfo createDefaultArchivedBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = activeTextBanner().withStatusArchived(true);
        fillTextDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
    }

    public TextBannerInfo createTextBannerWithMinusGeo(OldTextBanner textBanner, AdGroupInfo adGroupInfo,
                                                       List<Long> minusGeo) {
        return createTextBannerWithMinusGeo(textBanner, adGroupInfo, minusGeo, BannersMinusGeoType.current);
    }

    public TextBannerInfo createTextBannerWithMinusGeo(OldTextBanner textBanner, AdGroupInfo adGroupInfo,
                                                       List<Long> minusGeo, BannersMinusGeoType minusGeoType) {
        fillTextDefaultSystemFields(textBanner);
        TextBannerInfo textBannerInfo = createBanner(textBanner, adGroupInfo);

        testBannerRepository.addMinusGeo(textBannerInfo.getShard(), textBannerInfo.getBannerId(),
                minusGeoType, minusGeo.stream().map(Object::toString).collect(joining(",")));

        return textBannerInfo;
    }

    public TextBannerInfo createDefaultTextBannerWithMinusGeo(AdGroupInfo adGroupInfo, List<Long> minusGeo) {
        TextBannerInfo bannerInfo = createDefaultBanner(adGroupInfo);

        testBannerRepository.addMinusGeo(bannerInfo.getShard(), bannerInfo.getBannerId(),
                BannersMinusGeoType.current, minusGeo.stream().map(Object::toString).collect(joining(",")));

        return bannerInfo;
    }

    public TextBannerInfo createActiveTextBannerWithFlags(AdGroupInfo adGroupInfo, BannerFlags flags) {
        OldTextBanner banner = activeTextBanner().withFlags(flags);
        fillTextDefaultSystemFields(banner);
        return createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
    }

    public void setFlags(int shard, long bannerId, BannerFlags flags) {
        testBannerRepository.updateFlags(shard, bannerId, flags);
    }

    public TextBannerInfo createActiveTextBannerWithEmptyFlags(AdGroupInfo adGroupInfo) {
        OldTextBanner banner = activeTextBanner();
        fillTextDefaultSystemFields(banner);
        TextBannerInfo textBannerInfo = createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
        testBannerRepository.setEmptyFlags(adGroupInfo.getShard(), banner.getId());

        return textBannerInfo;
    }

    public BannerInfo createBanner(OldBanner banner) {
        return createBanner(new BannerInfo().withBanner(banner));
    }

    public TextBannerInfo createBanner(OldTextBanner banner) {
        return createBanner(new TextBannerInfo().withBanner(banner));
    }

    public ImageHashBannerInfo createBanner(OldImageHashBanner banner) {
        return createBanner(new ImageHashBannerInfo().withBanner(banner));
    }

    public BannerInfo createBanner(OldBanner banner, ClientInfo clientInfo) {
        return createBanner(banner, new CampaignInfo().withClientInfo(clientInfo));
    }

    public TextBannerInfo createBanner(OldTextBanner banner, ClientInfo clientInfo) {
        return createBanner(banner, new CampaignInfo().withClientInfo(clientInfo));
    }

    public BannerInfo createBanner(OldBanner banner, CampaignInfo campaignInfo) {
        return createBanner(banner, new AdGroupInfo().withCampaignInfo(campaignInfo));
    }

    public TextBannerInfo createBanner(OldTextBanner banner, CampaignInfo campaignInfo) {
        return createBanner(banner, new AdGroupInfo().withCampaignInfo(campaignInfo));
    }

    public ImageCreativeBannerInfo createImageCreativeBanner(OldImageCreativeBanner banner, CampaignInfo campaignInfo) {
        return createImageCreativeBanner(banner, new AdGroupInfo().withCampaignInfo(campaignInfo));
    }

    public TextBannerInfo createBanner(@Nullable OldTextBanner banner, AdGroupInfo adGroupInfo) {
        OldTextBanner textBanner = banner == null ? defaultTextBanner(null, null) : banner;
        fillTextDefaultSystemFields(textBanner);
        return createBanner(new TextBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(textBanner));
    }

    public DynamicBannerInfo createBanner(@Nullable OldDynamicBanner banner, AdGroupInfo adGroupInfo) {
        OldDynamicBanner dynamicBanner = banner == null ? activeDynamicBanner(null, null) : banner;
        fillDynamicDefaultSystemFields(dynamicBanner);
        return createBanner(new DynamicBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(dynamicBanner));
    }

    public ImageCreativeBannerInfo createImageCreativeBanner(OldImageCreativeBanner banner, AdGroupInfo adGroupInfo) {
        fillImageCreativeDefaultSystemFields(banner);
        return createBanner(new ImageCreativeBannerInfo()
                .withBanner(banner)
                .withAdGroupInfo(adGroupInfo));
    }

    public BannerInfo createBanner(OldBanner banner, AdGroupInfo adGroupInfo) {
        return createBanner(new BannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBanner(banner));
    }

    public <BI extends AbstractBannerInfo<? extends OldBanner>> BI createBannerInActiveTextAdGroup(BI bannerInfo) {
        if (bannerInfo.getAdGroupInfo() == null) {
            bannerInfo.withAdGroupInfo(new AdGroupInfo().withAdGroup(activeTextAdGroup(null)));
        }
        if (bannerInfo.getCampaignInfo() == null) {
            bannerInfo.withCampaignInfo(new CampaignInfo().withCampaign(activeTextCampaign(null, null)));
        }
        return createBanner(bannerInfo);
    }

    private <BI extends AbstractBannerInfo<? extends OldBanner>> BI createBannerInActiveDynamicAdGroup(BI bannerInfo) {
        if (bannerInfo.getAdGroupInfo() == null) {
            bannerInfo.withAdGroupInfo(new AdGroupInfo().withAdGroup(activeDynamicTextAdGroup(null)));
        }
        if (bannerInfo.getCampaignInfo() == null) {
            bannerInfo.withCampaignInfo(new CampaignInfo().withCampaign(activeDynamicCampaign(null, null)));
        }
        return createBanner(bannerInfo);
    }

    private <BI extends AbstractBannerInfo<? extends OldBanner>> BI createBannerInActiveCpmAdGroup(BI bannerInfo) {
        if (bannerInfo.getAdGroupInfo() == null) {
            bannerInfo.withAdGroupInfo(new AdGroupInfo().withAdGroup(activeCpmBannerAdGroup(null)));
        }
        if (bannerInfo.getCampaignInfo() == null) {
            bannerInfo.withCampaignInfo(new CampaignInfo().withCampaign(activeCpmBannerCampaign(null, null)));
        }
        return createBanner(bannerInfo);
    }

    public <BI extends AbstractBannerInfo<? extends OldBanner>> BI createBanner(BI bannerInfo) {
        if (bannerInfo.getBannerId() == null) {
            if (!bannerInfo.isAdGroupExists()) {
                adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
            }
            bannerInfo.getBanner()
                    .withCampaignId(bannerInfo.getCampaignId())
                    .withAdGroupId(bannerInfo.getAdGroupId());
            fillCommonDefaultSystemFields(bannerInfo.getBanner());
            bannerRepository.addBanners(bannerInfo.getShard(), List.of(bannerInfo.getBanner()));
        }
        return bannerInfo;
    }

    public void createBannerRow(Integer shard, OldCpmBanner banner, Long campaignId, Long adgroupId) {
        banner
                .withCampaignId(campaignId)
                .withAdGroupId(adgroupId);
        fillCommonDefaultSystemFields(banner);
        bannerRepository.addBanners(shard, List.of(banner));
    }

    public <BI extends AbstractBannerInfo<? extends OldBannerWithImage>> ImageInfo<BI> createImage(BI bannerInfo) {
        int shard = bannerInfo.getShard() == null ? 1 : bannerInfo.getShard();
        BannerImageFormat imageFormat = createImageAdImageFormat(bannerInfo.getClientInfo());
        Image image = defaultImage(bannerInfo.getBanner(), imageFormat.getImageHash());
        fillImageDefaultSystemFields(image);
        testImageRepository.delete(shard, List.of(bannerInfo.getBannerId()));
        testImageRepository.addImages(shard, List.of(image));
        bannerInfo.getBanner().withImage(image);
        return new ImageInfo<>(bannerInfo).withImage(image);
    }

    public <BI extends AbstractBannerInfo<? extends OldBannerWithBannerImage>> BannerImageInfo<BI> createBannerImage(
            BI bannerInfo) {
        BannerImageFormat bannerImageFormat;
        if (OldDynamicBanner.class.isAssignableFrom(bannerInfo.getBanner().getClass())) {
            bannerImageFormat = createRegularImageFormat(bannerInfo.getClientInfo());
        } else {
            bannerImageFormat = createBannerImageFormat(bannerInfo.getClientInfo());
        }
        OldBannerImage bannerImage = defaultBannerImage(bannerInfo.getBannerId(), bannerImageFormat.getImageHash());
        return createBannerImage(bannerInfo, bannerImageFormat, bannerImage);
    }

    public <BI extends AbstractBannerInfo<? extends OldBannerWithBannerImage>> BannerImageInfo<BI> createBannerImage(
            BI bannerInfo, BannerImageFormat bannerImageFormat, OldBannerImage bannerImage) {
        int shard = bannerInfo.getShard() == null ? 1 : bannerInfo.getShard();
        bannerImage.withBannerId(bannerInfo.getBannerId())
                .withImageHash(bannerImageFormat.getImageHash());

        BannerImageInfo<BI> bannerImageInfo = new BannerImageInfo<>(bannerInfo)
                .withBannerImage(bannerImage)
                .withBannerImageFormat(bannerImageFormat);

        fillBannerImageDefaultSystemFields(bannerImage);
        testBannerImageRepository.delete(shard, List.of(bannerInfo.getBannerId()));
        testBannerImageRepository.addBannerImages(bannerInfo.getShard(), List.of(bannerImage));
        bannerInfo.getBanner().withBannerImage(bannerImage);
        return bannerImageInfo;
    }

    public BannerImageFormat createBannerImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.defaultBannerImageFormat(null));
    }

    public BannerImageFormat createImageAdImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.defaultImageBannerImageFormat(null));
    }

    public BannerImageFormat createRegularImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.regularImageFormat(null));
    }

    public BannerImageFormat createWideImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.wideImageFormat(null));
    }

    public BannerImageFormat createSmallImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.smallImageFormat(null));
    }

    public BannerImageFormat createLogoImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.logoImageFormat(null));
    }

    public BannerImageFormat createBigKingImageFormat(ClientInfo clientInfo) {
        return createBannerImageFormat(clientInfo, TestBanners.bigKingImageFormat());
    }

    public BannerImageFormat createBannerImageFormat(ClientInfo clientInfo, BannerImageFormat bannerImageFormat) {
        int shard = clientInfo.getShard();

        testBannerImageRepository.addBannerImageFormats(shard, List.of(bannerImageFormat));
        addImageToImagePool(shard, clientInfo.getClientId(), bannerImageFormat.getImageHash());

        return bannerImageFormat;
    }

    public void addImageToImagePool(int shard, ClientId clientId, String hash) {
        BannerImageFromPool bannerImageFromPool = new BannerImageFromPool()
                .withName(String.format(DEFAULT_IMAGE_NAME_TEMPLATE, hash))
                .withImageHash(hash)
                .withCreateTime(LocalDateTime.now())
                .withSource(BannerImageSource.DIRECT)
                .withClientId(clientId.asLong());
        bannerImagePoolRepository
                .addOrUpdateImagesToPool(shard, clientId, List.of(bannerImageFromPool));
    }

    public void addBannerReModerationFlag(int shard, Long bannerId, Collection<RemoderationType> subObjects) {
        testBannerRepository.addBannerReModerationFlag(shard, bannerId, subObjects);
    }

    public boolean isBannerReModerationFlagPresent(int shard, Long bannerId) {
        return testBannerRepository.isBannerReModerationFlagPresent(shard, bannerId);
    }

    public boolean isBannerReModerationFlagPresentForType(int shard, Long bannerId, RemoderationType remoderationType) {
        return testBannerRepository.isBannerReModerationFlagPresentForType(shard, bannerId,
                remoderationType.getTableFieldRef());
    }

    public void addBannerAutoModerationFlag(int shard, Long bannerId) {
        testModerationRepository.addAutoModerate(shard, bannerId);
    }

    public boolean isBannerAutoModerationFlagPresent(int shard, Long bannerId) {
        return testModerationRepository.getAutoAcceptanceRecord(shard, bannerId) != null;
    }

    public <B extends OldBannerWithTurboLanding> void addTurbolandingMetricaCounters(int shard, B banner,
                                                                                     List<Long> metrikaCounters) {
        testTurboLandingRepository.addTurbolandingMetricaCounters(shard, banner, metrikaCounters);
    }

    public <B extends BannerWithTurboLanding> void addTurbolandingMetricaCounters(int shard, B banner,
                                                                                  List<Long> metrikaCounters) {
        testTurboLandingRepository.addTurbolandingMetricaCounters(shard, banner, metrikaCounters);
    }

    public void setImageSize(int shard, String hash, ImageSize newSize) {
        int updatedRows = testBannerImageFormatRepository.updateSize(shard, hash, newSize);
        checkState(updatedRows == 1, "No row has been updated");
    }

    public void setImageCreationDate(int shard, String hash, LocalDateTime dateTime) {
        int updatedRows = testBannerImageFormatRepository.updateCreateTime(shard, hash, dateTime);
        checkState(updatedRows == 1, "No row has been updated");
    }

    public void setImageName(int shard, String hash, String name) {
        int updatedRows = testBannerImageFormatRepository.updateName(shard, hash, name);
        checkState(updatedRows == 1, "No row has been updated");
    }

    public <B extends BannerWithLanguage> void setLanguage(NewBannerInfo bannerInfo, Language language) {
        int shard = bannerInfo.getShard();
        Long bannerId = bannerInfo.getBannerId();
        B srcBanner = bannerInfo.getBanner();
        AppliedChanges<BannerWithLanguage> appliedChanges = new ModelChanges<>(bannerId, BannerWithLanguage.class)
                .process(language, BannerWithLanguage.LANGUAGE)
                .applyTo(srcBanner);
        bannerCommonRepository.updateBannersLanguages(dslContextProvider.ppc(shard), singleton(appliedChanges));
        B dstBanner = (B) bannerTypedRepository.getTyped(shard, singletonList(bannerId)).get(0);
        bannerInfo.withBanner(dstBanner);
    }

    public void deleteBanners(Integer shard, List<Long> adIds) {
        bannerCommonRepository.deleteBanners(shard, adIds);
    }
}
