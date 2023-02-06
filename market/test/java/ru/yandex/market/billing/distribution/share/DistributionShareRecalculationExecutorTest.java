package ru.yandex.market.billing.distribution.share;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link DistributionShareRecalculationExecutor}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class DistributionShareRecalculationExecutorTest extends FunctionalTest {
    private static final String ENV_NAME_PROCESSED_DATE = "mbi.billing.tasks.distribution_share.calculation.start_date";

    @Autowired
    private EnvironmentService environmentService;

    @Mock
    private DistributionShareCalculationService distributionShareCalculationService;

    private DistributionShareRecalculationExecutor executor;

    @BeforeEach
    void beforeEach() {
        executor = new DistributionShareRecalculationExecutor(
                distributionShareCalculationService,
                environmentService
        );
    }

    @Test
    void test_recalculateShare() {
        LocalDate currentDate = LocalDate.now();

        executor.doJob(null);

        IntStream.rangeClosed(1, 15) //на основе окна в 14 дней + 1  и по текущий день
                .mapToObj(currentDate::minusDays)
                .forEach(dateArg -> verify(distributionShareCalculationService).recalculateShare(dateArg));

        verifyNoMoreInteractions(distributionShareCalculationService);
    }

    @DisplayName("Расчет при выставленой переменной окружения")
    @Test
    void test_recalculateShare_withExplicitEnv() {
        LocalDate currentDate = LocalDate.now();

        environmentService.setValue(ENV_NAME_PROCESSED_DATE,
                currentDate.minusDays(2).format(DateTimeFormatter.ISO_DATE));

        executor.doJob(null);

        IntStream.rangeClosed(1, 2) //на основе явно выставленной даты 2 дня назад и по текущий день
                .mapToObj(currentDate::minusDays)
                .forEach(dateArg -> verify(distributionShareCalculationService).recalculateShare(dateArg));

        verifyNoMoreInteractions(distributionShareCalculationService);
    }
}
