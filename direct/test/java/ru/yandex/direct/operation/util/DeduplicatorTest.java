package ru.yandex.direct.operation.util;

import java.util.Collection;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@RunWith(Parameterized.class)
public class DeduplicatorTest {
    private final List<Integer> inputList;
    private final List<Integer> deduplicated;
    private final List<List<Integer>> deduplicationInfo;

    public DeduplicatorTest(
            List<Integer> inputList,
            List<Integer> deduplicated,
            List<List<Integer>> deduplicationInfo
    ) {
        this.inputList = inputList;
        this.deduplicated = deduplicated;
        this.deduplicationInfo = deduplicationInfo;
    }

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Collection<Object[]> data() {
        //noinspection ArraysAsListWithZeroOrOneArgument
        return asList(new Object[][]{
                {emptyList(), emptyList(), emptyList()},
                {asList(1, 2, 3), asList(1, 2, 3),
                        asList(asList(0), asList(1), asList(2))},
                {asList(1, 1, 1), asList(1),
                        asList(asList(0, 1, 2))},
                {asList(1, 2, 1), asList(1, 2),
                        asList(asList(0, 2), asList(1))},
                {asList(1, 2, 1, 2), asList(1, 2),
                        asList(asList(0, 2), asList(1, 3))},
        });
    }

    @Test
    public void deduplicate() {
        Deduplicator.Result<Integer> result = Deduplicator.deduplicate(inputList);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(result.getDeduplicated()).isEqualTo(deduplicated);
            softAssertions.assertThat(result.getDeduplicationInfo()).isEqualTo(deduplicationInfo);
        });
    }
}
