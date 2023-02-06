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
public class PhrasePredicatesMaxLengthPositiveTest {

    private static final int MAX_LENGTH = KEYWORD_MAX_LENGTH;
    private static final String MAX_LENGTH_KEYWORD = StringUtils.leftPad("word", KEYWORD_MAX_LENGTH, "x");

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"пустая минус-фраза", ""},
                {"1 символ", "a"},
                {"максимально длинная минус-фраза", MAX_LENGTH_KEYWORD},
                {"максимально длинная минус-фраза + спец. символы и пробелы",
                        addSpecSymbolsAndSpace(MAX_LENGTH_KEYWORD)},
                {"максимально длинная минус-фраза пробелы", "    " + MAX_LENGTH_KEYWORD + "     "},
                {"минус-фраза из пробелов", "    "},
                {"несколько символов", "word"},
                {"небольшая строка с различными символами", "!+-[a*/b@c#d]"},
        });
    }

    private static String addSpecSymbolsAndSpace(String keyword) {
        return "-!" + keyword.substring(1);
    }


    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String keyword;

    @Test
    public void testParametrized() {
        assertThat(maxLengthWithoutMinusExclamationAndSpaces(MAX_LENGTH).test(keyword), is(true));
    }
}
