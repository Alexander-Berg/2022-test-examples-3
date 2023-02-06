package ru.yandex.direct.jobs.advq.offline.export;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

class OfflineAdvqReducerIntersectionsTest {

    private static OfflineAdvqReducer reducer = new OfflineAdvqReducer(singleton("в"));

    static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"\"привет\"", Arrays.asList("один", "два"), Collections.emptyList()},
                {"привет", Arrays.asList("один", "два"), Arrays.asList("один", "два")},
                {"привет мир", Arrays.asList("один", "два"), Arrays.asList("один", "два")},
                {"привет мир", Arrays.asList("один", "два", "привет"), Arrays.asList("один", "два")},
                {"привет мир", Arrays.asList("один", "два", "!приветы", "!миры"),
                        Arrays.asList("один", "два", "!приветы", "!миры")},
                {"!приветы мир", Arrays.asList("один", "два", "!приветы", "!миры"),
                        Arrays.asList("один", "два", "!миры")},
                {"привет мир", singletonList("[зеленый мир]"), singletonList("[зеленый мир]")},
                {"зеленый мир", singletonList("[зеленый мир]"), singletonList("[зеленый мир]")},
        });
    }

    @ParameterizedTest(name = "{1} - ({0}) == {2}")
    @MethodSource("params")
    void testMinusWordsWithoutIntersections(String plusKeyword, List<String> minusKeywordsList,
            List<String> expectedMinusKeywordsList)
    {
        List<String> minusWords = reducer.getMinusWordsWithoutIntersections(plusKeyword, minusKeywordsList);
        assertThat("Минус фразы совпали", minusWords, containsInAnyOrder(expectedMinusKeywordsList.toArray()));
    }
}
