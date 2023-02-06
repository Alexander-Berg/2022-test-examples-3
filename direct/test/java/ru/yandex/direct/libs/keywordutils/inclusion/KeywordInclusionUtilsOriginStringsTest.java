package ru.yandex.direct.libs.keywordutils.inclusion;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.StopWordMatcher;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Проверки, что метод getIncludedMinusKeywords возвращает исходные строки,
 * а не строки после парсинга
 */
@RunWith(Parameterized.class)
public class KeywordInclusionUtilsOriginStringsTest {

    private final StopWordMatcher stopWordMatcher = text -> text.equals("в");
    private final KeywordWithLemmasFactory keywordWithLemmasFactory = new KeywordWithLemmasFactory();

    @Parameterized.Parameter(0)
    public String plusKeyword;
    @Parameterized.Parameter(1)
    public String minusKeyword;

    @Parameterized.Parameters(name = "returns origin string for input minus keyword {1}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                // SingleKeyword cases:
                {"кофе", "кофе"},
                {"!кофе", "!кофе"},
                {"кофе", "+кофе"},
                {"кофе", "[кофе]"},
                {"\"кофе\"", "\"кофе\""},

                // case sensitive cases
                {"конь", "Конь"},
                {"!Серый конь", "Конь серый"},

                // char replacement cases
                {"елка", "ёлка"},
                {"!Ешкин конь", "Конь ёшкин"},

                // split words cases
                {"санкт-петербург", "санкт-петербурге"},
                {"санкт-петербург", "санкт петербурге"},
                {"санкт петербург", "санкт-петербурге"},
                {"по-русски", "по-русски"},
                {"по-русски", "по русски"},
                {"по русски", "по-русски"},

                // complex cases (sorting etc)
                {"\"[!конь !серый] белый [ёжик в санкт-петербург]\"",
                        "\"[!конь !серый] в Санкт-петербурге Белом ёжик\""},
                {"\"[!конь !серый] белый [серый ёжик в тумане белом]\"",
                        "\"[!конь !серый] [в тумане] белый ёжик\""},

        });
    }

    @Test
    public void getIncludedMinusKeywords_ReturnsOriginStrings() {
        Collection<String> result = KeywordInclusionUtils.getIncludedMinusKeywords(keywordWithLemmasFactory,
                stopWordMatcher,
                singletonList(plusKeyword), singletonList(minusKeyword));
        Assert.assertThat(result, beanDiffer(Collections.singleton(minusKeyword)));
    }
}
