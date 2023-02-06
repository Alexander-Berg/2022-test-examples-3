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
public class MinusPhrasePredicatesMaxLengthPositiveTest {

    private static final int MAX_LENGTH = GROUP_MINUS_KEYWORDS_MAX_LENGTH;
    private static final String MAX_LENGTH_KEYWORD = StringUtils.leftPad("word", GROUP_MINUS_KEYWORDS_MAX_LENGTH, "x");

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        int middle = MAX_LENGTH / 2;
        String keyword1 = MAX_LENGTH_KEYWORD.substring(0, middle);
        String keyword2 = MAX_LENGTH_KEYWORD.substring(middle, MAX_LENGTH);
        return asList(new Object[][]{
                {
                        "пустая минус-фраза",
                        singletonList("")
                },
                {
                        "1 символ",
                        singletonList("a")
                },
                {
                        "максимально длинная минус-фраза",
                        singletonList(MAX_LENGTH_KEYWORD)
                },
                {
                        "максимально длинная минус-фраза + спец. символы и пробелы",
                        singletonList(addSpecSymbolsAndSpace(MAX_LENGTH_KEYWORD))
                },
                {
                        "максимально длинная минус-фраза пробелы",
                        singletonList("    " + MAX_LENGTH_KEYWORD + "     ")
                },
                {
                        "минус-фраза из пробелов",
                        singletonList("    ")
                },
                {
                        "несколько символов",
                        singletonList("word")
                },
                {
                        "небольшая строка с различными символами",
                        singletonList("!+-[a*/b@c#d]")
                },
                {
                        "две строки в сумме максимальной длины",
                        asList(keyword1, keyword2)
                },
                {
                        "две строки c пробелами и спец. символами в сумме максимальной длины",
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
        assertThat(maxLengthKeywordsWithoutSpecSymbolsAndSpaces(MAX_LENGTH).test(minusKeywords), is(true));
    }
}
