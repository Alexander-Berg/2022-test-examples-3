package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.misc.test.Assert;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SimpleManualTriggerTest extends PipeStateCalculatorTest {
    @Override
    protected Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(DummyJob.class)
            .withManualTrigger();

        return builder.build();
    }

    @Test
    public void manualJobNotTriggeredAutomatically() {
        recalc(null);

        JobState singleJobState = getPipeLaunch().getJobs().values().iterator().next();
        Assert.assertTrue(singleJobState.isReadyToRun());
        Assert.assertEmpty(getTriggeredJobs());
    }

}
