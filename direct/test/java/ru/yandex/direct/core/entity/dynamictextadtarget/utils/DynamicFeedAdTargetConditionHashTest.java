package ru.yandex.direct.core.entity.dynamictextadtarget.utils;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterConditionDBFormatParser;

import static java.util.Arrays.asList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetMapping.dynamicFeedRulesToJson;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;

@RunWith(Parameterized.class)
public class DynamicFeedAdTargetConditionHashTest {

    @Parameterized.Parameter
    public List<DynamicFeedRule> conditions;
    @Parameterized.Parameter(1)
    public String expectedConditionJson;
    @Parameterized.Parameter(2)
    public BigInteger expectedConditionHash;

    // значения хешей взяты из базы. а в базу добавлены через перл реализацию
    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        List.of(condition("categoryId", Operator.EQUALS, "[1]")),
                        "{\"categoryId ==\":[1]}",
                        new BigInteger("9029039109406902040")
                },
                // available
                {
                        List.of(condition("categoryId", Operator.EQUALS, "[1]"),
                                condition("available", Operator.EQUALS, "true")
                        ),
                        "{\"available\":\"true\",\"categoryId ==\":[1]}",
                        new BigInteger("12737433948301979946")
                },
                {
                        List.of(condition("categoryId", Operator.EQUALS, "[1]"),
                                condition("available", Operator.EQUALS, "false")
                        ),
                        "{\"categoryId ==\":[1]}",
                        new BigInteger("9029039109406902040")
                },
                // проверяем что есть дробная часть
                {
                        List.of(condition("price", Operator.RANGE, "[\"300-500\"]")),
                        "{\"price <->\":[\"300.00-500.00\"]}",
                        new BigInteger("10473878146619731318")
                },
                // exists
                {
                        List.of(condition("vendor", Operator.EXISTS, "1")),
                        "{\"vendor exists\":\"1\"}",
                        new BigInteger("3330899150158696476")
                },
                {
                        List.of(condition("vendor", Operator.EXISTS, "1"),
                                condition("vendor", Operator.CONTAINS, "[\"nokian\"]")),
                        "{\"vendor exists\":\"1\",\"vendor ilike\":[\"nokian\"]}",
                        new BigInteger("9979517326462454852")
                },
                {
                        List.of(condition("vendor", Operator.EXISTS, "0"),
                                condition("vendor", Operator.CONTAINS, "[\"nokian\"]")),
                        "{\"vendor ilike\":[\"nokian\"]}",
                        new BigInteger("7819502149875148392")
                },
                {
                        List.of(condition("vendor", Operator.EXISTS, "test"),
                                condition("vendor", Operator.CONTAINS, "[\"nokian\"]")),
                        "{\"vendor ilike\":[\"nokian\"]}",
                        new BigInteger("7819502149875148392")
                },
                {
                        List.of(condition("vendor", Operator.CONTAINS, "[\"nokian\"]")),
                        "{\"vendor ilike\":[\"nokian\"]}",
                        new BigInteger("7819502149875148392")
                },
                // сортировка (по fieldName потом по operator)
                {
                        List.of(
                                condition("name", Operator.CONTAINS, "[\"Люстры\"]"),
                                condition("categoryId", Operator.EQUALS, "[93]"),
                                condition("url", Operator.CONTAINS, "[\"/product/\"]"),
                                condition("available", Operator.EQUALS, "true"),
                                condition("vendor", Operator.CONTAINS, "[\"Odeon Light\",\"lumion\"]")
                        ),
                        "{\"available\":\"true\",\"categoryId ==\":[93],\"name ilike\":[\"Люстры\"]," +
                                "\"url ilike\":[\"/product/\"],\"vendor ilike\":[\"Odeon Light\",\"lumion\"]}",
                        new BigInteger("16506368981312025915")
                },
                {
                        List.of(
                                condition("available", Operator.EQUALS, "true"),
                                condition("categoryId", Operator.GREATER, "[10]"),
                                condition("categoryId", Operator.LESS, "[100]"),
                                condition("categoryId", Operator.EQUALS, "[75,77]"),
                                condition("categoryId", Operator.RANGE, "[\"70-80\"]"),
                                condition("vendor", Operator.NOT_CONTAINS, "[\"bork\",\"miele\"]"),
                                condition("vendor", Operator.CONTAINS, "[\"bosch\"]")
                        ),
                        "{\"available\":\"true\"," +
                                "\"categoryId <\":[100],\"categoryId <->\":[\"70-80\"]," +
                                "\"categoryId ==\":[75,77],\"categoryId >\":[10]," +
                                "\"vendor ilike\":[\"bosch\"],\"vendor not ilike\":[\"bork\",\"miele\"]}",
                        new BigInteger("10786676769710244257")
                },
                { // не валидное условие (parsedValue = null), проверяем что не падает вычисление conditionHash
                        List.of(condition("vendor2", Operator.EXISTS, "1")),
                        "{\"vendor2 exists\":null}",
                        new BigInteger("11767636627401342699")
                },
                {
                        List.of(
                                condition("oldprice", Operator.RANGE, "[\"30000.00-39999.99\"]")
                        ),
                        "{\"oldprice <->\":[\"30000.00-39999.99\"]}",
                        new BigInteger("1791568826625297971")
                }
        });
    }

    private static DynamicFeedRule<?> condition(String fieldName, Operator operator, String stringValue) {
        return new DynamicFeedRule<>(fieldName, operator, stringValue);
    }

    @Test
    public void test() {
        FilterSchema filterSchema = new PerformanceDefault();
        PerformanceFilterConditionDBFormatParser.setParsedValue(filterSchema, conditions);

        String actualConditionJson = dynamicFeedRulesToJson(conditions);
        BigInteger actualConditionHash = getHashForDynamicFeedRules(conditions);

        assertSoftly(softly -> {
            softly.assertThat(actualConditionJson).isEqualTo(expectedConditionJson);
            softly.assertThat(actualConditionHash).isEqualTo(expectedConditionHash);
        });
    }
}
