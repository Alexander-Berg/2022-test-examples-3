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
public class PhrasePredicatesWordsNumberNegativeTest {

    @Autowired
    private StopWordService stopWordService;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"максимальное количество слов фраза обязана cоодержать семь восемь"},
                {"максимальное количество [слов фраза] обязана содержать [семь восемь]"},
                {"\"максимальное количество слов фраза обязана содержать семь восемь\""},
                {"\"максимальное количество [слов фраза] обязана содержать семь восемь\""},

                // учет стоп-слов без скобок и кавычек
                {"максимальное количество слов фраза содержит +не семь восемь"},
                {"максимальное количество слов фраза содержит !не семь восемь"},

                // учет стоп-слов в скобках и кавычках
                {"максимальное количество слов фраза обязана содержать [не семь]"},
                {"максимальное количество слов фраза обязана содержать [!не семь]"},
                {"\"максимальное количество слов во фразе это не семь\""},
                {"\"максимальное количество слов во фразе это +не семь\""},
                {"\"максимальное количество слов во фразе это !не семь\""},
                {"\"максимальное количество слов во фразе это [не семь]\""},
                {"\"максимальное количество слов во фразе это [!не семь]\""},

                // не удаляются зафиксированные стоп-слова при наличии незафиксированных
                {"максимальное количество не слов фраза обязана содержать +не семь"},
                {"максимальное количество не слов фраза обязана содержать !не семь"},

                // учет дубликатов
                {"максимальное количество слов фраза обязана содержать семь семь"},
                {"[максимальное количество слов слов во фразе это семь]"},
                {"\"максимальное количество слов слов во фразе это семь\""},

                //учет сложных слов
                {"максимальное количество слов фраза обязана содержать семь-восемь"},
                {"максимальное количество слов фраза обязана содержать семь-семь"},

                // минус-слова не мешают
                {"максимальное количество слов фраза обязана содержать семь восемь -минус -два"},
                {"[максимальное количество] слов [во фразе это семь] восемь -минус -два"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(plusWordsNoMoreThanMax(7, stopWordService, parseWithMinuses(keyword)).test(keyword), is(false));
    }
}
