package ru.yandex.market.tsum.pipelines.common.jobs.multitesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.UpstreamLaunch;
import ru.yandex.market.tsum.pipelines.common.resources.DbaasClusterId;
import ru.yandex.market.tsum.pipelines.common.resources.FrontBalancer;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 23.10.2019
 */
public class RemoveMultitestingUpstreamEnvironmentJobTest {
    private JobState cleanJobState1;
    private JobState cleanJobState2;
    private PipeLaunch pipeLaunch;
    private RemoveMultitestingUpstreamEnvironmentJob job;

    @Before
    public void setup() {
        job = new RemoveMultitestingUpstreamEnvironmentJob();

        JobState jobState0 = createJobState0();
        JobState jobState11 = createJobState11();
        JobState jobState12 = createJobState12();
        cleanJobState1 = createCleanJobState("Clean1", "Job12");
        JobState jobState21 = createJobState21();
        JobState jobState22 = createJobState22();
        cleanJobState2 = createCleanJobState("Clean2", "Job22");

        Map<String, JobState> jobStateMap = new HashMap<>();
        jobStateMap.put(jobState0.getJobId(), jobState0);
        jobStateMap.put(jobState11.getJobId(), jobState11);
        jobStateMap.put(jobState12.getJobId(), jobState12);
        jobStateMap.put(cleanJobState1.getJobId(), cleanJobState1);
        jobStateMap.put(jobState21.getJobId(), jobState21);
        jobStateMap.put(jobState22.getJobId(), jobState22);
        jobStateMap.put(cleanJobState2.getJobId(), cleanJobState2);

        pipeLaunch = Mockito.mock(PipeLaunch.class);
        Mockito.when(pipeLaunch.getJobs()).thenReturn(jobStateMap);
    }

    @Test
    public void testForUpstreamResources() {
        RemoveMultitestingUpstreamEnvironmentJob alternateJob = new RemoveMultitestingUpstreamEnvironmentJob();

        Set<JobLaunch> firstUpstreamLaunches = alternateJob.getUpstreamLaunches(
            pipeLaunch, cleanJobState1, cleanJobState1.getLastLaunch().getNumber()
        );
        Assert.assertEquals(5, firstUpstreamLaunches.size());
        List<ResourceRef> firstUpNannyRefs = alternateJob.getProducedResourceRefs(firstUpstreamLaunches,
            NannyService.class.getName());
        Assert.assertEquals(firstUpNannyRefs.size(), 3);
        List<ResourceRef> firstUpBalanserRefs = alternateJob.getProducedResourceRefs(firstUpstreamLaunches,
            FrontBalancer.class.getName());
        Assert.assertEquals(firstUpBalanserRefs.size(), 1);
        List<ResourceRef> firstUpDbaasRefs = alternateJob.getProducedResourceRefs(firstUpstreamLaunches,
            DbaasClusterId.class.getName());
        Assert.assertEquals(firstUpDbaasRefs.size(), 0);
    }

    @Test
    public void testForLaunchAfterCleanup() {
        // Второая ветка пайплайна содержит запуск, который произошёл после запуска cleanJobState2.
        // Ресурсы этого запуска нужно игнорировать (второй запуск у Job21).
        Set<JobLaunch> secondUpstreamLaunches = job.getUpstreamLaunches(
            pipeLaunch, cleanJobState2, cleanJobState2.getLastLaunch().getNumber()
        );
        Assert.assertEquals(4, secondUpstreamLaunches.size());
        List<ResourceRef> secondUpNannyRefs = job.getProducedResourceRefs(secondUpstreamLaunches,
            NannyService.class.getName());
        Assert.assertEquals(1, secondUpNannyRefs.size());
        List<ResourceRef> secondUpBalanserRefs = job.getProducedResourceRefs(secondUpstreamLaunches,
            FrontBalancer.class.getName());
        Assert.assertEquals(1, secondUpBalanserRefs.size());
        List<ResourceRef> secondUDbaasRefs = job.getProducedResourceRefs(secondUpstreamLaunches,
            DbaasClusterId.class.getName());
        Assert.assertEquals(1, secondUDbaasRefs.size());
    }

    private JobState createJobState0() {
        JobState jobState0 = Mockito.mock(JobState.class);
        Mockito.when(jobState0.getJobId()).thenReturn("Job0");
        JobLaunch launch = createJobLaunch(
            Collections.emptyList(),
            1,
            Collections.emptyList()
        );
        Mockito.when(jobState0.getLaunches()).thenReturn(Collections.singletonList(launch));
        Mockito.when(jobState0.getLaunchByNumber(Mockito.eq(1))).thenReturn(launch);

        return jobState0;
    }

