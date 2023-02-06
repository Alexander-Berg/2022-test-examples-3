package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.adgroups.AdGroupUpdateItem;
import com.yandex.direct.api.v5.adgroups.DynamicTextFeedAdGroupUpdate;
import com.yandex.direct.api.v5.adgroups.MobileAppAdGroupUpdate;
import com.yandex.direct.api.v5.adgroups.ObjectFactory;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupUpdate;
import com.yandex.direct.api.v5.adgroups.TargetCarrierEnum;
import com.yandex.direct.api.v5.adgroups.TargetDeviceTypeEnum;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.AutotargetingCategoriesEnum;
import com.yandex.direct.api.v5.general.AutotargetingCategory;
import com.yandex.direct.api.v5.general.YesNoEnum;
import junitparams.converters.Nullable;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.AdGroupsValidationSignalContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.UpdateAdGroupsComplexPerformanceContainer;
import ru.yandex.direct.api.v5.entity.adgroups.container.UpdateAdGroupsSimpleContainer;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.container.ComplexPerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.api.v5.entity.adgroups.AdGroupTestUtils.getTextAdGroupFeedParamsUpdate;
import static ru.yandex.direct.api.v5.validation.DefectTypes.possibleOnlyOneField;

public class AdGroupsUpdateRequestConverterTest {
    private static final DynamicTextAdGroup DYNAMIC_TEXT_AD_GROUP = new DynamicTextAdGroup()
            .withId(5L)
            .withType(AdGroupType.DYNAMIC)
            .withRelevanceMatchCategories(Set.of(RelevanceMatchCategory.exact_mark,
                    RelevanceMatchCategory.accessory_mark));
    private static final DynamicFeedAdGroup DYNAMIC_FEED_AD_GROUP = new DynamicFeedAdGroup()
            .withId(8L)
            .withType(AdGroupType.DYNAMIC)
            .withRelevanceMatchCategories(Set.of(RelevanceMatchCategory.exact_mark,
                    RelevanceMatchCategory.accessory_mark));
    private static final PerformanceBannerMain PERFORMANCE_MAIN_BANNER = new PerformanceBannerMain()
            .withId(1L)
            .withAdGroupId(11L);
    private static final PerformanceAdGroup PERFORMANCE_AD_GROUP = new PerformanceAdGroup()
            .withId(PERFORMANCE_MAIN_BANNER.getAdGroupId())
            .withType(AdGroupType.PERFORMANCE)
            .withBanners(List.of(PERFORMANCE_MAIN_BANNER));

    private static final ObjectFactory FACTORY = new ObjectFactory();
    private final AdGroupsUpdateRequestConverter converter = new AdGroupsUpdateRequestConverter();

