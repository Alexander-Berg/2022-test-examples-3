package ru.yandex.market.tsum.pipelines.common.jobs.tsum;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.context.impl.ReleaseJobContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.release.dao.CreateReleaseCommandBuilder;
import ru.yandex.market.tsum.release.dao.Release;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 27.09.2021
 */
@RunWith(MockitoJUnitRunner.class)
public class TsumMultipleInnerReleaseJobTest {
    private static final String JOB_ID = "job/id";

    @Mock
    private PipeLaunch pipeLaunch;

    @Mock
    private ReleaseJobContextImpl jobContext;

    @Mock
    private Release innerRelease1;

    @Mock
    private Release innerRelease2;

    private Map<String, Release> nameToReleaseMap = new HashMap<>();
    private Map<String, Collection<? extends Resource>> launchToResourcesMap = new HashMap<>();
    private Map<String, Release> tagToReleaseMap = new HashMap<>();

    @Mock
    private JobProgressContext jobProgressContext;

    @Spy
    @SuppressWarnings("unused")
    private TsumReleaseJobConfig config = TsumReleaseJobConfig.builder().withPipelineId("a").withProjectId("b").build();

    @Spy
    @InjectMocks
    private TsumMultipleInnerReleaseJob job;

    @Before
    public void setup() {
        nameToReleaseMap = Map.of(
            "PRODUCTION", innerRelease1,
            "TESTING", innerRelease2
        );
        Set<String> keySet = nameToReleaseMap.keySet();
        keySet.forEach(name -> launchToResourcesMap.put(name, Collections.emptyList()));
        job.setLaunchToResourcesMap(launchToResourcesMap);

        Mockito.when(jobContext.getPipeLaunch()).thenReturn(pipeLaunch);

        Mockito.when(jobContext.release()).thenReturn(jobContext);


        tagToReleaseMap = nameToReleaseMap.entrySet().stream().collect(
            Collectors.toMap(entry -> TsumMultipleInnerReleaseJob.getTagForLaunch(JOB_ID, entry.getKey()),
                Map.Entry::getValue));
        Mockito
            .when(jobContext.launchRelease(any()))
            .thenAnswer(invocation -> tagToReleaseMap.get(invocation.getArgument(0,
                CreateReleaseCommandBuilder.class).getTag()));

        Mockito.when(innerRelease1.getPipeLaunchIds()).thenReturn(Collections.singletonList("id"));
        Mockito.when(innerRelease2.getPipeLaunchIds()).thenReturn(Collections.singletonList("id"));
        Mockito.when(jobContext.getPipeLaunch("id")).thenReturn(pipeLaunch);
        Mockito.when(jobContext.getFullJobId()).thenReturn(JOB_ID);
        tagToReleaseMap.forEach((key, value) -> Mockito.when(jobContext.findReleaseByTag(key)).thenReturn(value));
        Mockito.when(jobContext.progress()).thenReturn(jobProgressContext);
    }

    @Test
    public void createsAndPollsReleaseOnExecute() throws Exception {
        Mockito.when(innerRelease1.isFinished()).thenReturn(true);
        Mockito.when(innerRelease2.isFinished()).thenReturn(true);
        Map<String, JobState> jobsMap = getJobsMap(
            mockJobState("1", StatusChangeType.SUCCESSFUL)
        );
        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobsMap);

        job.execute(jobContext);

        Mockito.verify(jobContext, Mockito.times(2)).launchRelease(any());
    }

    @Test
    public void recover_allReleasesFinished_finishSuccessfully() throws Exception {
        Mockito.when(innerRelease1.isFinished()).thenReturn(true);
        Mockito.when(innerRelease2.isFinished()).thenReturn(true);
        Map<String, JobState> jobsMap = getJobsMap(
            mockJobState("1", StatusChangeType.SUCCESSFUL)
        );
        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobsMap);

        job.recover(jobContext);

        tagToReleaseMap.forEach((key, value) -> Mockito.verify(jobContext, Mockito.times(2))
            .findReleaseByTag(key));
        Mockito.verify(jobProgressContext).update(Mockito.any());
    }

    @Test
    public void recover_notAllReleasesLaunched_notLaunchedReleasesLaunched() throws Exception {
        String notLaunchedReleaseTag = TsumMultipleInnerReleaseJob.getTagForLaunch(JOB_ID, "TESTING");
        Mockito.when(jobContext.findReleaseByTag(notLaunchedReleaseTag)).thenReturn(null).thenReturn(innerRelease1);
        Mockito.when(innerRelease1.isFinished()).thenReturn(true);
        Mockito.when(innerRelease2.isFinished()).thenReturn(true);
        Map<String, JobState> jobsMap = getJobsMap(
            mockJobState("1", StatusChangeType.SUCCESSFUL)
        );
        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobsMap);

        job.recover(jobContext);

        tagToReleaseMap.forEach((key, value) -> Mockito.verify(jobContext, Mockito.times(2))
            .findReleaseByTag(key));
        Mockito.verify(jobProgressContext).update(Mockito.any());

        ArgumentCaptor<CreateReleaseCommandBuilder> argumentCaptor = ArgumentCaptor.forClass(
            CreateReleaseCommandBuilder.class);
        Mockito.verify(jobContext, Mockito.times(1)).launchRelease(argumentCaptor.capture());
        CreateReleaseCommandBuilder capturedArgument = argumentCaptor.getValue();
        Assert.assertThat(capturedArgument.getTag(), is(notLaunchedReleaseTag));
    }

    @Test(expected = InnerReleaseFailedException.class)
    public void failsOnFailedJobs() throws Exception {
        Mockito.when(innerRelease1.isFinished()).thenReturn(true);
        Mockito.when(innerRelease2.isFinished()).thenReturn(true);
        Map<String, JobState> jobsMap = getJobsMap(
            mockJobState("1", StatusChangeType.SUCCESSFUL),
            mockJobState("2", StatusChangeType.FAILED)
        );
        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobsMap);

        job.execute(jobContext);

        Mockito.verify(jobContext).findReleaseByTag(JOB_ID);
    }

    @Test
    public void reportsProgress() throws Exception {
        Mockito.when(innerRelease1.isFinished()).thenReturn(true);
        Mockito.when(innerRelease2.isFinished()).thenReturn(true);

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
            new JobLaunch(0, "", null,
                Collections.singletonList(new StatusChange(status)))
        );

        Mockito.when(state.getJobId()).thenReturn(id);

        return state;
    }
}
