package ru.yandex.direct.core.entity.vcard.service.validation;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.vcard.service.validation.OgrnConstraint.DefectDefinitions.invalidOgrn;
import static ru.yandex.direct.core.entity.vcard.service.validation.OgrnConstraint.ogrnIsValid;

@RunWith(Parameterized.class)
public class OgrnConstraintTest {

    @Parameterized.Parameter(0)
    public String ogrn;

    @Parameterized.Parameter(1)
    public Defect expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {null, null},
                {"", invalidOgrn()},

                // ОГРН
                {"102773951339", invalidOgrn()},
                {"10277395133944", invalidOgrn()},
                {"1027739513395", invalidOgrn()},
                {"1027739513394", null},
                {"5077746977435", null},

                // Недопустимая первая цифра
                {"002773951339", invalidOgrn()},
                {"402773951339", invalidOgrn()},
                {"602773951339", invalidOgrn()},
                {"702773951339", invalidOgrn()},
                {"802773951339", invalidOgrn()},
                {"902773951339", invalidOgrn()},

                // ОГРНИП
                {"304540707500034", null},
                {"304540220800032", null},
                {"309774611900857", null},
                {"310253706100022", null},

                {"310253706100021", invalidOgrn()},

                // Недопустимая первая цифра
                {"010253706100022", invalidOgrn()},
                {"410253706100022", invalidOgrn()},
                {"610253706100022", invalidOgrn()},
                {"710253706100022", invalidOgrn()},
                {"810253706100022", invalidOgrn()},
                {"910253706100022", invalidOgrn()},

        });
    }

    @Test
    public void testOgrnConstraint() {
        Defect defect = ogrnIsValid().apply(ogrn);
        if (expectedDefect == null) {
            assertThat("констрейнт не должен выдавать ошибку на огрн = " + ogrn, defect, nullValue());
        } else {
            assertThat("ошибка для огрн = " + ogrn + " не соответствует ожидаемой",
                    defect, is(expectedDefect));
        }
    }
}
