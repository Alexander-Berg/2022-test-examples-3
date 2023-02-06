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
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class AddClientValidationServiceCommonFieldsTest {
    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public LoginOrUid loginOrUid;
    @Parameterized.Parameter(2)
    public long region;
    @Parameterized.Parameter(3)
    public CurrencyCode currencyCode;

    @Parameterized.Parameter(4)
    public Path defectPath;
    @Parameterized.Parameter(5)
    public Defect defect;

    public AddClientValidationService service = new AddClientValidationService(mock(CurrencyService.class));

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"Пустой логин",
                        LoginOrUid.of(""), TURKEY_REGION_ID, TRY, path(field("login")), notEmptyString()},
                new Object[]{"Некорректный uid",
                        LoginOrUid.of(-1L), TURKEY_REGION_ID, TRY, path(field("uid")), validId()},
                new Object[]{"Некорректный регион",
                        LoginOrUid.of("login"), GLOBAL_REGION_ID, TRY, path(field("country")), validId()},
                new Object[]{"Не указана валюта",
                        LoginOrUid.of("login"), TURKEY_REGION_ID, null, path(field("currency")), notNull()},
                new Object[]{"Фишечный клиент", LoginOrUid.of("login"), RUSSIA_REGION_ID, YND_FIXED,
                        path(field("currency")), invalidValue()}
        );
    }

    @Test
    public void testCommonFieldValidation() {
        ValidationResult<Object, Defect> vr =
                service.checkCommonFields(new Object(), region, currencyCode, loginOrUid);
        assertThat(vr, hasDefectDefinitionWith(validationError(defectPath, defect)));
    }
}
