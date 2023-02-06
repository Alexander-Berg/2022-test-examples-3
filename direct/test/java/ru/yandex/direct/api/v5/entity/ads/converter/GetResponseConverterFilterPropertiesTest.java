package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.ads.AdBuilderAdGetItem;
import com.yandex.direct.api.v5.ads.AdCategoryEnum;
import com.yandex.direct.api.v5.ads.AdExtensionAdGetItem;
import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.AdGetItem;
import com.yandex.direct.api.v5.ads.AdSubtypeEnum;
import com.yandex.direct.api.v5.ads.AdTypeEnum;
import com.yandex.direct.api.v5.ads.AgeLabelEnum;
import com.yandex.direct.api.v5.ads.ArrayOfAdCategoryEnum;
import com.yandex.direct.api.v5.ads.ButtonExtensionAction;
import com.yandex.direct.api.v5.ads.ButtonExtensionGetItem;
import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdGet;
import com.yandex.direct.api.v5.ads.CpmBannerAdBuilderAdGet;
import com.yandex.direct.api.v5.ads.DynamicTextAdGet;
import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdGet;
import com.yandex.direct.api.v5.ads.MobileAppAdFeatureGetItem;
import com.yandex.direct.api.v5.ads.MobileAppAdFieldEnum;
import com.yandex.direct.api.v5.ads.MobileAppAdGet;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdGet;
import com.yandex.direct.api.v5.ads.MobileAppFeatureEnum;
import com.yandex.direct.api.v5.ads.MobileAppImageAdGet;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.PriceExtensionGetItem;
import com.yandex.direct.api.v5.ads.TextAdBuilderAdGet;
import com.yandex.direct.api.v5.ads.TextAdGet;
import com.yandex.direct.api.v5.ads.TextImageAdGet;
import com.yandex.direct.api.v5.ads.TrackingPixelGetArray;
import com.yandex.direct.api.v5.ads.TrackingPixelGetItem;
import com.yandex.direct.api.v5.ads.VideoExtensionGetItem;
import com.yandex.direct.api.v5.general.ExtensionModeration;
import com.yandex.direct.api.v5.general.MobileAppAdActionEnum;
import com.yandex.direct.api.v5.general.StateEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.general.YesNoUnknownEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.entity.ads.delegate.AdAnyFieldEnum;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.util.PropertyFilter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.defaultNewBannerPrice;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetResponseConverterFilterPropertiesTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private static final Long id = 1L;
    private static final Long adgroupId = 2L;
    private static final Long campaignId = 3L;
    private static final StateEnum state = StateEnum.ON;
    private static final StatusEnum status = StatusEnum.ACCEPTED;
    private static final String statusClarification = "Потому что!";
    private static final JAXBElement<ArrayOfAdCategoryEnum> adCategories = FACTORY.createAdGetItemAdCategories(
            new ArrayOfAdCategoryEnum().withItems(AdCategoryEnum.BABY_FOOD, AdCategoryEnum.DIETARY_SUPPLEMENTS));
    private static final JAXBElement<AgeLabelEnum> ageLabel = FACTORY.createAdGetItemAgeLabel(AgeLabelEnum.AGE_18);
    private static final AdTypeEnum type = AdTypeEnum.TEXT_AD;
    private static final AdSubtypeEnum subtype = AdSubtypeEnum.NONE;
    private static final String title = "title";
    private static final JAXBElement<String> title2 = FACTORY.createTextAdGetTitle2("title2");
    private static final String text = "text";
    private static final String href = "href";
    private static final JAXBElement<String> textAdHref = FACTORY.createTextAdGetHref(href);
    private static final JAXBElement<String> textImageAdHref = FACTORY.createTextImageAdGetHref(href);
    private static final JAXBElement<String> cpmBannerAdBuilderAdHref = FACTORY.createCpmBannerAdBuilderAdGetHref(href);
    private static final JAXBElement<String> textAdBuilderAdHref = FACTORY.createTextAdBuilderAdGetHref(href);
    private static final YesNoEnum isMobile = YesNoEnum.YES;
    private static final JAXBElement<String> displayDomain = FACTORY.createTextAdGetDisplayDomain("displayDomain");
    private static final JAXBElement<String> displayUrlPath = FACTORY.createTextAdGetDisplayUrlPath("displayUrlPath");
    private static final JAXBElement<Long> vcardId = FACTORY.createTextAdGetBaseVCardId(1L);
    private static final String imageHash = "adImageHash";
    private static final JAXBElement<String> adImageHash = FACTORY.createTextAdGetBaseAdImageHash(imageHash);
    private static final JAXBElement<Long> sitelinkSetId = FACTORY.createTextAdGetBaseSitelinkSetId(1L);
    private static final List<AdExtensionAdGetItem> adExtensions = singletonList(
            FACTORY.createAdExtensionAdGetItem().withAdExtensionId(1L).withType(AdExtensionTypeEnum.CALLOUT));
    private static final ExtensionModeration extensionModeration =
            new ExtensionModeration().withStatus(StatusEnum.ACCEPTED)
                    .withStatusClarification("Extension status clarification");
    private static final JAXBElement<ExtensionModeration> adImageModeration =
            FACTORY.createTextAdGetBaseAdImageModeration(extensionModeration);
    private static final JAXBElement<ExtensionModeration> displayUrlPathModeration =
            FACTORY.createTextAdGetDisplayUrlPathModeration(extensionModeration);
    private static final JAXBElement<ExtensionModeration> sitelinksModeration =
            FACTORY.createTextAdGetBaseSitelinksModeration(extensionModeration);
    private static final JAXBElement<ExtensionModeration> vCardModeration =
            FACTORY.createTextAdGetBaseVCardModeration(extensionModeration);
    private static final JAXBElement<VideoExtensionGetItem> videoExtension = FACTORY.createTextAdGetVideoExtension(
            new VideoExtensionGetItem().withCreativeId(1L).withPreviewUrl("previewUrl").withThumbnailUrl("thumbnailUrl")
                    .withStatus(StatusEnum.ACCEPTED));
    private static final ButtonExtensionGetItem buttonExt =
            new ButtonExtensionGetItem().withHref("ya.ru").withAction(ButtonExtensionAction.APPLY);
    private static final JAXBElement<ButtonExtensionGetItem> buttonExtension =
            FACTORY.createTextAdGetButtonExtension(buttonExt);
    private static final String trackingUrl = "trackingUrl";
    private static final String impressionUrl = "impressionUrl";
    private static final JAXBElement<String> mobileAppTrackingUrl =
            FACTORY.createMobileAppAdBaseTrackingUrl(trackingUrl);
    private static final JAXBElement<String> mobileAppImpressionUrl =
            FACTORY.createMobileAppAdBaseImpressionUrl(impressionUrl);
    private static final MobileAppAdActionEnum action = MobileAppAdActionEnum.INSTALL;
    private static final JAXBElement<String> mobileAppAdImageHash = FACTORY.createMobileAppAdBaseAdImageHash(imageHash);
    private static final JAXBElement<ExtensionModeration> mobileAppAdImageModeration =
            FACTORY.createMobileAppAdGetAdImageModeration(extensionModeration);
    private static final List<MobileAppAdFeatureGetItem> features = singletonList(
            new MobileAppAdFeatureGetItem().withIsAvailable(YesNoUnknownEnum.YES).withEnabled(YesNoEnum.YES)
                    .withFeature(MobileAppFeatureEnum.ICON));
    private static final JAXBElement<String> mobileAppImageAdTrackingUrl =
            FACTORY.createMobileAppImageAdGetTrackingUrl(trackingUrl);
    private static final JAXBElement<String> mobileAppAdBuilderTrackingUrl =
            FACTORY.createMobileAppAdBuilderAdGetTrackingUrl(trackingUrl);
    private static final AdBuilderAdGetItem creative =
            FACTORY.createAdBuilderAdGetItem().withCreativeId(1L).withPreviewUrl("previewUrl")
                    .withThumbnailUrl("thumbnailUrl");
    private static final TrackingPixelGetItem trackingPixel = FACTORY.createTrackingPixelGetItem()
            .withTrackingPixel("somePixel")
            .withProvider("someProvider");
    private static final JAXBElement<PriceExtensionGetItem> priceExtension =
            FACTORY.createTextAdGetPriceExtension(BannerPriceConverter.fromCore(defaultNewBannerPrice()));
    private static final JAXBElement<String> mobileAppCpcVideoAdTrackingUrl =
            FACTORY.createMobileAppCpcVideoAdBuilderAdGetTrackingUrl(trackingUrl);
    private static final JAXBElement<String> cpcVideoAdHref =
            FACTORY.createCpcVideoAdBuilderAdGetHref(href);

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<AdGetItem> items;

    @Parameterized.Parameter(2)
    public Set<AdAnyFieldEnum> requestedFields;

    @Parameterized.Parameter(3)
    public List<AdGetItem> expectedItems;

    private GetResponseConverter converter;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                //region base ad fields
                {"filter out all fields except id", singletonList(buildAdGetItem()), EnumSet.of(AdAnyFieldEnum.AD_ID),
                        singletonList(new AdGetItem().withId(id))},
                {"filter out all fields except adgroup id", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_ADGROUP_ID),
                        singletonList(new AdGetItem().withAdGroupId(adgroupId))},
                {"filter out all fields except campaign id", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_CAMPAIGN_ID),
                        singletonList(new AdGetItem().withCampaignId(campaignId))},
                {"filter out all fields except state", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_STATE), singletonList(new AdGetItem().withState(state))},
                {"filter out all fields except status", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_STATUS), singletonList(new AdGetItem().withStatus(status))},
                {"filter out all fields except status clarification", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_STATUS_CLARIFICATION),
                        singletonList(new AdGetItem().withStatusClarification(statusClarification))},
                {"filter out all fields except ad categories", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_CATEGORIES),
                        singletonList(new AdGetItem().withAdCategories(adCategories))},
                {"filter out all fields except age label", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_AGE_LABEL), singletonList(new AdGetItem().withAgeLabel(ageLabel))},
                {"filter out all fields except type", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_TYPE), singletonList(new AdGetItem().withType(type))},
                {"filter out all fields except subtype", singletonList(buildAdGetItem()),
                        EnumSet.of(AdAnyFieldEnum.AD_SUBTYPE), singletonList(new AdGetItem().withSubtype(subtype))},
                //endregion
                //region text ad fields
                {"filter out all fields except title", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_TITLE),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withTitle(title)))},
                {"filter out all fields except title2", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_TITLE_2),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withTitle2(title2)))},
                {"filter out all fields except text", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_TEXT),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withText(text)))},
                {"filter out all fields except href", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_HREF),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withHref(textAdHref)))},
                {"filter out all fields except mobile", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_MOBILE),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withMobile(isMobile)))},
                {"filter out all fields except display domain", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_DISPLAY_DOMAIN),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withDisplayDomain(displayDomain)))},
                {"filter out all fields except display url path", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_DISPLAY_URL_PATH),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withDisplayUrlPath(displayUrlPath)))},
                {"filter out all fields except vcard id", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_V_CARD_ID),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withVCardId(vcardId)))},
                {"filter out all fields except ad image hash", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_IMAGE_HASH),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withAdImageHash(adImageHash)))},
                {"filter out all fields except sitelink set id", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_SITELINK_SET_ID),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withSitelinkSetId(sitelinkSetId)))},
                {"filter out all fields except ad extensions", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_EXTENSIONS),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withAdExtensions(adExtensions)))},
                {"filter out all fields except ad image moderation", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_IMAGE_MODERATION), singletonList(
                        new AdGetItem().withTextAd(new TextAdGet().withAdImageModeration(adImageModeration)))},
                {"filter out all fields except display url path moderation", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_DISPLAY_URL_PATH_MODERATION), singletonList(new AdGetItem()
                        .withTextAd(new TextAdGet().withDisplayUrlPathModeration(displayUrlPathModeration)))},
                {"filter out all fields except sitelinks moderation", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_SITELINKS_MODERATION), singletonList(
                        new AdGetItem().withTextAd(new TextAdGet().withSitelinksModeration(sitelinksModeration)))},
                {"filter out all fields except vcard moderation", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_V_CARD_MODERATION), singletonList(
                        new AdGetItem().withTextAd(new TextAdGet().withVCardModeration(vCardModeration)))},
                {"filter out all fields except video extension", singletonList(buildTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_VIDEO_EXTENSION),
                        singletonList(new AdGetItem().withTextAd(new TextAdGet().withVideoExtension(videoExtension)))},
                //endregion
                //region mobile app ad fields
                {"filter out all fields except title", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_TITLE),
                        singletonList(new AdGetItem().withMobileAppAd(new MobileAppAdGet().withTitle(title)))},
                {"filter out all fields except text", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_TEXT),
                        singletonList(new AdGetItem().withMobileAppAd(new MobileAppAdGet().withText(text)))},
                {"filter out all fields except tracking url", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_TRACKING_URL), singletonList(
                        new AdGetItem().withMobileAppAd(new MobileAppAdGet().withTrackingUrl(mobileAppTrackingUrl)))},
                {"filter out all fields except action", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_ACTION),
                        singletonList(new AdGetItem().withMobileAppAd(new MobileAppAdGet().withAction(action)))},
                {"filter out all fields except ad image hash", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_IMAGE_HASH), singletonList(
                        new AdGetItem().withMobileAppAd(new MobileAppAdGet().withAdImageHash(mobileAppAdImageHash)))},
                {"filter out all fields except ad image moderation", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_IMAGE_MODERATION), singletonList(new AdGetItem()
                        .withMobileAppAd(new MobileAppAdGet().withAdImageModeration(mobileAppAdImageModeration)))},
                {"filter out all fields except features", singletonList(buildMobileAppAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_FEATURES),
                        singletonList(new AdGetItem().withMobileAppAd(new MobileAppAdGet().withFeatures(features)))},
                //endregion
                //region dynamic text ad fields
                {"filter out all fields except text", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_TEXT),
                        singletonList(new AdGetItem().withDynamicTextAd(new DynamicTextAdGet().withText(text)))},
                {"filter out all fields except vcard id", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_V_CARD_ID),
                        singletonList(new AdGetItem().withDynamicTextAd(new DynamicTextAdGet().withVCardId(vcardId)))},
                {"filter out all fields except image hash", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_IMAGE_HASH), singletonList(
                        new AdGetItem().withDynamicTextAd(new DynamicTextAdGet().withAdImageHash(adImageHash)))},
                {"filter out all fields except sitelink set id", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_SITELINK_SET_ID), singletonList(
                        new AdGetItem().withDynamicTextAd(new DynamicTextAdGet().withSitelinkSetId(sitelinkSetId)))},
                {"filter out all fields except ad extensions", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_EXTENSIONS), singletonList(
                        new AdGetItem().withDynamicTextAd(new DynamicTextAdGet().withAdExtensions(adExtensions)))},
                {"filter out all fields except vcard moderation", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_V_CARD_MODERATION), singletonList(new AdGetItem()
                        .withDynamicTextAd(new DynamicTextAdGet().withVCardModeration(vCardModeration)))},
                {"filter out all fields except sitelinks moderation", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_SITELINKS_MODERATION), singletonList(new AdGetItem()
                        .withDynamicTextAd(new DynamicTextAdGet().withSitelinksModeration(sitelinksModeration)))},
                {"filter out all fields except ad image moderation", singletonList(buildDynamicTextAdGet()),
                        EnumSet.of(AdAnyFieldEnum.DYNAMIC_TEXT_AD_IMAGE_MODERATION), singletonList(new AdGetItem()
                        .withDynamicTextAd(new DynamicTextAdGet().withAdImageModeration(adImageModeration)))},
                //endregion
                //region text image ad fields
                {"filter out all fields except hash", singletonList(buildTextImageAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_IMAGE_AD_HREF),
                        singletonList(new AdGetItem().withTextImageAd(
                                new TextImageAdGet().withHref(textImageAdHref)))},
                {"filter out all fields except ad image hash", singletonList(buildTextImageAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_IMAGE_AD_IMAGE_HASH),
                        singletonList(new AdGetItem().withTextImageAd(
                                new TextImageAdGet().withAdImageHash(imageHash)))},
                //endregion
                //region mobile app image ad fields
                {"filter out all fields except tracking url", singletonList(buildMobileAppImageAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_IMAGE_AD_TRACKING_URL),
                        singletonList(new AdGetItem().withMobileAppImageAd(
                                new MobileAppImageAdGet().withTrackingUrl(mobileAppImageAdTrackingUrl)))},
                {"filter out all fields except ad image hash", singletonList(buildMobileAppImageAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_IMAGE_AD_IMAGE_HASH),
                        singletonList(new AdGetItem().withMobileAppImageAd(
                                new MobileAppImageAdGet().withAdImageHash(imageHash)))},
                //endregion
                //region mobile app cpc_video ad fields
                {"filter out all fields except tracking url", singletonList(buildMobileAppCpcVideoAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_CPC_VIDEO_AD_BUILDER_AD_TRACKING_URL),
                        singletonList(new AdGetItem().withMobileAppCpcVideoAdBuilderAd(
                                new MobileAppCpcVideoAdBuilderAdGet().withTrackingUrl(mobileAppCpcVideoAdTrackingUrl)))},
                {"filter out all fields except creative", singletonList(buildMobileAppCpcVideoAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_CPC_VIDEO_AD_BUILDER_AD_CREATIVE),
                        singletonList(new AdGetItem().withMobileAppCpcVideoAdBuilderAd(
                                new MobileAppCpcVideoAdBuilderAdGet().withCreative(creative)))},
                //endregion
                //region cpc_video ad fields
                {"filter out all fields except href", singletonList(buildCpcVideoAdGet()),
                        EnumSet.of(AdAnyFieldEnum.CPC_VIDEO_AD_BUILDER_AD_HREF),
                        singletonList(new AdGetItem().withCpcVideoAdBuilderAd(
                                new CpcVideoAdBuilderAdGet().withHref(cpcVideoAdHref)))},
                {"filter out all fields except creative", singletonList(buildCpcVideoAdGet()),
                        EnumSet.of(AdAnyFieldEnum.CPC_VIDEO_AD_BUILDER_AD_CREATIVE),
                        singletonList(new AdGetItem().withCpcVideoAdBuilderAd(
                                new CpcVideoAdBuilderAdGet().withCreative(creative)))},
                //endregion
                //region text adbuilder ad fields
                {"filter out all fields except creative", singletonList(buildTextAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_BUILDER_AD_CREATIVE),
                        singletonList(new AdGetItem().withTextAdBuilderAd(
                                new TextAdBuilderAdGet().withCreative(creative)))},
                {"filter out all fields except href", singletonList(buildTextAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.TEXT_AD_BUILDER_AD_HREF),
                        singletonList(new AdGetItem().withTextAdBuilderAd(
                                new TextAdBuilderAdGet().withHref(textAdBuilderAdHref)))},
                //endregion
                //region mobile app adbuilder ad fields
                {"filter out all fields except creative", singletonList(buildMobileAppAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_BUILDER_AD_CREATIVE),
                        singletonList(new AdGetItem().withMobileAppAdBuilderAd(
                                new MobileAppAdBuilderAdGet().withCreative(creative)))},
                {"filter out all fields except tracking url", singletonList(buildMobileAppAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.MOBILE_APP_AD_BUILDER_AD_TRACKING_URL),
                        singletonList(new AdGetItem().withMobileAppAdBuilderAd(
                                new MobileAppAdBuilderAdGet().withTrackingUrl(mobileAppAdBuilderTrackingUrl)))},
                //endregion
                //region cpm_banner ad fields
                {"filter out all fields except creative", singletonList(buildCpmBannerAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.CPM_BANNER_AD_BUILDER_AD_CREATIVE),
                        singletonList(new AdGetItem()
                                .withCpmBannerAdBuilderAd(new CpmBannerAdBuilderAdGet().withCreative(creative)))},
                {"filter out all fields except href", singletonList(buildCpmBannerAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.CPM_BANNER_AD_BUILDER_AD_HREF),
                        singletonList(new AdGetItem()
                                .withCpmBannerAdBuilderAd(new CpmBannerAdBuilderAdGet().withHref(cpmBannerAdBuilderAdHref)))},
                {"filter out all fields except tracking pixels", singletonList(buildCpmBannerAdBuilderAdGet()),
                        EnumSet.of(AdAnyFieldEnum.CPM_BANNER_AD_BUILDER_AD_TRACKING_PIXELS),
                        singletonList(new AdGetItem()
                                .withCpmBannerAdBuilderAd(
                                        new CpmBannerAdBuilderAdGet().withTrackingPixels(
                                                FACTORY.createCpmBannerAdBuilderAdGetTrackingPixels(
                                                        new TrackingPixelGetArray().withItems(trackingPixel)))))},
                //endregion
                //region boarding cases
                {"try to leave not exist fields", singletonList(buildTextAdGet()),
                        Arrays.stream(MobileAppAdFieldEnum.values())
                                .map(AdAnyFieldEnum::fromMobileAppAdFieldEnum).collect(toSet()),
                        singletonList(new AdGetItem())},
                {"filter out all except base fields", singletonList(buildDynamicTextAdGet()),
                        Arrays.stream(AdFieldEnum.values()).map(AdAnyFieldEnum::fromAdFieldEnum).collect(toSet()),
                        singletonList(buildDynamicTextAdGet().withDynamicTextAd(null))},
                {"filter out all fields", buildAdGetItemsOfAllTypes(), emptySet(),
                        asList(new AdGetItem(), new AdGetItem(), new AdGetItem(), new AdGetItem(), new AdGetItem(),
                                new AdGetItem(), new AdGetItem(), new AdGetItem(), new AdGetItem())},
                {"leave all fields", buildAdGetItemsOfAllTypes(), EnumSet.allOf(AdAnyFieldEnum.class),
                        buildAdGetItemsOfAllTypes()},
                //endregion
        };
    }

    private static AdGetItem buildAdGetItem() {
        return FACTORY
                .createAdGetItem()
                .withId(id)
                .withAdGroupId(adgroupId)
                .withCampaignId(campaignId)
                .withState(state)
                .withStatus(status)
                .withStatusClarification(statusClarification)
                .withAdCategories(adCategories)
                .withAgeLabel(ageLabel)
                .withType(type)
                .withSubtype(subtype);
    }

    private static AdGetItem buildTextAdGet() {
        TextAdGet textAdGet = FACTORY
                .createTextAdGet()
                .withTitle(title)
                .withTitle2(title2)
                .withText(text)
                .withHref(textAdHref)
                .withMobile(isMobile)
                .withDisplayDomain(displayDomain)
                .withDisplayUrlPath(displayUrlPath)
                .withVCardId(vcardId)
                .withAdImageHash(adImageHash)
                .withSitelinkSetId(sitelinkSetId)
                .withAdExtensions(adExtensions)
                .withAdImageModeration(adImageModeration)
                .withDisplayUrlPathModeration(displayUrlPathModeration)
                .withSitelinksModeration(sitelinksModeration)
                .withVCardModeration(vCardModeration)
                .withVideoExtension(videoExtension)
                .withButtonExtension(buttonExtension)
                .withPriceExtension(priceExtension);
        return buildAdGetItem().withTextAd(textAdGet);
    }

    private static AdGetItem buildMobileAppAdGet() {
        MobileAppAdGet mobileAppAdGet = FACTORY
                .createMobileAppAdGet()
                .withTitle(title)
                .withText(text)
                .withTrackingUrl(mobileAppTrackingUrl)
                .withAction(action)
                .withAdImageHash(mobileAppAdImageHash)
                .withAdImageModeration(mobileAppAdImageModeration)
                .withFeatures(features);
        return buildAdGetItem().withType(AdTypeEnum.MOBILE_APP_AD).withMobileAppAd(mobileAppAdGet);
    }

    private static AdGetItem buildDynamicTextAdGet() {
        DynamicTextAdGet dynamicTextAdGet = FACTORY
                .createDynamicTextAdGet()
                .withText(text)
                .withVCardId(vcardId)
                .withAdImageHash(adImageHash)
                .withSitelinkSetId(sitelinkSetId)
                .withAdExtensions(adExtensions)
                .withVCardModeration(vCardModeration)
                .withSitelinksModeration(sitelinksModeration)
                .withAdImageModeration(adImageModeration);
        return buildAdGetItem().withType(AdTypeEnum.DYNAMIC_TEXT_AD).withDynamicTextAd(dynamicTextAdGet);
    }

    private static AdGetItem buildTextImageAdGet() {
        TextImageAdGet textImageAdGet = FACTORY
                .createTextImageAdGet()
                .withHref(textImageAdHref)
                .withAdImageHash(imageHash);
        return buildAdGetItem().withType(AdTypeEnum.IMAGE_AD).withSubtype(AdSubtypeEnum.TEXT_IMAGE_AD)
                .withTextImageAd(textImageAdGet);
    }

    private static AdGetItem buildMobileAppImageAdGet() {
        MobileAppImageAdGet mobileAppImageAdGet = FACTORY
                .createMobileAppImageAdGet()
                .withTrackingUrl(mobileAppImageAdTrackingUrl)
                .withAdImageHash(imageHash);
        return buildAdGetItem().withType(AdTypeEnum.IMAGE_AD).withSubtype(AdSubtypeEnum.MOBILE_APP_IMAGE_AD)
                .withMobileAppImageAd(mobileAppImageAdGet);
    }

    private static AdGetItem buildMobileAppCpcVideoAdGet() {
        MobileAppCpcVideoAdBuilderAdGet mobileAppCpcVideoAdGet = FACTORY
                .createMobileAppCpcVideoAdBuilderAdGet()
                .withTrackingUrl(mobileAppCpcVideoAdTrackingUrl)
                .withCreative(creative);
        return buildAdGetItem().withType(AdTypeEnum.CPC_VIDEO_AD).withSubtype(AdSubtypeEnum.MOBILE_APP_CPC_VIDEO_AD_BUILDER_AD)
                .withMobileAppCpcVideoAdBuilderAd(mobileAppCpcVideoAdGet);
    }

    private static AdGetItem buildCpcVideoAdGet() {
        CpcVideoAdBuilderAdGet cpcVideoAdGet = FACTORY
                .createCpcVideoAdBuilderAdGet()
                .withCreative(creative)
                .withHref(cpcVideoAdHref);
        return buildAdGetItem().withType(AdTypeEnum.CPC_VIDEO_AD).withSubtype(AdSubtypeEnum.NONE)
                .withCpcVideoAdBuilderAd(cpcVideoAdGet);
    }

    private static AdGetItem buildTextAdBuilderAdGet() {
        TextAdBuilderAdGet textAdBuilderAdGet = FACTORY
                .createTextAdBuilderAdGet()
                .withCreative(creative)
                .withHref(textAdBuilderAdHref);
        return buildAdGetItem().withType(AdTypeEnum.IMAGE_AD).withSubtype(AdSubtypeEnum.TEXT_AD_BUILDER_AD)
                .withTextAdBuilderAd(textAdBuilderAdGet);
    }

    private static AdGetItem buildMobileAppAdBuilderAdGet() {
        MobileAppAdBuilderAdGet mobileAppAdBuilderAdGet = FACTORY
                .createMobileAppAdBuilderAdGet()
                .withCreative(creative)
                .withTrackingUrl(mobileAppAdBuilderTrackingUrl);
        return buildAdGetItem().withType(AdTypeEnum.IMAGE_AD).withSubtype(AdSubtypeEnum.MOBILE_APP_AD_BUILDER_AD)
                .withMobileAppAdBuilderAd(mobileAppAdBuilderAdGet);
    }

    private static AdGetItem buildCpmBannerAdBuilderAdGet() {
        CpmBannerAdBuilderAdGet cpmBannerAdBuilderAdGet = FACTORY
                .createCpmBannerAdBuilderAdGet()
                .withCreative(creative)
                .withHref(cpmBannerAdBuilderAdHref)
                .withTrackingPixels(FACTORY.createCpmBannerAdBuilderAdGetTrackingPixels(
                        new TrackingPixelGetArray().withItems(trackingPixel)));
        return buildAdGetItem()
                .withType(AdTypeEnum.CPM_BANNER_AD)
                .withCpmBannerAdBuilderAd(cpmBannerAdBuilderAdGet);
    }

    private static List<AdGetItem> buildAdGetItemsOfAllTypes() {
        return asList(buildTextAdGet(), buildMobileAppAdGet(), buildDynamicTextAdGet(), buildTextImageAdGet(),
                buildMobileAppImageAdGet(), buildTextAdBuilderAdGet(), buildMobileAppAdBuilderAdGet(),
                buildCpmBannerAdBuilderAdGet(), buildMobileAppCpcVideoAdGet());
    }

    @Before
    public void preparations() {
        converter = new GetResponseConverter(new PropertyFilter(), mock(TranslationService.class),
                mock(ButtonExtensionConverter.class), mock(LogoConverter.class));
    }

    @Test
    public void test() {
        converter.filterProperties(items, requestedFields);
        assertThat(items, beanDiffer(expectedItems));
    }

}
