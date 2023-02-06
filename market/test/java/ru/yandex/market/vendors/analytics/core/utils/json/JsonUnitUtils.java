package ru.yandex.market.vendors.analytics.core.utils.json;

import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;

/**
 * @author antipov93.
 */
public class JsonUnitUtils {

    private JsonUnitUtils() {
    }

    /**
     * Сравнивает два json'а на равенство, заменяя заглушки дат на значения, рассчитанные от текущей даты.
     * В expected можно написать заглушку следующим образом: "date": "${json-unit.matches:today}-1d",
     * тогда в поле "date" будет ожидаться вчеращняя дата.
     * <p>
     * Возможные значения параметров описаны в {@link JsonTodayDateMatcher}.
     */
    public static void assertJsonWithDatesEquals(String expected, String actual) {
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.withMatcher("today", new JsonTodayDateMatcher()));
    }

    public static void assertJsonEqualsIgnoreArrayOrder(String expected, String actual) {
        JsonAssert.assertJsonEquals(expected, actual, Configuration.empty().when(Option.IGNORING_ARRAY_ORDER));
    }
}
