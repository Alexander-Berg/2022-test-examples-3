package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.KEYWORD_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.maxLengthWithoutMinusExclamationAndSpaces;

@RunWith(Parameterized.class)
public class PhrasePredicatesMaxLengthNegativeTest {

    private static final int MAX_LENGTH = KEYWORD_MAX_LENGTH;
    private static final String MAX_LENGTH_KEYWORD = StringUtils.leftPad("word", KEYWORD_MAX_LENGTH, "x");

    private static final String EXTRA_LENGTH_KEYWORD = "1" + MAX_LENGTH_KEYWORD;

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"строка, превышающая макс. длину на 1 символ", EXTRA_LENGTH_KEYWORD},
                {"строка с пробелами, превышающая макс. длину на 1 символ",
                        "    " + EXTRA_LENGTH_KEYWORD + "     "},
                {"строка с пробелами и спец. символами, превышающая макс. длину на 1 символ",
                        addSpecSymbolsAndSpace(EXTRA_LENGTH_KEYWORD)}
        });
    }

    private static String addSpecSymbolsAndSpace(String keyword) {
        return "-![\" +" + keyword + " \"]";
    }


    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String keyword;

    @Test
    public void testParametrized() {
        assertThat(maxLengthWithoutMinusExclamationAndSpaces(MAX_LENGTH).test(keyword), is(false));
    }
}
