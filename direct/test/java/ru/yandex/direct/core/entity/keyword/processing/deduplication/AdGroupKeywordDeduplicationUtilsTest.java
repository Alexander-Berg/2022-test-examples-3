package ru.yandex.direct.core.entity.keyword.processing.deduplication;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class AdGroupKeywordDeduplicationUtilsTest {
    private static final Integer ADGROUP_INDEX = 1;
    private static final Integer ADGROUP_INDEX2 = 2;

    @Parameterized.Parameter(0)
    public List<DuplicatingContainer> duplicatingContainers;

    @Parameterized.Parameter(1)
    public List<Pair<Integer, Integer>> expectedPairIndexes;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // пустой список
                {
                        emptyList(),
                        emptyList()
                },
                // один элемент
                {
                        singletonList(container(0, ADGROUP_INDEX, "купить слон")),
                        emptyList()
                },
                {
                        singletonList(container(3, ADGROUP_INDEX, "купить слон")),
                        emptyList()
                },
                // нет дубликатов
                {
                        asList(
                                container(0, ADGROUP_INDEX, "купить слон"),
                                container(1, ADGROUP_INDEX, "купить конь")),
                        emptyList()
                },
                {
                        asList(
                                container(7, ADGROUP_INDEX, "купить слон"),
                                container(3, ADGROUP_INDEX, "купить конь")),
                        emptyList()
                },
                // утилита определения дубликатов используется с учетом минус-слов
                {
                        asList(
                                container(0, ADGROUP_INDEX, "купить -слон"),
                                container(1, ADGROUP_INDEX, "купить -конь")),
                        emptyList()
                },
                // дубликаты в одной группе
                {
                        asList(
                                container(0, ADGROUP_INDEX, "конь"),
                                container(1, ADGROUP_INDEX, "конь")),
                        asList(Pair.of(0, 1), Pair.of(1, 0))
                },
                {
                        asList(
                                container(5, ADGROUP_INDEX, "конь"),
                                container(3, ADGROUP_INDEX, "конь")),
                        asList(Pair.of(5, 3), Pair.of(3, 5))
                },
                {
                        asList(
                                container(0, ADGROUP_INDEX, "купить коня -слон"),
                                container(2, ADGROUP_INDEX, "купить коня -вакуумный"),
                                container(3, ADGROUP_INDEX, "купить коня -слон"),
                                container(7, ADGROUP_INDEX, "купить коня -слон")),
                        asList(Pair.of(0, 3), Pair.of(0, 7),
                                Pair.of(3, 0), Pair.of(3, 7),
                                Pair.of(7, 0), Pair.of(7, 3))
                },
                {
                        asList(
                                container(0, ADGROUP_INDEX, "купить коня -слон"),
                                container(2, ADGROUP_INDEX, "купить коня -конь"),
                                container(3, ADGROUP_INDEX, "купить коня -слон"),
                                container(7, ADGROUP_INDEX, "купить коня -конь"),
                                container(9, ADGROUP_INDEX, "купить коня -вакуумный")),
                        asList(Pair.of(0, 3),
                                Pair.of(3, 0),
                                Pair.of(2, 7),
                                Pair.of(7, 2))
                },
                // дубликаты в разных группах не учитываются
                {
                        asList(
                                container(0, ADGROUP_INDEX, "конь"),
                                container(1, ADGROUP_INDEX2, "конь")),
                        emptyList()
                },
                // комбо
                {
                        asList(
                                container(3, ADGROUP_INDEX, "купить коня -слон"),
                                container(7, ADGROUP_INDEX2, "купить слона"),
                                container(11, ADGROUP_INDEX, "купить коня -слон"),
                                container(17, ADGROUP_INDEX2, "купить слона"),
                                container(19, ADGROUP_INDEX2, "конь")),
                        asList(Pair.of(3, 11),
                                Pair.of(11, 3),
                                Pair.of(7, 17),
                                Pair.of(17, 7))
                }
        });
    }

    @Test
    public void computeDuplicates_WorksFine() {
        Multimap<Integer, Integer> actualDuplicateIndexes =
                AdGroupKeywordDeduplicationUtils.computeDuplicates(duplicatingContainers);
        assertThat("полученные индексы дубликатов должны соответствать ожидаемым",
                actualDuplicateIndexes.asMap(), beanDiffer(getExpectedIndexes(expectedPairIndexes).asMap()));
    }

    private Multimap<Integer, Integer> getExpectedIndexes(List<Pair<Integer, Integer>> pairs) {
        Multimap<Integer, Integer> multimap = HashMultimap.create();
        pairs.forEach(p -> multimap.put(p.getLeft(), p.getRight()));
        return multimap;
    }

    private static DuplicatingContainer container(Integer index, Integer adGroupIndex, String keyword) {
        return new DuplicatingContainer(index, adGroupIndex, KeywordParser.parseWithMinuses(keyword), false);
    }
}
