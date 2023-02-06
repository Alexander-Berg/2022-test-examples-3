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
public class PhrasePredicatesContainsPlusWordsPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // плюс-слово с/без минус-слов
                {"слово"},
                {" слово "},

                {"слово -слово"},
                {" слово  -слово "},

                {"!слово -слово"},
                {"+слово -слово"},
                {"[слово] -слово"},
                {"\"слово\" -слово"},

                {"слово -!слово"},
                {"слово -+слово"},
                {"слово -[слово]"},

                // плюс-слово через дефис с/без минус-слов
                {"дабл-слово"},
                {" дабл-слово "},

                {"дабл-слово -слово"},
                {" дабл-слово -слово "},

                {"!дабл-слово -слово"},
                {"+дабл-слово -слово"},
                {"[дабл-слово] -слово"},
                {"\"дабл-слово\" -слово"},

                {"дабл-слово -!слово"},
                {"дабл-слово -+слово"},
                {"дабл-слово -[слово]"},

                // плюс-слово и несколько минус-слов
                {"слово -слово -другое"},
                {"!слово -слово -другое"},
                {"+слово -слово -другое"},
                {"[слово] -слово -другое"},
                {"\"слово\" -слово -другое"},

                {"слово -!слово -другое"},
                {"слово -слово -!другое"},
                {"слово -+слово -другое"},
                {"слово -слово -+другое"},
                {"слово -[слово] -другое"},
                {"слово -слово -[другое]"},
                {"слово -[слово другое]"},

                // сложные фразы
                {"розовый слон -слово"},
                {"[розовый слон] -слово"},
                {"\"[розовый] слон\" -слово"},
                {"\"[розовый] слон\" -слово -другое -!третье"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(containsPlusWords().test(keyword), is(true));
    }
}
