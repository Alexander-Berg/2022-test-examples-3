package ru.yandex.direct.core.testing.data;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.MobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.old.OldAbstractBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerSubtypeEnum;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCreative;
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
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.old.TemplateVariable;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.yandex.direct.core.entity.banner.type.body.BannerWithBodyConstants.MC_BANNER_BODY;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MC_BANNER_TITLE;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.TestImages.defaultImage;
import static ru.yandex.direct.core.testing.info.BannerImageFormat.AvatarHost;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_8_RESOURCE_1;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_8_RESOURCE_2;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_8_RESOURCE_3;
import static ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils.TEMPLATE_8_RESOURCE_4;
import static ru.yandex.direct.utils.CommonUtils.nvl;

public final class TestBanners {

    public static final Long VALID_CONTENT_PROMOTION_VIDEO_ID = 1234L;
    public static final Long VALID_CONTENT_PROMOTION_ID = 12345L;
    public static final Long ANOTHER_VALID_CONTENT_PROMOTION_VIDEO_ID = 5678L;
    public static final Long INVALID_CONTENT_PROMOTION_VIDEO_ID = 8765L;
    public static final Long INACCESSIBLE_CONTENT_PROMOTION_VIDEO_ID = 1001L;
    public static final String VK_TEST_PUBLIC_HREF = "http://vk.com/test";
    public static final String VK_TEST_PUBLIC_AGGREGATOR_DOMAIN = "test.vk.com";
    public static final long DEFAULT_BS_BANNER_ID = 77788890L;
    public static final long ANOTHER_DEFAULT_BS_BANNER_ID = 23458732L;
    public static final long YET_ANOTHER_DEFAULT_BS_BANNER_ID = 234587434L;
    public static final long THIRD_DEFAULT_BS_BANNER_ID = 345794890L;

    private TestBanners() {
    }

    public static final Map<Class<? extends OldBanner>, OldBannerType> CLASS_TO_BANNER_TYPE =
            new ImmutableMap.Builder<Class<? extends OldBanner>, OldBannerType>()
                    .put(OldTextBanner.class, OldBannerType.TEXT)
                    .put(OldMobileAppBanner.class, OldBannerType.MOBILE_CONTENT)
                    .put(OldImageCreativeBanner.class, OldBannerType.IMAGE_AD)
                    .put(OldImageHashBanner.class, OldBannerType.IMAGE_AD)
                    .put(OldDynamicBanner.class, OldBannerType.DYNAMIC)
                    .put(OldCpcVideoBanner.class, OldBannerType.CPC_VIDEO)
                    .put(OldCpmBanner.class, OldBannerType.CPM_BANNER)
                    .put(OldCpmAudioBanner.class, OldBannerType.CPM_AUDIO)
                    .put(OldCpmOutdoorBanner.class, OldBannerType.CPM_OUTDOOR)
                    .put(OldCpmIndoorBanner.class, OldBannerType.CPM_INDOOR)
                    .put(OldContentPromotionBanner.class, OldBannerType.CONTENT_PROMOTION)
                    .put(OldInternalBanner.class, OldBannerType.INTERNAL)
                    .put(OldPerformanceBanner.class, OldBannerType.PERFORMANCE)
                    .put(OldMcBanner.class, OldBannerType.MCBANNER)
                    .put(OldCpmGeoPinBanner.class, OldBannerType.CPM_GEO_PIN)
                    .build();

