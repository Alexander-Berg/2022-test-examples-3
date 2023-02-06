package ru.yandex.direct.web.entity.adgroup.converter;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.container.ComplexContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroupType;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBannerContentRes;
import ru.yandex.direct.web.entity.bidmodifier.model.WebAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebRetargetingBidModifier;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;
import ru.yandex.direct.web.testing.data.TestBidModifiers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._18_24;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.MALE;
import static ru.yandex.direct.web.entity.adgroup.converter.ContentPromotionAdGroupConverter.webAdGroupToCoreComplexContentPromotionAdGroup;

public class ContentPromotionAdGroupConverterTest {

    private static final Long ADGROUP_ID = 111L;
    private static final Long BANNER_ID = 222L;
    private static final Long VALID_CONTENT_PROMOTION_VIDEO_ID = 1234L;
    private static final Long VALID_CONTENT_PROMOTION_COLLECTION_ID = 12345L;
    private static final String NAME = "name";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    @Test
    public void webAdGroupToCoreComplexContentPromotionVideoAdGroup_EmptyAdGroup() {
        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withId(ADGROUP_ID)
                .withName(NAME);
        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        ComplexContentPromotionAdGroup expected = new ComplexContentPromotionAdGroup()
                .withAdGroup(new ContentPromotionAdGroup()
                        .withId(ADGROUP_ID)
                        .withName(NAME)
                        .withType(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionType(ContentPromotionAdgroupType.VIDEO)
                        .withMinusKeywords(emptyList())
                );

        assertThat("Группа сконвертировалась корректно", complexContentPromotionAdGroup, beanDiffer(expected));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithBanner() {
        WebContentPromotionBanner banner = new WebContentPromotionBanner()
                .withId(BANNER_ID)
                .withAdType(BannersBannerType.content_promotion.getLiteral())
                .withTitle(TITLE)
                .withDescription(DESCRIPTION)
                .withContentResource(new WebContentPromotionBannerContentRes()
                        .withContentId(VALID_CONTENT_PROMOTION_VIDEO_ID));

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withId(ADGROUP_ID)
                .withName(NAME)
                .withBanners(singletonList(banner));

        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        var expectedBanner = new ContentPromotionBanner()
                .withId(BANNER_ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_VIDEO_ID);

        ComplexContentPromotionAdGroup expected = new ComplexContentPromotionAdGroup()
                .withAdGroup(new ContentPromotionAdGroup()
                        .withId(ADGROUP_ID)
                        .withName(NAME)
                        .withType(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionType(ContentPromotionAdgroupType.VIDEO)
                        .withMinusKeywords(emptyList())
                )
                .withBanners(singletonList(expectedBanner));

        assertThat("Баннер сконвертировался корректно", complexContentPromotionAdGroup, beanDiffer(expected));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithGeo() {
        String sourceGeo = "123,456";
        List<Long> destGeo = asList(123L, 456L);

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withGeo(sourceGeo);
        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        assertThat(complexContentPromotionAdGroup.getAdGroup().getGeo(), is(destGeo));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithTags() {
        Map<String, Integer> sourceTags = ImmutableMap.of("123", 1, "345", 0, "567", 1);

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withTags(sourceTags);
        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        assertThat("Теги сконвертировались корректно", complexContentPromotionAdGroup.getAdGroup().getTags(),
                containsInAnyOrder(123L, 567L));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithMinusKeywords() {
        List<String> minusKeywords = asList("one", "two");

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withMinusKeywords(minusKeywords);
        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        assertThat("Минус слова сконвертировались корректно",
                complexContentPromotionAdGroup.getAdGroup().getMinusKeywords(), sameInstance(minusKeywords));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithKeywords() {
        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withKeywords(singletonList(new WebKeyword().withId(1L).withPhrase("456")));

        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);
        ComplexContentPromotionAdGroup expected = new ComplexContentPromotionAdGroup()
                .withKeywords(singletonList(new Keyword().withId(1L)
                        .withPhrase("456")));

        assertThat("Ключевые слова сконвертировались корректно", complexContentPromotionAdGroup,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithBidModifiers() {
        WebRetargetingBidModifier webRetargetingBidModifier = new WebRetargetingBidModifier()
                .withAdjustments(singletonMap("5", new WebAdjustment().withPercent(4)));
        BidModifierRetargeting expectedRetargetingModifier = new BidModifierRetargeting()
                .withRetargetingAdjustments(singletonList(new BidModifierRetargetingAdjustment()
                        .withRetargetingConditionId(5L)
                        .withPercent(4)));

        WebDemographicsBidModifier webDemographicsBidModifier = new WebDemographicsBidModifier()
                .withAdjustments(singletonList(new WebDemographicsAdjustment()
                        .withAge("18-24")
                        .withGender("male")));
        BidModifierDemographics expectedDemographyModifier = new BidModifierDemographics()
                .withDemographicsAdjustments(singletonList(new BidModifierDemographicsAdjustment()
                        .withAge(_18_24)
                        .withGender(MALE)));

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withBidModifiers(new WebAdGroupBidModifiers()
                        .withRetargetingBidModifier(webRetargetingBidModifier)
                        .withDemographicsBidModifier(webDemographicsBidModifier)
                        .withExpressionBidModifiers(List.of(TestBidModifiers.SAMPLE_WEB_EXPRESS_MODIFIER))
                );

        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);
        ComplexContentPromotionAdGroup expected = new ComplexContentPromotionAdGroup()
                .withComplexBidModifier(new ComplexBidModifier()
                        .withRetargetingModifier(expectedRetargetingModifier)
                        .withDemographyModifier(expectedDemographyModifier)
                        .withExpressionModifiers(List.of(TestBidModifiers.SAMPLE_EXPRESS_MODIFIER))
                );

        assertThat("Корректировки сконвертировались корректно", complexContentPromotionAdGroup,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithPageGroupTags() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withPageGroupTags(pageGroupTags);
        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        assertThat("Теги для таргетинга сконвертировались корректно",
                complexContentPromotionAdGroup.getAdGroup().getPageGroupTags(), sameInstance(pageGroupTags));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_AdGroupWithTargetTags() {
        List<String> targetTags = asList("target_tag1", "target_tag2");

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withTargetTags(targetTags);
        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        assertThat("Теги для таргетинга сконвертировались корректно",
                complexContentPromotionAdGroup.getAdGroup().getTargetTags(), sameInstance(targetTags));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_VideoType() {
        WebContentPromotionBanner banner = new WebContentPromotionBanner()
                .withId(BANNER_ID)
                .withTitle(TITLE)
                .withDescription(DESCRIPTION)
                .withContentResource(new WebContentPromotionBannerContentRes()
                        .withContentId(VALID_CONTENT_PROMOTION_VIDEO_ID));

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withId(ADGROUP_ID)
                .withName(NAME)
                .withBanners(singletonList(banner));

        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        var expectedBanner = new ContentPromotionBanner()
                .withId(BANNER_ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_VIDEO_ID);

        ComplexContentPromotionAdGroup expected = new ComplexContentPromotionAdGroup()
                .withAdGroup(new ContentPromotionAdGroup()
                        .withId(ADGROUP_ID)
                        .withName(NAME)
                        .withType(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionType(ContentPromotionAdgroupType.VIDEO)
                        .withMinusKeywords(emptyList())
                )
                .withBanners(singletonList(expectedBanner));

        assertThat("Баннер сконвертировался корректно", complexContentPromotionAdGroup, beanDiffer(expected));
    }

    @Test
    public void webAdGroupToCoreComplexContentPromotionAdGroup_CollectionType() {
        WebContentPromotionBanner banner = new WebContentPromotionBanner()
                .withId(BANNER_ID)
                .withTitle(TITLE)
                .withDescription(DESCRIPTION)
                .withContentResource(new WebContentPromotionBannerContentRes()
                        .withContentId(VALID_CONTENT_PROMOTION_COLLECTION_ID));

        WebContentPromotionAdGroup webContentPromotionAdGroup = getEmptyContentPromotionAdGroup()
                .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                .withId(ADGROUP_ID)
                .withName(NAME)
                .withBanners(singletonList(banner));

        ComplexContentPromotionAdGroup complexContentPromotionAdGroup =
                webAdGroupToCoreComplexContentPromotionAdGroup(webContentPromotionAdGroup);

        var expectedBanner = new ContentPromotionBanner()
                .withId(BANNER_ID)
                .withTitle(TITLE)
                .withBody(DESCRIPTION)
                .withContentPromotionId(VALID_CONTENT_PROMOTION_COLLECTION_ID);

        ComplexContentPromotionAdGroup expected = new ComplexContentPromotionAdGroup()
                .withAdGroup(new ContentPromotionAdGroup()
                        .withId(ADGROUP_ID)
                        .withName(NAME)
                        .withType(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionType(ContentPromotionAdgroupType.COLLECTION)
                        .withMinusKeywords(emptyList())
                )
                .withBanners(singletonList(expectedBanner));

        assertThat("Баннер сконвертировался корректно", complexContentPromotionAdGroup, beanDiffer(expected));
    }


    private WebContentPromotionAdGroup getEmptyContentPromotionAdGroup() {
        return new WebContentPromotionAdGroup();
    }
}
