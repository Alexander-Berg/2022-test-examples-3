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
public class VcardMappingsToDbTest {
    @Parameterized.Parameter
    public Phone phone;
    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{phone("+7", "495", "1", null), "+7#495#1#"},
                new Object[]{phone("+7", "495", "12345", null), "+7#495#1-23-45#"},
                new Object[]{phone("+7", "495", "123456", null), "+7#495#12-34-56#"},
                new Object[]{phone("+7", "495", "1234567", null), "+7#495#123-45-67#"},
                new Object[]{phone("+7", "495", "123456789", null), "+7#495#123-45-67-89#"},
                new Object[]{phone("+7", "495", "23-45-6-7-8", null), "+7#495#234-56-78#"},
                new Object[]{phone("+7", "495", "012345", null), "+7#495#01-23-45#"},
                new Object[]{phone("+375", "1", "12345-6-7-8", null), "+375#1#12-34-56-78#"},
                new Object[]{phone("+380", "1", "12 34-567", null), "+380#1#123-45-67#"},
                new Object[]{phone("+1", "1", "123 45-6-7-89", null), "+1#1#123 45-6-7-89#"},
                new Object[]{phone("+7", "495", "1234567", "123"), "+7#495#123-45-67#123"});
    }

    @Test
    public void test() {
        String actual = VcardMappings.phoneToDb(phone);

        assertThat(actual).isEqualTo(expected);
    }
}
