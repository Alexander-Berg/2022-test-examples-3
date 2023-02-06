package ru.yandex.direct.core.entity.performancefilter.schema.compiled;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.schema.parser.DecimalRangeListParser;
import ru.yandex.direct.core.entity.performancefilter.schema.parser.ObjectListParser;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;
import ru.yandex.direct.core.entity.performancefilter.validation.FilterConditionsValidator;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects.unknownField;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects.unknownOperator;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.JsonUtils.toJson;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeInInterval;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class OtherYandexMarketSchemaTest {
    private static final PerformanceFilterConditionDBFormatParser PARSER =
            PerformanceFilterConditionDBFormatParser.INSTANCE;
    private OtherYandexMarket schema;
    private FilterConditionsValidator validator;

    // Ограничение на 'categoryId' больше, чем общие ограничения на остальные поля
    private static final int MAX_ITEM_COUNT_CATEGORY_ID = 20000;

    private static Map<String, Object> map(String key, Object value) {
        return ImmutableMap.of(key, value);
    }

    private static List<String> list(String... values) {
        return asList(values);
    }

    public static List<String> parametersForValidate_successParams() {
        return StreamEx
                .of(
                        map("id ==", list("100", "200")),
                        map("id >", list("100")),
                        map("id <", list("100")),
                        map("id <->", list("100-200", "200-300")),
                        map("categoryId ==", list("100", "200")),
                        map("categoryId >", list("100")),
                        map("categoryId <", list("100")),
                        map("categoryId <->", list("100-200", "200-300")),
                        map("url ilike", list("Москва")),
                        map("url not ilike", list("Москва")),
                        map("url ==", list("Москва")),
                        map("name ilike", list("Москва")),
                        map("name not ilike", list("Москва")),
                        map("vendor ilike", list("Москва")),
                        map("vendor not ilike", list("Москва")),
                        map("vendor exists", "1"),
                        map("price ==", list("100", "200")),
                        map("price >", list("100")),
                        map("price <", list("100")),
                        map("price <->", list("100-200", "200-300")),
                        map("model ilike", list("Москва")),
                        map("model not ilike", list("Москва")),
                        map("description ilike", list("Москва")),
                        map("description not ilike", list("Москва")),
                        map("typePrefix ilike", list("Москва")),
                        map("typePrefix not ilike", list("Москва")),
                        map("typePrefix exists", "1"),
                        map("oldprice ==", list("100", "200")),
                        map("oldprice >", list("100")),
                        map("oldprice <", list("100")),
                        map("oldprice <->", list("100-200", "200-300")),
                        map("oldprice exists", "1"),
                        map("market_category ilike", list("Москва")),
                        map("market_category not ilike", list("Москва")),
                        map("market_category exists", "1"),
                        map("store ==", list("1")),
                        map("store exists", list("1")),
                        map("manufacturer_warranty ==", list("1")),
                        map("manufacturer_warranty exists", list("1")),
                        map("adult ==", list("1")),
                        map("adult exists", list("1")),
                        map("age ==", list("18")),
                        map("age exists", "1")
                )
                .map(JsonUtils::toJson)
                .toList();
    }

    @Before
    public void setUp() {
        schema = new OtherYandexMarket();
        validator = new FilterConditionsValidator(schema, PerformanceFilterTab.CONDITION);
    }

    @Test
    @Parameters(method = "parametersForValidate_successParams")
    public void validate_successParams(String jsonFilter) {
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, jsonFilter);
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_success_whenFirstBunchOfConditionsSet() {
        Map<String, List<String>> filter = Stream.of(new Object[][]{
                {"id ==", list("100", "200")},
                {"categoryId ==", list("6")},
                {"url ilike", singletonList("Москва")},
                {"name ilike", singletonList("Москва")},
                {"vendor ilike", singletonList("Москва")},
                {"price <->", asList("3000-100000", "111-222")},
                {"model ilike", singletonList("Москва")},
                {"description ilike", singletonList("Москва")},
                {"typePrefix ilike", singletonList("Москва")},
                {"oldprice <->", asList("3000-100000", "111-222")}
        }).collect(toMap(kv -> (String) kv[0], kv -> (List<String>) kv[1]));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_success_whenSecondBunchOfConditionsSet() {
        Map<String, List<String>> filter = Stream.of(new Object[][]{
                {"market_category ilike", singletonList("Москва")},
                {"store ==", singletonList("1")},
                {"manufacturer_warranty ==", singletonList("1")},
                {"adult ==", singletonList("1")},
                {"age ==", singletonList("0")}
        }).collect(toMap(kv -> (String) kv[0], kv -> (List<String>) kv[1]));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onIdItemsMoreThanLimit() {
        // todo maxlog: не уверен, что это честное ограничение
        List<String> ids = Stream.generate(() -> "1")
                .limit(ObjectListParser.MAX_ITEM_COUNT + 1)
                .collect(Collectors.toList());
        Map<String, Object> filter = map("id ==", ids);
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value")),
                collectionSizeInInterval(1, ObjectListParser.MAX_ITEM_COUNT)
        ))));
    }

    @Test
    public void validate_error_onIdItemsMoreThanLimit_range() {
        List<String> ids = Stream.generate(() -> "100-200")
                .limit(DecimalRangeListParser.RANGE_MAX_ITEM_COUNT + 1)
                .collect(Collectors.toList());
        Map<String, Object> filter = map("id <->", ids);
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value")),
                collectionSizeInInterval(1, DecimalRangeListParser.RANGE_MAX_ITEM_COUNT)
        ))));
    }

    @Test
    public void validate_success_onCategoryIdItemsMoreThanCommonLimit() {
        List<String> categoryIds = Stream.generate(() -> "1")
                .limit(ObjectListParser.MAX_ITEM_COUNT + 1)
                .collect(Collectors.toList());
        Map<String, Object> filter = map("categoryId ==", categoryIds);
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onCategoryIdItemsMoreThanLimit() {
        List<String> categoryIds = Stream.generate(() -> "1")
                .limit(MAX_ITEM_COUNT_CATEGORY_ID + 1)
                .collect(Collectors.toList());
        Map<String, Object> filter = map("categoryId ==", categoryIds);
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value")),
                collectionSizeInInterval(1, MAX_ITEM_COUNT_CATEGORY_ID)
        ))));
    }

    @Test
    public void validate_error_onCategoryIdItemsMoreThanLimit_range() {
        List<String> categoryIds = Stream.generate(() -> "100-200")
                .limit(DecimalRangeListParser.RANGE_MAX_ITEM_COUNT + 1)
                .collect(Collectors.toList());
        Map<String, Object> filter = map("categoryId <->", categoryIds);
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value")),
                collectionSizeInInterval(1, DecimalRangeListParser.RANGE_MAX_ITEM_COUNT)
        ))));
    }

    @Test
    public void validate_error_onUrlTooLongValue() {
        validate_error_onTooLongValue("url", Operator.EQUALS);
    }

    @Test
    public void validate_error_onNameIdTooLongValue() {
        validate_error_onTooLongValue("name", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onVendorTooLongValue() {
        validate_error_onTooLongValue("vendor", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onModelTooLongValue() {
        validate_error_onTooLongValue("model", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onDescriptionTooLongValue() {
        validate_error_onTooLongValue("description", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onTypePrefixTooLongValue() {
        validate_error_onTooLongValue("typePrefix", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onMarketCategoryTooLongValue() {
        validate_error_onTooLongValue("market_category", Operator.CONTAINS);
    }

    private void validate_error_onTooLongValue(String field, Operator operator) {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                field + " " + operator, singletonList(StringUtils.repeat('A', 176))
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                maxStringLength(175)
        ))));
    }

    @Test
    public void validate_error_onPriceNegativeValue() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "price ==", singletonList("-1")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                greaterThanOrEqualTo(0.0)
        ))));
    }

    @Test
    public void validate_error_onOldPriceNegativeValue() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "oldprice ==", singletonList("-1")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                greaterThanOrEqualTo(0.0)
        ))));
    }

    @Test
    public void validate_error_onAgeIsToTheLeftOfBounds() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "age ==", singletonList("-1")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                inCollection()
        ))));
    }

    @Test
    public void validate_error_onAgeIsToTheRightOfBounds() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "age ==", singletonList("19")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                inCollection()
        ))));
    }

    @Test
    public void validate_error_unknownField() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "unknown ilike", singletonList("Москва")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("fieldName")), unknownField())
        )));
    }

    @Test
    public void validate_error_unknownOperator() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "url equals", singletonList("Москва")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("operator")), unknownOperator()))));
    }

}
