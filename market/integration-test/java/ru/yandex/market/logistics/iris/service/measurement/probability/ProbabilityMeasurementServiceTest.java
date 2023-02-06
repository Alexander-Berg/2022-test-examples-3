package ru.yandex.market.logistics.iris.service.measurement.probability;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.exception.ProbabilityMeasurementRangeException;

public class ProbabilityMeasurementServiceTest extends AbstractContextualTest {

    private final Map<Integer, BigDecimal> RATING_PROBABILITY_MAP = ImmutableMap.of(
            0, BigDecimal.valueOf(0).setScale(2),
            2, BigDecimal.valueOf(5).setScale(2),
            15, BigDecimal.valueOf(25.5).setScale(2),
            92, BigDecimal.valueOf(55).setScale(2),
            101, BigDecimal.valueOf(100).setScale(2));

    @Autowired
    private ProbabilityMeasurementService computingService;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/measurement/probability/calculate/1.xml")
    public void shouldSuccessCalculateProbability() {

        RATING_PROBABILITY_MAP.forEach((dqScore, optionalProbability) -> {
            assertions().assertThat(computingService.calculate(dqScore)).isEqualTo(optionalProbability);
        });
    }

    /**
     * Проверяем получение ошибки, если
     * - Не найден подходящий диапазон;
     * - Значение скора меньше максимального значения скора из диппазона.
     */
    @Test(expected = ProbabilityMeasurementRangeException.class)
    @DatabaseSetup("classpath:fixtures/setup/measurement/probability/calculate/2.xml")
    public void shouldNotCalculateProbability() {
        computingService.calculate(46);
    }

    /**
     * Проверяем получение ошибки, если
     * - Диапазоны не заданы;
     * - Значение скора меньше максимального значения скора из диппазона.
     */
    @Test(expected = ProbabilityMeasurementRangeException.class)
    @DatabaseSetup("classpath:fixtures/setup/measurement/probability/calculate/3.xml")
    public void shouldNotCalculateProbabilityIfRagesDoNotExist() {
        computingService.calculate(50);
    }

    /**
     * Проверяем получение 100% на измерение, если скор больше максимально допустимого.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/measurement/probability/calculate/4.xml")
    public void shouldNotCalculateProbabilityIfScoreMoreThanMaxAvailableScore() {
        assertions().assertThat(computingService.calculate(1050)).isEqualTo(BigDecimal.valueOf(100));
    }
}
