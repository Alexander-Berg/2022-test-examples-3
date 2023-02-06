package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.diamond.DiamondPipe;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 20.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DiamondPipeRecalcTest extends PipeStateCalculatorTest {
    @Override
    protected String getPipeId() {
        return DiamondPipe.PIPE_NAME;
    }

    @Test
    public void triggerLastJobManually() {
        runToTheEnd();

        // Триггернём последнюю джобу ещё раз и убедимся, что её состояние не посчиталось дважды
        recalc(new TriggerEvent(DiamondPipe.END_JOB, USERNAME, false));

        Assert.assertEquals(1, getTriggeredJobs().size());
    }

    @Test
    public void triggerFirstJobManually() {
        runToTheEnd();

        recalc(new TriggerEvent(DiamondPipe.START_JOB, USERNAME, false));

        Assert.assertEquals(1, getTriggeredJobs().size());
        Assert.assertTrue(getPipeLaunch().getJobState(DiamondPipe.TOP_JOB).isOutdated());
        Assert.assertTrue(getPipeLaunch().getJobState(DiamondPipe.BOTTOM_JOB).isOutdated());
        Assert.assertTrue(getPipeLaunch().getJobState(DiamondPipe.END_JOB).isOutdated());
    }

    private void runToTheEnd() {
        recalc(null);
        Assert.assertEquals(1, getTriggeredJobs().size());
        getTriggeredJobs().clear();

        recalc(new JobRunningEvent(DiamondPipe.START_JOB, 1, DummyFullJobIdFactory.create()));
        recalc(new JobExecutorSucceededEvent(DiamondPipe.START_JOB, 1));
        recalc(new SubscribersSucceededEvent(DiamondPipe.START_JOB, 1));
        recalc(new JobSucceededEvent(DiamondPipe.START_JOB, 1));

        Assert.assertEquals(2, getTriggeredJobs().size());
        getTriggeredJobs().clear();

        recalc(new JobRunningEvent(DiamondPipe.TOP_JOB, 1, DummyFullJobIdFactory.create()));
        recalc(new JobRunningEvent(DiamondPipe.BOTTOM_JOB, 1, DummyFullJobIdFactory.create()));
        recalc(new JobExecutorSucceededEvent(DiamondPipe.TOP_JOB, 1));
        recalc(new JobExecutorSucceededEvent(DiamondPipe.BOTTOM_JOB, 1));
        recalc(new SubscribersSucceededEvent(DiamondPipe.TOP_JOB, 1));
        recalc(new SubscribersSucceededEvent(DiamondPipe.BOTTOM_JOB, 1));
        recalc(new JobSucceededEvent(DiamondPipe.TOP_JOB, 1));
        recalc(new JobSucceededEvent(DiamondPipe.BOTTOM_JOB, 1));

        Assert.assertEquals(1, getTriggeredJobs().size());
        getTriggeredJobs().clear();

        recalc(new JobRunningEvent(DiamondPipe.END_JOB, 1, DummyFullJobIdFactory.create()));
        recalc(new JobExecutorSucceededEvent(DiamondPipe.END_JOB, 1));
        recalc(new SubscribersSucceededEvent(DiamondPipe.END_JOB, 1));
        recalc(new JobSucceededEvent(DiamondPipe.END_JOB, 1));
    }
}
