package ru.yandex.direct.core.entity.performancefilter.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault;
import ru.yandex.direct.core.entity.performancefilter.schema.compiled.RetailGoogleMerchantSchema;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema.AVAILABLE;
import static ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema.URL_FIELD_NAME;
import static ru.yandex.direct.core.entity.performancefilter.schema.compiled.PerformanceDefault.PICKUP;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class PerformanceFilterConditionDBFormatSerializerTest {
    @Parameterized.Parameter
    public List<PerformanceFilterCondition> conditions;
    @Parameterized.Parameter(1)
    public String expectedConditionJson;
    @Parameterized.Parameter(2)
    public FilterSchema filterSchema;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return mapList(asList(new Object[][]{
                // "available"
                {singletonList(condition(AVAILABLE, Operator.EQUALS, "true")), "{\"available\":\"true\"}"},
                {singletonList(condition(AVAILABLE, Operator.EQUALS, "false")), "{}"},
                {singletonList(condition(AVAILABLE, Operator.EQUALS, "test")), "{}"},
                {singletonList(condition(AVAILABLE, Operator.EQUALS, "")), "{}"},
                {singletonList(condition(AVAILABLE, Operator.CONTAINS, "[\"true\"]")), "{}"},
                {singletonList(condition("categoryId", Operator.EQUALS, "[1234,5678]")),
                        "{\"categoryId ==\":[1234,5678]}"},

                // "pickup"
                {List.of(condition(PICKUP, Operator.EQUALS, "true")), "{\"pickup\":\"true\"}"},
                {List.of(condition(PICKUP, Operator.EQUALS, "false")), "{\"pickup\":\"false\"}"},
                {List.of(condition(PICKUP, Operator.EQUALS, "test")), "{}"},
                {List.of(condition(PICKUP, Operator.EQUALS, "")), "{}"},
                {List.of(condition(PICKUP, Operator.CONTAINS, "[\"true\"]")), "{}"},

                // "url"
                {singletonList(condition(URL_FIELD_NAME, Operator.EQUALS, "[\"http://ya.ru\"]")),
                        "{\"url\":[\"http://ya.ru\"]}"},
                {singletonList(condition(URL_FIELD_NAME, Operator.CONTAINS, "[\"http://yandex.ru\"]")),
                        "{\"url ilike\":[\"http://yandex.ru\"]}"},
                {singletonList(condition("price", Operator.EQUALS, "[555,777]")),
                        "{\"price ==\":[555,777]}"},
                {singletonList(condition("model", Operator.CONTAINS, "[\"test1\",\"test2\"]")),
                        "{\"model ilike\":[\"test1\",\"test2\"]}"},
                {singletonList(condition("availability", Operator.EQUALS, "[\"in stock\"]")),
                        "{\"availability\":[\"in stock\"]}", new RetailGoogleMerchantSchema()},

                // сортировка условий
                {asList( // условия в произвольном порядке
                        condition("categoryId", Operator.RANGE, "[\"10-20\",\"100-200\"]"),
                        condition("vendor", Operator.EXISTS, "1"),
                        condition("categoryId", Operator.LESS, "[100]"),
                        condition("vendor", Operator.CONTAINS, "[\"test\"]"),
                        condition("categoryId", Operator.EQUALS, "[123,456,789]"),
                        condition("vendor", Operator.NOT_CONTAINS, "[\"test2\"]"),
                        condition("categoryId", Operator.GREATER, "[10]"),
                        condition("available", Operator.EQUALS, "true"),
                        condition(PICKUP, Operator.EQUALS, "true"),
                        condition("model", Operator.CONTAINS, "[\"test\"]")

                ), // в БД должно быть упорядочено по ключу
                        "{\"available\":\"true\","
                                + "\"categoryId <\":[100],"
                                + "\"categoryId <->\":[\"10-20\",\"100-200\"],"
                                + "\"categoryId ==\":[123,456,789],"
                                + "\"categoryId >\":[10],"
                                + "\"model ilike\":[\"test\"],"
                                + "\"pickup\":\"true\","
                                + "\"vendor exists\":\"1\","
                                + "\"vendor ilike\":[\"test\"],"
                                + "\"vendor not ilike\":[\"test2\"]}"},

                {asList(
                        condition("description", Operator.CONTAINS, "[\"test\"]"),
                        condition("id", Operator.GREATER, "[10]"),
                        condition("name", Operator.CONTAINS, "[\"test\"]"),
                        condition("vendor", Operator.NOT_CONTAINS, "[\"test2\"]"),
                        condition("model", Operator.CONTAINS, "[\"test\"]"),
                        condition("oldprice", Operator.EQUALS, "[100]"),
                        condition("url", Operator.CONTAINS, "[\"http://ya.ru\"]"),
                        condition("categoryId", Operator.GREATER, "[7]"),
                        condition("market_category", Operator.NOT_CONTAINS, "[\"100\"]"),
                        condition("typePrefix", Operator.EXISTS, "1")
                ),
                        "{\"categoryId >\":[7],"
                                + "\"description ilike\":[\"test\"],"
                                + "\"id >\":[10],"
                                + "\"market_category not ilike\":[\"100\"],"
                                + "\"model ilike\":[\"test\"],"
                                + "\"name ilike\":[\"test\"],"
                                + "\"oldprice ==\":[100],"
                                + "\"typePrefix exists\":\"1\","
                                + "\"url ilike\":[\"http://ya.ru\"],"
                                + "\"vendor not ilike\":[\"test2\"]}"},

                {asList(
                        condition("url", Operator.CONTAINS, "[\"http://yandex.ru\"]"),
                        condition("url", Operator.NOT_CONTAINS, "[\"https://yandex.ru\"]"),
                        condition("url", Operator.EQUALS, "[\"http://ya.ru\",\"http://yandex.ru\"]")
                ),
                        "{\"url\":[\"http://ya.ru\",\"http://yandex.ru\"],"
                                + "\"url ilike\":[\"http://yandex.ru\"],"
                                + "\"url not ilike\":[\"https://yandex.ru\"]}"},

                // поддерживаем передачу числовых параметров и в виде строк, конвертируя их в числа
                {singletonList(condition("id", Operator.EQUALS, "[\"10\"]")),
                        "{\"id ==\":[10]}"},
                {singletonList(condition("categoryId", Operator.EQUALS, "[\"1234\",\"5678\"]")),
                        "{\"categoryId ==\":[1234,5678]}"},
                {singletonList(condition("price", Operator.LESS, "[\"100\"]")),
                        "{\"price <\":[100]}"},
                {singletonList(condition("oldprice", Operator.GREATER, "[\"100\"]")),
                        "{\"oldprice >\":[100]}"},
        }), parameters -> {
            if (parameters.length == 2) {
                parameters = Arrays.copyOf(parameters, 3);
            }
            parameters[2] = nvl(parameters[2], new PerformanceDefault());
            return parameters;
        });
    }

    private static PerformanceFilterCondition<?> condition(String fieldName, Operator operator, String stringValue) {
        return new PerformanceFilterCondition<>(fieldName, operator, stringValue);
    }

    @Test
    public void test() {
        PerformanceFilterConditionDBFormatParser.setParsedValue(filterSchema, conditions);

        String actualConditionJson = PerformanceFilterConditionDBFormatSerializer.INSTANCE.serialize(conditions);
        assertThat(actualConditionJson).isEqualTo(expectedConditionJson);
    }
}
