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
@Description("Проверка валидатора имени в Telegram")
public class ContactsConstraintsTelegramTest {

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public String telegram;
    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "telegram=\"{1}\" description=\"{0}\"")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"значение null допустимо", null, null},
                {"пустая строка допустима", "", null},
                {"корректное имя пропускаем", "vasiliy_pupkin", null},
                {"заглавные буквы в имени допустимы", "Alexandr_Matrosov", null},

                {"слишком короткое имя", "abcd", invalidValue()},
                {"слишком длинное имя", "ochen_dlinnyj_login_nu_ochen_dlin", invalidValue()},
                {"начальный знак @ недопустим", "@akakij_akakievich", invalidValue()},
                {"точка в имени недопустима", "ivan.ivanov", invalidValue()},
                {"пробелы в имени не допустимы", "petr petrov", invalidValue()},
                {"кирилица в имени не доупстима и на ней тест не бросает исключение", "не_скажу", invalidValue()},
                {"номер телефона вместо имени не допустим", "+74951111111", invalidValue()},
        });
    }

    @Test
    public void validTelegram() {
        assertThat(ContactsConstraints.validTelegram().apply(telegram)).isEqualTo(expectedDefect);
    }
}
