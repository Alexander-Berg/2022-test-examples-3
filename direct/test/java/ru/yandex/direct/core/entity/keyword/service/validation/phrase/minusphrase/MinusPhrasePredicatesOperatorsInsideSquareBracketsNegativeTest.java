package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.operatorsInsideSquareBrackets;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesOperatorsInsideSquareBracketsNegativeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"[+his]"},
//                {"[+his][his]"},              а это баг
                {"[лететь +на луну]"},
                {"[\"his\"]"},
                {"[лететь \"на луну]"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(operatorsInsideSquareBrackets().test(keyword), is(false));
    }
}
