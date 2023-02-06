package ru.yandex.autotests.directapi.test.error53.impl;

import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.test.error53.Error53TestConfig;

@FunctionalInterface
public interface FixtureAction {
    void invoke(ApiSteps api, AuthenticationInfo authenticationInfo);
}
