package ru.yandex.direct.core.entity.keyword.processing;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class KeywordProcessingUtilsGetLengthWithoutMinusExclamationAndSpacesTest {

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"", 0},
                {"   ", 0},
                {"abc", 3},
                {"  abc   ", 3},
                {"a b", 3},
                {"a    b", 3},
                {"-!", 1},
                {"a       b", 3},
                {"a -! b", 5},
        });
    }

    @Parameterized.Parameter
    public String input;

    @Parameterized.Parameter(1)
    public int length;

    @Test
    public void testParametrized() {
        assertThat(KeywordProcessingUtils.getLengthWithoutMinusExclamationAndSpaces(input), is(length));
    }
}
