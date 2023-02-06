package ru.yandex.autotests.directapi.test.units;

import ru.yandex.autotests.directapi.rules.ApiSteps;

import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTesterImpl.DEFAULT_LOCK_TIMEOUT_MINUTES;

public class UnitsWithdrawalTesterBuilder {

    private ApiSteps api;
    private boolean useManualUnits = false;
    private int lockTimeoutMinutes = DEFAULT_LOCK_TIMEOUT_MINUTES;

    public UnitsWithdrawalTesterBuilder api(ApiSteps api) {
        this.api = api;
        return this;
    }

    public UnitsWithdrawalTesterBuilder useManualUnits(boolean useManualUnits) {
        this.useManualUnits = useManualUnits;
        return this;
    }

    public UnitsWithdrawalTesterBuilder lockTimeoutMinutes(int lockTimeoutMinutes) {
        this.lockTimeoutMinutes = lockTimeoutMinutes;
        return this;
    }

    public UnitsWithdrawalTesterImpl build() {
        return new UnitsWithdrawalTesterImpl(api, useManualUnits, lockTimeoutMinutes);
    }

}
