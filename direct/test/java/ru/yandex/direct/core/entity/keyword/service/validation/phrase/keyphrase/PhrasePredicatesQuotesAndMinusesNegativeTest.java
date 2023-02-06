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
public class PhrasePredicatesQuotesAndMinusesNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // одна кавычка и разные одиночные символы с операторами в качестве минус-слова
                {"\" -а"},
                {"\" -!а"},
                {"\" -+а"},
                {"\" --а"},

                {"\" -1"},
                {"\" -!1"},
                {"\" -+1"},
                {"\" --1"},

                // одна кавычка и минус-слово с различными операторами
                {"\" -абв"},
                {"\" -!абв"},
                {"\" -+абв"},

                {"\" -а.бв"},
                {"\" -!а.бв"},
                {"\" -+а.бв"},

                {"\" [-абв]"},
                {"\" [-!абв]"},
                {"\" [-+абв]"},
                {"\"[-+абв]"},
                {"\" [ -+абв ]"},

                {"\" [абв]-где"},

                {"\" -[абв]"},
                {"\" -[!абв]"},
                {"\" -[+абв]"},
                {"\"-[+абв]"},
                {"\" -[ +абв ]"},

                // одна кавычка и минус-фраза с различными операторами
                {"\" -абв где"},

                {"\" -[абв где]"},
                {"\" -[!абв где]"},
                {"\" -[+абв где]"},
                {"\"-[+абв где]"},
                {"\" -[ +абв где ]"},

                // две кавычки и минус-слова/минус-фразы
                {"\"абв\" -абв"},
                {"\"абв\"-абв"},
                {"\"абв\" -абв -где"},
                {"\"абв\" -абв где -жзи"},

                {"-абв \"абв\""},
                {"-абв\"абв\""},

                {"\" \" -абв"},

                // минус-слово внутри кавычек с различными операторами
                {"\"-абв\""},
                {"\"-!абв\""},
                {"\"-+абв\""},
                {"\" -абв \""},
                {"\" -!абв \""},
                {"\" -+абв \""},
                {"\"абв -абв абв\""},
                {"\"абв -!абв абв\""},
                {"\"абв -+абв абв\""},
                {"\"[-абв]\""},
                {"\"[-!абв]\""},
                {"\"[-+абв]\""},

                {"\"[абв]-где\""},

                {"\"слов -абв еще\""},
                {"\"[слов -абв еще]\""},
                {"\"вне [слов -абв еще] скобок\""},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noBothQuotesAndMinusWords().test(keyword), is(false));
    }
}
