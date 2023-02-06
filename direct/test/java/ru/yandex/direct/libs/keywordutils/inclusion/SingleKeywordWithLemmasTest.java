package ru.yandex.direct.libs.keywordutils.inclusion;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.inclusion.model.SingleKeywordFactory;
import ru.yandex.direct.libs.keywordutils.inclusion.model.SingleKeywordWithLemmas;
import ru.yandex.direct.libs.keywordutils.model.SingleKeyword;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class SingleKeywordWithLemmasTest {

    private final SingleKeywordFactory singleKeywordFactory = new SingleKeywordFactory();

    @Parameterized.Parameter
    public String left;
    @Parameterized.Parameter(1)
    public String right;
    @Parameterized.Parameter(2)
    public Boolean includedExpected;

    @Parameterized.Parameters(name = "\"{0}\".isIncludedIn(\"{1}\") == {2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"кофе", "кофе", true},
                {"!кофе", "кофе", true},
                {"кофе", "!кофе", false},
                {"кофе", "конь", false},
                {"!коня", "коню", true},
                //сложные слова
                {"санкт-петербург", "санкт-петербург", true},
                {"санкт петербург", "санкт-петербург", true},
                {"санкт-петербург", "санкт петербург", true},

                //для не стоп-слов + не учитывается
                {"кофе", "+кофе", true},
                {"+кофе", "кофе", true},
                {"+кофе", "+кофе", true},
                {"+кофе", "!кофе", false},
                {"!кофе", "+кофе", true},

                // многолемные слова
                {"уха", "уха", true},
                {"ухо", "уха", true},
                {"уха", "ухо", false},

                {"ухо", "!уха", false},
                {"!ухо", "уха", true},
                {"уши", "!уха", false},
                {"!уха", "уши", true},
        });
    }

    @Test
    public void isIncluded() {
        SingleKeywordWithLemmas leftKeyword =
                singleKeywordFactory.singleKeywordsFrom(SingleKeyword.from(left).getWord()).get(0);
        SingleKeywordWithLemmas rightKeyword =
                singleKeywordFactory.singleKeywordsFrom(SingleKeyword.from(right).getWord()).get(0);

        boolean includedActual = KeywordInclusionUtils.isFirstIncludedInSecond(leftKeyword, rightKeyword);
        assertThat(includedActual, is(includedExpected));
    }

}
