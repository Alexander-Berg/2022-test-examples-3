package ru.yandex.direct.libs.keywordutils.inclusion;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.StopWordMatcher;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.libs.keywordutils.model.Keyword;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static org.hamcrest.Matchers.containsInAnyOrder;

@RunWith(Parameterized.class)
public class KeywordInclusionUtilsKeyword {

    private final StopWordMatcher stopWordMatcher = text -> text.equals("в");
    private final KeywordWithLemmasFactory keywordWithLemmasFactory = new KeywordWithLemmasFactory();

    @Parameterized.Parameter(0)
    public String phraseText;
    @Parameterized.Parameter(1)
    public List<String> expectedMinusWords;

    @Parameterized.Parameters(name = "return set of intersected minus words")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"навигационная система gps -спутниковый -система", Collections.singletonList("система")},
                {"навигационная система gps -спутниковый -навигационный", Collections.singletonList("навигационный")},
                {"книга -книга -книги", Arrays.asList("книги", "книга")},
                {"бронировать фары -бронированная", Collections.singletonList("бронированная")},
                {"бронировать фары -!бронированная", Collections.emptyList()},
                {"очки !в оправе -!в", Collections.singletonList("!в")},
                {"бронировать стоп-слова -стоп слова", Collections.singletonList("стоп слова")},
        });
    }

    @Test
    public void getIntersectedPlusWords_ReturnIntersectedMinusKeywords() {
        List<Keyword> result = KeywordInclusionUtils.getIntersectedPlusWords(keywordWithLemmasFactory,
                stopWordMatcher,
                KeywordParser.parseWithMinuses(phraseText));
        List<String> intersectedWords = result.stream().map(Keyword::toString).collect(Collectors.toList());
        Assert.assertThat(intersectedWords, containsInAnyOrder(expectedMinusWords.toArray()));
    }
}
