package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORD_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.maxWordLength;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesMaxWordLengthPositiveTest {

    private static final String MAX_LENGTH_WORD = StringUtils.leftPad("s", WORD_MAX_LENGTH, "o");

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"один символ", "a"},
                {"несколько символов", "аб"},
                {"максимально длинное слово", MAX_LENGTH_WORD},

                {"максимально длинное слово в кавычках", "\"" + MAX_LENGTH_WORD + "\""},
                {"максимально длинное слово в квадратных скобках", "[" + MAX_LENGTH_WORD + "]"},
                {"максимально длинное слово с \"!\"", "!" + MAX_LENGTH_WORD},
                {"максимально длинное слово с \"+\"", "+" + MAX_LENGTH_WORD},
                {"максимально длинное слово с \"-\"", "+" + MAX_LENGTH_WORD},

                {"несколько длинных слов с различными операторами",
                        "[" + MAX_LENGTH_WORD + "] + !" + MAX_LENGTH_WORD + " -!" + MAX_LENGTH_WORD},
        });
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(maxWordLength(WORD_MAX_LENGTH).test(keyword), is(true));
    }
}
