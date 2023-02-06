package ru.yandex.direct.core.entity.performancefilter.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault.CATEGORY_ID;
import static ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault.PICKUP;
import static ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault.PRICE;

@RunWith(Parameterized.class)
public class PerformanceFilterConditionDBFormatParserTest {
    @Parameterized.Parameter
    public String conditionJson;
    @Parameterized.Parameter(1)
    public List<PerformanceFilterCondition<?>> expectedConditions;

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{
                {"{\"categoryId ==\":[281,2810005933,28100059330123630]}",
                        List.of(condition(CATEGORY_ID, Operator.EQUALS, "[\"281\",\"2810005933\",\"28100059330123630\"]")
                                .withParsedValue(List.of(281L, 2810005933L, 28100059330123630L)))},
                {"{\"price ==\":[555,777]}",
                        List.of(condition(PRICE, Operator.EQUALS, "[555,777]")
                                .withParsedValue(List.of(555.0, 777.0)))},
                {"{\"pickup\":\"true\"}",
                        List.of(condition(PICKUP, Operator.EQUALS, "true")
                                .withParsedValue(true))}
        };
    }

    private static <V> PerformanceFilterCondition<V> condition(String fieldName, Operator operator, String stringValue) {
        return new PerformanceFilterCondition<>(fieldName, operator, stringValue);
    }

    @Test
    public void test() {
        PerformanceDefault filterSchema = new PerformanceDefault();
        List<PerformanceFilterCondition> conditions =
                PerformanceFilterConditionDBFormatParser.INSTANCE.parse(filterSchema, conditionJson);
        assertThat(conditions).isEqualTo(expectedConditions);
    }
}
