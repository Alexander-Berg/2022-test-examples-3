package ru.yandex.autotests.directapi.test.error53.impl.cases;

import ru.yandex.autotests.directapi.test.error53.impl.Logins;
import ru.yandex.autotests.directapi.test.error53.impl.TestCaseBuilder;

import static ru.yandex.autotests.directapi.test.error53.impl.Errors.*;
import static ru.yandex.autotests.directapi.test.error53.impl.FixtureActions.*;
import static ru.yandex.autotests.directapi.test.error53.impl.TestActions.invokeGetMethod;
import static ru.yandex.autotests.directapi.test.error53.impl.TestActions.invokeNotGetMethod;

public final class ApiAccessCases {
    public static Object[] clientWithStatusBlocked() {
        return new TestCaseBuilder()
                .withDescription("Заблокированный клиент (StatusBlocked = Yes)")
                .withOperator(Logins.CLIENT_WITH_STATUS_BLOCKED)
                .withSetUpAction(blockOperatorUser())
                .withTestAction(invokeGetMethod())
                .withExpectedError(accessToApiDeniedError())
                .build();
    }

    public static Object[] clientWithBlockedApiAccess() {
        return new TestCaseBuilder()
                .withDescription("Клиент с заблокированным доступом к API")
                .withOperator(Logins.CLIENT_WITH_BLOCKED_API_ACCESS)
                .withSetUpAction(blockApiAccessToOperator())
                .withTestAction(invokeGetMethod())
                .withExpectedError(accessToApiDeniedError())
                .build();
    }

    public static Object[] clientWithNotAllowedIP() {
        return new TestCaseBuilder()
                .withDescription("Клиент с недопустимым IP-адресом")
                .withOperator(Logins.CLIENT_WITH_NOT_ALLOWED_IP)
                .withSetUpAction(blockOperatorByIpAddress())
                .withTestAction(invokeGetMethod())
                .withExpectedError(accessToApiDeniedNotAllowedIPError())
                .build();
    }

    public static Object[] yaAgencyClient() {
        return new TestCaseBuilder()
                .withDescription("YaAgencyClient")
                .withOperator(Logins.YA_AGENCY_CLIENT)
                .withSetUpAction(makeOperatorYaAgencyClient())
                .withTestAction(invokeGetMethod())
                .withExpectedError(accessToApiDeniedForYaAgencyError())
                .build();
    }

    public static Object[] clientWithConvertingCurrency() {
        return new TestCaseBuilder()
                .withDescription("Клиент при переводе кампаний в валюту")
                .withOperator(Logins.CLIENT_WITH_CONVERTING_CURRENCY)
                .withSetUpAction(startConvertingCurrencyForOperator())
                .withTestAction(invokeGetMethod())
                .withTearDownAction(stopConvertingCurrencyForOperator())
                .withExpectedError(accessToApiDeniedWhileConvertingCurrencyError())
                .build();
    }

    public static Object[] agencyClientWithStatusBlockedGetMethod() {
        return new TestCaseBuilder()
                .withDescription("Заблокированный клиент агенства (StatusBlocked = Yes)")
                .withOperator(Logins.AGENCY)
                .withClientLogin(Logins.AGENCY_CLIENT_WITH_STATUS_BLOCKED)
                .withSetUpAction(blockSubclientUser())
                .withTestAction(invokeGetMethod())
                .build();
    }

    public static Object[] agencyClientWithStatusBlockedNotGetMethod() {
        return new TestCaseBuilder()
                .withDescription("Заблокированный клиент агенства (StatusBlocked = Yes)")
                .withOperator(Logins.AGENCY)
                .withClientLogin(Logins.AGENCY_CLIENT_WITH_STATUS_BLOCKED)
                .withSetUpAction(blockSubclientUser())
                .withTestAction(invokeNotGetMethod())
                .withExpectedError(accessToApiDeniedError())
                .build();
    }
}
