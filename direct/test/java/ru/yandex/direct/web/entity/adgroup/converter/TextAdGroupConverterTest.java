package ru.yandex.direct.web.entity.adgroup.converter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.container.ComplexAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.testing.data.TestBidModifiers;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.SimpleConversionMatcher.converts;
import static ru.yandex.direct.web.entity.adgroup.converter.TextAdGroupConverter.webAdGroupToCoreComplexAdGroup;
import static ru.yandex.direct.web.entity.adgroup.converter.TextAdGroupConverter.webAdGroupsToCoreComplexTextAdGroups;

@SuppressWarnings("ConstantConditions")
public class TextAdGroupConverterTest {

    @Test
    public void convertPrimitives() {
        Set<String> fieldsNotToFill = ImmutableSet.of("minusKeywords", "geo", "tags",
                "pageGroupTags", "targetTags", "generalPrice", "keywords",
                "relevanceMatches", "retargetings", "bidModifiers", "banners");
        assertThat(TextAdGroupConverter::webAdGroupToCoreComplexAdGroup,
                converts(new WebTextAdGroup(), fieldsNotToFill, ComplexAdGroup::getAdGroup));
    }

    @Test
    public void convertGeoAndMinusKeywords() {
        String sourceGeo = "123,456";
        List<Long> destGeo = asList(123L, 456L);
        List<String> minusKeywords = asList("one", "two");

        WebTextAdGroup webTextAdGroup = new WebTextAdGroup()
                .withGeo(sourceGeo)
                .withMinusKeywords(minusKeywords);

        ComplexAdGroup complexAdGroup = webAdGroupToCoreComplexAdGroup(webTextAdGroup);
        assertThat(complexAdGroup.getAdGroup().getGeo(), equalTo(destGeo));
        assertThat(complexAdGroup.getAdGroup().getMinusKeywords(), sameInstance(minusKeywords));
    }

    @Test
    public void convertTags() {
        Map<String, Integer> sourceTags = ImmutableMap.of("123", 1, "345", 0, "567", 1);

        WebTextAdGroup webTextAdGroup = new WebTextAdGroup()
                .withTags(sourceTags);

        ComplexAdGroup complexAdGroup = webAdGroupToCoreComplexAdGroup(webTextAdGroup);
        assertThat(complexAdGroup.getAdGroup().getTags(), containsInAnyOrder(123L, 567L));
    }

    @Test
    public void convertPageGroupTags() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2", "page_group_tag3");

        WebTextAdGroup webTextAdGroup = new WebTextAdGroup()
                .withPageGroupTags(pageGroupTags);

        ComplexAdGroup complexAdGroup = webAdGroupToCoreComplexAdGroup(webTextAdGroup);
        assertThat(complexAdGroup.getAdGroup().getPageGroupTags(), sameInstance(pageGroupTags));
    }

    @Test
    public void convertTargetTags() {
        List<String> targetTags = asList("target_tag1", "target_tag2", "target_tag3");

        WebTextAdGroup webTextAdGroup = new WebTextAdGroup()
                .withTargetTags(targetTags);

        ComplexAdGroup complexAdGroup = webAdGroupToCoreComplexAdGroup(webTextAdGroup);
        assertThat(complexAdGroup.getAdGroup().getTargetTags(), sameInstance(targetTags));
    }

    @Test
    public void convertNullFields() {
        WebTextAdGroup webTextAdGroup = new WebTextAdGroup();
        ComplexAdGroup complexAdGroup = webAdGroupToCoreComplexAdGroup(webTextAdGroup);
        assertThat(complexAdGroup.getAdGroup(), notNullValue());
    }

    @Test
    public void convertNull() {
        ComplexAdGroup complexAdGroup = webAdGroupToCoreComplexAdGroup(null);
        assertThat(complexAdGroup, nullValue());
    }

    @Test
    public void convertNullList() {
        List<ComplexTextAdGroup> complexAdGroups = webAdGroupsToCoreComplexTextAdGroups(null);
        assertThat(complexAdGroups, nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convertListWithNullItem() {
        List<ComplexTextAdGroup> complexAdGroups =
                webAdGroupsToCoreComplexTextAdGroups(asList(new WebTextAdGroup(), null));
        assertThat(complexAdGroups, contains(notNullValue(), nullValue()));
    }

    @Test
    public void convertAdGroupWithBidModifiers() {
        WebTextAdGroup webTextAdGroup = new WebTextAdGroup()
                .withBidModifiers(new WebAdGroupBidModifiers()
                        .withExpressionBidModifiers(List.of(TestBidModifiers.SAMPLE_WEB_EXPRESS_MODIFIER))
                );
        ComplexTextAdGroup expected = new ComplexTextAdGroup()
                .withComplexBidModifier(new ComplexBidModifier()
                        .withExpressionModifiers(List.of(TestBidModifiers.SAMPLE_EXPRESS_MODIFIER))
                );

        ComplexTextAdGroup actual = webAdGroupToCoreComplexAdGroup(webTextAdGroup);
        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }
}
