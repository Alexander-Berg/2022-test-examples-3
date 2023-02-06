package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.combinationSpecialSymbols;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesSpecialSymbolCombinationNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"1..st"},
                {"1..2"},
                {"-+invalid"},
                {"[.]"},
                {"!."},
                {"+."},
                {".+"},
                {"-."},
                {".-"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(combinationSpecialSymbols().test(keyword), is(false));
    }
}
