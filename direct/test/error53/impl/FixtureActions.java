package ru.yandex.autotests.directapi.test.error53.impl;

import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.UsersStatusblocked;
import ru.yandex.autotests.directapi.rules.ApiSteps;

import java.util.Objects;

public final class FixtureActions {
    private static Long getUidByLogin(ApiSteps api, String login) {
        return api.userSteps.getDirectJooqDbSteps()
                .shardingSteps()
                .getUidByLogin(login);

    }

    private static Long getClientIdByLogin(ApiSteps api, String login) {
        return api.userSteps.getDirectJooqDbSteps()
                .shardingSteps()
                .getClientIdByLogin(login);

    }

    private static void blockUserByLogin(ApiSteps api, String login) {
        Objects.requireNonNull(login, "login");

        api.userSteps.getDirectJooqDbSteps()
                .useShardForLogin(login)
                .usersSteps()
                .setBlocked(getUidByLogin(api, login), UsersStatusblocked.Yes);
    }

    public static FixtureAction blockOperatorUser() {
        return (api, authenticationInfo) -> {
            blockUserByLogin(api, authenticationInfo.getOperator());
        };
    }

    public static FixtureAction blockSubclientUser() {
        return (api, authenticationInfo) -> {
            blockUserByLogin(api, authenticationInfo.getClientLogin());
        };
    }

    public static FixtureAction blockApiAccessToOperator() {
        return (api, authenticationInfo) -> {
            api.userSteps.getDirectJooqDbSteps()
                    .useShardForLogin(authenticationInfo.getOperator())
                    .clientsApiOptionsSteps()
                    .disableApiAccess(getClientIdByLogin(api, authenticationInfo.getOperator()));
        };
    }

    public static FixtureAction makeOperatorYaAgencyClient() {
        return (api, authenticationInfo) -> {
            api.userSteps.getDirectJooqDbSteps()
                    .useShardForLogin(authenticationInfo.getOperator())
                    .clientsOptionsSteps()
                    .makeYaAgencyClient(getClientIdByLogin(api, authenticationInfo.getOperator()));
        };
    }

    public static FixtureAction blockOperatorByIpAddress() {
        return (api, authenticationInfo) -> {
            api.userSteps.getDirectJooqDbSteps()
                    .useShardForLogin(authenticationInfo.getOperator())
                    .usersApiOptionsSteps()
                    .setAllowedIps(
                            getUidByLogin(api, authenticationInfo.getOperator()),
                            // Нам нужно задать любое недопустимое значение
                            "255.255.255.255");
        };
    }

    public static FixtureAction startConvertingCurrencyForOperator() {
        return (api, authenticationInfo) -> {
            api.userSteps.getDirectJooqDbSteps()
                    .useShardForLogin(authenticationInfo.getOperator())
                    .currencyConvertQueueSteps()
                    .startConvertingCurrency(
                            getClientIdByLogin(api, authenticationInfo.getOperator()),
                            // По умолчанию блокировка клиента происходит за 15 минут, до
                            // начала конвертации, мы ставим 10, чтобы сработало это условие
                            // и у нас было время на выполнение теста и удаление соотвествующей
                            // записи из очереди до срабатывания обрабочика
                            10);
        };
    }

    public static FixtureAction stopConvertingCurrencyForOperator() {
        return (api, authenticationInfo) -> {
            api.userSteps.getDirectJooqDbSteps()
                    .useShardForLogin(authenticationInfo.getOperator())
                    .currencyConvertQueueSteps()
                    .deleteCurrencyConvertQueue(getClientIdByLogin(api, authenticationInfo.getOperator()));
        };
    }
}
