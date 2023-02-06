package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.inclusion.model.SingleKeywordWithLemmas;
import ru.yandex.direct.libs.keywordutils.model.SingleKeyword;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.processing.KeywordCombinator.combineAllLemmas;

@RunWith(Parameterized.class)
public class KeywordCombinatorTest {

    @Parameterized.Parameter
    public List<String> words;

    @Parameterized.Parameter(1)
    public List<Integer> combinedLemmaHashes;

    @Parameterized.Parameters(name = "входные слова: {0}, ожидаемые комбинации лемм: {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        List.of("уши"),
                        List.of(Objects.hash("ухо"))
                },
                {
                        List.of("уху"),
                        List.of(Objects.hash("уха"), Objects.hash("ухо"))
                },
                {
                        List.of("купить", "уху"),
                        List.of(Objects.hash("уха", "купить"), Objects.hash("ухо", "купить"))
                }
        });
    }

    @Test
    public void test() {
        var singleKeywordsCache = new SingleKeywordsCache();
        List<SingleKeywordWithLemmas> singleKeywordWithLemmas = StreamEx.of(words)
                .map(SingleKeyword::from)
                .flatCollection(kw -> singleKeywordsCache.singleKeywordsFrom(kw.getWord()))
                .toList();
        Set<Integer> actualCombinedLemmaHashes = combineAllLemmas(singleKeywordWithLemmas);
        assertThat("полученные хеши комбинаций лемм не соответствуют ожидаемым",
                actualCombinedLemmaHashes, containsInAnyOrder(combinedLemmaHashes.toArray()));
    }
}
