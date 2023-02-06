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
public class PhrasePredicatesBracketsInMinusWordsNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {" -[слово]"},
                {" -[+слово]"},
                {" -[!слово]"},

                {" -слово -!другое -+третье -[пятое] -!десятое"},

                // плюс-слово и минус-слово
                {"фраза -[слово]"},
                {"фраза -слово -[слово]"},
                {"фраза -[слово] -слово"},
                {"фраза -слово -[слово] -слово"},
                {"[фраза] -слово -[слово] -слово"},
                {"\"фраза\" -слово -[слово] -слово"},

                // плюс-слово и минус-слово c дефисом
                {"фраза -[санкт-петербург]"},
                {"фраза -слово -[санкт-петербург]"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noBracketsInMinusWords().test(keyword), is(false));
    }
}
