package ru.yandex.market.tsum.core.bazinga;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.commune.bazinga.scheduler.schedule.ReschedulePolicy;

import java.util.concurrent.TimeUnit;


/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 03/08/2017
 */
public class TsumBazingaTaskTest {


    @Test
    public void reschedulePolicy() throws Exception {
        Task task = new Task();
        ReschedulePolicy policy = task.reschedulePolicy();
        checkReschedulePolicy(policy, 1, 1);
        checkReschedulePolicy(policy, 100, 100);
        checkReschedulePolicy(policy, 3599, 3599);
        checkReschedulePolicy(policy, 3600, 3600);
        checkReschedulePolicy(policy, 3601, 3600);
        checkReschedulePolicy(policy, 10000, 3600);
        checkReschedulePolicy(policy, Integer.MAX_VALUE, 3600);

    }

    private static void checkReschedulePolicy(ReschedulePolicy policy, int retryNum, int expectedDelaySeconds) {
        long currentTimeMillis = 1_500_000_000_000L;
        Instant currentTime = new Instant(currentTimeMillis);
        Option<Instant> schedulteTime = policy.rescheduleAt(currentTime, retryNum);
        Assert.assertTrue(schedulteTime.isPresent());
        long actualDelayMillis = schedulteTime.get().getMillis() - currentTimeMillis;
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(expectedDelaySeconds), actualDelayMillis);
    }

    public static class Task extends TsumBazingaTask {

        public Task() {
            super("");
        }

        @Override
        protected void execute(Object parameters, ExecutionContext context) throws Exception {

        }

        @Override
        public Duration timeout() {
            return Duration.standardMinutes(1);
        }
    }

}