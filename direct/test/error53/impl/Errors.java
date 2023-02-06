package ru.yandex.autotests.directapi.test.error53.impl;

import ru.yandex.autotests.directapi.apiclient.errors.Api5Error;
import ru.yandex.autotests.directapi.apiclient.errors.Api5ErrorDetails;

public final class Errors {
    public static Api5Error accessToApiDeniedError() {
        return new Api5Error(3000, Api5ErrorDetails.ACCOUNT_BLOCKED);
    }

    public static Api5Error accessToApiDeniedNotAllowedIPError() {
        return new Api5Error(3000, Api5ErrorDetails.ACCESS_DENIED_NOT_ALLOWED_IP);
    }

    public static Api5Error accessToApiDeniedWhileConvertingCurrencyError() {
        return new Api5Error(3000, Api5ErrorDetails.ACCESS_DENIED_WHILE_CONVERTING_CURRENCY);
    }

    public static Api5Error accessToApiDeniedForYaAgencyError() {
        return new Api5Error(3000, Api5ErrorDetails.ACCESS_DENIED_FOR_YA_AGENCY);
    }

    public static Api5Error notClientInClientLoginError() {
        return new Api5Error(8000, Api5ErrorDetails.NOT_CLIENT_IN_CLIENT_LOGIN);
    }

    public static Api5Error unknownLoginInClientLoginError() {
        return new Api5Error(8800, Api5ErrorDetails.UNKNOWN_LOGIN_IN_CLIENT_LOGIN);
    }
}
