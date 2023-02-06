package ru.yandex.direct.test.utils;

import java.util.List;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class QueryUtils {

    private QueryUtils() {
    }

    // Select#toString() в jooq возращает выбираемые поля в произвольном порядке,
    // и просто сравнить два запроса нельзя.
    // Поэтому сортируем строки из запроса и проверяем совпадение отсортированных списков
    // При изменении просто нужно вставить требуемый запрос в файл, не редактируя
    public static void compareQueries(String expected, String actual) {

//        Раскомментировать при расхождении для удобства поиска ошибки
//        assertEquals(expected, actual);

        Function<String, List<String>> preparator = q -> StreamEx.split(q, "\n")
                .map(String::trim)
                .map(s -> StringUtils.stripEnd(s, ","))
                .sorted()
                .collect(toList());
        assertEquals(preparator.apply(expected), preparator.apply(actual));
    }
}
