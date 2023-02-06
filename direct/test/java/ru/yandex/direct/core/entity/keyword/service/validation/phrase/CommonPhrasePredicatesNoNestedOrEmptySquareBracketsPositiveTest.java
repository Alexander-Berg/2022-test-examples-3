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
public class CommonPhrasePredicatesNoNestedOrEmptySquareBracketsPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"word"},
                {"[word]"},
                {" [ word ] "},
                {"\"[word]\""},
                {" \" [word] \" "},
                {"купить [в магазине]"},
                {"[купить в Москве] [недорого у метро]"},
                {"по улицам [купить в Москве] слона [недорого у метро] водили"},
                {"]"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noNestedOrEmptySquareBrackets().test(keyword), is(true));
    }
}
