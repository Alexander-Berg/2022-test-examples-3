package ru.yandex.market.pers.tms.moderation.filter;

import org.junit.Test;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.moderation.Object4Moderation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author vvolokh
 * 15.10.2018
 */
public class WordFilterTest {
    @Test
    public void testAscii() {
        Filter filter = PatternFilter.words("test", Collections.singletonList("test"));

        List<String> positiveCases = Arrays.asList("test", " test", "test ", " test ", "is test", "test me", "please test me", "-test-", ",test ", "please-test-me");
        List<Object4Moderation> positiveCasesList = prepareModObjectsFromTexts(positiveCases);

        List<String> negativeCases = Arrays.asList("testing", "atest", "for testing", "testing setup", "best testing setup", "testestest");
        List<Object4Moderation> negativeCasesList = prepareModObjectsFromTexts(negativeCases);

        Collection<Object4Moderation> markedPositiveCasesList = filter.match(positiveCasesList).getMatched();
        Collection<Object4Moderation> markerNegativeCasesList = filter.match(negativeCasesList).getMatched();

        assertEquals(markedPositiveCasesList.size(), positiveCases.size());
        assertEquals(markerNegativeCasesList.size(), 0);
    }

    @Test
    public void testNonAscii() {
        Filter filter = PatternFilter.words("тест", Collections.singletonList("тест"));

        List<String> positiveCases = Arrays.asList("тест", " тест", "тест ", " тест ", "это тест", "тест фраза", "это тест фраза", "-тест-", ",тест ", "это-тест-фраза");
        List<Object4Moderation> positiveCasesList = prepareModObjectsFromTexts(positiveCases);

        List<String> negativeCases = Arrays.asList("тестирование", "этотест", "для теста", "тестовая фраза", "лучшая тестовая фраза", "тестестест");
        List<Object4Moderation> negativeCasesList = prepareModObjectsFromTexts(negativeCases);

        Collection<Object4Moderation> markedPositiveCasesList = filter.match(positiveCasesList).getMatched();
        Collection<Object4Moderation> markerNegativeCasesList = filter.match(negativeCasesList).getMatched();

        assertEquals(positiveCases.size(), markedPositiveCasesList.size());
        assertEquals(0, markerNegativeCasesList.size());
    }

    private List<Object4Moderation> prepareModObjectsFromTexts(List<String> texts) {
        List<Object4Moderation> objects = new ArrayList<>();
        int count = 0;
        for (String str: texts) {
            objects.add(Object4Moderation.forModeration(++count, ModState.READY, str));
        }
        return objects;
    }
}
