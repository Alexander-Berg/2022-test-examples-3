package ru.yandex.direct.libs.keywordutils.parser;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class KeywordWithMinusesParserTest {

    @Parameterized.Parameter
    public String input;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "parse({0}) == {1}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{

                //кейсы с минус-словами
                {"конь -купить", "конь -купить"},
                {"конь -\"купить\"", "конь -\"купить\""},
                {"конь -[купить]", "конь -[купить]"},
                {"конь -!купить", "конь -!купить"},
                {"конь -купить слона", "конь -купить слона"},
                {"конь -купить -слона", "конь -купить -слона"},
        });
    }

    @Test
    public void test() {
        String actual = String.valueOf(KeywordParser.parseWithMinuses(input));
        assertThat(actual, is(expected));
    }
}
