package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.balancedSquareBrackets;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesBalancedSquareBracketsPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"[word]"},
                {" [ word ] "},
                {"word"},
                {"\"[word]\""},
                {" \" [word] \" "},
                {"купить [в магазине]"},
                {"на[конь]"},
                {"[конь]на"},
                {"купить[в]магазине[на]диване[сегодня]"},
                {"[купить в Москве] [недорого у метро]"},
                {"[]"},
                {"текст []на русском"},
                {"[[][]]"},
                {"[текст [с] [вложенными] скобками]"},
                {"[[два]]"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(balancedSquareBrackets().test(keyword), is(true));
    }
}
