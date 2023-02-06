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
@Description("Проверка валидатора номера телефона")
public class ContactsConstraintsPhoneTest {

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public String phone;
    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "phone=\"{1}\" description=\"{0}\"")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"значение null допустимо", null, null},
                {"пустая строка допустима", "", null},
                {"телефон через восьмёрку пропускаем", "84951234567", null},
                {"телефон в международной нотации, да ещё со скобками, пробелами и дефисами - всё равно пропускаем",
                        "+7 (495) 123-45-67", null},

                {"латиница в номере телефона не разрешена", "qwerty", invalidValue()},
                {"номер короче пяти цифр не разрешён", "1234", invalidValue()},
                {"кирилица в номере телефона не разрешена и на ней тест не бросает исключение",
                        "+7 495 три топора -66-01", invalidValue()},
                {"номер длиннее пятнадцати цифр не разрешён", "1111111111111111111", invalidValue()},
                {"добавочный номер не разрешён", "+74951111111#4567", invalidValue()},
        });
    }

    @Test
    public void validPhone() {
        assertThat(ContactsConstraints.validPhone().apply(phone)).isEqualTo(expectedDefect);
    }
}
