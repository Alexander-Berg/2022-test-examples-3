package ru.yandex.direct.core.entity.performancefilter.schema.compiled;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
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
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeInInterval;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class HotelsGoogleHotelsSchemaTest {
    private static final PerformanceFilterConditionDBFormatParser PARSER =
            PerformanceFilterConditionDBFormatParser.INSTANCE;
    private HotelsGoogleHotelsSchema schema;
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
                        map("Price ==", list("100", "200")),
                        map("Price >", list("100")),
                        map("Price <", list("100")),
                        map("Price <->", list("100-200", "200-300")),
                        map("Description ilike", list("Москва")),
                        map("Description not ilike", list("Москва")),
                        map("name ilike", list("Москва")),
                        map("name not ilike", list("Москва")),
                        map("location ilike", list("Москва")),
                        map("location not ilike", list("Москва")),
                        map("class ==", list("5")),
                        map("class exists", "1"),
                        map("url ilike", list("Москва")),
                        map("url not ilike", list("Москва")),
                        map("OfferID ==", list("100", "200")),
                        map("OfferID >", list("100")),
                        map("OfferID <", list("100")),
                        map("OfferID <->", list("100-200", "200-300")),
                        map("score ==", list("100", "200")),
                        map("score >", list("100")),
                        map("score <", list("100")),
                        map("score <->", list("100-200", "200-300")),
                        map("max_score ilike", list("Москва")),
                        map("max_score not ilike", list("Москва"))
                )
                .map(JsonUtils::toJson)
                .toList();
    }

    @Before
    public void setUp() {
        schema = new HotelsGoogleHotelsSchema();
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
                "Price <->", asList("3000-100000", "111-222"),
                "Description ilike", singletonList("Москва"),
                "name ilike", singletonList("Москва"),
                "location ilike", singletonList("Москва"),
                "class ==", list("2"),
                "url ilike", singletonList("Москва"),
                "OfferID ==", singletonList("2"),
                "score ==", singletonList("6"),
                "max_score ilike", singletonList("Москва")
        ).toMap();
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onPriceItemsMoreThan100() {
        List<String> ids = Stream.generate(() -> "1").limit(101).collect(Collectors.toList());
        Map<String, Object> filter = map("Price ==", ids);
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value")),
                collectionSizeInInterval(1, 100)
        ))));
    }

    @Test
    public void validate_error_onDescriptionTooLongValue() {
        validate_error_onTooLongValue("Description");
    }

    @Test
    public void validate_error_onNameTooLongValue() {
        validate_error_onTooLongValue("name");
    }

    @Test
    public void validate_error_onLocationTooLongValue() {
        validate_error_onTooLongValue("location");
    }

    @Test
    public void validate_error_onUrlTooLongValue() {
        validate_error_onTooLongValue("url");
    }

    @Test
    public void validate_error_onMaxScoreTooLongValue() {
        validate_error_onTooLongValue("max_score");
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
        validate_error_onNegativeValue("Price", false);
    }

    @Test
    public void validate_error_onOfferIdNegativeValue() {
        validate_error_onNegativeValue("OfferID", true);
    }

    @Test
    public void validate_error_onScoreNegativeValue() {
        validate_error_onNegativeValue("score", false);
    }

    public void validate_error_onNegativeValue(String field, boolean isId) {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                field + " ==", singletonList("-1"));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        Defect<NumberDefectParams> expectedDefectType =
                isId ? greaterThanOrEqualTo(0L) : greaterThanOrEqualTo(0.0d);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)), expectedDefectType))));
    }

    @Test
    public void validate_error_onClassIsToTheLeftOfBounds() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "class ==", singletonList("0")
        );
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                inCollection()
        ))));
    }

    @Test
    public void validate_error_onClassIsToTheRightOfBounds() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                "class ==", singletonList("6")
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
