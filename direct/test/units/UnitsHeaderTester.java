package ru.yandex.autotests.directapi.test.units;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.directapi.rules.ApiSteps;

import static org.hamcrest.Matchers.lessThan;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Класс, проверяющий заголовок ответа {@code Units} в двух звпросах.
 * Предназначен для проверки работы механизма списания баллов на проде.
 */
public class UnitsHeaderTester {

    private static final Logger log = LoggerFactory.getLogger(UnitsHeaderTester.class);

    private static UnitsLock lock = UnitsLock.INSTANCE;

    private static int LOCK_TIMEOUT_MINUTES = 1;

    private final ApiSteps api;

    public static UnitsHeaderTester newInstance(ApiSteps api) {
        return new UnitsHeaderTester(api);
    }

    private UnitsHeaderTester(ApiSteps api) {
        this.api = api;
    }

    public void init() {
        lock.acquire(LOCK_TIMEOUT_MINUTES);
    }

    /**
     * Проверить списание баллов, используя лишь значения заголовков {@code Units} между двумя запросами.
     *
     * @param serviceCall      вызов сервиса, возвращающий значение заголовка {@code Units}
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

        String headerBefore = serviceCall.get();
        String headerAfter = serviceCall.get();

        int unitsBefore = getUnitsBalance(headerBefore);
        int unitsAfter = getUnitsBalance(headerAfter);

        log.info("Header value before: {}, units balance: {}", headerBefore, unitsBefore);
        log.info("Header value after: {}, units balance: {}", headerAfter, unitsAfter);

        assertThat("Баллы списались", unitsAfter, lessThan(unitsBefore));
    }

    private int getUnitsBalance(String unitsHeaderValue) {
        return Integer.parseInt(unitsHeaderValue.split("/")[1]);
    }

    public void shutdown() {
        lock.release();
    }

}
