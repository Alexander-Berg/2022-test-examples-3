package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.validPoint;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesPointNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // точка
                {"."},
                {" . "},
                {"white . black"},

                // точка в начале
                {".а"},
                {" .а "},
                {"слово .а слово"},
                {"!.а"},
                {" !.а "},
                {"слово !.а слово"},
                {"+.а"},
                {" +.а "},
                {"слово +.а слово"},

                {".слово"},
                {"слово .слово слово"},

                {"[.слово]"},
                {"слово [.слово] слово"},

                {"\".слово\""},
                {"\"слово [.слово] слово\""},

                // две точки
                {".."},
                {" .. "},
                {"white .. black"},
                {"..слово"},
                {"[..слово]"},
                {"\"..слово\""},
                {" ..слово "},
                {" слово.. "},
                {" сло..во "},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validPoint().test(keyword), is(false));
    }
}
