package ru.yandex.autotests.directapi.test.units;

import java.util.Collection;

/**
 * Потребитель тестовых данных по списанию баллов.
 * <p>
 * Предполагается, что это тестовые классы.
 */
public interface UnitsWithdrawalTestDataConsumer {

    void setDescription(String description);

    void setOperatorLogin(String operatorLogin);

    void setClientLogin(String clientLogin);

    void setUseOperatorUnits(String useOperatorUnits);

    void setUnitsDelta(int unitsDelta);

    void setExpectedUnitsWithdrawLogins(Collection<String> expectedUnitsWithdrawLogins);

    void setExpectedUnitsKeepLogins(Collection<String> expectedUnitsKeepLogins);

}
