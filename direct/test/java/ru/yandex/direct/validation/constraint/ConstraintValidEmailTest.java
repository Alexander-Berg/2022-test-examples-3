package ru.yandex.direct.validation.constraint;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ConstraintValidEmailTest {
    @Parameterized.Parameter
    public String email;
    @Parameterized.Parameter(value = 1)
    public Defect expectedError;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{"login@yandex.ru", null},
                new Object[]{"sales@ahealth.shop", null},

                new Object[]{"login", CommonDefects.invalidValue()},
                new Object[]{"login@", CommonDefects.invalidValue()},
                new Object[]{"sales@ahealth.shop1", CommonDefects.invalidValue()});
    }

    @Test
    public void test() {
        Defect<?> actualError = StringConstraints.validEmail()
                .apply(email);
        assertThat(actualError).isEqualTo(expectedError);
    }
}
