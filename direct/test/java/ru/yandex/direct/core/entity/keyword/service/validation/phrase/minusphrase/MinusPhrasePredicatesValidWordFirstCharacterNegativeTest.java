package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.validWordFirstCharacter;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesValidWordFirstCharacterNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // точка
                {".тчк"},
                {".456"},
                {"."},
                {" . "},
                {" .. "},
                {"точка . отдельно"},

                // точка после различных спец-символов
                {"!.dot"},
                {"+.dot"},
                {"-.dot"},
                {"[.dot"},
                {"].dot"},
                {"\".dot"},
                {"..dot"},
                {"'.dot"},

                {" .тчк"},
                {" !.dot"},
                {" +.dot"},
                {" -.dot"},
                {" [.dot"},
                {" ].dot"},
                {" \".dot"},
                {" ..dot"},
                {" '.dot"},

                {"[!.dot"},
                {"]!.dot"},
                {"[+.dot"},
                {"]+.dot"},
                {"[-.dot"},
                {"]-.dot"},
                {"[..dot"},
                {"]..dot"},
                {"['.dot"},
                {"]'.dot"},

                {"\"!.dot"},
                {"\"+.dot"},
                {"\"-.dot"},
                {"\"..dot"},
                {"\"'.dot"},

                // апостроф
                {"'тчк"},
                {"'456"},
                {"'"},
                {" ' "},
                {" '' "},
                {"апостроф ' отдельно"},

                // апостроф после различных спец-символов
                {"!'dot"},
                {"+'dot"},
                {"-'dot"},
                {"['dot"},
                {"]'dot"},
                {"\"'dot"},
                {".'dot"},
                {"''dot"},

                {" 'тчк"},
                {" !'dot"},
                {" +'dot"},
                {" -'dot"},
                {" ['dot"},
                {" ]'dot"},
                {" \"'dot"},
                {" .'dot"},
                {" ''dot"},

                {"[!'dot"},
                {"]!'dot"},
                {"[+'dot"},
                {"]+'dot"},
                {"[-'dot"},
                {"]-'dot"},
                {"[.'dot"},
                {"].'dot"},
                {"[''dot"},
                {"]''dot"},

                {"\"!'dot"},
                {"\"+'dot"},
                {"\"-'dot"},
                {"\".'dot"},
                {"\"''dot"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validWordFirstCharacter().test(keyword), is(false));
    }
}
