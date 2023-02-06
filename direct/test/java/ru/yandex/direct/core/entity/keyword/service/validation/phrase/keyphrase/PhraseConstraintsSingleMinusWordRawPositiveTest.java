package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseConstraints.onlySingleMinusWordsRaw;

@RunWith(Parameterized.class)
public class PhraseConstraintsSingleMinusWordRawPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"фраза -слово"},
                {"фраза - слово"},
                {"фраза -+слово"},
                {"фраза -!слово"},
                {"фраза -[слово]"},
                {"фраза -\"слово\""},

                {"фраза -слово -другое"},
                {"фраза -слово -!другое"},
                {"фраза -слово -+другое"},

                {"фраза -слово -!другое -+третье -пятое -!десятое"},

                // плюс-слово и минус-слово
                {"фраза -слово"},
                {"[фраза] -слово"},
                {"\"фраза\" -слово"},
                {"\"[фраза]\" -слово"},

                // ключевая фраза из двух слов
                {"длинная фраза -слово"},
                {"[длинная фраза] -слово"},
                {"[длинная] фраза -слово"},
                {"длинная [фраза] -слово"},
                {"\"длинная фраза\" -слово"},
                {"\"[длинная фраза]\" -слово"},
                {"\"[длинная] фраза\" -слово"},
                {"\"длинная [фраза]\" -слово"},

                // ключевая фраза с дефисами + минус-слова
                {"длинная-предлинная -слово"},
                {"длинная-предлинная -слово -другое"},
                {"!длинная-предлинная -слово -другое"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(onlySingleMinusWordsRaw().apply(keyword), nullValue());
    }
}
