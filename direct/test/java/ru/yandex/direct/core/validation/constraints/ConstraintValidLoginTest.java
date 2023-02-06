package ru.yandex.direct.core.validation.constraints;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ConstraintValidLoginTest {
    @Parameterized.Parameter
    public String login;
    @Parameterized.Parameter(1)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{"login", null},
                new Object[]{"yndx.buhter-super", null},
                new Object[]{"a------s", null},
                new Object[]{"info@mbyte.help", null},
                new Object[]{"1a--..__@@", null},

                new Object[]{"holodilnik?ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik/ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik%ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik'ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik,ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik!ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik#ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik*ru", CommonDefects.validLogin()},
                new Object[]{"holodilnik\"ru", CommonDefects.validLogin()},
                new Object[]{"", CommonDefects.validLogin()});
    }

    @Test
    public void test() {
        Defect<?> actualDefect = Constraints.validLogin()
                .apply(login);
        assertThat(actualDefect).isEqualTo(expectedDefect);
    }
}
