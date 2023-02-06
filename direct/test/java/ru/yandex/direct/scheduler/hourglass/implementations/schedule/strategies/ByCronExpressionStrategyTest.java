package ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.Test;

import ru.yandex.direct.hourglass.implementations.TaskProcessingResultImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies.data.ScheduleCronData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ByCronExpressionStrategyTest {
    private static final ZoneId ZONE_ID = TimeZone.getDefault().toZoneId();
    private final ByCronExpressionStrategy byCronExpressionStrategy = new ByCronExpressionStrategy();

    @Test
    public void getNexDateTest() {
        var lastFinishTime = LocalDateTime.of(2019, 9, 30, 10, 30).atZone(ZONE_ID).toInstant();
        var scheduleData = new ScheduleCronData("0 20 10,21 * * ?");
        var cronNextRunStrategy = new ByCronExpressionStrategy();

        var taskProcessingResult = TaskProcessingResultImpl.builder().withLastFinishTime(lastFinishTime).build();
        var gotNextDate = cronNextRunStrategy.getNextDate(taskProcessingResult, scheduleData);
        var expectedNextDate = LocalDateTime.of(2019, 9, 30, 21, 20);
        assertThat(LocalDateTime.ofInstant(gotNextDate, ZONE_ID)).isEqualTo(expectedNextDate);
    }

    @Test
    public void getNexDateTest_ExceptionInPreviousRun() {

        var lastFinishTime = LocalDateTime.of(2019, 9, 30, 10, 30).atZone(ZONE_ID).toInstant();
        var expected = LocalDateTime.of(2019, 9, 30, 21, 20).atZone(ZONE_ID).toInstant();

        var scheduleData = new ScheduleCronData("0 20 10,21 * * ?");
        var cronNextRunStrategy = new ByCronExpressionStrategy();

        var taskProcessingResult = TaskProcessingResultImpl.builder().withLastFinishTime(lastFinishTime)
                .withException(new RuntimeException()).build();


        var gotNextDate = cronNextRunStrategy.getNextDate(taskProcessingResult, scheduleData);
        var offset = new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES);

        assertThat(gotNextDate).isCloseTo(expected, offset);
    }

    @Test
    public void byCronExpressionStrategy_InvalidCronTest() {
        var scheduleData = new ScheduleCronData("0 20 10,21 * * ? 2");
        var taskProcessingResult = TaskProcessingResultImpl.builder().withLastFinishTime(Instant.now()).build();
        assertThatThrownBy(() -> byCronExpressionStrategy.getNextDate(taskProcessingResult, scheduleData))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
