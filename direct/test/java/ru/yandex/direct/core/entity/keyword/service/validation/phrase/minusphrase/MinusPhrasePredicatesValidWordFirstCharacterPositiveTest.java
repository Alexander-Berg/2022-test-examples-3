package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.validWordFirstCharacter;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesValidWordFirstCharacterPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"янв"},
                {" янв "},
                {"123"},
                {" 123 "},
                {"\"абв 123 !где [+fd 48]\""},

                // точка
                {"янв."},
                {"янв.]"},
                {"янв.["},
                {"янв.\""},

                {"123."},
                {"123.]"},
                {"123.["},
                {"123.\""},

                {"точ.ка"},
                {"точ.ка"},
                {"точ.121"},
                {"121.точ"},

                {"1.2.3"},
                {"127.0.0.1"},

                {"456."},
                {"v.1"},
                {"abc 127.0.0.1"},

                {"виски 0.5 0.7"},

                // апостроф
                {"д'артаньян"},
                {"дартаньян'"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validWordFirstCharacter().test(keyword), is(true));
    }
}
