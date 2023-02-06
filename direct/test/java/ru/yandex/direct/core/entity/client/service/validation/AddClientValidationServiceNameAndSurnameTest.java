package ru.yandex.direct.core.entity.client.service.validation;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.currency.service.CurrencyService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.currency.CurrencyCode.TRY;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class AddClientValidationServiceNameAndSurnameTest {
    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public String login;
    @Parameterized.Parameter(2)
    public String name;
    @Parameterized.Parameter(3)
    public String surname;
    @Parameterized.Parameter(4)
    public long region;
    @Parameterized.Parameter(5)
    public CurrencyCode currencyCode;

    @Parameterized.Parameter(6)
    public Path defectPath;
    @Parameterized.Parameter(7)
    public Defect defect;

    public AddClientValidationService service = new AddClientValidationService(mock(CurrencyService.class));

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"Пустое имя",
                        "login", "", "fio", TURKEY_REGION_ID, TRY, path(field("name")), notEmptyString()},
                new Object[]{"Имя = Null",
                        "login", null, "fio", TURKEY_REGION_ID, TRY, path(field("name")), notNull()},
                new Object[]{"Пустая фамилия",
                        "login", "test", "", TURKEY_REGION_ID, TRY, path(field("surname")), notEmptyString()},
                new Object[]{"Фамилия = Null",
                        "login", "test", null, TURKEY_REGION_ID, TRY, path(field("surname")), notNull()},

                // один кейс для проверки, что вызывается checkCommonFields
                new Object[]{"Пустой логин",
                        "", "test", "fio", TURKEY_REGION_ID, TRY, path(field("login")), notEmptyString()}
        );
    }

    @Test
    public void testNameAndSurnameValidation() {
        ValidationResult<Object, Defect> vr =
                service.validateAddClientRequest(LoginOrUid.of(login, null), name, surname, region, currencyCode);
        assertThat(vr, hasDefectDefinitionWith(validationError(defectPath, defect)));
    }
}
