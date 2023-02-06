package ru.yandex.direct.core.entity.vcard.service.validation;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.InstantMessenger;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.vcard.service.validation.InstantMessengerValidator.ICQ;
import static ru.yandex.direct.core.entity.vcard.service.validation.InstantMessengerValidator.JABBER;
import static ru.yandex.direct.core.entity.vcard.service.validation.InstantMessengerValidator.MAIL_AGENT;
import static ru.yandex.direct.core.entity.vcard.service.validation.InstantMessengerValidator.MSN;
import static ru.yandex.direct.core.entity.vcard.service.validation.InstantMessengerValidator.SKYPE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class InstantMessegerValidatorTest {
    @Parameterized.Parameter
    public String type;
    @Parameterized.Parameter(value = 1)
    public String login;
    @Parameterized.Parameter(value = 2)
    public Matcher<ValidationResult<InstantMessenger, Defect>> acceptedByMatcher;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{ICQ.toLowerCase(), "123-45-67", hasNoDefectsDefinitions()},
                new Object[]{ICQ.toUpperCase(), "123-45-67", hasNoDefectsDefinitions()},

                new Object[]{MAIL_AGENT, "login@mail.ru", hasNoDefectsDefinitions()},
                new Object[]{MAIL_AGENT, "login@inbox.ru", hasNoDefectsDefinitions()},
                new Object[]{MAIL_AGENT, "login@list.ru", hasNoDefectsDefinitions()},
                new Object[]{MAIL_AGENT, "login@bk.ru", hasNoDefectsDefinitions()},
                new Object[]{JABBER, "login@jabber.ru", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login12345", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login.12345", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login-12345", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login_12345.a", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login.12345.a", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login12345@outlook.com", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login.12345@outlook.com", hasNoDefectsDefinitions()},
                new Object[]{SKYPE, "login-12345@outlook.com", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login12345", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login.12345", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login-12345", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login_12345.a", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login.12345.a", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login12345@outlook.com", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login.12345@outlook.com", hasNoDefectsDefinitions()},
                new Object[]{MSN, "login-12345@outlook.com", hasNoDefectsDefinitions()},

                new Object[]{
                        null,
                        "login",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.TYPE.name())),
                                        InstantMessengerValidator.VoidDefectIds.TYPE_IS_NULL))},
                new Object[]{
                        "messenger", "login",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.TYPE.name())),
                                        InstantMessengerValidator.VoidDefectIds.UNSUPPORTED_TYPE))},

                // Null строка
                new Object[]{
                        ICQ, null,
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.LOGIN_IS_NULL))},
                // Слишком длинный login
                new Object[]{
                        ICQ, StringUtils.repeat('a', InstantMessengerValidator.LOGIN_MAX_LENGTH + 1),
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.StringDefectIds.LOGIN_IS_TOO_LONG))},

                // Пустая строка
                new Object[]{
                        ICQ, "",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_ICQ_LOGIN_FORMAT))},
                // Недопустимые символы
                new Object[]{
                        ICQ, "123abc",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_ICQ_LOGIN_FORMAT))},
                // Меньше 5 цифр
                new Object[]{
                        ICQ, "1234",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_ICQ_LOGIN_FORMAT))},
                // Меньше 5 цифр с дефисами
                new Object[]{
                        ICQ, "1-2-3-4",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_ICQ_LOGIN_FORMAT))},
                // Больше 10 цифр
                new Object[]{
                        ICQ, "012-344-567-89-0",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_ICQ_LOGIN_FORMAT))},

                // Пустая строка
                new Object[]{
                        MAIL_AGENT, "",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_MAIL_AGENT_LOGIN_FORMAT))},
                // Не email
                new Object[]{
                        MAIL_AGENT, "login",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_MAIL_AGENT_LOGIN_FORMAT))},
                // Недопустимый домен
                new Object[]{
                        MAIL_AGENT, "login@gmail.com",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_MAIL_AGENT_LOGIN_FORMAT))},

                // Пустая строка
                new Object[]{
                        JABBER, "",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_JABBER_LOGIN_FORMAT))},
                // Не email
                new Object[]{
                        JABBER, "login",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_JABBER_LOGIN_FORMAT))},

                // Пустая строка
                new Object[]{
                        SKYPE, "",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_SKYPE_OR_MSN_LOGIN_FORMAT))},
                // Недопустимые символы
                new Object[]{
                        SKYPE, "123\tabc",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_SKYPE_OR_MSN_LOGIN_FORMAT))},
                // Недопустимые символы
                new Object[]{
                        SKYPE, "123\tabc@outlook.com",
                        hasDefectWithDefinition(
                                validationError(
                                        path(field(InstantMessenger.LOGIN.name())),
                                        InstantMessengerValidator.VoidDefectIds.INVALID_SKYPE_OR_MSN_LOGIN_FORMAT))});
    }

    @Test
    public void test() {
        ValidationResult<InstantMessenger, Defect> actualResult =
                InstantMessengerValidator.instantMessengerValidator()
                        .apply(new InstantMessenger().withType(type).withLogin(login));
        assertThat(actualResult, acceptedByMatcher);
    }
}
