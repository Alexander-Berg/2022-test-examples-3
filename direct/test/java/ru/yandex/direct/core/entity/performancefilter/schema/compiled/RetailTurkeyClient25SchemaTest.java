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

import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterStorage;
import ru.yandex.direct.core.entity.performancefilter.validation.FilterConditionsValidator;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
public class RetailTurkeyClient25SchemaTest {
    private static final PerformanceFilterConditionDBFormatParser PARSER =
            PerformanceFilterConditionDBFormatParser.INSTANCE;

    private FilterConditionsValidator validator;

    private RetailTurkeyClient25Schema schema;

    private static Map<String, Object> map(String key, Object value) {
        return ImmutableMap.of(key, value);
    }

    private static List<String> list(String... values) {
        return asList(values);
    }

    public static List<String> parametersForValidate_successParams() {
        return StreamEx
                .of(
                        map("campaign_id ==", list("1", "54")),
                        map("campaign_id >", list("1")),
                        map("campaign_id <", list("1")),
                        map("campaign_id <->", list("100-200", "200-300")),
                        map("discount_amount ==", list("1", "54")),
                        map("discount_amount >", list("1")),
                        map("discount_amount <", list("1")),
                        map("discount_amount <->", list("100-200", "200-300")),
                        map("cpc_target ==", list("1", "54")),
                        map("cpc_target >", list("1")),
                        map("cpc_target <", list("1")),
                        map("cpc_target <->", list("100-200", "200-300")),
                        map("domain ==", list("http://example.com")),
                        map("domain ilike", list("Москва")),
                        map("domain not ilike", list("Москва")),
                        map("merchant_name ilike", list("Москва")),
                        map("merchant_name not ilike", list("Москва")),
                        map("merchant_name exists", "1"),
                        map("conditions ilike", list("Москва")),
                        map("conditions not ilike", list("Москва")),
                        map("conditions exists", "1"),
                        map("geo_target ilike", list("Москва")),
                        map("geo_target not ilike", list("Москва")),
                        map("geo_target exists", "1"),
                        map("auditory_target ilike", list("Москва")),
                        map("auditory_target not ilike", list("Москва")),
                        map("auditory_target exists", "1"),
                        map("description ilike", list("Москва")),
                        map("description not ilike", list("Москва")),
                        map("description exists", "1"),
                        map("discount_type ilike", list("Москва")),
                        map("discount_type not ilike", list("Москва")),
                        map("discount_type exists", "1")
                )
                .map(JsonUtils::toJson)
                .toList();
    }

    @Before
    public void setUp() {
        schema = new RetailTurkeyClient25Schema();
        PerformanceFilterStorage storageMock = mock(PerformanceFilterStorage.class);
        when(storageMock.getFilterSchema(eq(BusinessType.RETAIL), eq(FeedType.TURKEY_CLIENT_25)))
                .thenReturn(schema);
        when(storageMock.getFilterSchema(any(PerformanceFilter.class)))
                .thenReturn(schema);
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
                "campaign_id ==", singletonList("23"),
                "discount_amount ==", singletonList("23"),
                "cpc_target ==", singletonList("23"),
                "domain ==", singletonList("http://example.com"),
                "merchant_name ilike", singletonList("Москва"),
                "conditions ilike", singletonList("Москва"),
                "geo_target ilike", singletonList("Москва"),
                "auditory_target ilike", singletonList("Москва"),
                "description ilike", singletonList("Москва"),
                "discount_type ilike", singletonList("Москва")
        ).toMap();
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onCampaignIdNegativeValue() {
        validate_error_onNegativeValue("campaign_id");
    }

    @Test
    public void validate_error_onDiscountAmountNegativeValue() {
        validate_error_onNegativeValue("discount_amount");
    }

    @Test
    public void validate_error_onCpcTargetNegativeValue() {
        validate_error_onNegativeValue("cpc_target");
    }

    private void validate_error_onNegativeValue(String field) {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(field + " ==", singletonList("-1"));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                greaterThanOrEqualTo(0.0)
        ))));
    }

    @Test
    public void validate_error_onDomainTooLongValue() {
        validate_error_onTooLongValue("domain");
    }

    @Test
    public void validate_error_onMerchantNameTooLongValue() {
        validate_error_onTooLongValue("merchant_name");
    }

    @Test
    public void validate_error_onConditionsTooLongValue() {
        validate_error_onTooLongValue("conditions");
    }

    @Test
    public void validate_error_onGeoTargetTooLongValue() {
        validate_error_onTooLongValue("geo_target");
    }

    @Test
    public void validate_error_onAuditoryTargetTooLongValue() {
        validate_error_onTooLongValue("auditory_target");
    }

    @Test
    public void validate_error_onDescriptionTooLongValue() {
        validate_error_onTooLongValue("description");
    }

    @Test
    public void validate_error_onDiscountTypeTooLongValue() {
        validate_error_onTooLongValue("discount_type");
    }

    private void validate_error_onTooLongValue(String field) {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of(
                field + " ilike", singletonList(StringUtils.repeat('A', 176)));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field("parsed_value"), index(0)),
                maxStringLength(175)))));
    }

    @Test
    public void validate_error_unknownField() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of("unknown ilike", singletonList("Москва"));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(
                matchedBy(hasDefectWithDefinition(
                        validationError(
                                path(index(0), field("fieldName")), unknownField()))));
    }

    @Test
    public void validate_error_unknownOperator() {
        ImmutableMap<String, List<String>> filter = ImmutableMap.of("geo_target equals", singletonList("Москва"));
        List<PerformanceFilterCondition> conditions = PARSER.parse(schema, toJson(filter));
        ValidationResult<?, Defect> result = validator.apply(conditions);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("operator")), unknownOperator()))));
    }
}
