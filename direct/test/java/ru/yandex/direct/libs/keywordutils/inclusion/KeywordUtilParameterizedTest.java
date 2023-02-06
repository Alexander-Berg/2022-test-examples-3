package ru.yandex.direct.libs.keywordutils.inclusion;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.StopWordMatcher;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordForInclusion;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class KeywordUtilParameterizedTest {

    private final StopWordMatcher stopWordMatcher = text -> text.equals("которого");
    private final KeywordWithLemmasFactory keywordWithLemmasFactory = new KeywordWithLemmasFactory();

    @Parameterized.Parameter
    public String plusKeyword;
    @Parameterized.Parameter(1)
    public String minusKeyword;
    @Parameterized.Parameter(2)
    public Boolean emptyResult;

    @Parameterized.Parameters(name = "(({0}) - ({1})).isEmpty() == {2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                // SingleKeyword cases:
                {"кофе", "кофе", true},
                {"!кофе", "кофе", true},
                {"кофе", "!кофе", false},
                {"кофе", "конь", false},
                {"!коня", "коню", true},
                // complex SingleKeyword cases:
                {"санкт-петербург", "санкт петербург", true},
                {"санкт петербург", "санкт-петербург", true},
                // Multiple lemmas cases:
                {"киев", "кий", false},
                {"кий", "киев", true},
                {"\"киев\"", "\"кий\"", false},
                {"\"кий\"", "\"киев\"", true},
                {"уха", "ухой", false},
                {"ухо", "уха", true},
                {"\"уха\"", "\"ухой\"", false},
                {"\"ухо\"", "\"уха\"", true},

                // case sentitive cases
                {"конь", "Конь", true},
                {"!Серый конь", "Конь серый", true},

                // char replacement cases
                {"ёлка", "елка", true},
                {"елка", "ёлка", true},

                {"конь серый", "конь", true},
                {"конь серый", "конь серый", true},
                {"конь серый", "конь !серый", false},
                {"конь серый", "конь серый белый", false},
                {"конь !серый", "конь", true},
                {"конь !серый", "конь серый", true},
                {"конь !серый", "конь !серый", true},
                {"конь !серый", "!конь !серый", false},
                {"конь !серый", "конь серый белый", false},
                {"!конь !серый", "конь серый", true},
                {"!конь !серый", "конь серый", true},
                {"!конь !серый", "конь !серый", true},
                {"!конь !серый", "!конь !серый", true},
                {"!конь !серый", "конь серый белый", false},
                {"\"конь серый\"", "конь серый", true},
                {"\"конь серый\"", "конь !серый", false},
                {"\"конь серый\"", "конь серый белый", false},
                {"\"конь серый\"", "\"конь серый\"", true},
                {"\"конь серый\"", "\"серый конь\"", true},
                {"\"конь серый\"", "\"конь серый белый\"", false},
                {"\"конь серый\"", "\"конь\"", false},
                {"\"конь\"", "\"конь\"", true},
                {"\"конь\"", "конь", true},
                {"\"конь\"", "!конь", false},
                {"\"!конь !серый\"", "\"конь\"", false},
                {"\"!конь !серый\"", "конь", true},
                {"\"!конь !серый\"", "конь серый", true},
                {"\"!конь !серый\"", "конь серый белый", false},
                {"[конь серый]", "конь", true},
                {"[конь серый]", "конь серый", true},
                {"[конь серый]", "\"конь\"", false},
                {"[конь серый]", "серый конь", true},
                {"[конь серый]", "[серый конь]", false},
                {"[!конь !серый]", "конь", true},
                {"[!конь !серый]", "[конь серый]", true},
                {"[!конь !серый]", "[конь серый белый]", false},
                {"\"[!конь !серый]\"", "конь", true},
                {"\"[!конь !серый]\"", "\"конь\"", false},
                {"\"[!конь !серый]\"", "\"[конь серый]\"", true},
                {"\"[!конь !серый]\"", "\"[серый конь]\"", false},
                //
                {"\"[!конь !серый] белый\"", "\"конь\"", false},
                {"\"[!конь !серый] белый\"", "конь", true},
                {"\"[!конь !серый] белый\"", "\"белый !серый !конь\"", true},
                {"\"[!конь !серый] белый\"", "\"белый [!серый !конь]\"", false},
                {"\"[!конь !серый] белый\"", "\"белый [!конь !серый]\"", true},
                {"[!конь !серый] белый [ёжик в тумане]", "белый [!конь !серый]", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "[!конь !серый]", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "серый", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "!серый", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "!конь", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "серый конь", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "!белое", false},
                {"[!конь !серый] белый [серый ёжик в тумане]", "[серый]", true},
                {"[!конь !серый] белый [серый ёжик в тумане]", "[серый] !серое", false},
                {"[!конь !серый] белый [серый ёжик в тумане]", "[в тумане]", true},
                {"\"[!конь !серый] белый [ёжик в тумане]\"", "белый [!конь !серый]", true},
                {"\"[!конь !серый] белый [ёжик в тумане]\"", "\"белый [!конь !серый]\"", false},
                {"\"[!конь !серый] белый [ёжик в тумане]\"", "\"[!конь !серый] в тумане белом ёжик\"", true},
                {"\"[!конь !серый] белый [серый ёжик в тумане белом]\"",
                        "\"[!конь !серый] [в тумане] белый ёжик\"", true},

                // Sergey's cases:
                {"конь", "серый", false},
                {"+конь", "серый", false},
                {"!конь", "серый", false},
                {"[конь]", "серый", false},
                {"[+конь]", "серый", false},
                {"[!конь]", "серый", false},
                {"\"конь\"", "серый", false},
                {"\"+конь\"", "серый", false},
                {"\"!конь\"", "серый", false},
                {"\"[конь]\"", "серый", false},
                {"\"[+конь]\"", "серый", false},
                {"\"[!конь]\"", "серый", false},

                {"конь", "конь", true},
                {"конь", "кони", true},
                {"конь", "+конь", true},
                {"конь", "+кони", true},
                {"конь", "!конь", false},
                {"конь", "[конь]", true},
                {"конь", "[кони]", true},
                {"конь", "[+конь]", true},
                {"конь", "[+кони]", true},
                {"конь", "[!конь]", false},
                {"конь", "\"конь\"", false},
                {"конь", "\"кони\"", false},
                {"конь", "\"!конь\"", false},
                {"конь", "\"+конь\"", false},
                {"конь", "\"+кони\"", false},
                {"конь", "\"[+конь]\"", false},
                {"конь", "\"[+кони]\"", false},
                {"конь", "\"[конь]\"", false},
                {"конь", "\"[кони]\"", false},
                {"конь", "\"[!конь]\"", false},

                {"!конь", "конь", true},
                {"!конь", "кони", true},
                {"!конь", "+конь", true},
                {"!конь", "+кони", true},
                {"!конь", "!конь", true},
                {"!конь", "!кони", false},
                {"!конь", "[конь]", true},
                {"!конь", "[кони]", true},
                {"!конь", "[+конь]", true},
                {"!конь", "[+кони]", true},
                {"!конь", "[!конь]", true},
                {"!конь", "[!кони]", false},
                {"!конь", "\"конь\"", false},
                {"!конь", "\"кони\"", false},
                {"!конь", "\"+конь\"", false},
                {"!конь", "\"!конь\"", false},
                {"!конь", "\"[конь]\"", false},
                {"!конь", "\"[!конь]\"", false},
                {"!конь", "\"[+конь]\"", false},

                {"[конь]", "конь", true},
                {"[конь]", "кони", true},
                {"[конь]", "+конь", true},
                {"[конь]", "+кони", true},
                {"[конь]", "!конь", false},
                {"[конь]", "!кони", false},
                {"[конь]", "[конь]", true},
                {"[конь]", "[кони]", true},
                {"[конь]", "[+конь]", true},
                {"[конь]", "[+кони]", true},
                {"[конь]", "[!конь]", false},
                {"[конь]", "[!кони]", false},
                {"[конь]", "\"конь\"", false},
                {"[конь]", "\"кони\"", false},
                {"[конь]", "\"+конь\"", false},
                {"[конь]", "\"!конь\"", false},
                {"[конь]", "\"[конь]\"", false},
                {"[конь]", "\"[!конь]\"", false},
                {"[конь]", "\"[+конь]\"", false},

                {"+конь", "конь", true},
                {"+конь", "кони", true},
                {"+конь", "+кони", true},
                {"+конь", "!конь", false},
                {"+конь", "[кони]", true},
                {"+конь", "[+конь]", true},
                {"+конь", "[+кони]", true},
                {"+конь", "[!конь]", false},
                {"+конь", "\"конь\"", false},
                {"+конь", "\"!конь\"", false},

                {"\"конь\"", "конь", true},
                {"\"конь\"", "кони", true},
                {"\"конь\"", "+кони", true},
                {"\"конь\"", "!конь", false},
                {"\"конь\"", "!кони", false},
                {"\"конь\"", "[конь]", true},
                {"\"конь\"", "[кони]", true},
                {"\"конь\"", "[+кони]", true},
                {"\"конь\"", "[!конь]", false},
                {"\"конь\"", "[!кони]", false},
                {"\"конь\"", "\"конь\"", true},
                {"\"конь\"", "\"кони\"", true},
                {"\"конь\"", "\"+конь\"", true},
                {"\"конь\"", "\"+кони\"", true},
                {"\"конь\"", "\"!конь\"", false},
                {"\"конь\"", "\"!кони\"", false},
                {"\"конь\"", "\"[конь]\"", true},
                {"\"конь\"", "\"[!конь]\"", false},
                {"\"конь\"", "\"[+конь]\"", true},

                //стоп-слова
                {"которого", "которого", false},
                {"которого", "+которого", false},
                {"+которого", "которого", true},
                {"которого", "!которого", false},
                {"!которого", "которого", true},
                {"+которого", "+которого", true},
                {"+которого", "!которого", false},
                {"!которого", "+которого", true},

                //стоп-слова в квадратных скобках
                {"[+которого]", "которого", true},
                {"[которого]", "которого", true},
                {"[которого]", "!которого", false},
                {"[которого]", "+которого", true},
                {"которого", "[которого]", false},
                {"которого", "[!которого]", false},
                {"которого", "[+которого]", false},
                {"[!которого]", "которого", true},
                {"[которого]", "[которого]", true},
                {"[которого]", "[!которого]", false},
                {"[!которого]", "[которого]", true},
                {"[которого]", "[+которого]", true},
                {"[+которого]", "[которого]", true},

                //стоп-слова в кавычках
                {"\"которого\"", "\"+которого\"", true},
                {"\"которого\"", "\"!которого\"", false},
                {"\"+которого\"", "\"+которого\"", true},
                {"\"+которого\"", "\"+которого\"", true},
                {"\"которого\"", "\"[+которого]\"", true},
        });
    }

    @Test
    public void test() {
        KeywordForInclusion plus = keywordWithLemmasFactory.keywordFrom(KeywordParser.parse(plusKeyword));
        KeywordForInclusion minus = keywordWithLemmasFactory.keywordFrom(KeywordParser.parse(minusKeyword));

        boolean actual = KeywordInclusionUtils.isEmptyResultForKeywords(stopWordMatcher, plus, minus);

        assertThat(actual, is(emptyResult));
    }

    @Test
    public void bulkCheck() {
        KeywordForInclusion plus = keywordWithLemmasFactory.keywordFrom(KeywordParser.parse(plusKeyword));
        KeywordForInclusion minus = keywordWithLemmasFactory.keywordFrom(KeywordParser.parse(minusKeyword));

        Map<KeywordForInclusion, List<KeywordForInclusion>> result =
                KeywordInclusionUtils
                        .getPlusKeywordsGivenEmptyResult(stopWordMatcher, singletonList(plus), singletonList(minus));
        boolean actual = result.containsKey(plus);

        assertThat(actual, is(emptyResult));
    }
}
