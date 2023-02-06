package ru.yandex.market.tsum.pipe.engine.runtime.state;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.pipe.engine.definition.job.Job;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobStateChangedEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipe.engine.source_code.model.JobExecutorObject;

import java.util.Collections;
import java.util.UUID;

/**
 * @author Dmitry Poluyanov <a href="https://t.me/neiwick">Dmitry Poluyanov</a>
 * @since 09.08.17
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JobProgressContextServiceTest.JobProgressServiceTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobProgressContextServiceTest {
    @Autowired
    private JobProgressService jobProgressService;

    @Autowired
    private PipeStateService pipeStateService;

    private TestJobContext jobContext;
    private JobLaunch firstLaunch;

    @Before
    public void before() {
        Job job = new Job(PipelineBuilder.create().withJob(TestJobExecutor.class), Collections.emptySet());

        firstLaunch = new JobLaunch(1, "me", Collections.emptyList(), Collections.emptyList());

        JobState jobState = new JobState(
            job,
            new JobExecutorObject(
                UUID.randomUUID(), TestJobExecutor.class, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList()
            ),
            Collections.emptySet(),
            ResourceRefContainer.empty()
        );

        jobState.addLaunch(firstLaunch);
        jobContext = new TestJobContext();
        jobContext.setJobStateMock(jobState);

        Mockito.when(jobContext.getPipeLaunch().getId()).thenReturn(new ObjectId());
        Mockito.when(jobContext.getPipeLaunch().getJobs()).thenReturn(Collections.emptyMap());
    }

    @Test
    public void newStatusTransitionCallsRecalcState() throws Exception {
        jobProgressService.changeProgress(jobContext, "New status", 0.1f, null);

        Mockito.verify(pipeStateService, Mockito.times(1))
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));
    }

    @Test
    public void sameStatusTransitionCallsIgnored() throws Exception {
        firstLaunch.setStatusText("Current status");
        firstLaunch.setTotalProgress(0.1f);

        jobProgressService.changeProgress(jobContext, "Current status", 0.1f, null);

        Mockito.verify(pipeStateService, Mockito.never())
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));
    }

    @Test
    public void globalStatusUpdateLeadToNewState() throws Exception {
        firstLaunch.setStatusText("Current status");
        firstLaunch.setTotalProgress(0.1f);

        jobProgressService.changeProgress(jobContext, "New status", 0.1f, null);

        Mockito.verify(pipeStateService, Mockito.times(1))
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));

    }

    @Test
    public void totalProgressUpdateLeadToNewState() throws Exception {
        firstLaunch.setStatusText("Current status");
        firstLaunch.setTotalProgress(0.1f);

        jobProgressService.changeProgress(jobContext, "Current status", 0.2f, null);

        Mockito.verify(pipeStateService, Mockito.times(1))
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));
    }

    @Test
    public void taskAdditionLeadsToNewState() throws Exception {
        firstLaunch.setStatusText("Current status");
        firstLaunch.setTotalProgress(0.1f);

        jobProgressService.changeProgress(jobContext, "Current status", 0.1f,
            Collections.singletonList(
                new TaskState(Module.TEAMCITY, "http://localhost", TaskState.TaskStatus.SUCCESSFUL)));

        Mockito.verify(pipeStateService, Mockito.times(1))
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));
    }

    @Test
    public void sameTaskStateIgnored() throws Exception {
        firstLaunch.setStatusText("Current status");
        firstLaunch.setTotalProgress(0.1f);
        firstLaunch.setTaskStates(Collections.singletonList(
            new TaskState(Module.TEAMCITY, "http://localhost", TaskState.TaskStatus.SUCCESSFUL)));

        jobProgressService.changeProgress(jobContext, "Current status", 0.1f,
            Collections.singletonList(
                new TaskState(Module.TEAMCITY, "http://localhost", TaskState.TaskStatus.SUCCESSFUL)));

        Mockito.verify(pipeStateService, Mockito.never())
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));
    }

    @Test
    public void taskChangesLeadToNewState() throws Exception {
        firstLaunch.setStatusText("Current status");
        firstLaunch.setTotalProgress(0.1f);
        firstLaunch.setTaskStates(Collections.singletonList(
            new TaskState(Module.TEAMCITY, "http://localhost", TaskState.TaskStatus.RUNNING)));

        jobProgressService.changeProgress(jobContext, "Current status", 0.1f,
            Collections.singletonList(
                new TaskState(Module.TEAMCITY, "http://localhost", TaskState.TaskStatus.SUCCESSFUL)));

        Mockito.verify(pipeStateService, Mockito.times(1))
            .recalc(Mockito.anyString(), Mockito.any(JobStateChangedEvent.class));
    }

    @Configuration
    static class JobProgressServiceTestConfiguration {
        @Bean
        JobProgressService jobProgressService(PipeStateService pipeStateService) {
            return new JobProgressService(pipeStateService);
        }

        @Bean
        PipeStateService pipeStateService() {
            return Mockito.mock(PipeStateService.class);
        }
    }

    static class TestJobExecutor implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("b386baf5-3b28-4efc-9f9b-063cd629b624");
        }

        @Override
        public void execute(JobContext context) throws Exception {
        }
    }
}
