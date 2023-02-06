package ru.yandex.direct.core.entity.vcard.service.validation;

import java.util.Collection;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.Phone;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.vcard.Phones.phone;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.countryCodeMustNotStartWithPlus;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.countryCodeMustStartWithPlus;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.emptyCityCode;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.emptyCountryCode;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.emptyPhoneNumber;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.invalidCityCodeFormat;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.invalidCountryCodeFormat;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.invalidEntirePhoneLength;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.invalidExtensionFormat;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.invalidPhoneNumberFormat;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.nullCityCode;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.nullCountryCode;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.nullPhoneNumber;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.tooLongCityCode;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.tooLongCountryCode;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.tooLongEntirePhoneWithExtension;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.tooLongExtension;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.tooLongPhoneNumber;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.DefectDefinitions.tooShortPhoneNumber;
import static ru.yandex.direct.core.entity.vcard.service.validation.PhoneValidator.phoneIsValid;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class PhoneValidatorTest {
    @Parameterized.Parameter(0)
    public Phone phone;

    @Parameterized.Parameter(1)
    public Matcher<DefectInfo<Defect>> defectInfoMatcher;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{

                /*
                    позитивные кейсы
                 */

                // код страны
                {phone("+1", "921", "7777777", "123"), null},
                {phone("+999", "921", "7777777", "123"), null},
                {phone("+15", "921", "7777777", "123"), null},
                {phone("8", "800", "7777777", "123"), null},    // специальная комбинация
                {phone("8", "804", "7777777", "123"), null},    // специальная комбинация
                {phone("0", "800", "7777777", "123"), null},    // специальная комбинация
                {phone("+8", "458", "7777777", "123"), null},    // специальный код страны

                // код города
                {phone("+7", "1", "7777777", "123"), null},
                {phone("+7", "00", "7777777", "123"), null},
                {phone("+7", "00000", "7777777", "123"), null},
                {phone("+7", "00001", "7777777", "123"), null},
                {phone("+7", "99999", "7777777", "123"), null},
                {phone("+7", "812", "7777777", "123"), null},
                {phone("+73", "800", "7777777", "123"), null},  // специальный код города

                // код города может быть пустой
                {phone("+90", "", "444-7777", "123"), null},  // специальный код города

                // номер телефона
                {phone("+7", "81212", "00000", "123"), null},
                {phone("+7", "81212", "00001", "123"), null},
                {phone("+7", "812", "000000000", "123"), null},
                {phone("+7", "812", "999999999", "123"), null},
                {phone("+7", "812", "7777777", "123"), null},
                {phone("+7", "812", "777-77-77", "123"), null},

                // добавочный номер
                {phone("+7", "812", "7777777", null), null},
                {phone("+7", "812", "7777777", ""), null},
                {phone("+7", "812", "7777777", "0"), null},
                {phone("+7", "812", "7777777", "00000"), null},
                {phone("+7", "812", "7777777", "99999"), null},
                {phone("+7", "812", "7777777", "123"), null},
                {phone("+7", "812", "7777777", "123456"), null},

                // суммарная длина номера телефона
                {phone("+777", "8888", "777777777", "123"), null},
                {phone("+77", "88888", "777777777", "123"), null},
                {phone("+777", "88888", "77777777", "123"), null},
                {phone("+777", "88888", "77777777", "123"), null},
                {phone("+777", "88888", "777777777", "123"), null},
                {phone("+7", "888", "777777777", "12345"), null},

                /*
                    негативные кейсы
                 */

                // код страны
                {
                        phone(null, "921", "7777777", "123"),
                        validationError(path(field("countryCode")), nullCountryCode())
                },
                {
                        phone("", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), emptyCountryCode())
                },
                {
                        phone("a", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), invalidCountryCodeFormat())
                },
                {
                        phone("+", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), invalidCountryCodeFormat())
                },
                {
                        phone("+a", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), invalidCountryCodeFormat())
                },
                {
                        phone("++7", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), invalidCountryCodeFormat())
                },
                {
                        phone("0", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustStartWithPlus())
                },
                {
                        phone("1", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustStartWithPlus())
                },
                {
                        phone("35", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustStartWithPlus())
                },
                {
                        phone("8", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustStartWithPlus())
                },
                {
                        phone("+10000", "921", "7777777", "123"),
                        validationError(path(field("countryCode")), tooLongCountryCode())
                },
                {
                        phone("+8", "800", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustNotStartWithPlus("800"))
                },
                {
                        phone("+8", "804", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustNotStartWithPlus("804"))
                },
                {
                        phone("+0", "800", "7777777", "123"),
                        validationError(path(field("countryCode")), countryCodeMustNotStartWithPlus("800"))
                },

                // код города
                {
                        phone("+7", null, "7777777", "123"),
                        validationError(path(field("cityCode")), nullCityCode())
                },
                {
                        phone("+7", "", "7777777", "123"),
                        validationError(path(field("cityCode")), emptyCityCode())
                },
                {
                        phone("+90", "", "7777777", "123"),
                        validationError(path(field("cityCode")), emptyCityCode())
                },
                {
                        phone("+7", "333333", "7777777", "123"),
                        validationError(path(field("cityCode")), tooLongCityCode())
                },
                {
                        phone("+7", "a123", "7777777", "123"),
                        validationError(path(field("cityCode")), invalidCityCodeFormat())
                },
                {
                        phone("+7", "+1", "7777777", "123"),
                        validationError(path(field("cityCode")), invalidCityCodeFormat())
                },
                {
                        phone("+7", "0", "7777777", "123"),
                        validationError(path(field("cityCode")), invalidCityCodeFormat())
                },

                // номер телефона
                {
                        phone("+7", "812", null, "123"),
                        validationError(path(field("phoneNumber")), nullPhoneNumber())
                },
                {
                        phone("+7", "812", "", "123"),
                        validationError(path(field("phoneNumber")), emptyPhoneNumber())
                },
                {
                        phone("+7", "812", "1234", "123"),
                        validationError(path(field("phoneNumber")), tooShortPhoneNumber())
                },
                {
                        phone("+7", "812", "1234567890", "123"),
                        validationError(path(field("phoneNumber")), tooLongPhoneNumber())
                },
                {
                        phone("+7", "812", "a12345", "123"),
                        validationError(path(field("phoneNumber")), invalidPhoneNumberFormat())
                },
                {
                        phone("+7", "812", "+123456", "123"),
                        validationError(path(field("phoneNumber")), invalidPhoneNumberFormat())
                },

                // добавочный номер
                {
                        phone("+7", "812", "7777777", "1234567"),
                        validationError(path(field("extension")), tooLongExtension())
                },
                {
                        phone("+7", "812", "7777777", "a123"),
                        validationError(path(field("extension")), invalidExtensionFormat())
                },
                {
                        phone("+7", "812", "7777777", "+123"),
                        validationError(path(field("extension")), invalidExtensionFormat())
                },

                // суммарная длина номера телефона
                {
                        phone("+7", "8", "77777", null),
                        validationError(path(), invalidEntirePhoneLength())
                },
                {
                        phone("+3801", "12345", "123456789", null),
                        validationError(path(), invalidEntirePhoneLength())
                },
                {
                        phone("+7", "123", "123456789", "123456"),
                        validationError(path(), tooLongEntirePhoneWithExtension())
                },
        });
    }

    @Test
    public void testPhoneValidator() {
        ValidationResult<Phone, Defect> vr = phoneIsValid().apply(phone);
        if (defectInfoMatcher != null) {
            assertThat("результат валидации должен содержать ошибку",
                    vr,
                    hasDefectDefinitionWith(defectInfoMatcher));
        } else {
            assertThat("результат валидации должен быть полностью положительным",
                    vr.hasAnyErrors(),
                    is(false));
        }
    }
}
