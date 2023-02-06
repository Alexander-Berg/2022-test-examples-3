package ru.yandex.direct.core.entity.keyword.processing.deduplication;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
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
public class AdGroupKeywordDeduplicationUtilsExistingTest {

    private static final Integer ADGROUP_INDEX = 1;
    private static final Integer ADGROUP_INDEX2 = 2;

    @Parameterized.Parameter(0)
    public List<DuplicatingContainer> newContainers;

    @Parameterized.Parameter(1)
    public List<DuplicatingContainer> existingContainers;

    @Parameterized.Parameter(2)
    public List<Pair<Integer, Integer>> expectedPairIndexes;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // пустые мапы
                {
                        emptyList(),
                        emptyList(),
                        emptyList()
                },
                {
                        singletonList(container(4, ADGROUP_INDEX, "купить слон")),
                        emptyList(),
                        emptyList()
                },
                {
                        emptyList(),
                        singletonList(container(4, ADGROUP_INDEX, "купить слон")),
                        emptyList()
                },
                // нет дубликатов
                {
                        singletonList(container(0, ADGROUP_INDEX, "купить слон")),
                        singletonList(container(0, ADGROUP_INDEX, "купить конь")),
                        emptyList()
                },
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить слон")),
                        singletonList(container(7, ADGROUP_INDEX, "купить конь")),
                        emptyList()
                },
                // утилита определения дубликатов используется с учетом минус-слов
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить -слон")),
                        singletonList(container(7, ADGROUP_INDEX, "купить -конь")),
                        emptyList()
                },
                // дубликаты в одной группе
                {
                        singletonList(container(0, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(container(0, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(Pair.of(0, 0))
                },
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(container(12, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(Pair.of(1, 12))
                },
                {
                        asList(
                                container(1, ADGROUP_INDEX, "купить коня -слон"),
                                container(7, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(container(2, ADGROUP_INDEX, "купить коня -слон")),
                        asList(Pair.of(1, 2), Pair.of(7, 2))
                },
                {
                        asList(
                                container(3, ADGROUP_INDEX, "купить коня -конь"),
                                container(4, ADGROUP_INDEX, "купить коня -вакуумный"),
                                container(7, ADGROUP_INDEX, "купить коня -слон")),
                        asList(
                                container(12, ADGROUP_INDEX, "купить коня -слон"),
                                container(14, ADGROUP_INDEX, "купить коня -конь")),
                        asList(Pair.of(3, 14), Pair.of(7, 12))
                },
                // присутствуют не-дубликаты среди существующих
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить коня -слон")),
                        asList(
                                container(2, ADGROUP_INDEX, "купить коня -слон"),
                                container(7, ADGROUP_INDEX, "купить коня -конь")),
                        singletonList(Pair.of(1, 2))
                },
                // присутствуют не-дубликаты среди новых
                {
                        asList(
                                container(1, ADGROUP_INDEX, "купить коня -слон"),
                                container(7, ADGROUP_INDEX, "купить коня -конь")),
                        singletonList(
                                container(2, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(Pair.of(1, 2))
                },
                // игнорирование нескольких дубликатов
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить коня -слон")),
                        asList(
                                container(3, ADGROUP_INDEX, "купить коня -слон"),
                                container(12, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(Pair.of(1, 3))
                },
                // дубликаты в разных группах не учитываются
                {
                        singletonList(container(0, ADGROUP_INDEX, "купить слон")),
                        singletonList(container(0, ADGROUP_INDEX2, "купить слон")),
                        emptyList()
                },
                // дубликаты с префиксом автотаргетинга
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить коня -слон", true)),
                        singletonList(container(2, ADGROUP_INDEX, "купить коня -слон")),
                        emptyList()
                },
                // дубликаты с префиксом автотаргетинга
                {
                        singletonList(container(1, ADGROUP_INDEX, "купить коня -слон", true)),
                        asList(
                                container(3, ADGROUP_INDEX, "купить коня -слон", true),
                                container(12, ADGROUP_INDEX, "купить коня -слон")),
                        singletonList(Pair.of(1, 3))
                },
                // комбо
                {
                        asList(
                                container(3, ADGROUP_INDEX, "купить коня -конь"),
                                container(4, ADGROUP_INDEX2, "купить коня -вакуумный"),
                                container(7, ADGROUP_INDEX, "купить коня -слон")),
                        asList(
                                container(12, ADGROUP_INDEX2, "купить коня -вакуумный"),
                                container(3, ADGROUP_INDEX, "купить коня -конь"),
                                container(17, ADGROUP_INDEX, "купить коня -рыба")),
                        asList(Pair.of(3, 3), Pair.of(4, 12))
                }
        });
    }

    @Test
    public void computeDuplicates_WorksFine() {
        Map<Integer, Integer> actualDuplicateIndexes =
                AdGroupKeywordDeduplicationUtils.computeDuplicates(newContainers, existingContainers);
        assertThat("полученные индексы дубликатов должны соответствать ожидаемым",
                actualDuplicateIndexes, beanDiffer(getExpectedIndexes(expectedPairIndexes)));
    }

    private Map<Integer, Integer> getExpectedIndexes(List<Pair<Integer, Integer>> pair) {
        return StreamEx.of(pair)
                .mapToEntry(Pair::getLeft, Pair::getRight)
                .toMap();
    }

    private static DuplicatingContainer container(Integer index, Integer adGroupIndex, String keyword) {
        return container(index, adGroupIndex, keyword, false);
    }

    private static DuplicatingContainer container(Integer index, Integer adGroupIndex, String keyword,
                                                  boolean isAutotargeting) {
        return new DuplicatingContainer(index, adGroupIndex, KeywordParser.parseWithMinuses(keyword), isAutotargeting);
    }
}
