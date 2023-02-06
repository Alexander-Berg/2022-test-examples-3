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
public class PhrasePredicatesMinusDeletesPlusNegativeTest {

    private final StopWordMatcher stopWordMatcher = text -> text.equals("в");

    KeywordWithLemmasFactory keywordFactory = new KeywordWithLemmasFactory();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // учитываются формы
                {"фраза -фраза"},
                {"фраза -фразу"},
                {"фразу -фраза"},
                {"+фраза -фраза"},
                {"+фраза -фразу"},
                {"!фраза -фраза"},
                {"!фраза -фразу"},
                {"!фраза -!фраза"},

                // стоп-слова считаются плюс-словами, если они зафиксированы,
                // и в этом случае не могут вычитаться
                {"идти +в лес -в"},
                {"идти +в лес -+в"},
                {"идти !в лес -в"},

                // внутри скобок и кавычек
                {"[фраза] -фразу"},
                {"\"!фраза\" -фразу"},

                // несколько минус-слов и только одно вычитает плюс-слово
                {"фраза -слово -фразу"},
                {"фраза -слово -фразу -слово"},
                {"длинная фраза нереально -слово -фразу"},
                {"длинная фраза нереально -слово -фразу -слово"},
                {"длинная [фраза нереально] -слово -фразу -слово"},
                {"\"длинная [фраза нереально]\" -слово -фразу -слово"},

                // минус-слово вычитает два плюс-слова
                {"!фраза !фразу -слово -фразу"},

                // несколько минус-слов вычитают несколько плюс-слов
                {"слово !фразу -слово -фраза"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noMinusWordsDeletingPlusWords(keywordFactory, stopWordMatcher, parseWithMinuses(keyword))
                .test(keyword), is(false));
    }
}