    private static final String FORMAT_JSON = "{"
            + "\"x300\":{\"height\":\"399\",\"width\":\"300\"},"
            + "\"x450\":{\"height\":\"599\",\"width\":\"450\"}"
            + "}";
    private static final String MDS_META_JSON = "{"
            + "\"meta\":{"
            + "\"orig-format\": \"JPEG\", \"orig-size-bytes\": 1024,"
            + "\"crc64\": \"FE0A206B58E39F91\","
            + "\"MainColor\": \"#F8F8F8\","
            + "\"ColorWizBack\": \"#FEFFFF\","
            + "\"ColorWizText\": \"#7E7E7E\","
            + "\"ColorWizButton\": \"#ECE3D9\","
            + "\"average-color\": \"#00CCCC\","
            + "\"background-colors\": {" +
            "      \"top\": \"#FFFEFF\"," +
            "      \"left\": \"#FEFFFF\"," +
            "      \"right\": \"#00FFFF\"," +
            "      \"bottom\": \"#11FFFF\"" +
            "    },"
            + "\"ColorWizButtonText\": \"#814300\""
            + "},"
            + "\"sizes\":{\n"
            + "\"x300\":{\"path\":\"/get-direct/4437/mMRBaLF0fxJI_UoSMcVWXw/x300\",\"width\":300,\"height\":399,"
            + " \"smart-centers\":{\"3:4\":{\"h\":399,\"w\":300,\"x\":1,\"y\":0}},"
            + "\"1:1\":{\"h\":300,\"w\":300,\"x\":0,\"y\":1}},"
            + "\"x450\":{\"path\":\"/get-direct/4437/mMRBaLF0fxJI_UoSMcVWXw/x450\",\"width\":450,\"height\":599,"
            + " \"smart-centers\":{\"1:1\":{\"h\":450,\"w\":450,\"x\":0,\"y\":1},\"3:4\":{\"h\":599,\"w\":449," +
            "\"x\":1,\"y\":0}}}}}";

    public static OldTextBanner defaultTextBanner(Long campaignId, Long adGroupId) {
        return activeTextBanner(campaignId, adGroupId, textBannerData());
    }

    public static OldAbstractBanner activeBannerByCampaignType(CampaignType campaignType) {
        if (campaignType == CampaignType.DYNAMIC) {
            return activeDynamicBanner();
        }
        return activeTextBanner();
    }

    public static OldTextBanner activeTextBanner() {
        return activeTextBanner(textBannerData());
    }

    public static OldTextBanner activeTextBanner(OldTextBanner banner) {
        return activeTextBanner(null, null, banner);
    }

    public static OldTextBanner draftTextBanner() {
        return draftTextBanner(null, null);
    }

    public static OldTextBanner activeTextBanner(Long campaignId, Long adGroupId) {
        return activeTextBanner(campaignId, adGroupId, textBannerData());
    }

    public static OldTextBanner activeTextBanner(Long campaignId, Long adGroupId, OldTextBanner banner) {
        OldTextBanner activeBanner = banner
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(THIRD_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.TEXT);
        fillSystemFieldsForActiveTextBanner(activeBanner);
        return activeBanner;
    }

    public static OldTextBanner draftTextBanner(Long campaignId, Long adGroupId) {
        OldTextBanner banner = textBannerData()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(THIRD_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.TEXT);
        fillSystemFieldsForDraftBanner(banner);
        return banner;
    }

    public static OldDynamicBanner activeDynamicBanner() {
        return activeDynamicBanner(null, null);
    }

    public static OldDynamicBanner activeDynamicBanner(Long campaignId, Long adGroupId) {
        OldDynamicBanner banner = dynamicBannerData()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(THIRD_DEFAULT_BS_BANNER_ID);
        fillSystemFieldsForActiveDynamicBanner(banner);
        return banner
                .withBannerImage(defaultBannerImage(null, randomAlphanumeric(16)));
    }

    public static OldImageHashBanner activeImageHashBanner(Long campaignId, Long adGroupId) {
        Long bsBannerId = THIRD_DEFAULT_BS_BANNER_ID;
        return fillSystemFieldsForActiveBanner(new OldImageHashBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(bsBannerId)
                .withBannerType(OldBannerType.IMAGE_AD)
                .withSubType(OldBannerSubtypeEnum.TEXT_IMAGE_AD)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru/company")

                .withImage(defaultImage(campaignId, adGroupId));
    }

    public static OldImageHashBanner defaultClientImageHashBanner(Long campaignId, Long adGroupId,
                                                                  String imageHash) {
        Image image = new Image()
                .withStatusModerate(StatusImageModerate.READY)
                .withImageHash(imageHash)
                .withImageText("default image text");
        return new OldImageHashBanner()
                .withCampaignId(campaignId)
                .withGeoFlag(true)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.IMAGE_AD)
                .withSubType(OldBannerSubtypeEnum.TEXT_IMAGE_AD)
                .withIsMobile(false)
                .withHref("https://www.yandex.ru/company")
                .withImage(image);
    }


