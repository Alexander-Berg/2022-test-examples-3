package ru.yandex.market.communication.proxy.telephony;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.service.environment.EnvironmentService;

public class IsRedirectEnabledCalculatorTest extends AbstractCommunicationProxyTest {

    @Autowired
    private EnvironmentService environmentService;

    @ParameterizedTest(name = "{0}")
    @MethodSource("getTestDataWithTelephonyForAll")
    @DbUnitDataSet(before = "isRedirectEnabledCalculatorTest.allEnbled.before.csv")
    void testWithTelephonyForAll(String testName, long partnerId, boolean expectedResult) {
        Assertions.assertEquals(expectedResult, new IsRedirectEnabledCalculator(environmentService).apply(partnerId));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getTestDataWithTelephonyForSpecific")
    @DbUnitDataSet(before = "isRedirectEnabledCalculatorTest.specificEnbled.before.csv")
    void testWithTelephonyForSpecific(String testName, long partnerId, boolean expectedResult) {
        Assertions.assertEquals(expectedResult, new IsRedirectEnabledCalculator(environmentService).apply(partnerId));
    }

    public static Stream<Arguments> getTestDataWithTelephonyForAll() {
        return Stream.of(
                Arguments.of("Тест рандомного магзина при включенном проксировании для всех", 10L, true),
                Arguments.of("Тест магазина из белого списка при включенном проксировании для всех", 1L, true),
                Arguments.of("Тест магазина из черного списка при включенном проксировании для всех 1", 3L, false),
                Arguments.of("Тест магазина из черного списка при включенном проксировании для всех 2", 2L, false)
        );
    }

    public static Stream<Arguments> getTestDataWithTelephonyForSpecific() {
        return Stream.of(
                Arguments.of("Тест магазин не из белого листа", 10L, false),
                Arguments.of("Тест магазина из пересечения белого и черного листов", 2L, false),
                Arguments.of("Тест магазина из белого листа", 1L, true)
        );
    }

}
