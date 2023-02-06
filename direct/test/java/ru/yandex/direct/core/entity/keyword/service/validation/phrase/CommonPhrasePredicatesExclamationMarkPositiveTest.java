package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.validExclamationMark;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesExclamationMarkPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"!а"},
                {"слово !а"},
                {"слово  !а"},

                {"!для"},
                {"слово !для"},
                {"слово  !для"},

                {"!1"},
                {"слово !1"},
                {"слово  !1"},

                {"!123"},
                {"слово !123"},
                {"слово  !123"},

                {"[!абв]"},
                {"[ !абв]"},

                {"\"!абв\""},
                {"\" !абв\""},

                {"-!абв"},
                {"-!абв"},

                {"!слово [!не воробей] -!абв"},
                {"!слово [!не воробей] -!абв -!еще"},
                {"!слово [!не воробей] -!абв -слово -!еще"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validExclamationMark().test(keyword), is(true));
    }
}
