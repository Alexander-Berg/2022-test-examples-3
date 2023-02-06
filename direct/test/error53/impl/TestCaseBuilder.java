package ru.yandex.autotests.directapi.test.error53.impl;

import ru.yandex.autotests.directapi.apiclient.errors.Api5Error;
import ru.yandex.autotests.directapi.model.User;

/**
 * Вспомогательный класс для построения test case-ов
 */
public final class TestCaseBuilder {
    /**
     * Описание
     */
    private String description;
    /**
     * Оператор под которым будет выполняться операция
     */
    private String operator;
    /**
     * Клиент от имени которого будет выполняться операция
     */
    private String clientLogin;
    /**
     * Токен с которым будет выполняться обращение к api
     */
    private String apiToken;
    /**
     * Фиктура по подготовке среды для выполнения теста
     */
    private FixtureAction setUpAction;
    /**
     * Тестовое действие
     */
    private TestAction testAction;
    /**
     * Фиктура для очистки среды после выполнения теста
     */
    private FixtureAction tearDownAction;
    /**
     * Ожидаемая ошибка (null - ожидается успех)
     */
    private Api5Error expectedError;
    /**
     * Требуется ли блокировка на время выполнения теста
     */
    private boolean lockRequired;

    public String getDescription() {
        return description;
    }

    public TestCaseBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getOperator() {
        return operator;
    }

    public TestCaseBuilder withOperator(String operator) {
        this.operator = operator;
        this.apiToken = User.get(operator).getToken();
        return this;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public TestCaseBuilder withClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
        return this;
    }

    public String getApiToken() {
        return apiToken;
    }

    public TestCaseBuilder withApiToken(String apiToken) {
        this.apiToken = apiToken;
        return this;
    }

    public FixtureAction getSetUpAction() {
        return setUpAction;
    }

    public TestCaseBuilder withSetUpAction(FixtureAction setUpAction) {
        this.setUpAction = setUpAction;
        return this;
    }

    public TestAction getTestAction() {
        return testAction;
    }

    public TestCaseBuilder withTestAction(TestAction testAction) {
        this.testAction = testAction;
        return this;
    }

    public FixtureAction getTearDownAction() {
        return tearDownAction;
    }

    public TestCaseBuilder withTearDownAction(FixtureAction tearDownAction) {
        this.tearDownAction = tearDownAction;
        return this;
    }

    public Api5Error getExpectedError() {
        return expectedError;
    }

    public TestCaseBuilder withExpectedError(Api5Error expectedError) {
        this.expectedError = expectedError;
        return this;
    }

    public boolean isLockRequired() {
        return lockRequired;
    }

    public TestCaseBuilder withLockRequired(boolean lockRequired) {
        this.lockRequired = lockRequired;
        return this;
    }

    public Object[] build() {
        return new Object[]{
                description,
                operator,
                clientLogin,
                apiToken,
                setUpAction,
                testAction,
                tearDownAction,
                expectedError,
                lockRequired};
    }
}
