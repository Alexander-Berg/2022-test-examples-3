package ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import ru.yandex.direct.scheduler.hourglass.HourglassJob;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

public class RandomStartTimeTest {

    private static final int PRIME_PERIOD_DURATION = 47;

    @Test
    public void overflowTest() {
        RandomStartTime randomStartTime = new RandomStartTime();

        assertEquals(Instant.MAX, randomStartTime.plusWithOverflow(Instant.MAX, Duration.ofHours(1)));
    }

    @Test
    public void randomDelta_oneJobInGroup() {
        RandomDeltaCalculator randomDeltaCalculator = new RandomDeltaCalculator();

        long result = randomDeltaCalculator
                .randomDelta("veryLongClassNameThatHopefullyHasVeryLongHashCode", 1, 1, PRIME_PERIOD_DURATION);

        assertThat(result).isLessThan(PRIME_PERIOD_DURATION);
    }

    @Test
    public void randomDelta_twoJobsInGroup() {
        RandomDeltaCalculator randomDeltaCalculator = new RandomDeltaCalculator();

        long result = randomDeltaCalculator
                .randomDelta("veryLongClassNameThatHopefullyHasVeryLongHashCode", 1, 2, PRIME_PERIOD_DURATION);

        assertThat(result).isLessThan(PRIME_PERIOD_DURATION);
    }


    @Test
    public void randomDelta_secondJobInGroup() {
        RandomDeltaCalculator randomStartTime = new RandomDeltaCalculator();
        long result = randomStartTime
                .randomDelta("veryLongClassNameThatHopefullyHasVeryLongHashCode", 2, 2, PRIME_PERIOD_DURATION);

        assertThat(result).isLessThan(PRIME_PERIOD_DURATION);
    }

    public static class MyJob implements HourglassJob {
        @Override
        public void execute(TaskParametersMap parametersMap) {
        }

        @Override
        public void onShutdown() {
        }
    }

}
