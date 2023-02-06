package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.maxWordsInKeywords;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesMaxWordsNegativeTest {

    private static final int MAX_WORDS = 7;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"один два три четыре пять шесть семь восемь"},
                {"1 2 3 4 5 6 7 8"},
                {"1 [2 3] 4 [5 6] 7 8"},
                {"\"1 [2 3] 4 [5 6] 7 8\""},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(maxWordsInKeywords(MAX_WORDS).test(keyword), is(false));
    }
}
