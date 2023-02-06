package ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhrasePredicates.numberWithPoint;

@RunWith(Parameterized.class)
public class MinusPhrasePredicatesNumberWithPointPositiveTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"янв."},
                {"точ.ка"},
                {".тчк"},

                {"[янв.]"},
                {"[точ.ка]"},
                {"[.тчк]"},

                {"\"янв.\""},
                {"\"точ.ка\""},
                {"\".тчк\""},

                {"первое янв. норм"},
                {"перед точ.ка после"},
                {"перед .тчк после"},

                {"виски 1 2 3 5"},
                {"12.456"},
                {"виски 0.5 ром"},
                {"\"0.5\""},
                {"\"виски 0.5 ром\""},
                {"[0.5]"},
                {"[виски 0.5 ром]"},
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(numberWithPoint().test(keyword), is(true));
    }
}
