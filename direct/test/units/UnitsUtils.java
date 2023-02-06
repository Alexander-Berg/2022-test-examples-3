package ru.yandex.autotests.directapi.test.units;

import ru.yandex.autotests.directapi.rules.ApiSteps;

public class UnitsUtils {

    public static Long getUid(ApiSteps api, String login) {
        return api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).usersSteps().getUser(login).getUid();
    }

    public static void setManualUnitsLimit(ApiSteps api, String login, long value) {
        api.userSteps.getDirectJooqDbSteps().useShardForLogin(login).usersApiOptionsSteps()
                .setUserApiUnitsDailyLimit(getUid(api, login), value);
    }

    public static void clearSpendUnits(ApiSteps api, String login) {
        api.userSteps.clientFakeSteps().fakeClearClientSpentUnits(login);
    }

    public static long getUnitsBalance(ApiSteps api, String login) {
        return api.userSteps.clientFakeSteps().fakeClientUnitsBalance(login);
    }

    public static void spendAllUnits(ApiSteps api, String login) {
        api.userSteps.clientFakeSteps().fakeWithdrawClientUnits(login, (int) getUnitsBalance(api, login));
    }
}
