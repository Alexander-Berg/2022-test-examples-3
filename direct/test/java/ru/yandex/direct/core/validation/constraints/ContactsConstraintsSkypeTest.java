package ru.yandex.direct.core.validation.constraints;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.constraint.ContactsConstraints;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

@RunWith(Parameterized.class)
@Description("Проверка валидатора skype")
public class ContactsConstraintsSkypeTest {

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public String skype;
    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "skype=\"{1}\" description=\"{0}\"")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"значение null допустимо", null, null},
                {"пустая строка допустима", "", null},
                {"дефисы и цифры допустимы", "my-skype1", null},
                {"точки допустимы", "my.skype", null},
                {"нижние подчеркивания допустимы", "my_skype", null},
                {"двоеточия допустимы", "my:skype", null},
                {"логин не может начинаться с цифры", "231my:skype", invalidValue()},
                {"логин не может начинаться с подчеркивания", "_my:skype", invalidValue()},
                {"слишком короткое имя", "my", invalidValue()}
        });
    }

    @Test
    public void validTelegram() {
        assertThat(ContactsConstraints.validSkype().apply(skype)).isEqualTo(expectedDefect);
    }
}
