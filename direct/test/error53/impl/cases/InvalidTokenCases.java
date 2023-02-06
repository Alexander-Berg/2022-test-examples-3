package ru.yandex.autotests.directapi.test.error53.impl.cases;

import ru.yandex.autotests.directapi.apiclient.errors.Api5Error;
import ru.yandex.autotests.directapi.apiclient.errors.Api5ErrorDetails;
import ru.yandex.autotests.directapi.test.error53.impl.Logins;
import ru.yandex.autotests.directapi.test.error53.impl.TestCaseBuilder;

import static ru.yandex.autotests.directapi.test.error53.impl.TestActions.invokeGetMethod;

public final class InvalidTokenCases {
    private static final String INVALID_TOKEN = "$abc$";

    public static Object[] expiredToken() {
        return new TestCaseBuilder()
                .withDescription("Недействительный OAuth-токен")
                .withOperator(Logins.CLIENT_WITH_EXPIRED_TOKEN)
                .withTestAction(invokeGetMethod())
                .withExpectedError(new Api5Error(53, Api5ErrorDetails.EXPIRED_OAUTH_TOKEN))
                .build();
    }

    public static Object[] absentToken() {
        return new TestCaseBuilder()
                .withDescription("OAuth-токен не указан")
                .withOperator(Logins.CLIENT)
                .withApiToken(null)
                .withTestAction(invokeGetMethod())
                .withExpectedError(new Api5Error(8000, Api5ErrorDetails.ABSENT_OAUTH_TOKEN))
                .build();
    }

    public static Object[] invalidTokenFormat() {
        return new TestCaseBuilder()
                .withDescription("Неверный формат OAuth-токена")
                .withOperator(Logins.CLIENT)
                .withApiToken(INVALID_TOKEN)
                .withTestAction(invokeGetMethod())
                .withExpectedError(new Api5Error(8000, Api5ErrorDetails.INVALID_OAUTH_TOKEN_FORMAT))
                .build();
    }
}
