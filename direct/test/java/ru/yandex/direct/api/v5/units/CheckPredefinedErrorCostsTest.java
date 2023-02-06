package ru.yandex.direct.api.v5.units;

import java.util.Collection;

import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.units.Costs;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.units.ApiUnitsService.UNITS_COSTS_CONF;

/**
 * Проверяем что предопределенные ошибки стоят заданное кол-во баллов
 */
@RunWith(Parameterized.class)
public class CheckPredefinedErrorCostsTest {
    private static Costs costs;

    @Parameterized.Parameter
    public int errorCode;
    @Parameterized.Parameter(value = 1)
    public int expectedCostInUnits;

    @BeforeClass
    public static void setUpClass() {
        costs = new Costs(ConfigFactory.load(UNITS_COSTS_CONF));
    }

    @AfterClass
    public static void tearDownClass() {
        costs = null;
    }

    @Parameterized.Parameters(name = "errorCode: {0}, expectedCostInUnits: {1}")
    public static Collection<Object[]> getParameters() {
        return singletonList(
                new Object[]{57, 10} // ConcurrentLimitExceed
        );
    }

    @Test
    public void checkErrorCost() {
        assertThat(costs.getServiceErrorCost(errorCode)).isEqualTo(expectedCostInUnits);
    }
}
