package ru.yandex.direct.core.entity.performancefilter.schema.compiled;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;
import ru.yandex.direct.core.entity.performancefilter.validation.FilterConditionsValidator;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects.unknownField;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects.unknownOperator;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.JsonUtils.toJson;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class AutoAutoRuSchemaTest {
    private static final PerformanceFilterConditionDBFormatParser PARSER =
            PerformanceFilterConditionDBFormatParser.INSTANCE;

    private FilterConditionsValidator validator;

    private AutoAutoRu schema;

    private static Map<String, Object> map(String key, Object value) {
        return ImmutableMap.of(key, value);
    }

    private static List<String> list(String... values) {
        return asList(values);
    }

    public static List<String> parametersForValidate_successParams() {
        return StreamEx
                .of(
                        map("mark_id ilike", list("Москва")),
                        map("mark_id not ilike", list("Москва")),
                        map("folder_id ilike", list("Москва")),
                        map("folder_id not ilike", list("Москва")),
                        map("body_type ilike", list("Москва")),
                        map("body_type not ilike", list("Москва")),
                        map("wheel ilike", list("левый")),
                        map("color ilike", list("белый")),
                        map("metallic ilike", list("да")),
                        map("availability ilike", list("на заказ")),
                        map("year ==", list("1990")),
                        map("url ilike", list("Москва")),
                        map("url not ilike", list("Москва")),
                        map("url ==", list("Москва")),
                        map("price ==", list("100", "200")),
                        map("price >", list("100")),
                        map("price <", list("100")),
                        map("price <->", list("100-200", "200-300"))
                )
                .map(JsonUtils::toJson)
                .toList();
    }

    @Before
    public void setUp() {
        schema = new AutoAutoRu();
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
    public void validate_success_whenAllConditionsSet() {
        Map<String, List<String>> filter = EntryStream.of(
                "mark_id ilike", list("Москва"),
                "folder_id ilike", list("Москва"),
                "body_type ilike", list("Москва"),
                "wheel ilike", list("левый"),
                "color ilike", list("белый"),
                "metallic ilike", list("да"),
                "availability ilike", list("в наличии"),
                "year ==", list("1990"),
                "url ==", singletonList("https://example.com"),
                "price <->", asList("3000-100000", "111-222")
        ).toMap();
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));

        ValidationResult<?, Defect> result = validator.apply(conditions);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onMarkIdTooLongValue() {
        validate_error_onTooLongValue("mark_id", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onFolderIdTooLongValue() {
        validate_error_onTooLongValue("folder_id", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onBodyTypeTooLongValue() {
        validate_error_onTooLongValue("body_type", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onColorTooLongValue() {
        validate_error_onTooLongValue("color", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onUrlTooLongValue() {
        validate_error_onTooLongValue("url", Operator.EQUALS);
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
    public void validate_error_onWheelTooLongValue() {
        validate_error_onTooLongValue("wheel", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onMetallicTooLongValue() {
        validate_error_onTooLongValue("metallic", Operator.CONTAINS);
    }

    @Test
    public void validate_error_onAvailabilityTooLongValue() {
        validate_error_onTooLongValue("availability", Operator.CONTAINS);
    }

    @Test
    public void validate_error_invalidYear() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "year ==", singletonList("1969")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));

        ValidationResult<?, Defect> result = validator.apply(conditions);

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("parsed_value"), index(0)),
                        greaterThanOrEqualTo(1970.0))
        )));
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
