package ru.yandex.market.billing.distribution.share.stats;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DistributionOrderStatsExecutorTest extends FunctionalTest {
    private static final Instant NOW = Instant.parse("2021-01-02T00:00:00.00Z");
    private final TestableClock clock = new TestableClock();
    @Mock
    private DistributionOrderStatsService distributionOrderStatsService;

    @Mock
    private DistributionOrderStatsYtExporter exporter;

    @Autowired
    private EnvironmentService environmentService;
    private DistributionOrderStatsExecutor distributionOrderStatsExecutor;

    @BeforeEach
    public void setup() {
        clock.setFixed(NOW, ZoneOffset.UTC);
        distributionOrderStatsExecutor = new DistributionOrderStatsExecutor(
                environmentService, clock, distributionOrderStatsService, exporter, "//home/tst"
        );
        environmentService.setValue(
                DistributionOrderStatsExecutor.ENV_NAME_DISTR_ORDER_STATS_LAST_REFRESH,
                DateTimes.toLocalDateTime(NOW).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Test
    public void saveLastRunDateTime() {
        distributionOrderStatsExecutor.doJob(null);
        LocalDateTime from = environmentService.getValue(
                DistributionOrderStatsExecutor.ENV_NAME_DISTR_ORDER_STATS_LAST_REFRESH)
                .map(d -> LocalDateTime.parse(d, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElse(null);
        Assertions.assertThat(from).isEqualTo(DateTimes.toLocalDateTime(NOW));
    }

    @Test
    public void passLastRefreshTime() {

        clock.setFixed(NOW, ZoneOffset.UTC);
        distributionOrderStatsExecutor.doJob(null);
        verify(distributionOrderStatsService, times(1))
                .refreshStats(DateTimes.toLocalDateTime(NOW).minusDays(1));
        verify(exporter).exportForDate(LocalDate.of(2021, 1, 1), "//home/tst");
        verify(exporter).exportForDate(LocalDate.of(2021, 1, 2), "//home/tst");

        Mockito.reset(distributionOrderStatsService, exporter);

        clock.setFixed(NOW.plus(Duration.ofDays(2)), ZoneOffset.UTC);
        distributionOrderStatsExecutor.doJob(null);
        verify(distributionOrderStatsService, times(1))
                .refreshStats(DateTimes.toLocalDateTime(NOW));
        verify(exporter).exportForDate(LocalDate.of(2021, 1, 2), "//home/tst");
        verify(exporter).exportForDate(LocalDate.of(2021, 1, 3), "//home/tst");
        verify(exporter).exportForDate(LocalDate.of(2021, 1, 4), "//home/tst");
    }
}
