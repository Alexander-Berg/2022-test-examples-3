package ru.yandex.direct.core.entity.keyword.service.validation.phrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.CommonPhrasePredicates.validQuotes;

@RunWith(Parameterized.class)
public class CommonPhrasePredicatesQuotesNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"\"\""},
                {"\"  \""},

                {"b\"ad quotes\""},
                {"\"bad quote\"s"},
                {"1\"bad quotes\""},
                {"\"bad quote\"1"},
                {"!\"bad quotes\""},
                {"\"bad quote\"!"},
                {"-\"bad quotes\""},
                {"\"bad quote\"-"},
                {"+\"bad quotes\""},
                {"\"bad quote\"+"},
                {"]\"bad quotes\""},
                {"\"bad quote\"]"},
                {"[\"bad quotes\""},
                {"\"bad quote\"["},
                {".\"bad quotes\""},
                {"\"bad quote\"."},
                {" \"bad quotes\""},
                {"\"bad quote\" "},

                {"\"hmm"},
                {"hmm\""},

                {"\"ba\"d quotes\""},
                {"\"bad\" \"quotes\""},
                {"\"\"текст\""},
                {"\"\"текст\"\""},

        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validQuotes().test(keyword), is(false));
    }
}
