package ru.yandex.market.tsum.pipelines.common.jobs.tsum;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.context.impl.ReleaseJobContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.release.dao.Release;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 15.01.18
 */
@RunWith(MockitoJUnitRunner.class)
public class TsumInnerReleaseJobTest {
    private static final String JOB_ID = "job/id";

    @Mock
    private PipeLaunchDao pipeLaunchDao;

    @Mock
    private PipeLaunch pipeLaunch;

    @Mock
    private ReleaseJobContextImpl jobContext;

    @Mock
    private Release innerRelease;

    @Mock
    private JobProgressContext jobProgressContext;

    @Spy
    @SuppressWarnings("unused")
    private TsumReleaseJobConfig config = TsumReleaseJobConfig.builder().withPipelineId("a").withProjectId("b").build();

    @Spy
    @InjectMocks
    private TsumInnerReleaseJob job;

    @Before
    public void setup() {
        Mockito.when(jobContext.getPipeLaunch()).thenReturn(pipeLaunch);

        Mockito.when(jobContext.release()).thenReturn(jobContext);
        Mockito
            .when(jobContext.launchRelease(any()))
            .thenReturn(innerRelease);

        Mockito.when(innerRelease.getPipeLaunchIds()).thenReturn(Collections.singletonList("id"));
        Mockito.when(jobContext.getPipeLaunch("id")).thenReturn(pipeLaunch);
        Mockito.when(jobContext.getFullJobId()).thenReturn(JOB_ID);
        Mockito.when(jobContext.findReleaseByTag(JOB_ID)).thenReturn(innerRelease);
        Mockito.when(jobContext.progress()).thenReturn(jobProgressContext);
    }

    @Test
    public void createsAndPollsReleaseOnExecute() throws Exception {
        Mockito.when(innerRelease.isFinished()).thenReturn(true);

        job.execute(jobContext);

        Mockito.verify(jobContext).launchRelease(any());
        Mockito.verify(jobContext).findReleaseByTag(JOB_ID);
    }

    @Test
    public void recoversAndPollsRelease() throws Exception {
        Mockito.when(innerRelease.isFinished()).thenReturn(true);

        job.recover(jobContext);

        Mockito.verify(jobContext, Mockito.never()).launchRelease(any());
        Mockito.verify(jobContext, Mockito.times(2)).findReleaseByTag(JOB_ID);
        Mockito.verify(jobProgressContext).update(Mockito.any());
    }

    @Test(expected = InnerReleaseFailedException.class)
    public void failsOnFailedJobs() throws Exception {
        Map<String, JobState> jobsMap = getJobsMap(
            mockJobState("1", StatusChangeType.SUCCESSFUL),
            mockJobState("2", StatusChangeType.FAILED)
        );

        Mockito.when(innerRelease.isFinished()).thenReturn(true);
        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobsMap);

        job.execute(jobContext);

        Mockito.verify(jobContext).findReleaseByTag(JOB_ID);
    }

    @Test
    public void reportsProgress() throws Exception {
        Mockito.when(innerRelease.isFinished()).thenReturn(true);

        Map<String, JobState> jobsMap = getJobsMap(
            mockJobState("1", StatusChangeType.SUCCESSFUL),
            mockJobState("2", StatusChangeType.QUEUED),
            mockJobState("3", StatusChangeType.WAITING_FOR_STAGE),
            mockJobState("4", StatusChangeType.RUNNING)
        );

        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobsMap).thenReturn(jobsMap).thenReturn(new HashMap<>());

        job.execute(jobContext);

        Mockito.verify(jobProgressContext).update(Mockito.any());
    }

    private Map<String, JobState> getJobsMap(JobState... states) {
        return Arrays.stream(states).collect(Collectors.toMap(JobState::getJobId, j -> j));
    }

    private JobState mockJobState(String id, StatusChangeType status) {
        JobState state = Mockito.mock(JobState.class);

        Mockito.when(state.getLastLaunch()).thenReturn(
            new JobLaunch(0, "", null, Collections.singletonList(new StatusChange(status)))
        );

        Mockito.when(state.getJobId()).thenReturn(id);

        return state;
    }
}
