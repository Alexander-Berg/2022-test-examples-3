package ru.yandex.market.yt.binding;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.yt.binding.AbstractYTFetchTask.YTFetchTaskQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.yt.binding.AbstractYTFetchTask.buildQuery;

class YTFetchTaskQueryTest {

    @ParameterizedTest
    @MethodSource("exampleEnds")
    void testQuerySimple(String endsWith) {
        var query = new YTFetchTaskQuery("a1,a2 FROM [test] ORDER BY a1" + endsWith);
        assertEquals("a1,a2", query.columns);
        assertEquals("${1} FROM [test] ${2} ${3} ORDER BY a1 ${4} ${5}", normalize(query.template.getTemplate()));

        assertEquals("a1,a2 FROM [test] where a1 = a5 ORDER BY a1 limit 10",
                normalize(buildQuery(query, query.columns, "a1 = a5", 0, 10)));

        assertEquals("a1,a2 FROM [test] where a1 = a5 ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, query.columns, "a1 = a5", 1, 5)));

        assertEquals("a1,a2 FROM [test] ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, query.columns, "", 1, 5)));

        assertEquals("a1 FROM [test] ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, "a1", "", 1, 5)));

        assertEquals("a1 FROM [test] where a1 = a5 ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, "a1", "a1 = a5", 1, 5)));
    }

    @ParameterizedTest
    @MethodSource("exampleEnds")
    void testQueryWith(String endsWith) {
        var query = new YTFetchTaskQuery("a1,a2 FROM [test] where a2 > 0 ORDER BY a1" + endsWith);
        assertEquals("a1,a2", query.columns);
        assertEquals("${1} FROM [test] where a2 > 0 ${2} ${3} ORDER BY a1 ${4} ${5}",
                normalize(query.template.getTemplate()));

        assertEquals("a1,a2 FROM [test] where a2 > 0 and a1 = a5 ORDER BY a1 limit 10",
                normalize(buildQuery(query, query.columns, "a1 = a5", 0, 10)));

        assertEquals("a1,a2 FROM [test] where a2 > 0 and a1 = a5 ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, query.columns, "a1 = a5", 1, 5)));

        assertEquals("a1,a2 FROM [test] where a2 > 0 ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, query.columns, "", 1, 5)));

        assertEquals("a1 FROM [test] where a2 > 0 ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, "a1", "", 1, 5)));

        assertEquals("a1 FROM [test] where a2 > 0 and a1 = a5 ORDER BY a1 offset 1 limit 5",
                normalize(buildQuery(query, "a1", "a1 = a5", 1, 5)));
    }

    static Object[] exampleEnds() {
        // Неважно, как заканчивается условие - offset и limit будут удалены из запроса
        return new Object[]{
                "", " offset 1", " offset 1 limit 5", " limit 5"
        };
    }

    static String normalize(String text) {
        while (text.contains("  ")) { // Только для тестов
            text = text.replace("  ", " ");
        }
        return text;
    }
}
