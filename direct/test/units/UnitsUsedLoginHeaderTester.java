package ru.yandex.autotests.directapi.test.units;

import java.util.Collection;
import java.util.function.Supplier;

import ru.yandex.autotests.directapi.rules.ApiSteps;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.clearSpendUnits;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.setManualUnitsLimit;
import static ru.yandex.autotests.directapi.test.units.UnitsUtils.spendAllUnits;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.BRAND_CHIEF_LOGIN;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.BRAND_MEMBER_LOGIN;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTestData.INACTIVE_UNITS_LIMIT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Класс, проверяющий заголовок ответа {@code Units-Used-Login}.
 */
public class UnitsUsedLoginHeaderTester {

    private static UnitsLock lock = UnitsLock.INSTANCE;

    private static final int LOCK_TIMEOUT_MINUTES = 1;

    private final ApiSteps api;

    public static UnitsUsedLoginHeaderTester newInstance(ApiSteps api) {
        return new UnitsUsedLoginHeaderTester(api);
    }

    private UnitsUsedLoginHeaderTester(ApiSteps api) {
        this.api = api;
    }

    public void init() {
        lock.acquire(LOCK_TIMEOUT_MINUTES);

        setManualUnitsLimit(api, BRAND_MEMBER_LOGIN, INACTIVE_UNITS_LIMIT);
        setManualUnitsLimit(api, BRAND_CHIEF_LOGIN, INACTIVE_UNITS_LIMIT);
    }

    public void reset(Collection<String> loginsWithoutUnits, Collection<String> loginsToClear) {
        loginsWithoutUnits.forEach(login -> spendAllUnits(api, login));
        loginsToClear.forEach(login -> clearSpendUnits(api, login));
    }


    /**
     * Вызвать сервис и осуществить проверку заголовка {@code Units-Used-Login}.
     * Проверка состоит в том, что логин из заголовка является логином
     * либо оператора (владельца токена), либо клиента (Client-Login).
     *
     * @param serviceCall      вызов сервиса, возвращающий значение заголовка
     *                         {@code Units-Used-Login}
     * @param operatorLogin    логин владельца токена
     * @param clientLogin      значение заголовка запроса {@code Client-Login}
     * @param useOperatorUnits значение заголовка запроса {@code Use-Operator-Units}
     */
    public void test(Supplier<String> serviceCall,
            String operatorLogin,
            String clientLogin,
            String useOperatorUnits)
    {
        api
                .as(operatorLogin)
                .clientLogin(clientLogin)
                .useOperatorUnits(useOperatorUnits);

        String unitsUsedLogin = serviceCall.get();

        assertThat("В заголовке Units-Used-Login логин оператора или клиента",
                unitsUsedLogin, anyOf(equalTo(operatorLogin), equalTo(clientLogin)));
    }

    /**
     * Вызвать сервис и осуществить проверку заголовка {@code Units-Used-Login}.
     *
     * @param serviceCall               вызов сервиса, возвращающий значение заголовка {@code Units-Used-Login}
     * @param operatorLogin             логин владельца токена
     * @param clientLogin               значение заголовка запроса {@code Client-Login}
     * @param useOperatorUnits          значение заголовка запроса {@code Use-Operator-Units}
     * @param expectedUnitsUsedLogin    ожидаемое значение заголовка {@code Units-Used-Login}
     */
    public void test(Supplier<String> serviceCall,
            String operatorLogin,
            String clientLogin,
            String useOperatorUnits,
            String expectedUnitsUsedLogin)
    {
        api
                .as(operatorLogin)
                .clientLogin(clientLogin)
                .useOperatorUnits(useOperatorUnits);

        String unitsUsedLogin = serviceCall.get();

        assertThat("В заголовке Units-Used-Login ожидаемый логин",
                unitsUsedLogin, is(expectedUnitsUsedLogin));
    }

    public void shutdown() {
        lock.release();
    }

}
