package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.noBothQuotesAndMinusWords;

@RunWith(Parameterized.class)
public class PhrasePredicatesQuotesAndMinusesPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"-а"},
                {"-1"},

                {"-абв"},
                {"-а.бв"},
                {"-!абв"},
                {"-+абв"},
                {"[-абв]"},

                {" -абв "},
                {"абв -абв"},
                {"[абв] -абв"},
                {"[абв]-абв"},
                {"-абв [абв]"},
                {"-абв[абв]"},

                {"слово [много !слов] +слова -абв"},
                {"слово [много !слов] +слова -абв -где"},

                {"слово [много !слов] +слова -абв где"},
                {"слово [много !слов] +слова -абв где -ололо трололо"},

                // дефис с кавычками
                {"\"санкт-петербург\""},
                {"\" санкт-петербург \""},
                {"\"хорошо в санкт-петербурге зимой\""},

                {"санкт-петербург \"норм\""},
                {"\"норм\" санкт-петербург"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noBothQuotesAndMinusWords().test(keyword), is(true));
    }
}
