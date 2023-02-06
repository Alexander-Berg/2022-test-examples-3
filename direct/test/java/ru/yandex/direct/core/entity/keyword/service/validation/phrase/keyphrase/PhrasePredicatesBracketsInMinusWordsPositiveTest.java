package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.noBracketsInMinusWords;

@RunWith(Parameterized.class)
public class PhrasePredicatesBracketsInMinusWordsPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {" -слово"},
                {" -+слово"},
                {" -!слово"},

                {" -слово -!другое -+третье -пятое -!десятое"},

                // плюс-слово и минус-слово
                {"фраза -слово"},
                {"[фраза] -слово"},
                {"\"фраза\" -слово"},
                {"\"[фраза]\" -слово"},

                // плюс-слово и минус-слово c дефисом
                {"фраза -санкт-петербург"},
                {"[фраза] -санкт-петербург"},
                {"\"фраза\" -санкт-петербург"},
                {"\"[фраза]\" -санкт-петербург"},
                {"\"[фраза]\" -санкт-петербург -санкт-петербург"},
                {"\"[фраза]\" -санкт-петербург -москва"},

                // ключевая фраза с дефисами + минус-слова
                {"длинная-предлинная -слово"},
                {"длинная-предлинная -слово -другое"},
                {"!длинная-предлинная -слово -другое"},

                {"длинная-предлинная фраза [еще скобки] -слово -другое"},
                {"[длинная-предлинная фраза] [еще скобки] -слово -другое"},
                {"\"длинная-предлинная фраза [еще скобки]\" -слово -другое"},
                {"\"[длинная-предлинная фраза] [еще скобки]\" -слово -другое"},
                {"\"длинная-предлинная [фраза] [еще скобки]\" -слово -другое"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noBracketsInMinusWords().test(keyword), is(true));
    }
}
