package ru.yandex.direct.grid.processing.service.showcondition.keywords;

import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.grid.processing.model.showcondition.mutation.RefinedWord;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.showcondition.keywords.RefineKeywordService.REFINED_WORD_COMPARATOR;

@RunWith(JUnitParamsRunner.class)
public class RefineKeywordServiceComparatorTest {

    public static List<Object[]> parametersForSortTest() {
        return asList(new Object[][]{
                {word("aaa", 100), word("aaa", 100), 0, "Слова равны"},
                {word("aaa", 100), word("bbb", 100), -1, "При равенстве count меньше то, что раньше по алфавиту"},
                {word("aaa", 100), word("aaa", 10), -1, "Меньше слово с большим count"}
        });
    }

    public static RefinedWord word(String word, int count) {
        return new RefinedWord(word, Collections.emptyList(), count);
    }

    @Test
    @Parameters(method = "parametersForSortTest")
    @TestCaseName("[{index}] {3}")
    public void testDescription(RefinedWord left,
                                RefinedWord right,
                                int comparisonOutput,
                                @SuppressWarnings("unused") Object ignored) {
        assertThat(REFINED_WORD_COMPARATOR.compare(left, right))
                .isEqualTo(comparisonOutput);
    }

}
