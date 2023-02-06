package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.plusWordsNoMoreThanMax;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

@CoreTest
@RunWith(Parameterized.class)
public class PhrasePredicatesWordsNumberPositiveTest {

    @Autowired
    private StopWordService stopWordService;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"фраза"},
                {"длинная фраза"},
                {"максимальное количество слов во фразе это семь"},
                {"максимальное количество слов во фразе санкт-петербург"},
                {"[максимальное количество слов во фразе это семь]"},
                {"максимальное количество [слов во фразе это семь]"},
                {"\"максимальное количество слов во фразе это семь\""},
                {"\"максимальное количество [слов во фразе это семь]\""},

                // учет стоп-слов
                {"максимальное количество слов во фразе это не семь"},
                {"теперь максимальное количество слов во фразе это не семь"},

                // незафиксированные стоп-слова не учитываются при наличии зафиксированных
                {"теперь максимальное количество +не слов во фразе не семь"},
                {"теперь максимальное количество !не слов во фразе не семь"},

                // стоп-слова не мешают
                {"\"максимальное количество слов во фразе не семь\""},
                {"\"максимальное количество слов во фразе !не семь\""},
                {"\"максимальное количество слов во фразе +не семь\""},

                // минус-слова не учитываются
                {"максимальное количество слов во фразе это семь -минус"},
                {"максимальное количество слов во фразе это семь -минус -два"},
                {"максимальное количество слов -во -фразе -это -семь -минус -два -три -четыре -пять"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(plusWordsNoMoreThanMax(7, stopWordService, parseWithMinuses(keyword)).test(keyword), is(true));
    }
}