    public static OldImageCreativeBanner activeMobileCreativeBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return activeImageCreativeBanner(campaignId, adGroupId, creativeId)
                .withSubType(OldBannerSubtypeEnum.MOBILE_APP_AD_BUILDER_AD);
    }

    public static OldCpmAudioBanner activeCpmAudioBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldCpmAudioBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withCreativeId(creativeId)
                .withBannerType(OldBannerType.CPM_AUDIO)
                .withBsBannerId(DEFAULT_BS_BANNER_ID)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru/company")
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withPixels(emptyList());
    }

    public static OldCpcVideoBanner activeCpcVideoBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldCpcVideoBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withCreativeId(creativeId)
                .withBannerType(OldBannerType.CPC_VIDEO)
                .withBsBannerId(DEFAULT_BS_BANNER_ID)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru/company")
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
    }

    public static OldCpcVideoBanner clientCpcVideoBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return new OldCpcVideoBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withCreativeId(creativeId)
                .withBannerType(OldBannerType.CPC_VIDEO)
                .withIsMobile(false)
                .withHref("https://www.yandex.ru/company");
    }

    public static OldImageCreativeBanner clientImageCreativeBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return new OldImageCreativeBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.IMAGE_AD)
                .withSubType(OldBannerSubtypeEnum.TEXT_AD_BUILDER_AD)
                .withIsMobile(false)
                .withHref("https://www.yandex.ru/company")
                .withCreativeId(creativeId);
    }

    public static OldImageCreativeBanner activeImageCreativeBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldImageCreativeBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.IMAGE_AD)
                .withSubType(OldBannerSubtypeEnum.TEXT_AD_BUILDER_AD)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru/company")
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
    }

    public static OldMcBanner activeMcBanner() {
        return activeMcBanner(null, null);
    }

    public static OldMcBanner activeMcBanner(Long campaignId, Long adGroupId) {
        return fillSystemFieldsForActiveBanner(new OldMcBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(34532452L)
                .withTitle(MC_BANNER_TITLE)
                .withBody(MC_BANNER_BODY)
                .withBannerType(OldBannerType.MCBANNER)
                .withIsMobile(false)
                .withHref("https://www.yandex.ru/company")
                .withImage(defaultImage(campaignId, adGroupId));

    }

    public static OldMobileAppBanner clientMobileAppBanner(Long campaignId, Long adGroupId) {
        return new OldMobileAppBanner()
                .withTitle("test banner title " + randomNumeric(5))
                .withBody("test banner body " + randomNumeric(5))
                .withPrimaryAction(MobileContentPrimaryAction.BUY)
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.MOBILE_CONTENT)
                .withIsMobile(false)
                .withHref("https://trusted1.com")
                .withImpressionUrl("https://trusted.impression.com/impression");
    }

    public static OldMobileAppBanner activeMobileAppBanner(Long campaignId, Long adGroupId) {
        return fillSystemFieldsForActiveBanner(new OldMobileAppBanner())
                .withTitle("test banner title " + randomNumeric(5))
                .withBody("test banner body " + randomNumeric(5))
                .withPrimaryAction(MobileContentPrimaryAction.BUY)
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.MOBILE_CONTENT)
                .withIsMobile(true)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://trusted1.com")
                .withImpressionUrl("https://trusted.impression.com/impression");
    }

    public static OldMobileAppBanner activeMobileAppBanner() {
        return activeMobileAppBanner(null, null);
    }

    public static OldContentPromotionVideoBanner activeContentPromotionVideoBanner() {
        return activeContentPromotionVideoBanner(null, null);
    }

    public static OldContentPromotionVideoBanner activeContentPromotionVideoBanner(Long campaignId, Long adGroupId) {
        return activeContentPromotionVideoBanner(campaignId, adGroupId, VALID_CONTENT_PROMOTION_VIDEO_ID);
    }

    private static OldContentPromotionVideoBanner activeContentPromotionVideoBanner(Long campaignId, Long adGroupId,
                                                                                    Long contentPromotionVideoId) {
        return fillSystemFieldsForActiveBanner(new OldContentPromotionVideoBanner())
                .withTitle("test banner title " + randomNumeric(5))
                .withBody("test banner body " + randomNumeric(5))
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withContentPromotionVideoId(contentPromotionVideoId)
                .withHref("https://www.youtube.com")
                .withDomain("www.youtube.com")
                .withReverseDomain("moc.ebutuoy.www")
                .withPackshotHref("https://www.yandex.ru/");
    }

    public static OldContentPromotionBanner activeContentPromotionBannerCollectionType() {
        return activeContentPromotionBannerCollectionType(null, null);
    }

    public static OldContentPromotionBanner activeContentPromotionBannerCollectionType(Long campaignId,
                                                                                       Long adGroupId) {
        return fillSystemFieldsForActiveBanner(new OldContentPromotionBanner())
                .withTitle(null)
                .withBody(null)
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID)
                .withHref("https://www.youtube.com")
                .withDomain("www.youtube.com")
                .withReverseDomain("moc.ebutuoy.www")
                .withVisitUrl("https://www.yandex.ru/");
    }

    public static OldContentPromotionBanner activeContentPromotionBannerServiceType(Long campaignId, Long adGroupId) {
        return fillSystemFieldsForActiveBanner(new OldContentPromotionBanner())
                .withTitle("test service promotion banner title " + randomNumeric(5))
                .withBody(null)
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID)
                .withHref("https://www.youtube.com")
                .withDomain("www.youtube.com")
                .withReverseDomain("moc.ebutuoy.www")
                .withVisitUrl(null);
    }

    public static OldContentPromotionBanner activeContentPromotionBannerEdaType(Long campaignId, Long adGroupId) {
        return fillSystemFieldsForActiveBanner(new OldContentPromotionBanner())
                .withTitle("test eda promotion banner title " + randomNumeric(5))
                .withBody("test eda promotion banner body " + randomNumeric(5))
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID)
                .withHref("https://www.youtube.com")
                .withDomain("www.youtube.com")
                .withReverseDomain("moc.ebutuoy.www")
                .withVisitUrl(null);
    }

    public static OldContentPromotionBanner activeContentPromotionBannerVideoType(Long campaignId, Long adGroupId) {
        return fillSystemFieldsForActiveBanner(new OldContentPromotionBanner())
                .withTitle("test title")
                .withBody("test body")
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID)
                .withHref("https://www.youtube.com")
                .withDomain("www.youtube.com")
                .withReverseDomain("moc.ebutuoy.www")
                .withVisitUrl("https://www.yandex.ru/");
    }

    private static void fillSystemFieldsForDraftBanner(OldTextBanner banner) {
        banner
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withStatusPostModerate(OldBannerStatusPostModerate.NEW)
                .withStatusSitelinksModerate(StatusSitelinksModerate.NEW)
                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.READY)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withStatusShow(true)
                .withStatusActive(false)
                .withStatusArchived(false)
                .withGeoFlag(false)
                .withLastChange(now())
                .withLanguage(Language.NO);
    }

    private static <B extends OldBanner> B fillSystemFieldsForActiveBanner(B banner) {
        banner
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusPostModerate(OldBannerStatusPostModerate.YES)
                .withStatusShow(true)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withGeoFlag(true)
                .withFlags(BannerFlags.fromSource("flag"))
                .withLastChange(now())
                .withLanguage(Language.RU_);
        return banner;
    }

    private static void fillSystemFieldsForActiveDynamicBanner(OldDynamicBanner banner) {
        fillSystemFieldsForActiveBanner(banner);
        banner
                .withStatusSitelinksModerate(
                        banner.getSitelinksSetId() == null ? StatusSitelinksModerate.NEW : StatusSitelinksModerate.YES)
                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.YES)
                .withPhoneFlag(banner.getVcardId() == null ? StatusPhoneFlagModerate.NEW : StatusPhoneFlagModerate.YES)
                .withShowDisplayHref(true);
    }

    private static void fillSystemFieldsForActiveTextBanner(OldTextBanner banner) {
        fillSystemFieldsForActiveBanner(banner);
        banner
                .withStatusSitelinksModerate(
                        banner.getSitelinksSetId() == null ? StatusSitelinksModerate.NEW : StatusSitelinksModerate.YES)
                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.YES)
                .withPhoneFlag(banner.getVcardId() == null ? StatusPhoneFlagModerate.NEW : StatusPhoneFlagModerate.YES);
    }

    public static void fillCommonDefaultSystemFields(OldBanner banner) {
        banner
                .withBsBannerId(nvl(banner.getBsBannerId(), 0L))
                .withStatusBsSynced(nvl(banner.getStatusBsSynced(), StatusBsSynced.NO))
                .withIsMobile(nvl(banner.getIsMobile(), false))
                .withStatusModerate(nvl(banner.getStatusModerate(), OldBannerStatusModerate.NEW))
                .withStatusPostModerate(nvl(banner.getStatusPostModerate(), OldBannerStatusPostModerate.NO))
                .withPhoneFlag(nvl(banner.getPhoneFlag(), StatusPhoneFlagModerate.NEW))
                .withStatusShow(nvl(banner.getStatusShow(), true))
                .withStatusActive(nvl(banner.getStatusActive(), false))
                .withStatusArchived(nvl(banner.getStatusArchived(), false))
                .withLanguage(nvl(banner.getLanguage(), Language.NO))
                .withLastChange(nvl(banner.getLastChange(), now()));
    }

    public static void fillTextDefaultSystemFields(OldTextBanner textBanner) {
        textBanner
                .withStatusSitelinksModerate(nvl(textBanner.getStatusSitelinksModerate(), StatusSitelinksModerate.NEW));
        fillBannerWithCreativeDefaultSystemFields(textBanner);
    }

    public static void fillDynamicDefaultSystemFields(OldDynamicBanner dynamicBanner) {
        dynamicBanner.withStatusSitelinksModerate(
                nvl(dynamicBanner.getStatusSitelinksModerate(), StatusSitelinksModerate.NEW));
    }

    public static void fillImageCreativeDefaultSystemFields(OldImageCreativeBanner imageCreativeBanner) {
        fillBannerWithCreativeDefaultSystemFields(imageCreativeBanner);
    }

    public static void fillCpmBannerDefaultSystemFields(OldCpmBanner cpmBanner) {
        fillBannerWithCreativeDefaultSystemFields(cpmBanner);
    }

    public static void fillCpmAudioBannerDefaultSystemFields(OldCpmAudioBanner cpmAudioBanner) {
        fillBannerWithCreativeDefaultSystemFields(cpmAudioBanner);
    }

    public static void fillCpcVideoDefaultSystemFields(OldCpcVideoBanner cpcVideoBanner) {
        fillBannerWithCreativeDefaultSystemFields(cpcVideoBanner);
    }

    public static void fillCpmGeoPinBannerDefaultSystemFields(OldCpmGeoPinBanner cpmBanner) {
        fillBannerWithCreativeDefaultSystemFields(cpmBanner);
    }

    public static void fillBannerWithCreativeDefaultSystemFields(OldBannerWithCreative bannerWithCreative) {
        if (bannerWithCreative.getCreativeId() != null) {
            bannerWithCreative.withCreativeStatusModerate(
                    nvl(bannerWithCreative.getCreativeStatusModerate(), OldBannerCreativeStatusModerate.NEW));
        }
    }

    public static void fillBannerImageDefaultSystemFields(OldBannerImage bannerImage) {
        bannerImage
                .withBsBannerId(nvl(bannerImage.getBsBannerId(), 0L))
                .withStatusModerate(nvl(bannerImage.getStatusModerate(), OldStatusBannerImageModerate.NEW))
                .withStatusShow(nvl(bannerImage.getStatusShow(), true))
                .withDateAdded(nvl(bannerImage.getDateAdded(), now()))
                .withImageType(nvl(bannerImage.getImageType(), ImageType.SMALL));
    }

    public static OldDynamicBanner dynamicBannerData() {
        return new OldDynamicBanner()
                .withBannerType(OldBannerType.DYNAMIC)
                .withBody("test body " + randomNumeric(5))
                .withHref("https://www.yandex.ru/company")
                .withDisplayHref("яндекс")
                .withCalloutIds(emptyList())
                .withIsMobile(false);
    }

    public static OldTextBanner textBannerData() {
        return new OldTextBanner()
                .withTitle("test title " + randomNumeric(5))
                .withTitleExtension("test titleExt " + randomNumeric(5))
                .withBody("test body " + randomNumeric(5))
                .withHref("https://yandex.ru/company")
                .withDisplayHref("яндекс")
                .withCalloutIds(emptyList())
                .withIsMobile(false)
                .withBannerType(OldBannerType.TEXT);
    }

    public static BannerImageFormat imageAdImageFormat(@Nullable String imageHash) {
        return defaultBannerImageFormat(imageHash).withImageType(BannerImageFormat.ImageType.IMAGE_AD);
    }

    public static BannerImageFormat regularImageFormat(@Nullable String imageHash) {
        return defaultBannerImageFormat(imageHash).withImageType(BannerImageFormat.ImageType.REGULAR);
    }

    public static BannerImageFormat smallImageFormat(@Nullable String imageHash) {
        return defaultBannerImageFormat(imageHash).withImageType(BannerImageFormat.ImageType.SMALL);
    }

    public static BannerImageFormat wideImageFormat(@Nullable String imageHash) {
        return defaultBannerImageFormat(imageHash).withImageType(BannerImageFormat.ImageType.WIDE);
    }

    public static BannerImageFormat logoImageFormat(@Nullable String imageHash) {
        return defaultBannerImageFormat(imageHash)
                .withImageType(BannerImageFormat.ImageType.LOGO)
                .withWidth(300L)
                .withHeight(300L);
    }

    public static BannerImageFormat bigKingImageFormat() {
        BannerImageFormat bif = TestBanners.defaultImageBannerImageFormat(null)
                .withWidth(516L)
                .withHeight(272L)
                .withFormatsJson("{\"orig\":{\"height\":\"516\",\"width\":\"272\"}}");
        return bif;
    }

    public static BannerImageFormat defaultBannerImageFormat() {
        return defaultBannerImageFormat(null);
    }

    public static BannerImageFormat defaultBannerImageFormat(@Nullable String imageHash) {
        if (imageHash == null) {
            imageHash = random(22, true, true);
        }
        return new BannerImageFormat()
                .withImageHash(imageHash)
                .withWidth(300L)
                .withHeight(601L)
                .withImageType(BannerImageFormat.ImageType.REGULAR)
                .withAvatarHost(AvatarHost.TEST)
                .withMdsGroupId(31337L)
                .withAvatarNamespace(BannerImageFormat.AvatarNamespace.DIRECT)
                .withFormatsJson(FORMAT_JSON)
                .withMdsMetaJson(MDS_META_JSON);
    }

    public static BannerImageFormat defaultImageBannerImageFormat(@Nullable String imageHash) {
        if (imageHash == null) {
            imageHash = random(22, true, true);
        }
        return new BannerImageFormat()
                .withImageHash(imageHash)
                .withWidth(300L)
                .withHeight(600L)
                .withImageType(BannerImageFormat.ImageType.IMAGE_AD)
                .withAvatarHost(AvatarHost.TEST)
                .withMdsGroupId(31337L)
                .withAvatarNamespace(BannerImageFormat.AvatarNamespace.DIRECT_PICTURE)
                .withFormatsJson("{"
                        + "\"orig\":{\"height\":\"600\",\"width\":\"300\"}"
                        + "}")
                .withMdsMetaJson(MDS_META_JSON);
    }

    public static BannerImageFormat defaultMcBannerImageFormat(@Nullable String imageHash) {
        return imageAdImageFormat(imageHash)
                .withWidth(240L)
                .withHeight(400L);
    }

    public static OldBannerImage defaultBannerImage(Long bannerId, String imageHash) {
        return new OldBannerImage()
                .withBannerId(bannerId)
                .withBsBannerId(0L)
                .withImageHash(imageHash)
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withImageType(ImageType.REGULAR)
                .withStatusShow(true)
                .withDateAdded(now());
    }

    public static OldCpmBanner activeCpmBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldCpmBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CPM_BANNER)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru/company")
                .withBody("Fake body")
                .withTitle("Fake title")
                .withTitleExtension("Fake title extension")
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withPixels(singletonList("https://mc.yandex.ru/pixel/529875658445014901?rnd=%aw_random%"))
                .withMeasurers(List.of(
                        new OldBannerMeasurer()
                                .withBannerId(123L)
                                .withBannerMeasurerSystem(OldBannerMeasurerSystem.ADMETRICA)
                                .withParams("{\"type\": \"banner\", \"criteria\": \"ya\", \"campaignId\": 4, " +
                                        "\"creativeId\": 4, \"placementId\": 4}")
                                .withHasIntegration(false)))
                .withAdditionalHrefs(emptyList());
    }

    public static OldCpmGeoPinBanner activeCpmGeoPinBanner(
            Long campaignId, Long adGroupId,
            Long creativeId, Long permalinkId) {
        return fillSystemFieldsForActiveBanner(new OldCpmGeoPinBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CPM_GEO_PIN)
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withPixels(singletonList("https://mc.yandex.ru/pixel/529875658445014901?rnd=%aw_random%"))
                .withMeasurers(List.of(
                        new OldBannerMeasurer()
                                .withBannerId(123L)
                                .withBannerMeasurerSystem(OldBannerMeasurerSystem.ADMETRICA)
                                .withParams("{\"type\": \"banner\", \"criteria\": \"ya\", \"campaignId\": 4, " +
                                        "\"creativeId\": 4, \"placementId\": 4}")
                                .withHasIntegration(false)))
                .withPermalinkId(permalinkId);
    }

    public static OldCpmBanner cpmBannerWithoutMeasurers(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldCpmBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CPM_BANNER)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru/company")
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withPixels(singletonList("https://mc.yandex.ru/pixel/529875658445014901?rnd=%aw_random%"))
                .withAdditionalHrefs(emptyList());
    }

    public static OldCpmBanner clientCpmBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return new OldCpmBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.CPM_BANNER)
                .withIsMobile(false)
                .withHref("https://www.yandex.ru/company")
                .withCreativeId(creativeId)
                .withPixels(singletonList("https://mc.yandex.ru/pixel/529875658445014901?rnd=%aw_random%"))
                .withMeasurers(List.of(
                        new OldBannerMeasurer()
                                .withBannerId(123L)
                                .withBannerMeasurerSystem(OldBannerMeasurerSystem.ADMETRICA)
                                .withParams("{\"type\": \"banner\", \"criteria\": \"ya\", \"campaignId\": 4, " +
                                        "\"creativeId\": 4, \"placementId\": 4}")
                                .withHasIntegration(false)));
    }

    public static OldCpmBanner activeCpmVideoBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return activeCpmBanner(campaignId, adGroupId, creativeId)
                .withPixels(singletonList(adfoxPixelUrl()));
    }

    public static OldCpmBanner activeCpmBannerWithTurbolanding(Long campaignId, Long adGroupId, Long creativeId,
                                                               Long turbolandingId) {
        return activeCpmBanner(campaignId, adGroupId, creativeId)
                .withHref(null)
                .withTurboLandingId(turbolandingId)
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES);
    }

    public static OldCpmOutdoorBanner activeCpmOutdoorBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldCpmOutdoorBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CPM_OUTDOOR)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru")
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN);
    }

    public static OldCpmIndoorBanner activeCpmIndoorBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldCpmIndoorBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.CPM_INDOOR)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref("https://www.yandex.ru")
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN);
    }

    public static OldCpmIndoorBanner clientCpmIndoorBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return new OldCpmIndoorBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.CPM_INDOOR)
                .withIsMobile(false)
                .withHref("https://www.yandex.ru")
                .withCreativeId(creativeId);
    }

    public static OldPerformanceBanner activePerformanceBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return fillSystemFieldsForActiveBanner(new OldPerformanceBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.PERFORMANCE)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withCreativeId(creativeId)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
    }

    public static OldPerformanceBanner clientPerformanceBanner(Long campaignId, Long adGroupId, Long creativeId) {
        return new OldPerformanceBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.PERFORMANCE)
                .withIsMobile(false)
                .withCreativeId(creativeId);
    }

    public static OldInternalBanner activeInternalBanner(Long campaignId, Long adGroupId) {
        return activeInternalBanner(campaignId, adGroupId,
                TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1,
                TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED);
    }

    public static OldInternalBanner activeInternalBanner(Long campaignId, Long adGroupId, long templateId,
                                                         long resourceId) {
        return fillSystemFieldsForActiveBanner(new OldInternalBanner())
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID)
                .withBannerType(OldBannerType.INTERNAL)
                .withIsMobile(false)
                .withPhoneFlag(StatusPhoneFlagModerate.NEW)
                .withHref(null)
                .withDescription("flylogo-oldprotect7_LL")
                .withTemplateId(templateId)
                .withTemplateVariables(
                        singletonList(new TemplateVariable()
                                .withTemplateResourceId(resourceId)
                                .withInternalValue("bbb")));
    }

    public static OldInternalBanner activeInternalBannerWithManyResources(Long campaignId, Long adGroupId,
                                                                          String res1Value, String res2Value,
                                                                          String res3Value, String res4Value) {
        return activeInternalBanner(campaignId, adGroupId)
                .withTemplateId(PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES)
                .withTemplateVariables(
                        List.of(
                                new TemplateVariable()
                                        .withTemplateResourceId(TEMPLATE_8_RESOURCE_1)
                                        .withInternalValue(res1Value),
                                new TemplateVariable()
                                        .withTemplateResourceId(TEMPLATE_8_RESOURCE_2)
                                        .withInternalValue(res2Value),
                                new TemplateVariable()
                                        .withTemplateResourceId(TEMPLATE_8_RESOURCE_3)
                                        .withInternalValue(res3Value),
                                new TemplateVariable()
                                        .withTemplateResourceId(TEMPLATE_8_RESOURCE_4)
                                        .withInternalValue(res4Value))
                );
    }

    public static OldInternalBanner clientInternalBanner(Long campaignId, Long adGroupId) {
        return new OldInternalBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.INTERNAL)
                .withIsMobile(false)
                .withHref(null)
                .withDescription("flylogo-oldprotect7_LL")
                .withTemplateId(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1)
                .withTemplateVariables(
                        singletonList(new TemplateVariable().withTemplateResourceId(
                                        TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED)
                                .withInternalValue("bbb")));
    }

    public static OldContentPromotionBanner clientContentPromotionBanner(Long campaignId, Long adGroupId) {
        return new OldContentPromotionBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId)
                .withBannerType(OldBannerType.CONTENT_PROMOTION)
                .withTitle("title")
                .withBody("description")
                .withContentPromotionId(VALID_CONTENT_PROMOTION_ID)
                .withVisitUrl("https://www.yandex.ru/");
    }

    public static List<OldBannerAdditionalHref> clientBannerAdditionalHrefs() {
        return List.of(
                new OldBannerAdditionalHref().withHref("http://google.com"),
                new OldBannerAdditionalHref().withHref("http://yahoo.com")
        );
    }

    public static List<BannerAdditionalHref> clientNewBannerAdditionalHrefs() {
        return List.of(
                new BannerAdditionalHref().withHref("http://google.com"),
                new BannerAdditionalHref().withHref("http://yahoo.com")
        );
    }
}
