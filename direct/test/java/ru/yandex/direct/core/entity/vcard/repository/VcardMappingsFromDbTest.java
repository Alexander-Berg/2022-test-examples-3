package ru.yandex.direct.core.entity.vcard.repository;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.Phone;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.vcard.Phones.phone;

@RunWith(Parameterized.class)
public class VcardMappingsFromDbTest {
    @Parameterized.Parameter
    public String decsription;
    @Parameterized.Parameter(1)
    public String phoneStr;
    @Parameterized.Parameter(2)
    public Phone expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{
                        "Телефон без добавочного",
                        "+7#495#123-25-67#", phone("+7", "495", "123-25-67", null)},
                new Object[]{
                        "Телефон с добавочным",
                        "+7#495#123-25-67#123", phone("+7", "495", "123-25-67", "123")},
                new Object[]{
                        "Некорректный формат номера в базе",
                        "123-456-7#", phone("", "", "123-456-7#", null)});
    }

    @Test
    public void test() {
        Phone actual = VcardMappings.phoneFromDb(phoneStr);

        assertThat(actual).isEqualTo(expected);
    }
}
