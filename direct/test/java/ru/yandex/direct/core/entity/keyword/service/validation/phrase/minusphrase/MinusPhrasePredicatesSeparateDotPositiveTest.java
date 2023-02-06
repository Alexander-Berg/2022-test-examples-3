package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.separateDot;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesSeparateDotPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"виски 1 2 3 5"},
                {"12.456"},
                {"виски 0.5"},
                {"янв."},
                {"точ.ка"},
                {".тчк"},
                {"-.dot"},
                {".456"},
                {"456."},
                {"v.1"},
                {".v."},
                {"1.2.3"},
                {"127.0.0.1"},
                {"abc 127.0.0.1"},
                {"1.1.1"},
                {"виски 0.5 0.7"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(separateDot().test(keyword), is(true));
    }
}
