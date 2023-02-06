package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.GROUP_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.maxLengthKeywordsWithoutSpecSymbolsAndSpaces;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesMaxLengthNegativeTest {

    private static final int MAX_LENGTH = GROUP_MINUS_KEYWORDS_MAX_LENGTH;
    private static final String MAX_LENGTH_KEYWORD = StringUtils.leftPad("word", GROUP_MINUS_KEYWORDS_MAX_LENGTH, "x");
    private static final String EXTRA_LENGTH_KEYWORD = "1" + MAX_LENGTH_KEYWORD;

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        int middle = MAX_LENGTH / 2;
        String keyword1 = MAX_LENGTH_KEYWORD.substring(0, middle);
        String keyword2 = MAX_LENGTH_KEYWORD.substring(middle, MAX_LENGTH) + 1;
        return asList(new Object[][]{
                {
                        "строка, превышающая макс. длину на 1 символ",
                        singletonList(EXTRA_LENGTH_KEYWORD)
                },
                {
                        "строка с пробелами, превышающая макс. длину на 1 символ",
                        singletonList("    " + EXTRA_LENGTH_KEYWORD + "     ")
                },
                {
                        "строка с пробелами и спец. символами, превышающая макс. длину на 1 символ",
                        singletonList(addSpecSymbolsAndSpace(EXTRA_LENGTH_KEYWORD))
                },
                {
                        "две строки, превышающие макс. длину на 1 символ",
                        asList(keyword1, keyword2)
                },
                {
                        "две строки с пробелами и спец. символами, превышающие макс. длину на 1 символ",
                        asList(addSpecSymbolsAndSpace(keyword1), addSpecSymbolsAndSpace(keyword2))
                },
        });
    }

    private static String addSpecSymbolsAndSpace(String keyword) {
        return "-![\" +" + keyword + " \"]";
    }


    @SuppressWarnings("unused")
    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public List<String> minusKeywords;

    @Test
    public void testParametrized() {
        assertThat(maxLengthKeywordsWithoutSpecSymbolsAndSpaces(MAX_LENGTH).test(minusKeywords), is(false));
    }
}
