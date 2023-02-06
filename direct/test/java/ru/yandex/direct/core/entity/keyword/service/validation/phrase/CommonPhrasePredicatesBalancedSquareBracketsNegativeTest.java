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
public class CommonPhrasePredicatesBalancedSquareBracketsNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"["},
                {"абв["},
                {"[абв"},

                {"]"},
                {"абв]"},
                {"]абв"},

                {"]]"},
                {"абв]абв]абв"},
                {"[["},
                {"абв[абв[абв"},

                {"[продать [выгодно]"},
                {"[выгодно] продать]"},
                {"]продать["},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(balancedSquareBrackets().test(keyword), is(false));
    }
}
