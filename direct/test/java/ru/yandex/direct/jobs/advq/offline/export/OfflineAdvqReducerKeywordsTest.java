package ru.yandex.direct.jobs.advq.offline.export;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

class OfflineAdvqReducerKeywordsTest {
    private static OfflineAdvqReducer reducer = new OfflineAdvqReducer(Collections.singleton("в"));

    static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"\"привет\"", "\"привет\"", Collections.emptyList()},
                {"привет -мир", "привет", Collections.singletonList("мир")},
                {"привет мир -пока", "привет мир", Collections.singletonList("пока")},
                {"привет !мой мир красно-черный -пока -вода", "привет !мой мир красно-черный",
                        Arrays.asList("пока", "вода")},
        });
    }

    @ParameterizedTest(name = "{0} == ({1}) - {2}")
    @MethodSource("params")
    void testMinusWordsWithoutIntersections(String plusKeyword,
            String expectedKeyword, List<String> expectedMinusKeywordsSet)
    {
        Set<String> minusKeywordsSet = new HashSet<>();
        String keyword = reducer.extractKeywordAndMinusKeywords(plusKeyword, minusKeywordsSet);
        assertThat("Плюс фразы совпали", keyword, equalTo(expectedKeyword));
        assertThat("Минус фразы совпали", minusKeywordsSet, containsInAnyOrder(expectedMinusKeywordsSet.toArray()));
    }
}
