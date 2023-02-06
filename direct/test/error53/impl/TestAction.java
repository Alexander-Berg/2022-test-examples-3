package ru.yandex.autotests.directapi.test.error53.impl;

import ru.yandex.autotests.directapi.apiclient.errors.Api5Error;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.test.error53.Error53TestConfig;

@FunctionalInterface
public interface TestAction {
    void invokeAndAssert(
            ApiSteps api,
            Error53TestConfig config,
            AuthenticationInfo authenticationInfo,
            Api5Error expectedError);
}
