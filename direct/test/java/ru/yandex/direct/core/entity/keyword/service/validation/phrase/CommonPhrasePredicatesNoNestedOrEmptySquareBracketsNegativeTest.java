package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.noNestedOrEmptySquareBrackets;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesNoNestedOrEmptySquareBracketsNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"на[конь]"},
                {"[конь]на"},
                {"купить[в]магазине[на]диване[сегодня]"},
                {"[]"},
                {"текст [] на русском"},
                {"]продать["},
                {"[[][]]"},
                {"[текст [с] [вложенными] скобками]"},
                {"[[два]]"},
                {"["},
                {"[[]"},
                {"[продать [выгодно]"},

        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noNestedOrEmptySquareBrackets().test(keyword), is(false));
    }
}
