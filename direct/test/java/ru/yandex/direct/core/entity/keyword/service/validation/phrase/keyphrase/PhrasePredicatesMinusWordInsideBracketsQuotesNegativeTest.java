package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.noMinusWordsInsideBracketsOrQuotes;

@RunWith(Parameterized.class)
public class PhrasePredicatesMinusWordInsideBracketsQuotesNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"[-слово]"},
                {"[ -слово ]"},
                {"[слово -слово]"},
                {"[-слово слово]"},
                {"[слово -слово слово]"},

                {"[-слово-слово]"},
                {"[ -слово-слово ]"},
                {"[слово -слово-слово]"},
                {"[-слово-слово слово]"},
                {"[слово -слово-слово слово]"},

                {"\"-слово\""},
                {"\" -слово \""},
                {"\"слово -слово\""},
                {"\"-слово слово\""},
                {"\"слово -слово слово\""},

                {"\"-слово-слово\""},
                {"\" -слово-слово \""},
                {"\"слово -слово-слово\""},
                {"\"-слово-слово слово\""},
                {"\"слово -слово-слово слово\""},

                {"\"[-слово]\""},
                {"\"[ -слово ]\""},
                {"\"[слово -слово]\""},
                {"\"[-слово слово]\""},
                {"\"[слово -слово слово]\""},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(noMinusWordsInsideBracketsOrQuotes().test(keyword), is(false));
    }
}