    private JobState createJobState11() {
        JobState jobState11 = Mockito.mock(JobState.class);
        Mockito.when(jobState11.getJobId()).thenReturn("Job11");
        JobLaunch firstLaunchJob11 = createJobLaunch(
            Collections.singletonList(
                new ResourceRef(new ObjectId(), NannyService.class.getName(), NannyService.SOURCE_CODE_ID)
            ),
            1,
            Collections.singletonList(new UpstreamLaunch("Job0", 1))
        );
        JobLaunch secondLaunchJob11 = createJobLaunch(
            Collections.singletonList(
                new ResourceRef(new ObjectId(), NannyService.class.getName(), NannyService.SOURCE_CODE_ID)
            ),
            2,
            Collections.singletonList(new UpstreamLaunch("Job0", 1))
        );
        Mockito.when(jobState11.getLaunches()).thenReturn(Arrays.asList(firstLaunchJob11, secondLaunchJob11));
        Mockito.when(jobState11.getLaunchByNumber(Mockito.eq(1))).thenReturn(firstLaunchJob11);
        Mockito.when(jobState11.getLaunchByNumber(Mockito.eq(2))).thenReturn(secondLaunchJob11);

        return jobState11;
    }

    private JobState createJobState12() {
        JobState jobState12 = Mockito.mock(JobState.class);
        Mockito.when(jobState12.getJobId()).thenReturn("Job12");
        JobLaunch firstLaunchJob12 = createJobLaunch(
            Arrays.asList(
                new ResourceRef(new ObjectId(), NannyService.class.getName(), NannyService.SOURCE_CODE_ID),
                new ResourceRef(new ObjectId(), FrontBalancer.class.getName(), new FrontBalancer().getSourceCodeId())
            ),
            1,
            Collections.singletonList(new UpstreamLaunch("Job11", 2))
        );
        Mockito.when(jobState12.getLaunches()).thenReturn(Collections.singletonList(firstLaunchJob12));
        Mockito.when(jobState12.getLaunchByNumber(Mockito.eq(1))).thenReturn(firstLaunchJob12);

        return jobState12;
    }

    private JobState createJobState21() {
        UUID dbaasClusterSourceCodeId = new DbaasClusterId(null, null).getSourceCodeId();
        JobState jobState21 = Mockito.mock(JobState.class);
        Mockito.when(jobState21.getJobId()).thenReturn("Job21");
        JobLaunch firstLaunchJob21 = createJobLaunch(
            Arrays.asList(
                new ResourceRef(new ObjectId(), NannyService.class.getName(), NannyService.SOURCE_CODE_ID),
                new ResourceRef(new ObjectId(), DbaasClusterId.class.getName(), dbaasClusterSourceCodeId)
            ),
            1,
            Collections.singletonList(new UpstreamLaunch("Job0", 1))
        );
        JobLaunch secondLaunchJob21 = createJobLaunch(
            Arrays.asList(
                new ResourceRef(new ObjectId(), DbaasClusterId.class.getName(), dbaasClusterSourceCodeId),
                new ResourceRef(new ObjectId(), FrontBalancer.class.getName(), new FrontBalancer().getSourceCodeId())
            ),
            2,
            Collections.singletonList(new UpstreamLaunch("Job0", 1))
        );
        Mockito.when(jobState21.getLaunches()).thenReturn(Arrays.asList(firstLaunchJob21, secondLaunchJob21));
        Mockito.when(jobState21.getLaunchByNumber(Mockito.eq(1))).thenReturn(firstLaunchJob21);
        Mockito.when(jobState21.getLaunchByNumber(Mockito.eq(2))).thenReturn(secondLaunchJob21);

        return jobState21;
    }

    private JobState createJobState22() {
        JobState jobState22 = Mockito.mock(JobState.class);
        Mockito.when(jobState22.getJobId()).thenReturn("Job22");
        JobLaunch firstLaunchJob22 = createJobLaunch(
            Collections.singletonList(
                new ResourceRef(new ObjectId(), FrontBalancer.class.getName(), new FrontBalancer().getSourceCodeId())
            ),
            1,
            Collections.singletonList(new UpstreamLaunch("Job21", 1))
        );
        Mockito.when(jobState22.getLaunches()).thenReturn(Collections.singletonList(firstLaunchJob22));
        Mockito.when(jobState22.getLaunchByNumber(Mockito.eq(1))).thenReturn(firstLaunchJob22);

        return jobState22;
    }

    private JobState createCleanJobState(String jobId, String lastLaunchId) {
        JobState cleanJobState = Mockito.mock(JobState.class);
        Mockito.when(cleanJobState.getJobId()).thenReturn(jobId);
        JobLaunch launch = createJobLaunch(
            Collections.emptyList(),
            1,
            Collections.singletonList(new UpstreamLaunch(lastLaunchId, 1))
        );
        Mockito.when(cleanJobState.getLaunches()).thenReturn(Collections.singletonList(launch));
        Mockito.when(cleanJobState.getLaunchByNumber(Mockito.eq(1))).thenReturn(launch);
        Mockito.when(cleanJobState.getLastLaunch()).thenReturn(launch);

        return cleanJobState;
    }

    private JobLaunch createJobLaunch(List<ResourceRef> resources, int number, List<UpstreamLaunch> upstreamLaunches) {
        JobLaunch jobLaunch = new JobLaunch(number, "test", upstreamLaunches, null);
        jobLaunch.setProducedResources(new ResourceRefContainer(resources));
        return jobLaunch;
    }

}
