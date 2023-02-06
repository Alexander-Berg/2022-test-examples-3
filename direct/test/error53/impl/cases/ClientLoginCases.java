package ru.yandex.autotests.directapi.test.error53.impl.cases;

import ru.yandex.autotests.directapi.test.error53.impl.Logins;
import ru.yandex.autotests.directapi.test.error53.impl.TestCaseBuilder;

import static ru.yandex.autotests.directapi.test.error53.impl.Errors.notClientInClientLoginError;
import static ru.yandex.autotests.directapi.test.error53.impl.Errors.unknownLoginInClientLoginError;
import static ru.yandex.autotests.directapi.test.error53.impl.TestActions.invokeGetMethod;

public final class ClientLoginCases {
    public static Object[] agencySuccess() {
        return new TestCaseBuilder()
                .withDescription("Существующий клиент агенства в Client-Login")
                .withOperator(Logins.AGENCY)
                .withClientLogin(Logins.AGENCY_CLIENT)
                .withTestAction(invokeGetMethod())
                .build();
    }

    public static Object[] agencyAbsentClientLogin() {
        return new TestCaseBuilder()
                .withDescription("Client-Login не задан")
                .withOperator(Logins.AGENCY)
                .withTestAction(invokeGetMethod())
                .withExpectedError(notClientInClientLoginError())
                .build();
    }

    public static Object[] agencyUnknownLogin() {
        return new TestCaseBuilder()
                .withDescription("Несуществующий клиент в Client-Login")
                .withOperator(Logins.AGENCY)
                .withClientLogin(Logins.UNKNOWN_LOGIN)
                .withTestAction(invokeGetMethod())
                .withExpectedError(unknownLoginInClientLoginError())
                .build();
    }

    public static Object[] agencyDirectClient() {
        return new TestCaseBuilder()
                .withDescription("Прямой клиент в Client-Login")
                .withOperator(Logins.AGENCY)
                .withClientLogin(Logins.CLIENT)
                .withTestAction(invokeGetMethod())
                .withExpectedError(unknownLoginInClientLoginError())
                .build();
    }

    public static Object[] agencyAnotherAgencyClient() {
        return new TestCaseBuilder()
                .withDescription("Клиент другого агенства в Client-Login")
                .withOperator(Logins.AGENCY)
                .withClientLogin(Logins.ANOTHER_AGENCY_CLIENT)
                .withTestAction(invokeGetMethod())
                .withExpectedError(unknownLoginInClientLoginError())
                .build();
    }

    public static Object[] simpleClientReplSuccess() {
        return new TestCaseBuilder()
                .withDescription("Обычный клиент с представителем в Client-Login")
                .withOperator(Logins.CLIENT)
                .withClientLogin(Logins.CLIENT_REP)
                .withTestAction(invokeGetMethod())
                .build();
    }

    public static Object[] simpleClientMainReplSuccess() {
        return new TestCaseBuilder()
                .withDescription("Обычный клиент с представителем в Client-Login")
                .withOperator(Logins.CLIENT_REP)
                .withClientLogin(Logins.CLIENT)
                .withTestAction(invokeGetMethod())
                .build();
    }

    public static Object[] simpleClientUnknownLogin() {
        return new TestCaseBuilder()
                .withDescription("Обычный клиент с несуществующим клиентом в Client-Login")
                .withOperator(Logins.CLIENT)
                .withClientLogin(Logins.UNKNOWN_LOGIN)
                .withTestAction(invokeGetMethod())
                .withExpectedError(unknownLoginInClientLoginError())
                .build();
    }

    public static Object[] simpleClientAnotherClient() {
        return new TestCaseBuilder()
                .withDescription("Обычный клиент с непредставителем в Client-Login")
                .withOperator(Logins.CLIENT)
                .withClientLogin(Logins.ANOTHER_CLIENT)
                .withTestAction(invokeGetMethod())
                .withExpectedError(unknownLoginInClientLoginError())
                .build();
    }

    public static Object[] expectedSimpleClient() {
        return new TestCaseBuilder()
                .withDescription("Ожидается обычный клиент в Client-Login")
                .withOperator(Logins.SUPER)
                .withClientLogin(Logins.AGENCY)
                .withTestAction(invokeGetMethod())
                .withExpectedError(notClientInClientLoginError())
                .build();
    }
}
