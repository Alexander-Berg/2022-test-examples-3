package ru.yandex.market.tsum.pipe.engine.runtime.state.model;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamLink;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamStyle;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamType;
import ru.yandex.market.tsum.pipe.engine.definition.job.Job;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.source_code.model.JobExecutorObject;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 15.06.2018
 */
public class PipeLaunchTest {
    @Test
    public void getDownstreamsRecursive_noDownstreams() {
        JobState job1 = jobState("1");
        assertTrue(pipeLaunch(job1).getDownstreamsRecursive("1").isEmpty());
    }

    @Test
    public void getDownstreamsRecursive_twoDownstreams() {
        JobState job1 = jobState("1");
        JobState job2 = jobState("2", "1");
        JobState job3 = jobState("3", "2");
        assertThat(
            pipeLaunch(job1, job2, job3).getDownstreamsRecursive("1"),
            containsInAnyOrder(sameInstance(job2), sameInstance(job3))
        );
    }

    @Test
    public void getDownstreamsRecursive_diamond() {
        JobState job1 = jobState("1");
        JobState job2 = jobState("2", "1");
        JobState job3 = jobState("3", "1");
        JobState job4 = jobState("4", "2", "3");
        assertThat(
            pipeLaunch(job1, job2, job3, job4).getDownstreamsRecursive("1"),
            containsInAnyOrder(sameInstance(job2), sameInstance(job3), sameInstance(job4))
        );
    }


    private static PipeLaunch pipeLaunch(JobState... jobStates) {
        return PipeLaunch.builder()
            .withLaunchRef(PipeLaunchRefImpl.create("pipe-id"))
            .withJobs(Stream.of(jobStates).collect(Collectors.toMap(
                JobState::getJobId,
                Function.identity()
            )))
            .withTriggeredBy("user")
            .withStages(Collections.emptyList())
            .withSkipStagesAllowed(false)
            .withProjectId("prj")
            .build();
    }

    private static JobState jobState(String id, String... upstreamIds) {
        Job job = mock(Job.class);
        Mockito.<Class<? extends JobExecutor>>when(job.getExecutorClass()).thenReturn(DummyJob.class);
        when(job.getId()).thenReturn(id);
        return new JobState(
            job,
            new JobExecutorObject(
                UUID.randomUUID(), DummyJob.class, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList()
            ),
            Stream.of(upstreamIds)
                .map(upstreamId -> new UpstreamLink<>(upstreamId, UpstreamType.ALL_RESOURCES, UpstreamStyle.MODERN))
                .collect(Collectors.toSet()),
            null
        );
    }
}
