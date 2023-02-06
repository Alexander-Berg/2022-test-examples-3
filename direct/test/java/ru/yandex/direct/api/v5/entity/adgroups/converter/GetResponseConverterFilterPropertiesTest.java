package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.adgroups.AdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupGetItem;
import com.yandex.direct.api.v5.adgroups.AdGroupSubtypeEnum;
import com.yandex.direct.api.v5.adgroups.AppAvailabilityStatusEnum;
import com.yandex.direct.api.v5.adgroups.DynamicTextAdGroupGet;
import com.yandex.direct.api.v5.adgroups.DynamicTextFeedAdGroupGet;
import com.yandex.direct.api.v5.adgroups.MobileAppAdGroupFieldEnum;
import com.yandex.direct.api.v5.adgroups.MobileAppAdGroupGet;
import com.yandex.direct.api.v5.adgroups.ObjectFactory;
import com.yandex.direct.api.v5.adgroups.SourceProcessingStatusEnum;
import com.yandex.direct.api.v5.adgroups.SourceTypeGetEnum;
import com.yandex.direct.api.v5.adgroups.TargetCarrierEnum;
import com.yandex.direct.api.v5.adgroups.TargetDeviceTypeEnum;
import com.yandex.direct.api.v5.general.AdGroupTypesEnum;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.AutotargetingCategoryArray;
import com.yandex.direct.api.v5.general.ExtensionModeration;
import com.yandex.direct.api.v5.general.MobileOperatingSystemTypeEnum;
import com.yandex.direct.api.v5.general.ServingStatusEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.entity.adgroups.delegate.AdGroupAnyFieldEnum;
import ru.yandex.direct.api.v5.entity.ads.converter.LogoConverter;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.util.PropertyFilter;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class GetResponseConverterFilterPropertiesTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    private static final Long id = 1L;
    private static final String name = "adgroup";
    private static final Long campaignId = 2L;
    private static final StatusEnum status = StatusEnum.ACCEPTED;
    private static final AdGroupTypesEnum typeText = AdGroupTypesEnum.TEXT_AD_GROUP;
    private static final AdGroupSubtypeEnum subtypeNone = AdGroupSubtypeEnum.NONE;
    private static final ServingStatusEnum servingStatus = ServingStatusEnum.ELIGIBLE;
    private static final JAXBElement<ArrayOfLong> restrictedRegionIds =
            FACTORY.createAdGroupGetItemRestrictedRegionIds(new ArrayOfLong().withItems(187L, 983L));
    private static final List<Long> regionIds = Arrays.asList(225L, 213L);
    private static final JAXBElement<ArrayOfString> negativeKeywords =
            FACTORY.createAdGroupBaseNegativeKeywords(new ArrayOfString().withItems("очень", "остроумная", "фраза"));
    private static final JAXBElement<ArrayOfLong> negativeKeywordSharedSetIds =
            FACTORY.createAdGroupBaseNegativeKeywordSharedSetIds(new ArrayOfLong().withItems(1L, 2L, 3L));
    private static final String trackingParams = "from=direct&ad={ad_id}";
    private static final AdGroupTypesEnum typeMobileApp = AdGroupTypesEnum.MOBILE_APP_AD_GROUP;
    private static final String url = "https://yandex.ru";
    private static final Long feedId = 55L;
    private static final TargetDeviceTypeEnum targetDeviceType = TargetDeviceTypeEnum.DEVICE_TYPE_MOBILE;
    private static final TargetCarrierEnum targetCarrier = TargetCarrierEnum.WI_FI_AND_CELLULAR;
    private static final String targetOperationSystemVersion = "vedriod super edition";
    private static final JAXBElement<ExtensionModeration> appIconModeration =
            FACTORY.createMobileAppAdGroupGetAppIconModeration(new ExtensionModeration().withStatus(status));
    private static final MobileOperatingSystemTypeEnum appOperatingSystemType = MobileOperatingSystemTypeEnum.ANDROID;
    private static final AppAvailabilityStatusEnum appAvailabilityStatus = AppAvailabilityStatusEnum.AVAILABLE;
    private static final AdGroupTypesEnum typeDynamicText = AdGroupTypesEnum.DYNAMIC_TEXT_AD_GROUP;
    private static final AutotargetingCategoryArray autotargetingCategories = new AutotargetingCategoryArray()
            .withItems(new AutotargetingCategory()
                            .withCategory(AutotargetingCategoriesEnum.ACCESSORY)
                            .withValue(YesNoEnum.NO),
                    new AutotargetingCategory()
                            .withCategory(AutotargetingCategoriesEnum.ALTERNATIVE)
                            .withValue(YesNoEnum.YES),
                    new AutotargetingCategory()
                            .withCategory(AutotargetingCategoriesEnum.BROADER)
                            .withValue(YesNoEnum.YES),
                    new AutotargetingCategory()
                            .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                            .withValue(YesNoEnum.NO),
                    new AutotargetingCategory()
                            .withCategory(AutotargetingCategoriesEnum.EXACT)
                            .withValue(YesNoEnum.YES));
    private static final AdGroupSubtypeEnum subtypeWebpage = AdGroupSubtypeEnum.WEBPAGE;
    private static final SourceProcessingStatusEnum sourceProcessingStatus = SourceProcessingStatusEnum.PROCESSED;
    private static final AdGroupSubtypeEnum subtypeFeed = AdGroupSubtypeEnum.FEED;
    private static final SourceTypeGetEnum sourceType = SourceTypeGetEnum.RETAIL_FEED;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<AdGroupGetItem> items;

    @Parameterized.Parameter(2)
    public Set<AdGroupAnyFieldEnum> requestedFields;

    @Parameterized.Parameter(3)
    public List<AdGroupGetItem> expectedItems;

    private GetResponseConverter converter;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                // text
                {"filter out all fields except id", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_ID),
                        Collections.singletonList(new AdGroupGetItem().withId(id))},
                {"filter out all fields except name", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_NAME),
                        Collections.singletonList(new AdGroupGetItem().withName(name))},
                {"filter out all fields except campaignId", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_CAMPAIGN_ID),
                        Collections.singletonList(new AdGroupGetItem().withCampaignId(campaignId))},
                {"filter out all fields except status", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_STATUS),
                        Collections.singletonList(new AdGroupGetItem().withStatus(status))},
                {"filter out all fields except type", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_TYPE),
                        Collections.singletonList(new AdGroupGetItem().withType(typeText))},
                {"filter out all fields except subtype", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_SUBTYPE),
                        Collections.singletonList(new AdGroupGetItem().withSubtype(subtypeNone))},
                {"filter out all fields except serving status", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_SERVING_STATUS),
                        Collections.singletonList(new AdGroupGetItem().withServingStatus(servingStatus))},
                {"filter out all fields except restricted region ids",
                        Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_RESTRICTED_REGION_IDS),
                        Collections.singletonList(new AdGroupGetItem().withRestrictedRegionIds(restrictedRegionIds))},
                {"filter out all fields except region ids", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_REGION_IDS),
                        Collections.singletonList(new AdGroupGetItem().withRegionIds(regionIds))},
                {"filter out all fields except negative keywords", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_NEGATIVE_KEYWORDS),
                        Collections.singletonList(new AdGroupGetItem().withNegativeKeywords(negativeKeywords))},
                {"filter out all fields except negative keyword shared sets",
                        Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_NEGATIVE_KEYWORD_SHARED_SET_IDS),
                        Collections.singletonList(new AdGroupGetItem()
                                .withNegativeKeywordSharedSetIds(negativeKeywordSharedSetIds))},
                {"filter out all fields except tracking params", Collections.singletonList(buildTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.AD_GROUP_TRACKING_PARAMS),
                        Collections.singletonList(new AdGroupGetItem().withTrackingParams(trackingParams))},
                {"filter out all fields", Collections.singletonList(buildTextAdGroupItem()), Collections.emptySet(),
                        Collections.singletonList(new AdGroupGetItem())},
                // mobile app
                {"filter out all fields except store url", Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_STORE_URL),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet().withStoreUrl(url)))},
                {"filter out all fields except target device type",
                        Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_TARGET_DEVICE_TYPE),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet().withTargetDeviceType(targetDeviceType)))},
                {"filter out all fields except target carrier", Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_TARGET_CARRIER),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet().withTargetCarrier(targetCarrier)))},
                {"filter out all fields except target operating system version",
                        Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_TARGET_OPERATING_SYSTEM_VERSION),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet()
                                        .withTargetOperatingSystemVersion(targetOperationSystemVersion)))},
                {"filter out all fields except app icon moderation",
                        Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_APP_ICON_MODERATION),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet().withAppIconModeration(appIconModeration)))},
                {"filter out all fields except app operating system type",
                        Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_APP_OPERATING_SYSTEM_TYPE),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet()
                                        .withAppOperatingSystemType(appOperatingSystemType)))},
                {"filter out all fields except app availability status",
                        Collections.singletonList(buildMobileAppAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.MOBILE_APP_AD_GROUP_APP_AVAILABILITY_STATUS),
                        Collections.singletonList(new AdGroupGetItem().withMobileAppAdGroup(
                                FACTORY.createMobileAppAdGroupGet()
                                        .withAppAvailabilityStatus(appAvailabilityStatus)))},
                {"filter out all fields", Collections.singletonList(buildMobileAppAdGroupItem()),
                        Collections.emptySet(), Collections.singletonList(new AdGroupGetItem())},
                // dynamic text
                {"filter out all fields except autotargeting categories",
                        Collections.singletonList(buildDynamicTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_AD_GROUP_AUTOTARGETING_CATEGORIES),
                        Collections.singletonList(new AdGroupGetItem().withDynamicTextAdGroup(
                                FACTORY.createDynamicTextAdGroupGet()
                                        .withAutotargetingCategories(autotargetingCategories)))},
                {"filter out all fields except domail url", Collections.singletonList(buildDynamicTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_AD_GROUP_DOMAIN_URL),
                        Collections.singletonList(new AdGroupGetItem().withDynamicTextAdGroup(
                                FACTORY.createDynamicTextAdGroupGet().withDomainUrl(url)))},
                {"filter out all fields except domain url processing status",
                        Collections.singletonList(buildDynamicTextAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_AD_GROUP_DOMAIN_URL_PROCESSING_STATUS),
                        Collections.singletonList(
                                new AdGroupGetItem().withDynamicTextAdGroup(FACTORY.createDynamicTextAdGroupGet()
                                        .withDomainUrlProcessingStatus(sourceProcessingStatus)))},
                {"filter out all fields", Collections.singletonList(buildDynamicTextAdGroupItem()),
                        Collections.emptySet(), Collections.singletonList(new AdGroupGetItem())},
                // dynamic feed
                {"filter out all fields except autotargeting categories",
                        Collections.singletonList(buildDynamicTextFeedAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_AUTOTARGETING_CATEGORIES),
                        Collections.singletonList(new AdGroupGetItem().withDynamicTextFeedAdGroup(
                                FACTORY.createDynamicTextFeedAdGroupGet()
                                        .withAutotargetingCategories(autotargetingCategories)))},
                {"filter out all fields except source", Collections.singletonList(buildDynamicTextFeedAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_SOURCE),
                        Collections.singletonList(new AdGroupGetItem().withDynamicTextFeedAdGroup(
                                FACTORY.createDynamicTextFeedAdGroupGet().withSource(url)))},
                {"filter out all fields except feed id", Collections.singletonList(buildDynamicTextFeedAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_FEED_ID),
                        Collections.singletonList(new AdGroupGetItem().withDynamicTextFeedAdGroup(
                                FACTORY.createDynamicTextFeedAdGroupGet().withFeedId(feedId)))},
                {"filter out all fields except source type",
                        Collections.singletonList(buildDynamicTextFeedAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_SOURCE_TYPE),
                        Collections.singletonList(new AdGroupGetItem().withDynamicTextFeedAdGroup(
                                FACTORY.createDynamicTextFeedAdGroupGet().withSourceType(sourceType)))},
                {"filter out all fields except source processing status",
                        Collections.singletonList(buildDynamicTextFeedAdGroupItem()),
                        EnumSet.of(AdGroupAnyFieldEnum.DYNAMIC_TEXT_FEED_AD_GROUP_SOURCE_PROCESSING_STATUS),
                        Collections.singletonList(new AdGroupGetItem()
                                .withDynamicTextFeedAdGroup(FACTORY.createDynamicTextFeedAdGroupGet()
                                        .withSourceProcessingStatus(sourceProcessingStatus)))},
                {"filter out all fields", Collections.singletonList(buildDynamicTextAdGroupItem()),
                        Collections.emptySet(), Collections.singletonList(new AdGroupGetItem())},
                //
                {"try to leave not exist fields",
                        Collections.singletonList(buildTextAdGroupItem()),
                        Arrays.stream(MobileAppAdGroupFieldEnum.values())
                                .map(AdGroupAnyFieldEnum::fromMobileAppAdGroupFieldEnum).collect(toSet()),
                        Collections.singletonList(new AdGroupGetItem())},
                {"filter out all except base fields",
                        Collections.singletonList(buildDynamicTextFeedAdGroupItem()),
                        Arrays.stream(AdGroupFieldEnum.values()).map(AdGroupAnyFieldEnum::fromAdGroupFieldEnum).collect(
                                toSet()),
                        Collections.singletonList(buildDynamicTextFeedAdGroupItem().withDynamicTextFeedAdGroup(null))},
                // several types at once
                {"filter out all fields", buildAdGroupItemsOfAllTypes(), Collections.emptySet(),
                        Arrays.asList(new AdGroupGetItem(), new AdGroupGetItem(), new AdGroupGetItem(),
                                new AdGroupGetItem())},
                {"leave all fields", buildAdGroupItemsOfAllTypes(), EnumSet.allOf(AdGroupAnyFieldEnum.class),
                        buildAdGroupItemsOfAllTypes()},
        };
    }

    private static AdGroupGetItem buildTextAdGroupItem() {
        return new AdGroupGetItem()
                .withId(id)
                .withName(name)
                .withCampaignId(campaignId)
                .withStatus(status)
                .withType(typeText)
                .withSubtype(subtypeNone)
                .withServingStatus(servingStatus)
                .withRestrictedRegionIds(restrictedRegionIds)
                .withRegionIds(regionIds)
                .withNegativeKeywords(negativeKeywords)
                .withNegativeKeywordSharedSetIds(negativeKeywordSharedSetIds)
                .withTrackingParams(trackingParams);
    }

    private static AdGroupGetItem buildMobileAppAdGroupItem() {
        MobileAppAdGroupGet mobileAppItem =
                FACTORY.createMobileAppAdGroupGet()
                        .withStoreUrl(url)
                        .withTargetDeviceType(targetDeviceType)
                        .withTargetCarrier(targetCarrier)
                        .withTargetOperatingSystemVersion(targetOperationSystemVersion)
                        .withAppIconModeration(appIconModeration)
                        .withAppOperatingSystemType(appOperatingSystemType)
                        .withAppAvailabilityStatus(appAvailabilityStatus);

        return buildTextAdGroupItem()
                .withType(typeMobileApp)
                .withMobileAppAdGroup(mobileAppItem);
    }

    private static AdGroupGetItem buildDynamicTextAdGroupItem() {
        DynamicTextAdGroupGet dynamicTextItem =
                FACTORY.createDynamicTextAdGroupGet()
                        .withAutotargetingCategories(autotargetingCategories)
                        .withDomainUrl(url)
                        .withDomainUrlProcessingStatus(sourceProcessingStatus);

        return buildTextAdGroupItem()
                .withType(typeDynamicText)
                .withSubtype(subtypeWebpage)
                .withDynamicTextAdGroup(dynamicTextItem);
    }

    private static AdGroupGetItem buildDynamicTextFeedAdGroupItem() {
        DynamicTextFeedAdGroupGet dynamicTextFeedItem =
                FACTORY.createDynamicTextFeedAdGroupGet()
                        .withAutotargetingCategories(autotargetingCategories)
                        .withSource(url)
                        .withFeedId(feedId)
                        .withSourceType(sourceType)
                        .withSourceProcessingStatus(sourceProcessingStatus);

        return buildTextAdGroupItem()
                .withType(typeDynamicText)
                .withSubtype(subtypeFeed)
                .withDynamicTextFeedAdGroup(dynamicTextFeedItem);
    }

    private static List<AdGroupGetItem> buildAdGroupItemsOfAllTypes() {
        return Arrays.asList(buildTextAdGroupItem(), buildMobileAppAdGroupItem(), buildDynamicTextAdGroupItem(),
                buildDynamicTextFeedAdGroupItem());
    }

    @Before
    public void preparations() {
        converter = new GetResponseConverter(new PropertyFilter(), mock(TranslationService.class),
                mock(LogoConverter.class));
    }

    @Test
    public void test() {
        converter.filterProperties(items, requestedFields);
        assertThat(items, beanDiffer(expectedItems));
    }
}
