package ru.yandex.direct.scheduler.hourglass.implementations.schedule.strategies;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.scheduler.hourglass.schedule.strategies.ScheduleType;

import static org.assertj.core.api.Assertions.assertThat;

public class NextRunStrategiesFactoryImplTest {

    private NextRunStrategiesFactoryImpl nextRunStrategiesFactory = new NextRunStrategiesFactoryImpl(
            List.of(
                    new PeriodicStrategy(),
                    new ByCronExpressionStrategy(),
                    new DaemonRunStrategy(),
                    new FarFutureStrategy()));

    @Test
    public void getPeriodicStrategyTest() {
        var nextRunStrategy = nextRunStrategiesFactory
                .getNextRunStrategy(ScheduleType.PERIODIC);
        assertThat(nextRunStrategy).isInstanceOf(PeriodicStrategy.class);
    }

    @Test
    public void getCronStrategyTest() {
        var nextRunStrategy =
                nextRunStrategiesFactory.getNextRunStrategy(ScheduleType.CRON);
        assertThat(nextRunStrategy).isInstanceOf(ByCronExpressionStrategy.class);
    }

    @Test
    public void getDaemonStrategyTest() {
        var nextRunStrategy =
                nextRunStrategiesFactory.getNextRunStrategy(ScheduleType.DAEMON);
        assertThat(nextRunStrategy).isInstanceOf(DaemonRunStrategy.class);
    }

    @Test
    public void getFarFutureStrategyTest() {
        var nextRunStrategy = nextRunStrategiesFactory.getNextRunStrategy(ScheduleType.FAR_FUTURE);
        assertThat(nextRunStrategy).isInstanceOf(FarFutureStrategy.class);
    }
}
