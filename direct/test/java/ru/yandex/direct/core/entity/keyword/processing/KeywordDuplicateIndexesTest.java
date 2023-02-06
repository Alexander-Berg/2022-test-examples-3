package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.model.Keyword;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.processing.MinusKeywordsDeduplicator.getDuplicateIndexes;
import static ru.yandex.direct.utils.CollectionUtils.orderedSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class KeywordDuplicateIndexesTest {

    @Parameterized.Parameter
    public List<String> keywordsStr;

    @Parameterized.Parameter(1)
    public Map<Integer, Set<Integer>> expectedDuplicateIndexes;

    @Parameterized.Parameters(name = "входные фразы: {0}, ожидаемые индексы дубликатов: {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // слова с одной леммой
                {
                        List.of("купить конь"),
                        Map.of()
                },
                {
                        List.of("купить конь", "друг вася"),
                        Map.of()
                },
                {
                        List.of("купить конь", "купить конь"),
                        Map.of(0, orderedSet(1))
                },
                {
                        List.of("купить конь", "купить конь", "купить конь"),
                        Map.of(0, orderedSet(1, 2))
                },
                {
                        List.of("купить конь", "купить коня"),
                        Map.of(0, orderedSet(1))
                },
                {
                        List.of("купить коня", "купить конь"),
                        Map.of(0, orderedSet(1))
                },
                {
                        List.of("купить !коня", "купить конь"),
                        Map.of(1, orderedSet(0))
                },

                // слова с несколькими леммами
                {
                        List.of("купить ухо", "купить уха"),
                        Map.of(1, orderedSet(0))
                },
                {
                        List.of("купить ухо", "купить уха", "купить ухо"),
                        Map.of(1, orderedSet(0, 2))
                },
                {
                        List.of("купить уха", "купить ухо"),
                        Map.of(0, orderedSet(1))
                },
                {
                        List.of("купить !уха", "купить ухо"),
                        Map.of(1, orderedSet(0))
                },
                {
                        List.of("купить !ухи", "купить ухо"),
                        Map.of()
                },
        });
    }

    @Test
    public void deduplicateSubKeywords_ok() {
        List<Keyword> keywords = mapList(keywordsStr, KeywordParser::parse);
        Int2ObjectOpenHashMap<IntSet> duplicateIndexes = getDuplicateIndexes(new SingleKeywordsCache(), keywords);
        assertThat("полученные индексы дубликатов должны соответствать ожидаемым",
                duplicateIndexes, beanDiffer(expectedDuplicateIndexes));
    }
}
