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
public class OtherGoogleCustomSchemaTest {
    private static final PerformanceFilterConditionDBFormatParser PARSER =
            PerformanceFilterConditionDBFormatParser.INSTANCE;

    private OtherGoogleCustomSchema schema;
    private FilterConditionsValidator validator;

    private static Map<String, Object> map(String key, Object value) {
        return ImmutableMap.of(key, value);
    }

    private static List<String> list(String... values) {
        return asList(values);
    }

    public static List<String> parametersForValidate_successParams() {
        return StreamEx
                .of(
                        map("name ilike", list("Москва")),
                        map("name not ilike", list("Москва")),
                        map("name exists", "1"),
                        map("url ==", list("https://example.com")),
                        map("url ilike", list("example")),
                        map("url not ilike", list("example")),
                        map("second_title ilike", list("Москва")),
                        map("second_title not ilike", list("Москва")),
                        map("second_title exists", "1"),
                        map("description ilike", list("Москва")),
                        map("description not ilike", list("Москва")),
                        map("description exists", "1"),
                        map("category ilike", list("Москва")),
                        map("category not ilike", list("Москва")),
                        map("category exists", "1"),
                        map("price ==", list("100", "200")),
                        map("price >", list("100")),
                        map("price <", list("100")),
                        map("price <->", list("100-200", "200-300")),
                        map("sale_price ==", list("100", "200")),
                        map("sale_price >", list("100")),
                        map("sale_price <", list("100")),
                        map("sale_price <->", list("100-200", "200-300"))
                )
                .map(JsonUtils::toJson)
                .toList();
    }

    @Before
    public void setUp() {
        schema = new OtherGoogleCustomSchema();
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
                "name ilike", list("Москва"),
                "url ilike", list("example.com"),
                "second_title ilike", list("Москва"),
                "description ilike", list("Москва"),
                "category ilike", list("Москва"),
                "price <->", asList("3000-100000", "111-222"),
                "sale_price ==", asList("300", "2222.3463")
        ).toMap();
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));

        ValidationResult<?, Defect> result = validator.apply(conditions);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onNameTooLongValue() {
        validate_error_onTooLongValue("name");
    }

    @Test
    public void validate_error_onURLTooLongValue() {
        validate_error_onTooLongValue("url");
    }

    @Test
    public void validate_error_onSecondTitleTooLongValue() {
        validate_error_onTooLongValue("second_title");
    }

    @Test
    public void validate_error_onDescriptionTooLongValue() {
        validate_error_onTooLongValue("description");
    }

    @Test
    public void validate_error_onCategoryTooLongValue() {
        validate_error_onTooLongValue("category");
    }

    private void validate_error_onTooLongValue(String field) {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                field + " ilike", singletonList(StringUtils.repeat('A', 176))
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
    public void validate_error_onSalePriceNegativeValue() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "sale_price ==", singletonList("-1")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));

        ValidationResult<?, Defect> result = validator.apply(conditions);

        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                greaterThanOrEqualTo(0.0)
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
