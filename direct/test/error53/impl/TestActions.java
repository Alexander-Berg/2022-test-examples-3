package ru.yandex.autotests.directapi.test.error53.impl;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeThat;

public final class TestActions {
    public static TestAction invokeGetMethod() {
        return (api, config, authenticationInfo, expectedError) -> {
            api.as(authenticationInfo.getOperator())
                    .clientLogin(authenticationInfo.getClientLogin())
                    .token(authenticationInfo.getApiToken())
                    .userSteps
                    .shouldGetErrorOn(
                            config.getServiceName(),
                            null,
                            config.getGetAction(),
                            config.getGetBean(),
                            expectedError);
        };
    }

    public static TestAction invokeNotGetMethod() {
        return (api, config, authenticationInfo, expectedError) -> {
            // Для некоторых сервисов не-get метода может не существовать
            assumeThat(config.getNotGetAction(), not(nullValue()));

            api.as(authenticationInfo.getOperator())
                    .clientLogin(authenticationInfo.getClientLogin())
                    .userSteps
                    .shouldGetErrorOn(
                            config.getServiceName(),
                            null,
                            config.getNotGetAction(),
                            config.getNotGetBean(),
                            expectedError);
        };
    }
}
