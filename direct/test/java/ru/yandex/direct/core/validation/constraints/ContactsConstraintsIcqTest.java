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
@Description("Проверка валидатора номера ICQ")
public class ContactsConstraintsIcqTest {

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public String icq;
    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "icq=\"{1}\" description=\"{0}\"")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"значение null допустимо", null, null},
                {"пустая строка допустима", "", null},
                {"десятизначный номер допустим", "1234567890", null},
                {"пятизначный номер допустим", "12345", null},

                {"четырёхзначный номер не допустим", "1234", invalidValue()},
                {"одинадцатизначный номер не допустим", "12345678901", invalidValue()},
                {"дефисы в номере не допустимы", "123-456-789", invalidValue()},
                {"кирилица не допустима и на ней тест не бросает исключение", "не скажу", invalidValue()},
                {"номер телефона не допустим", "+74951111111", invalidValue()},
        });
    }

    @Test
    public void validIcq() {
        assertThat(ContactsConstraints.validIcq().apply(icq)).isEqualTo(expectedDefect);
    }
}
