package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobStateChangedEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitry Poluyanov <a href="https://t.me/neiwick">Dmitry Poluyanov</a>
 * @since 09.08.17
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobStateChangesTest extends PipeStateCalculatorTest {
    private static final String JOB_ID = "jobId1";

    @Test
    public void globalStatusChanged() {
        recalc(null);

        recalc(new JobStateChangedEvent(JOB_ID, 1, "New Status", .1f,
            Collections.emptyList()));

        JobLaunch lastLaunch = getPipeLaunch().getJobState(JOB_ID).getLastLaunch();
        assertEquals("New Status", lastLaunch.getStatusText());
    }

    @Test
    public void totalProgressChanges() {
        recalc(null);

        recalc(new JobStateChangedEvent(JOB_ID, 1, "New Status", .1f,
            Collections.emptyList()));

        JobLaunch lastLaunch = getPipeLaunch().getJobState(JOB_ID).getLastLaunch();
        assertEquals(.1f, lastLaunch.getTotalProgress(), .0f);
    }

    @Test
    public void taskAdded() {
        recalc(null);

        TaskState taskState = new TaskState(Module.TSUM_UI, "http://localhost", TaskState.TaskStatus.RUNNING);

        recalc(new JobStateChangedEvent(JOB_ID, 1, "New Status", .1f,
            Collections.singletonList(taskState)));

        JobLaunch lastLaunch = getPipeLaunch().getJobState(JOB_ID).getLastLaunch();
        assertEquals(lastLaunch.getTaskStates().get(0), taskState);
    }

    @Test
    public void taskRemoved() {
        recalc(null);

        TaskState taskState = new TaskState(Module.TSUM_UI, "http://localhost", TaskState.TaskStatus.RUNNING);

        recalc(new JobStateChangedEvent(JOB_ID, 1, "New Status", .1f,
            Collections.singletonList(taskState)));

        JobLaunch lastLaunch = getPipeLaunch().getJobState(JOB_ID).getLastLaunch();
        assertEquals(lastLaunch.getTaskStates().get(0), taskState);

        recalc(new JobStateChangedEvent(JOB_ID, 1, "New Status", .1f,
            Collections.emptyList()));

        assertTrue(lastLaunch.getTaskStates().isEmpty());
    }

    @Override
    protected Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(DummyJob.class).withId(JOB_ID);

        return builder.build();
    }
}
