package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.model.Keyword;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.processing.MinusKeywordsDeduplicator.remove;
import static ru.yandex.direct.core.entity.keyword.processing.MinusKeywordsDeduplicator.removeStr;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class MinusKeywordRemoveTest {

    @Parameterized.Parameter
    public List<String> minusKeywordsStr;

    @Parameterized.Parameter(1)
    public List<String> removeMinusKeywordsStr;

    @Parameterized.Parameter(2)
    public List<String> expectedUniqueMinusKeywordsStr;

    @Parameterized.Parameters(name = "входные минус-фразы: {0}, минус-фразы которые надо удалить: {1} ожидаемые минус-фразы: {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        List.of("конь", "лошадь"),
                        singletonList("конь"),
                        singletonList("лошадь")
                },
                {
                        asList("Конь", "лошадь"),
                        singletonList("конь"),
                        singletonList("лошадь")
                },
                {
                        asList("конь.", "лошадь"),
                        singletonList("конь"),
                        singletonList("лошадь")
                },
                {
                        asList("Конь.", "лошадь"),
                        singletonList("конь"),
                        singletonList("лошадь")
                },
                {
                        singletonList("коня"),
                        singletonList("вася"),
                        singletonList("коня")
                },
                {
                        asList("идти", "велосипед", "конь", "трамвай", "лошадь"),
                        asList("идти", "коня", "велосипед", "трамвай"),
                        singletonList("лошадь")
                },

                // только "уха" может включать "ухо", но не наоборот
                {
                        singletonList("уха"),
                        singletonList("ухо"),
                        singletonList("уха")
                },
                {
                        asList("ухо", "лошадь"),
                        singletonList("уха"),
                        singletonList("лошадь")
                }
        });
    }

    @Test
    public void remove_WorksFine() {
        List<Keyword> minusKeywords = mapList(minusKeywordsStr, KeywordParser::parse);
        List<Keyword> removeMinusKeywords = mapList(removeMinusKeywordsStr, KeywordParser::parse);
        List<Keyword> actualUniqueMinusKeywords = remove(new SingleKeywordsCache(), minusKeywords, removeMinusKeywords);

        List<String> actualUniqueMinusKeywordsStr = mapList(actualUniqueMinusKeywords, Keyword::toString);
        assertThat("полученные минус-фразы без дубликатов не соответствуют ожидаемым",
                actualUniqueMinusKeywordsStr, contains(expectedUniqueMinusKeywordsStr.toArray()));
    }

    @Test
    public void removeDuplicatesStr_WorksFine() {
        List<String> actualUniqueMinusKeywordsStr =
                removeStr(new SingleKeywordsCache(), minusKeywordsStr, removeMinusKeywordsStr, t -> KeywordParser.parse(t));

        assertThat("полученные минус-фразы без дубликатов не соответствуют ожидаемым",
                actualUniqueMinusKeywordsStr, contains(expectedUniqueMinusKeywordsStr.toArray()));
    }
}
