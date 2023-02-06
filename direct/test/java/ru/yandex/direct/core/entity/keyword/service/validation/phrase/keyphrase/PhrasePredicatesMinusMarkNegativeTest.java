package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.validMinusMark;

@RunWith(Parameterized.class)
public class PhrasePredicatesMinusMarkNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"-"},
                {" - "},
                {"white - black"},
                {"а-"},
                {"а- слово"},

                {"--"},
                {" -- "},

                // знак минус + спецсимвол
                {"-."},
                {"-'"},
                {"-["},
                {"-]"},
                {"-\" "},
                {" -\" "},
                {" -\" слово"},
                {"-.слово"},

                // спецсимвол + знак минус
                {".-"},
                {"'-"},
                {"!-"},
                {"+-"},
                {"[-"},
                {"]-"},
                {"\"-"},
                {"!- "},
                {" !- "},
                {"!- слово"},
                {"!-слово"},

                // в конце слова
                {"абв-"},
                {" абв- "},
                {"[абв-"},
                {"\"абв-"},
                {"абв-]"},
                {"абв-\""},

                // двойной в начале
                {"--авб"},
                {" --авб "},
                {"[--авб"},
                {"\"--авб"},
                {"--авб]"},
                {"--авб\""},

                // двойной в конце
                {"авб--"},
                {" авб-- "},
                {"[авб--"},
                {"\"авб--"},
                {"авб--]"},
                {"авб--\""},

                // двойной в середине
                {"а--б"},
                {" а--б "},
                {"[а--б]"},
                {" [а--б] "},
                {"\"а--б\""},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validMinusMark().test(keyword), is(false));
    }
}
