package ru.yandex.autotests.direct.cmd.rules;

import org.junit.runner.Description;
import ru.yandex.autotests.direct.utils.BaseSteps;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.apiclient.RequestHeader;
import ru.yandex.autotests.directapi.apiclient.config.ConnectionConfig;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.steps.UserSteps;

/**
 * Рула для апи степов, отличается от оригинальной, что хранит состояние вместо использования singletone
 */
public class ApiStepsRule extends ApiSteps {

    private ApiStepsConfig apiStepsConfig;

    public ApiStepsRule() {
        this(DirectTestRunProperties.getInstance());
    }

    public ApiStepsRule(DirectTestRunProperties properties) {
        this.apiStepsConfig = new ApiStepsConfig(properties);
        this.requestHeader = apiStepsConfig.getRequestHeader();
        this.connectionConfig = apiStepsConfig.getConnectionConfig();
    }

    public ApiStepsConfig apiStepsConfig() {
        apiStepsConfig.withRequestHeader(requestHeader)
                .withConnectionConfig(connectionConfig);
        return apiStepsConfig;
    }

    private ApiUserSteps apiUserSteps() {
        return BaseSteps.getInstance(ApiUserSteps.class, apiStepsConfig());
    }

    public UserSteps userSteps() {
        return apiUserSteps().userSteps();
    }

    @Override
    protected void finished(Description description) {
        userSteps = userSteps();
        super.finished(description);
    }

    /**
     * Метод теперь просто изменяет контекст выполнения тестов
     */
    @Override
    protected void reloadUserSteps() {
        if (apiStepsConfig != null) {
            apiStepsConfig.withRequestHeader(this.requestHeader).withConnectionConfig(this.connectionConfig);
        }
    }

    public static class ApiUserSteps extends BaseSteps<ApiStepsConfig> {
        private UserSteps userSteps;

        @Override
        protected void init(ApiStepsConfig context) {
            super.init(context);
            userSteps = new UserSteps(context.getConnectionConfig(), context.getRequestHeader());
        }

        public UserSteps userSteps() {
            userSteps.setConnectionConfig(getContext().getConnectionConfig());
            userSteps.setRequestHeader(getContext().getRequestHeader());
            return userSteps;
        }
    }

    public static class ApiStepsConfig {
        protected ConnectionConfig connectionConfig;
        protected RequestHeader requestHeader;
        protected DirectTestRunProperties properties;

        public ApiStepsConfig(DirectTestRunProperties properties) {
            this.properties = properties;
            this.connectionConfig = new ConnectionConfig(properties.getDirectAPIHost());
            this.requestHeader = new RequestHeader(properties.getDirectAPILogin());
        }

        public ConnectionConfig getConnectionConfig() {
            return connectionConfig;
        }

        public ApiStepsConfig withConnectionConfig(ConnectionConfig connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }

        public RequestHeader getRequestHeader() {
            return requestHeader;
        }

        public ApiStepsConfig withRequestHeader(RequestHeader requestHeader) {
            this.requestHeader = requestHeader;
            return this;
        }
    }
}
