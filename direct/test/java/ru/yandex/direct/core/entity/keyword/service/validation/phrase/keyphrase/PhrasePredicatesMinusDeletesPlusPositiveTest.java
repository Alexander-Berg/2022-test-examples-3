package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.libs.keywordutils.StopWordMatcher;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.noMinusWordsDeletingPlusWords;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

@RunWith(Parameterized.class)
public class PhrasePredicatesMinusDeletesPlusPositiveTest {

    private final StopWordMatcher stopWordMatcher = text -> text.equals("в");
    KeywordWithLemmasFactory keywordFactory = new KeywordWithLemmasFactory();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"фраза"},
                {"длинная фраза"},
                {"длинная фраза c -минусом"},
                {"длинная фраза c -минус -словами"},

                {"фраза -!фраза"},
                {"фраза -!фразу"},
                {"длинная фраза -!фраза"},

                // стоп-слово "в" не считается плюс-словом,
                // так как является незафиксированным стоп-словом
                {"идти в лес -в"},
                {"идти в лес -+в"},
                {"идти в лес -!в"},
                // это вырожденный случай, когда проверка проходит из-за того что
                // форма вычитаемого стоп-слова зафиксирована, а исходного - нет
                {"идти +в лес -!в"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(
                noMinusWordsDeletingPlusWords(keywordFactory, stopWordMatcher, parseWithMinuses(keyword)).test(keyword),
                is(true));
    }
}
