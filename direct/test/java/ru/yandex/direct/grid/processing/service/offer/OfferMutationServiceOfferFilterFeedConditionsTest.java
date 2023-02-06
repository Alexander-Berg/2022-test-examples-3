package ru.yandex.direct.grid.processing.service.offer;

import java.util.List;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault;
import ru.yandex.direct.core.entity.performancefilter.schema.parser.ObjectListParser;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOffer;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.util.OfferTestDataUtils;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(Parameterized.class)
public class OfferMutationServiceOfferFilterFeedConditionsTest {
    private static class FeedCondition {
        private static final FilterSchema FILTER_SCHEMA = new PerformanceDefault();

        private final String fieldName;
        private final Operator operator;
        private final String stringValue;

        public FeedCondition(String fieldName, Operator operator, String stringValue) {
            this.fieldName = fieldName;
            this.operator = operator;
            this.stringValue = stringValue;
        }

        public <V> DynamicFeedRule<V> asDynamicFeedRule() {
            var dynamicFeedRule = new DynamicFeedRule<V>(fieldName, operator, stringValue);
            PerformanceFilterConditionDBFormatParser.setParsedValue(FILTER_SCHEMA, List.of(dynamicFeedRule));
            return dynamicFeedRule;
        }

        public <V> PerformanceFilterCondition<V> asPerformanceFilterCondition() {
            var performanceFilterCondition = new PerformanceFilterCondition<V>(fieldName, operator, stringValue);
            PerformanceFilterConditionDBFormatParser.setParsedValue(FILTER_SCHEMA, List.of(performanceFilterCondition));
            return performanceFilterCondition;
        }
    }

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private OfferMutationService offerMutationService;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<FeedCondition> initialConditions;

    @Parameterized.Parameter(2)
    public List<String> urls;

    @Parameterized.Parameter(3)
    public List<FeedCondition> expectedConditions;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                {"пустые списки",
                        List.of(),
                        List.of(),
                        List.of()},
                {"один товар - нет отфильтрованных товаров",
                        List.of(),
                        List.of(url(0)),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(0)))},
                {"один товар - есть другие отфильтрованные товары",
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(1))),
                        List.of(url(0)),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(1, 0)))},
                {"один товар - есть отфильтрованные по другим признакам товары",
                        List.of(condition(FilterSchema.AVAILABLE, Operator.EQUALS, "true")),
                        List.of(url(0)),
                        List.of(condition(FilterSchema.AVAILABLE, Operator.EQUALS, "true"),
                                condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(0)))},
                {"один товар - товар уже отфильтрован",
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(0, 1))),
                        List.of(url(0)),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(0, 1)))},
                {"один товар - есть полностью заполненное условие",
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT)))),
                        List.of(url(ObjectListParser.MAX_ITEM_COUNT)),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT))),
                                condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(ObjectListParser.MAX_ITEM_COUNT)))},
                {"несколько товаров - нет отфильтрованных товаров, не влезет в одно условие",
                        List.of(),
                        IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT * 2)
                                .mapToObj(OfferMutationServiceOfferFilterFeedConditionsTest::url)
                                .toList(),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT))),
                                condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT,
                                                ObjectListParser.MAX_ITEM_COUNT * 2))))},
                {"несколько товаров - часть уже отфильтрована",
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(1, 3))),
                        List.of(url(0), url(2)),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(1, 3, 0, 2)))},
                {"несколько товаров - переполнит существующее условие",
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT - 1)))),
                        List.of(url(ObjectListParser.MAX_ITEM_COUNT - 1), url(ObjectListParser.MAX_ITEM_COUNT)),
                        List.of(condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(IntStreamEx.range(ObjectListParser.MAX_ITEM_COUNT))),
                                condition(FilterSchema.URL_FIELD_NAME, Operator.NOT_CONTAINS,
                                        urlsStringList(ObjectListParser.MAX_ITEM_COUNT)))}
        };
    }

    private static FeedCondition condition(String fieldName, Operator operator, String stringValue) {
        return new FeedCondition(fieldName, operator, stringValue);
    }

    private static String urlsStringList(int... ids) {
        return urlsStringList(IntStreamEx.of(ids));
    }

    private static String urlsStringList(IntStreamEx ids) {
        List<String> urls = ids.mapToObj(OfferMutationServiceOfferFilterFeedConditionsTest::url).toList();
        return JsonUtils.toJson(urls);
    }

    private static String url(int id) {
        return String.format("https://example.com/%d", id);
    }

    @Test
    public void testDynamicFeedAdTarget() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup();
        DynamicFeedAdTarget dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDynamicFeedAdTarget(adGroupInfo, defaultDynamicFeedAdTarget(adGroupInfo)
                        .withCondition(mapList(initialConditions, FeedCondition::asDynamicFeedRule)));

        List<GdiOffer> offers = StreamEx.generate(OfferTestDataUtils::defaultGdiOffer)
                .zipWith(urls.stream()).mapKeyValue(GdiOffer::withUrl).toList();

        ModelChanges<DynamicFeedAdTarget> modelChanges = offerMutationService
                .getOfferFilterModelChanges(dynamicFeedAdTarget, offers);

        assertThat(modelChanges.getChangedPropsNames())
                .containsOnly(DynamicFeedAdTarget.CONDITION, DynamicFeedAdTarget.CONDITION_HASH);
        assertThat(modelChanges.getChangedProp(DynamicFeedAdTarget.CONDITION))
                .containsExactlyElementsOf(mapList(expectedConditions, FeedCondition::asDynamicFeedRule));
        assertThat(modelChanges.getChangedProp(DynamicFeedAdTarget.CONDITION_HASH))
                .isEqualTo(DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules(mapList(expectedConditions,
                        FeedCondition::asDynamicFeedRule)));
    }

    @Test
    public void testPerformanceFilter() {
        PerformanceFilterInfo filterInfo = steps.performanceFilterSteps().createDefaultPerformanceFilter();
        steps.performanceFilterSteps().setPerformanceFilterProperty(filterInfo, PerformanceFilter.CONDITIONS,
                mapList(initialConditions, FeedCondition::asPerformanceFilterCondition));
        PerformanceFilter filter = filterInfo.getFilter();

        List<GdiOffer> offers = StreamEx.generate(OfferTestDataUtils::defaultGdiOffer)
                .zipWith(urls.stream()).mapKeyValue(GdiOffer::withUrl).toList();

        ModelChanges<PerformanceFilter> modelChanges = offerMutationService
                .getOfferFilterModelChanges(filter, offers);

        assertThat(modelChanges.getChangedPropsNames())
                .containsOnly(PerformanceFilter.CONDITIONS);
        assertThat(modelChanges.getChangedProp(PerformanceFilter.CONDITIONS))
                .containsExactlyElementsOf(mapList(expectedConditions, FeedCondition::asPerformanceFilterCondition));
    }
}
