package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.Set;

import com.yandex.direct.api.v5.adgroups.AdGroupAddItem;
import com.yandex.direct.api.v5.adgroups.ContentPromotionAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.CpmBannerKeywordsAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.CpmBannerUserProfileAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.DynamicTextFeedAdGroup;
import com.yandex.direct.api.v5.adgroups.MobileAppAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.PromotedContentTypeEnum;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupAdd;
import com.yandex.direct.api.v5.adgroups.TargetCarrierEnum;
import com.yandex.direct.api.v5.adgroups.TargetDeviceTypeEnum;
import com.yandex.direct.api.v5.adgroups.TextAdGroupFeedParamsAdd;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsValidationSignalContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.AddAdGroupsComplexPerformanceContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.AddAdGroupsSimpleContainer;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.container.ComplexPerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.AdGroupsAddRequestConverter.convertDynamicTextAdGroup;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.AdGroupsAddRequestConverter.convertDynamicTextFeedAdGroup;
import static ru.yandex.direct.api.v5.validation.DefectTypes.possibleOnlyOneField;

public class AdGroupsAddRequestConverterTest {
    private final AdGroupsAddRequestConverter converter = new AdGroupsAddRequestConverter();

    @Test
    public void convertItemToModel_campaignIdIsConverted() {
        long campaignId = 101;
        AdGroupAddItem request = new AdGroupAddItem().withCampaignId(campaignId);

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getCampaignId()).isEqualTo(campaignId);
    }

    @Test
    public void convertItemToModel_noAdditionalSections_TypeIsBase() {
        AdGroupAddItem request = new AdGroupAddItem();

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.BASE);
    }

    @Test
    public void convertItemToModel_textAdGroupFeedParamsSection_TypeIsBase() {

        var feedId = 12345L;
        AdGroupAddItem request = new AdGroupAddItem()
                .withTextAdGroupFeedParams(new TextAdGroupFeedParamsAdd()
                        .withFeedId(feedId));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.BASE);
    }

    @Test
    public void convertItemToModel_mobileAppSection_TypeIsMobileContent() {
        AdGroupAddItem request = new AdGroupAddItem().withMobileAppAdGroup(new MobileAppAdGroupAdd());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.MOBILE_CONTENT);
    }

    @Test
    public void convertToItemModel_dynamicTextSection_TypeIsDynamic() {
        AdGroupAddItem request =
                new AdGroupAddItem().withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.DYNAMIC);
    }

    @Test
    public void convertToItemModel_dynamicTextFeedSection_TypeIsDynamic() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withDynamicTextFeedAdGroup(new DynamicTextFeedAdGroup());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.DYNAMIC);
    }

    @Test
    public void convertItemToModel_cpmBannerWithKeywordsSection_TypeIsCpmBanner() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withCpmBannerKeywordsAdGroup(new CpmBannerKeywordsAdGroupAdd());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.CPM_BANNER);
    }

    @Test
    public void convertItemToModel_cpmBannerWithUsersProfileSection_TypeIsCpmBanner() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withCpmBannerUserProfileAdGroup(new CpmBannerUserProfileAdGroupAdd());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.CPM_BANNER);
    }

    @Test
    public void convertItemToModel_contentPromotionVideo_TypeIsContentPromotionVideo() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withContentPromotionAdGroup(new ContentPromotionAdGroupAdd()
                        .withPromotedContentType(PromotedContentTypeEnum.VIDEO));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.CONTENT_PROMOTION);
        assertThat(((ContentPromotionAdGroup) result).getContentPromotionType())
                .isEqualTo(ContentPromotionAdgroupType.VIDEO);
    }

    @Test
    public void convertItemToModel_contentPromotionCollection_TypeIsContentPromotionCollection() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withContentPromotionAdGroup(new ContentPromotionAdGroupAdd()
                        .withPromotedContentType(PromotedContentTypeEnum.COLLECTION));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.CONTENT_PROMOTION);
        assertThat(((ContentPromotionAdGroup) result).getContentPromotionType())
                .isEqualTo(ContentPromotionAdgroupType.COLLECTION);
    }

    @Test
    public void convertItemToModel_contentPromotionService_TypeIsContentPromotionService() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withContentPromotionAdGroup(new ContentPromotionAdGroupAdd()
                        .withPromotedContentType(PromotedContentTypeEnum.SERVICE));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.CONTENT_PROMOTION);
        assertThat(((ContentPromotionAdGroup) result).getContentPromotionType())
                .isEqualTo(ContentPromotionAdgroupType.SERVICE);
    }

    @Test
    public void convertItemToModel_contentPromotionEda_TypeIsContentPromotionEda() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withContentPromotionAdGroup(new ContentPromotionAdGroupAdd()
                        .withPromotedContentType(PromotedContentTypeEnum.EDA));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getType()).isEqualTo(AdGroupType.CONTENT_PROMOTION);
        assertThat(((ContentPromotionAdGroup) result).getContentPromotionType())
                .isEqualTo(ContentPromotionAdgroupType.EDA);
    }

    @Test
    public void convertToItemModel_nameIsConverted() {
        String name = "name";
        AdGroupAddItem request = new AdGroupAddItem().withName(name);

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getName()).isEqualTo(name);
    }

    @Test
    public void convertToItemModel_negativeKeywordsAreConverted() {
        String[] negativeKeywords = {"keyword", "minus keyword"};
        AdGroupAddItem request = new AdGroupAddItem()
                .withNegativeKeywords(new ArrayOfString().withItems(negativeKeywords));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getMinusKeywords()).containsExactlyInAnyOrder(negativeKeywords);
    }

    @Test
    public void convertToItemModel_negativeKeywordSharedSetIdsAreConverted() {
        Long[] negativeKeywordSetIds = {1L, 2L};
        AdGroupAddItem request = new AdGroupAddItem()
                .withNegativeKeywordSharedSetIds(new ArrayOfLong().withItems(negativeKeywordSetIds));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getLibraryMinusKeywordsIds()).containsExactlyInAnyOrder(negativeKeywordSetIds);
    }

    @Test
    public void convertToItemModel_regionIdsAreConverted() {
        Long[] regionIds = {1L, -2L};
        AdGroupAddItem request = new AdGroupAddItem().withRegionIds(regionIds);

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getGeo()).containsExactlyInAnyOrder(regionIds);
    }

    @Test
    public void convertToItemModel_trackingParamsAreConverted() {
        String trackingParams = "tracking params";
        AdGroupAddItem request = new AdGroupAddItem().withTrackingParams(trackingParams);

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result.getTrackingParams()).isEqualTo(trackingParams);
    }

    @Test
    public void convertItemToModel_textAdGroupFeedParamsAreConverted() {

        var feedId = 12345L;
        AdGroupAddItem request = new AdGroupAddItem()
                .withTextAdGroupFeedParams(new TextAdGroupFeedParamsAdd()
                        .withFeedId(feedId));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result).isInstanceOf(TextAdGroup.class);
    }

    @Test
    public void convertItemToModel_textAdGroupFeedIdIsConverted() {

        var feedId = 12345L;
        AdGroupAddItem request = new AdGroupAddItem()
                .withTextAdGroupFeedParams(new TextAdGroupFeedParamsAdd()
                        .withFeedId(feedId));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(((TextAdGroup) result).getOldFeedId()).isEqualTo(feedId);
    }

    @Test
    public void convertItemToModel_textAdGroupFeedCategorieIdsAreConverted() {

        var feedId = 12345L;
        ArrayOfLong feedCategorieIds = new ArrayOfLong().withItems(5L, 28L, 92L);
        AdGroupAddItem request = new AdGroupAddItem()
                .withTextAdGroupFeedParams(new TextAdGroupFeedParamsAdd()
                        .withFeedId(feedId)
                        .withFeedCategoryIds(feedCategorieIds));

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(((TextAdGroup) result).getFeedFilterCategories()).isEqualTo(feedCategorieIds.getItems());
    }

    @Test
    public void convertToItemModel_mobileAppSectionIsConverted() {
        AdGroupAddItem request = new AdGroupAddItem().withMobileAppAdGroup(new MobileAppAdGroupAdd());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result).isInstanceOf(MobileContentAdGroup.class);
    }

    @Test
    public void convertMobileAppAdGroup_storeUrlIsConverted() {
        String storeUrl = "http://store.url";
        AdGroupAddItem request = new AdGroupAddItem().withMobileAppAdGroup(new MobileAppAdGroupAdd()
                .withStoreUrl(storeUrl));

        MobileContentAdGroup result = extractSimpleAdGroup(AdGroupsAddRequestConverter.convertMobileAppAdGroup(request),
                MobileContentAdGroup.class);

        assertThat(result.getStoreUrl()).isEqualTo(storeUrl);
    }

    @Test
    public void convertMobileAppAdGroup_targetDeviceTypeIsConverted() {
        AdGroupAddItem request = new AdGroupAddItem().withMobileAppAdGroup(new MobileAppAdGroupAdd()
                .withTargetDeviceType(TargetDeviceTypeEnum.DEVICE_TYPE_MOBILE,
                        TargetDeviceTypeEnum.DEVICE_TYPE_TABLET));

        MobileContentAdGroup result = extractSimpleAdGroup(AdGroupsAddRequestConverter.convertMobileAppAdGroup(request),
                MobileContentAdGroup.class);

        assertThat(result.getDeviceTypeTargeting()).containsExactlyInAnyOrder(
                MobileContentAdGroupDeviceTypeTargeting.PHONE, MobileContentAdGroupDeviceTypeTargeting.TABLET);
    }

    @Test
    public void convertMobileAppAdGroup_targetCarrierIsConverted() {
        AdGroupAddItem request = new AdGroupAddItem().withMobileAppAdGroup(new MobileAppAdGroupAdd()
                .withTargetCarrier(TargetCarrierEnum.WI_FI_AND_CELLULAR));

        MobileContentAdGroup result = extractSimpleAdGroup(AdGroupsAddRequestConverter.convertMobileAppAdGroup(request),
                MobileContentAdGroup.class);

        assertThat(result.getNetworkTargeting()).containsExactlyInAnyOrder(
                MobileContentAdGroupNetworkTargeting.WI_FI, MobileContentAdGroupNetworkTargeting.CELLULAR);
    }

    @Test
    public void convertMobileAppAdGroup_targetOperatingSystemVersionIsConverted() {
        String osVersion = "Windows 10 Mobile";
        AdGroupAddItem request = new AdGroupAddItem().withMobileAppAdGroup(new MobileAppAdGroupAdd()
                .withTargetOperatingSystemVersion(osVersion));

        MobileContentAdGroup result = extractSimpleAdGroup(AdGroupsAddRequestConverter.convertMobileAppAdGroup(request),
                MobileContentAdGroup.class);

        assertThat(result.getMinimalOperatingSystemVersion()).isEqualTo(osVersion);
    }

    @Test
    public void convertToItemModel_dynamicTextSectionIsConverted() {
        AdGroupAddItem request =
                new AdGroupAddItem().withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result).isInstanceOf(DynamicTextAdGroup.class);
    }

    @Test
    public void convertToItemModel_dynamicTextFeedSectionIsConverted() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withDynamicTextFeedAdGroup(new DynamicTextFeedAdGroup());

        AdGroup result = convertSimpleAdGroup(request);

        assertThat(result).isInstanceOf(DynamicFeedAdGroup.class);
    }

    @Test
    public void convertDynamicTextAdGroup_autotargetingCategoriesAreConverted() {
        AdGroupAddItem request = new AdGroupAddItem().withDynamicTextAdGroup(
                new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup()
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO)));

        DynamicTextAdGroup result = extractSimpleAdGroup(convertDynamicTextAdGroup(request), DynamicTextAdGroup.class);

        assertThat(result.getRelevanceMatchCategories())
                .isEqualTo(Set.of(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.alternative_mark,
                        RelevanceMatchCategory.broader_mark));
    }

    @Test
    public void convertDynamicTextAdGroup_domainUrlIsConverted() {
        String domainUrl = "http://domain.url";
        AdGroupAddItem request = new AdGroupAddItem().withDynamicTextAdGroup(
                new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup().withDomainUrl(domainUrl));

        DynamicTextAdGroup result = extractSimpleAdGroup(convertDynamicTextAdGroup(request), DynamicTextAdGroup.class);

        assertThat(result.getDomainUrl()).isEqualTo(domainUrl);
    }

    @Test
    public void convertDynamicTextFeedAdGroup_autotargetingCategoriesAreConverted() {
        AdGroupAddItem request = new AdGroupAddItem().withDynamicTextFeedAdGroup(
                new DynamicTextFeedAdGroup()
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.COMPETITOR)
                                        .withValue(YesNoEnum.NO)));

        DynamicFeedAdGroup result = extractSimpleAdGroup(convertDynamicTextFeedAdGroup(request),
                DynamicFeedAdGroup.class);

        assertThat(result.getRelevanceMatchCategories())
                .isEqualTo(Set.of(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.alternative_mark,
                        RelevanceMatchCategory.broader_mark));
    }

    @Test
    public void convertDynamicTextFeedAdGroup_feedIdIsConverted() {
        long feedId = 55L;
        AdGroupAddItem request = new AdGroupAddItem().withDynamicTextFeedAdGroup(
                new DynamicTextFeedAdGroup().withFeedId(feedId));

        DynamicFeedAdGroup result = extractSimpleAdGroup(convertDynamicTextFeedAdGroup(request),
                DynamicFeedAdGroup.class);

        assertThat(result.getFeedId()).isEqualTo(feedId);
    }

    @Test
    public void convertSmartAdGroupToModel_paramsAreConverted() {
        long feedId = 55L;
        String feedTitle = "title";
        String feedBody = "body";
        SmartAdGroupAdd smartAdGroup = new SmartAdGroupAdd()
                .withFeedId(feedId)
                .withAdTitleSource(feedTitle)
                .withAdBodySource(feedBody);
        AdGroupAddItem request = new AdGroupAddItem()
                .withSmartAdGroup(smartAdGroup);
        PerformanceAdGroup result = convertSimpleAdGroup(request, PerformanceAdGroup.class);
        assertSoftly(softly -> {
            softly.assertThat(result.getFeedId()).as("FeedId").isEqualTo(feedId);
            softly.assertThat(result.getFieldToUseAsName()).as("FieldToUseAsName").isEqualTo(feedTitle);
            softly.assertThat(result.getFieldToUseAsBody()).as("FieldToUseAsBody").isEqualTo(feedBody);
        });
    }

    @Test
    public void convertSmartAdGroupNoCreatives_paramsAreConverted() {
        long feedId = 55L;
        String feedTitle = "title";
        String feedBody = "body";
        String logoImageHash = "logoImageHash";

        AdGroupAddItem request = new AdGroupAddItem().withSmartAdGroup(new SmartAdGroupAdd()
                .withFeedId(feedId)
                .withAdTitleSource(feedTitle)
                .withAdBodySource(feedBody)
                .withLogoExtensionHash(logoImageHash)
                .withNoCreatives(true));

        AdGroupsContainer result = converter.convertItem(request);

        assertThat(result).isInstanceOf(AddAdGroupsComplexPerformanceContainer.class);
        ComplexPerformanceAdGroup complexPerformanceAdGroup = ((AddAdGroupsComplexPerformanceContainer) result)
                .getComplexAdGroup();
        assertSoftly(softly -> {
            softly.assertThat(complexPerformanceAdGroup.getAdGroup()).satisfies(adGroup -> {
                assertThat(adGroup).isInstanceOf(PerformanceAdGroup.class);
                PerformanceAdGroup performanceAdGroup = (PerformanceAdGroup) adGroup;
                softly.assertThat(performanceAdGroup.getFeedId()).as("FeedId").isEqualTo(feedId);
                softly.assertThat(performanceAdGroup.getFieldToUseAsName()).as("FieldToUseAsName").isEqualTo(feedTitle);
                softly.assertThat(performanceAdGroup.getFieldToUseAsBody()).as("FieldToUseAsBody").isEqualTo(feedBody);
            });
            softly.assertThat(complexPerformanceAdGroup.getBanners()).satisfies(banners -> {
               assertThat(banners).isNotNull();
               softly.assertThat(banners).hasSize(1);
               softly.assertThat(banners.get(0)).satisfies(banner -> {
                   assertThat(banner).isNotNull();
                   softly.assertThat(banner.getLogoImageHash()).as("LogoImageHash").isEqualTo(logoImageHash);
               });
            });
        });
    }

    @Test
    public void convertDynamicAndMobileAdGroup_MixedAdGroupConverted() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withMobileAppAdGroup(new MobileAppAdGroupAdd())
                .withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup());

        DefectType result = extractDefectType(converter.convertItem(request));

        assertThat(result).isEqualTo(possibleOnlyOneField());
    }

    @Test
    public void convertCpmBannerAndDynamicTextAdGroup_MixedAdGroupConverted() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withCpmBannerKeywordsAdGroup(new CpmBannerKeywordsAdGroupAdd())
                .withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup());

        DefectType result = extractDefectType(converter.convertItem(request));

        assertThat(result).isEqualTo(possibleOnlyOneField());
    }

    @Test
    public void convertCpmBannerAndMobileAdGroup_MixedAdGroupConverted() {
        AdGroupAddItem request = new AdGroupAddItem()
                .withMobileAppAdGroup(new MobileAppAdGroupAdd())
                .withCpmBannerUserProfileAdGroup(new CpmBannerUserProfileAdGroupAdd());

        DefectType result = extractDefectType(converter.convertItem(request));

        assertThat(result).isEqualTo(possibleOnlyOneField());
    }

    private AdGroup convertSimpleAdGroup(AdGroupAddItem request) {
        return extractSimpleAdGroup(converter.convertItem(request), AdGroup.class);
    }

    private <T extends AdGroup> T convertSimpleAdGroup(AdGroupAddItem request, Class<T> clazz) {
        return extractSimpleAdGroup(converter.convertItem(request), clazz);
    }

    private <T extends AdGroup> T extractSimpleAdGroup(AdGroupsContainer container, Class<T> clazz) {
        assertThat(container).isInstanceOf(AddAdGroupsSimpleContainer.class);
        AdGroup adGroup = ((AddAdGroupsSimpleContainer) container).getAdGroup();
        assertThat(adGroup).isInstanceOf(clazz);
        return clazz.cast(adGroup);
    }

    private DefectType extractDefectType(AdGroupsContainer container) {
        assertThat(container).isInstanceOf(AdGroupsValidationSignalContainer.class);
        return ((AdGroupsValidationSignalContainer) container).getDefectType();
    }
}
