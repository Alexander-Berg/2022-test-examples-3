package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.containsPlusWords;

@RunWith(Parameterized.class)
public class PhrasePredicatesContainsPlusWordsNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // одно минус-слово
                {"-слово"},
                {" -слово "},

                {"-!слово"},
                {" -!слово "},

                {"-+слово"},
                {" -+слово "},

                {"-[слово]"},
                {" -[ слово ] "},

                // два минус-слова

                {"-слово -другое"},
                {" -слово -другое "},

                // минус-фраза

                {"-слово другое"},
                {" -слово другое "},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(containsPlusWords().test(keyword), is(false));
    }
}
