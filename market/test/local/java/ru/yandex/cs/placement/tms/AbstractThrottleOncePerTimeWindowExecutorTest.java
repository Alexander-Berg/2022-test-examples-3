package ru.yandex.cs.placement.tms;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractThrottleOncePerTimeWindowExecutorTest extends AbstractCsPlacementTmsFunctionalTest {
    private static final Instant NOW = Instant.parse("2022-07-27T16:30:00Z");
    @Autowired
    NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;
    @Autowired
    Clock clock;

    @Test
    void doJobFirstTime() {
        doTest(ChronoUnit.DAYS, true);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/AbstractThrottleOncePerTimeWindowExecutorTest/before.csv",
            dataSource = "vendorDataSource"
    )
    void doJobAfterCooldownDays() {
        doTest(ChronoUnit.DAYS, false);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/AbstractThrottleOncePerTimeWindowExecutorTest/before.csv",
            dataSource = "vendorDataSource"
    )
    void doJobAfterCooldownHours() {
        doTest(ChronoUnit.HOURS, true);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/AbstractThrottleOncePerTimeWindowExecutorTest/before.csv",
            dataSource = "vendorDataSource"
    )
    void doJobThrottledHour() {
        doTest(ChronoUnit.DAYS, false);
    }

    private void doTest(TemporalUnit throttleTimeWindowType, boolean executed) {
        var executedFlag = new AtomicBoolean(false);
        var job = new AbstractThrottleOncePerTimeWindowExecutor(
                vendorNamedParameterJdbcTemplate,
                clock,
                throttleTimeWindowType
        ) {
            @Override
            protected void executeThrottledJob(JobExecutionContext context) {
                executedFlag.set(true);
            }
        };
        var ctx = mock(JobExecutionContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getJobDetail().getKey()).thenReturn(new JobKey("name"));
        doReturn(NOW).when(clock).instant();
        job.doJob(ctx);
        assertThat(executedFlag.get()).isEqualTo(executed);
    }
}
