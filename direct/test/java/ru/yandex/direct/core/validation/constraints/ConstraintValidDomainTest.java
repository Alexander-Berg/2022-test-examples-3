package ru.yandex.direct.core.validation.constraints;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.validation.constraints.Constraints.validDomain;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

@RunWith(Parameterized.class)
public class ConstraintValidDomainTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"a.b.c.d", null},
                {"а.б.в.г", null},
                {"ukodelie.shop", null},
                {"домен.ру", null},
                {"www.yandex", null},
                {"ya.ru", null},


                {"", invalidValue()},
                {"   ", invalidValue()},
                {"wwwyandexru", invalidValue()},
                {"-a.b.c.d", invalidValue()},
                {"abcd", invalidValue()},
                {"Интер-Сервис", invalidValue()},
                {"машина_валеры.рф", invalidValue()},
                {"Магазин \"Фотик\" в Смоленске", invalidValue()}
        });
    }

    private final String domain;
    private final Defect expectedDefect;

    public ConstraintValidDomainTest(String domain, Defect defect) {
        this.domain = domain;
        this.expectedDefect = defect;
    }

    @Test
    public void testParameterized() {
        assertThat(validDomain().apply(domain)).isEqualTo(expectedDefect);
    }
}