    @Test
    public void convertItemToModel_noAdditionalSections_TypeIsAdGroup() {
        Long id = 1L;
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(id);

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo(id);
            softly.assertThat(result.getModelType()).isEqualTo(AdGroup.class);
        });
    }

    @Test
    public void convertItemToModel_mobileAppSection_TypeIsMobileContent() {
        Long id = 1L;
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(id)
                .withMobileAppAdGroup(new MobileAppAdGroupUpdate());

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo(id);
            softly.assertThat(result.getModelType()).isEqualTo(MobileContentAdGroup.class);
        });
    }

    @Test
    public void convertToItemModel_dynamicTextSection_TypeIsDynamicText() {
        Long id = 1L;
        AdGroupUpdateItem request =
                new AdGroupUpdateItem()
                        .withId(id)
                        .withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup());

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo(id);
            softly.assertThat(result.getModelType()).isEqualTo(DynamicTextAdGroup.class);
        });
    }

    @Test
    public void convertToItemModel_dynamicTextFeedSection_TypeIsDynamicFeed() {
        Long id = 1L;
        AdGroupUpdateItem request =
                new AdGroupUpdateItem()
                        .withId(id)
                        .withDynamicTextFeedAdGroup(new DynamicTextFeedAdGroupUpdate());

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo(id);
            softly.assertThat(result.getModelType()).isEqualTo(DynamicFeedAdGroup.class);
        });
    }

    @Test
    public void convertToItemModel_nameIsConverted() {
        String name = "name";
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withName(name);

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.NAME)).isEqualTo(name);
    }

    @Test
    public void convertToItemModel_absentNegativeKeywordsIsNotChanged() {
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L);

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.isPropChanged(AdGroup.MINUS_KEYWORDS))
                .as("Ожидается что %s не изменилось", AdGroup.MINUS_KEYWORDS.name())
                .isEqualTo(false);
    }

    @Test
    public void convertToItemModel_nullNegativeKeywordsAreConverted() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withNegativeKeywords(
                        new ObjectFactory()
                                .createAdGroupBaseNegativeKeywords(null));

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.MINUS_KEYWORDS)).isEmpty();
    }

    @Test
    public void convertToItemModel_notNullNegativeKeywordsAreConverted() {
        String[] negativeKeywords = {"keyword", "minus keyword"};
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withNegativeKeywords(
                        new ObjectFactory()
                                .createAdGroupBaseNegativeKeywords(new ArrayOfString().withItems(negativeKeywords)));

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.MINUS_KEYWORDS)).containsExactlyInAnyOrder(negativeKeywords);
    }

    @Test
    public void convertToItemModel_nullNegativeKeywordSharedSetIdsAreConverted() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withNegativeKeywordSharedSetIds(
                        new ObjectFactory()
                                .createAdGroupBaseNegativeKeywordSharedSetIds(null));

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS)).isEmpty();
    }

    @Test
    public void convertToItemModel_notNullNegativeKeywordSharedSetIdsAreConverted() {
        Long[] negativeKeywordIds = {1L, 2L};
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withNegativeKeywordSharedSetIds(
                        new ObjectFactory()
                                .createAdGroupBaseNegativeKeywordSharedSetIds(new ArrayOfLong().withItems(negativeKeywordIds)));

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS)).containsExactlyInAnyOrder(negativeKeywordIds);
    }

    @Test
    public void convertToItemModel_absentRegionIdsIsNotChanged() {
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L);

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.isPropChanged(AdGroup.GEO))
                .as("Ожидается что %s не изменилось", AdGroup.GEO.name())
                .isEqualTo(false);
    }

    @Test
    public void convertToItemModel_regionIdsAreConverted() {
        Long[] regionIds = {1L, -2L};
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withRegionIds(regionIds);

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.GEO)).containsExactlyInAnyOrder(regionIds);
    }

    @Test
    public void convertToItemModel_trackingParamsAreConverted() {
        String trackingParams = "tracking params";
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withTrackingParams(trackingParams);

        ModelChanges<AdGroup> result = convertSimpleModelChanges(request);

        assertThat(result.getChangedProp(AdGroup.TRACKING_PARAMS)).isEqualTo(trackingParams);
    }

    @Test
    public void convertToItemModel_smartAdGroupFeedId() {
        Long feedId = 123456L;
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withSmartAdGroup(new SmartAdGroupUpdate().withFeedId(feedId));
        ModelChanges<PerformanceAdGroup> result = convertSimpleModelChanges(request, PerformanceAdGroup.class);

        assertThat(result.getChangedProp(PerformanceAdGroup.FEED_ID)).isEqualTo(feedId);
    }

    @Test
    public void convertToItemModel_dynamicTextFeedAdGroupFeedId() {
        Long feedId = 123456L;
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withDynamicTextFeedAdGroup(new DynamicTextFeedAdGroupUpdate().withFeedId(feedId));
        ModelChanges<DynamicFeedAdGroup> result = convertSimpleModelChanges(request, DynamicFeedAdGroup.class);

        assertThat(result.getChangedProp(DynamicFeedAdGroup.FEED_ID)).isEqualTo(feedId);
    }

    @Test
    public void convertToItemModel_textAdGroupFeedId() {
        Long feedId = 123456L;
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withTextAdGroupFeedParams(
                getTextAdGroupFeedParamsUpdate(feedId, Collections.emptyList()));
        ModelChanges<TextAdGroup> result = convertSimpleModelChanges(request, TextAdGroup.class);

        assertThat(result.getChangedProp(TextAdGroup.OLD_FEED_ID)).isEqualTo(feedId);
    }

    @Test
    public void convertToItemModel_textAdGroupFeedCategoryIds() {
        Long feedId = 123456L;
        var feedCategorieIds = List.of(1L, 2L, 3L);
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withTextAdGroupFeedParams(
                getTextAdGroupFeedParamsUpdate(feedId, feedCategorieIds));
        ModelChanges<TextAdGroup> result = convertSimpleModelChanges(request, TextAdGroup.class);

        assertThat(result.getChangedProp(TextAdGroup.FEED_FILTER_CATEGORIES)).isEqualTo(feedCategorieIds);
    }

    @Test
    public void convertMobileAppAdGroup_absentTargetDeviceTypeIsNotChanged() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withMobileAppAdGroup(new MobileAppAdGroupUpdate());

        ModelChanges<MobileContentAdGroup> result = convertSimpleModelChanges(request, MobileContentAdGroup.class);

        assertThat(result.isPropChanged(MobileContentAdGroup.DEVICE_TYPE_TARGETING))
                .as("Ожидается что %s не изменилось", MobileContentAdGroup.DEVICE_TYPE_TARGETING.name())
                .isEqualTo(false);
    }

    @Test
    public void convertMobileAppAdGroup_targetDeviceTypeIsConverted() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withMobileAppAdGroup(new MobileAppAdGroupUpdate()
                        .withTargetDeviceType(TargetDeviceTypeEnum.DEVICE_TYPE_MOBILE,
                                TargetDeviceTypeEnum.DEVICE_TYPE_TABLET));

        ModelChanges<MobileContentAdGroup> result = convertSimpleModelChanges(request, MobileContentAdGroup.class);

        assertThat(result.getChangedProp(MobileContentAdGroup.DEVICE_TYPE_TARGETING)).containsExactlyInAnyOrder(
                MobileContentAdGroupDeviceTypeTargeting.PHONE, MobileContentAdGroupDeviceTypeTargeting.TABLET);
    }

    @Test
    public void convertMobileAppAdGroup_targetCarrierIsConverted() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withMobileAppAdGroup(new MobileAppAdGroupUpdate()
                        .withTargetCarrier(TargetCarrierEnum.WI_FI_AND_CELLULAR));

        ModelChanges<MobileContentAdGroup> result = convertSimpleModelChanges(request, MobileContentAdGroup.class);

        assertThat(result.getChangedProp(MobileContentAdGroup.NETWORK_TARGETING)).containsExactlyInAnyOrder(
                MobileContentAdGroupNetworkTargeting.WI_FI, MobileContentAdGroupNetworkTargeting.CELLULAR);
    }

    @Test
    public void convertMobileAppAdGroup_targetOperatingSystemVersionIsConverted() {
        String osVersion = "Windows 10 Mobile";
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withMobileAppAdGroup(new MobileAppAdGroupUpdate()
                        .withTargetOperatingSystemVersion(osVersion));

        ModelChanges<MobileContentAdGroup> result = convertSimpleModelChanges(request, MobileContentAdGroup.class);

        assertThat(result.getChangedProp(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION)).isEqualTo(osVersion);
    }

    @Test
    public void convertDynamicTextAdGroup_autotargetingCategoriesAreConverted() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup()
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES)));

        ModelChanges<DynamicTextAdGroup> result = convertSimpleModelChanges(request, DYNAMIC_TEXT_AD_GROUP,
                DynamicTextAdGroup.class);

        assertThat(result.getChangedProp(DynamicTextAdGroup.RELEVANCE_MATCH_CATEGORIES))
                .isEqualTo(Set.of(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.broader_mark));
    }

    @Test
    public void convertDynamicTextAdGroup_domainUrlIsConverted() {
        String domainUrl = "https://ya.ru";
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup()
                        .withDomainUrl(domainUrl));

        ModelChanges<DynamicTextAdGroup> result = convertSimpleModelChanges(request, DynamicTextAdGroup.class);

        assertThat(result.getChangedProp(DynamicTextAdGroup.DOMAIN_URL)).isEqualTo(domainUrl);
    }

    @Test
    public void convertDynamicTextFeedAdGroup_autotargetingCategoriesAreConverted() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withDynamicTextFeedAdGroup(new DynamicTextFeedAdGroupUpdate()
                        .withAutotargetingCategories(new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.EXACT)
                                        .withValue(YesNoEnum.NO),
                                new AutotargetingCategory()
                                        .withCategory(AutotargetingCategoriesEnum.BROADER)
                                        .withValue(YesNoEnum.YES)));

        ModelChanges<DynamicFeedAdGroup> result = convertSimpleModelChanges(request, DYNAMIC_FEED_AD_GROUP,
                DynamicFeedAdGroup.class);

        assertThat(result.getChangedProp(DynamicFeedAdGroup.RELEVANCE_MATCH_CATEGORIES))
                .isEqualTo(Set.of(RelevanceMatchCategory.accessory_mark, RelevanceMatchCategory.broader_mark));
    }

    @Test
    public void convertToItemModel_with_dynamicTextSection_and_mobileAppSection() {
        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(1L)
                .withDynamicTextAdGroup(new com.yandex.direct.api.v5.adgroups.DynamicTextAdGroup())
                .withMobileAppAdGroup(new MobileAppAdGroupUpdate());

        DefectType result = extractDefectType(converter.convert(request, null));

        assertThat(result).isEqualTo(possibleOnlyOneField());
    }

    @Test
    public void convertSmartAdGroupToModel_adTitleIsConverted() {
        String titleFieldName = "titleFieldName";
        String bodyFieldName = "bodyFieldName";
        SmartAdGroupUpdate smartAdGroup = new SmartAdGroupUpdate()
                .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(titleFieldName))
                .withAdBodySource(FACTORY.createSmartAdGroupGetAdBodySource(bodyFieldName));
        AdGroupUpdateItem request = new AdGroupUpdateItem().withId(1L).withSmartAdGroup(smartAdGroup);

        ModelChanges<PerformanceAdGroup> result = convertSimpleModelChanges(request, PerformanceAdGroup.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getChangedProp(PerformanceAdGroup.FIELD_TO_USE_AS_NAME))
                    .as(PerformanceAdGroup.FIELD_TO_USE_AS_NAME.name())
                    .isEqualTo(titleFieldName);
            softly.assertThat(result.getChangedProp(PerformanceAdGroup.FIELD_TO_USE_AS_BODY))
                    .as(PerformanceAdGroup.FIELD_TO_USE_AS_BODY.name())
                    .isEqualTo(bodyFieldName);
        });
    }

    @Test
    public void convertSmartAdGroupNoCreatives_paramsAreConverted() {
        String feedTitle = "title";
        String feedBody = "body";
        Long feedId = 123456L;
        String logoImageHash = "logoImageHash";

        AdGroupUpdateItem request = new AdGroupUpdateItem()
                .withId(PERFORMANCE_AD_GROUP.getId())
                .withSmartAdGroup(new SmartAdGroupUpdate()
                        .withAdTitleSource(FACTORY.createSmartAdGroupGetAdTitleSource(feedTitle))
                        .withAdBodySource(FACTORY.createSmartAdGroupGetAdBodySource(feedBody))
                        .withLogoExtensionHash(FACTORY.createSmartAdGroupUpdateLogoExtensionHash(logoImageHash))
                        .withFeedId(feedId));

        AdGroupsContainer result = converter.convert(request, PERFORMANCE_AD_GROUP);

        assertThat(result).isInstanceOf(UpdateAdGroupsComplexPerformanceContainer.class);
        ComplexPerformanceAdGroup complexPerformanceAdGroup = ((UpdateAdGroupsComplexPerformanceContainer) result)
                .getComplexAdGroup();
        assertSoftly(softly -> {
            softly.assertThat(complexPerformanceAdGroup.getAdGroup()).satisfies(adGroup -> {
                assertThat(adGroup).isInstanceOf(PerformanceAdGroup.class);
                PerformanceAdGroup performanceAdGroup = (PerformanceAdGroup) adGroup;
                softly.assertThat(performanceAdGroup.getId()).as("Id").isEqualTo(PERFORMANCE_AD_GROUP.getId());
                softly.assertThat(performanceAdGroup.getFeedId()).as("FeedId").isEqualTo(feedId);
                softly.assertThat(performanceAdGroup.getFieldToUseAsName()).as("FieldToUseAsName").isEqualTo(feedTitle);
                softly.assertThat(performanceAdGroup.getFieldToUseAsBody()).as("FieldToUseAsBody").isEqualTo(feedBody);
            });
            softly.assertThat(complexPerformanceAdGroup.getBanners()).satisfies(banners -> {
                assertThat(banners).isNotNull();
                softly.assertThat(banners).hasSize(1);
                softly.assertThat(banners.get(0)).satisfies(banner -> {
                    assertThat(banner).isNotNull();
                    softly.assertThat(banner.getId()).as("BannerId").isEqualTo(PERFORMANCE_MAIN_BANNER.getId());
                    softly.assertThat(banner.getLogoImageHash()).as("LogoImageHash").isEqualTo(logoImageHash);
                });
            });
        });
    }

    @Test
    public void updateHyperGeoIfOldAndNewGeoAreDifferent() {
        ModelChanges<AdGroup> result = convertSimpleModelChanges(
                new AdGroupUpdateItem().withId(1L).withRegionIds(List.of(345L, 123L)), new AdGroup().withGeo(List.of(123L, 345L))
        );

        Assert.assertFalse(result.isPropChanged(AdGroup.HYPER_GEO_ID));
    }

    @Test
    public void doNotUpdateHyperGeoIfOldAndNewGeoAreEquals() {
        ModelChanges<AdGroup> result = convertSimpleModelChanges(
                new AdGroupUpdateItem().withId(1L).withRegionIds(List.of(345L)), new AdGroup().withGeo(List.of(123L))
        );

        Assert.assertTrue(result.isPropChanged(AdGroup.HYPER_GEO_ID));
    }

    @Test
    public void updateHyperGeoIfOldAdGroupIsNull() {
        ModelChanges<AdGroup> result = convertSimpleModelChanges(new AdGroupUpdateItem().withId(1L).withRegionIds(List.of(345L)));

        Assert.assertTrue(result.isPropChanged(AdGroup.HYPER_GEO_ID));
    }

    private ModelChanges<AdGroup> convertSimpleModelChanges(AdGroupUpdateItem request) {
        return convertSimpleModelChanges(request, (AdGroup) null);
    }

    private ModelChanges<AdGroup> convertSimpleModelChanges(AdGroupUpdateItem request, @Nullable AdGroup oldAdGroup) {
        return convertSimpleModelChanges(request, oldAdGroup, AdGroup.class);
    }

    private <T extends AdGroup> ModelChanges<T> convertSimpleModelChanges(AdGroupUpdateItem request, Class<T> clazz) {
        return convertSimpleModelChanges(request, null, clazz);
    }

    private <T extends AdGroup> ModelChanges<T> convertSimpleModelChanges(
            AdGroupUpdateItem request, @Nullable AdGroup oldAdGroup, Class<T> clazz) {
        return extractSimpleModelChanges(converter.convert(request, oldAdGroup), clazz);
    }

    private <T extends AdGroup> ModelChanges<T> extractSimpleModelChanges(AdGroupsContainer container, Class<T> clazz) {
        assertThat(container).isInstanceOf(UpdateAdGroupsSimpleContainer.class);
        ModelChanges<? extends AdGroup> modelChanges = ((UpdateAdGroupsSimpleContainer) container).getModelChanges();
        assertThat(modelChanges.getModelType()).matches(clazz::isAssignableFrom);
        return modelChanges.castModel(clazz);
    }

    private DefectType extractDefectType(AdGroupsContainer container) {
        assertThat(container).isInstanceOf(AdGroupsValidationSignalContainer.class);
        return ((AdGroupsValidationSignalContainer) container).getDefectType();
    }
}
