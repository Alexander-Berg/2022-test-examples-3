package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.model.SingleKeyword;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.processing.MinusKeywordsDeduplicator.deduplicateSubKeywords;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class DeduplicateSubKeywordsTest {

    @Parameterized.Parameter
    public List<String> words;

    @Parameterized.Parameter(1)
    public List<String> expectedUniqueWords;

    @Parameterized.Parameters(name = "входные слова: {0}, ожидаемые слова: {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][] {
                // слова с одной леммой
                {singletonList("конь"), singletonList("конь")},
                {asList("конь", "конь"), singletonList("конь")},
                {asList("конь", "коня"), singletonList("конь")},
                {asList("коня", "конь"), singletonList("коня")},
                {asList("коня", "!конь"), singletonList("!конь")},
                {asList("конь", "вася"), asList("конь", "вася")},

                // слова с несколькими леммами
                {asList("ухо", "уха"), singletonList("ухо")},
                {asList("уха", "ухо"), singletonList("ухо")},
                {asList("!уха", "ухо"), singletonList("!уха")},
        });
    }

    @Test
    public void deduplicateSubKeywords_ok() {
        List<SingleKeyword> subKeywords = mapList(words, SingleKeyword::from);
        List<SingleKeyword> deduplicatedSubKeywords =
                deduplicateSubKeywords(new SingleKeywordsCache(), subKeywords, SingleKeyword.class);
        List<String> deduplicatedSubKeywordsStr = mapList(deduplicatedSubKeywords, SingleKeyword::toString);
        assertThat("полученные слова без дубликатов не соответствуют ожидаемым",
                deduplicatedSubKeywordsStr, contains(expectedUniqueWords.toArray()));
    }
}
