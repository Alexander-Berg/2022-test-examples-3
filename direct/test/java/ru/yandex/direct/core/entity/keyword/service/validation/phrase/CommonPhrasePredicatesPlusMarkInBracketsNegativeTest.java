package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.noPlusMarkInBrackets;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesPlusMarkInBracketsNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"[+]"},
                {"[ + ]"},
                {"\"[+]\""},
                {"\" [ + ] \""},

                {"[а+]"},
                {"[+а]"},
                {"[ +а ]"},
                {"[слово +а]"},
                {"[+а слово]"},
                {"[!слово +а]"},
                {"[+а !слово]"},
                {"[ололо +а !слово]"},

                {"[2+]"},
                {"[+2]"},
                {"[ +2 ]"},
                {"[слово +2]"},
                {"[+2 слово]"},
                {"[!слово +2]"},
                {"[+2 !слово]"},
                {"[ололо +2 !слово]"},

                {"[абв+]"},
                {"[+абв]"},
                {"[ +абв ]"},
                {"[слово +абв]"},
                {"[+абв слово]"},
                {"[!слово +абв]"},
                {"[+абв !слово]"},
                {"[ололо +абв !слово]"},

                {"[ололо +абв !слово +акг бдыщ]"},

                {"[на луну] [ололо +абв !слово +акг бдыщ]"},
                {"[ололо +абв !слово +акг бдыщ] [на луну]"},

                {"на луну [ололо +абв !слово +акг бдыщ] "},
                {"[ололо +абв !слово +акг бдыщ] на луну "},

                {"набор [ололо +абв !слово +акг бдыщ] слов [на луну]"},

                {"[ололо +абв !слово]"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noPlusMarkInBrackets().test(keyword), is(false));
    }
}
