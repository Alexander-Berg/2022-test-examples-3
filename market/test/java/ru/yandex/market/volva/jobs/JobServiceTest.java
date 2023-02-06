package ru.yandex.market.volva.jobs;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.class)
public class JobServiceTest {

    @Mock
    private JobDao jobDao;
    private JobService jobService;

    @Before
    public void init() {
        jobService = new JobService(jobDao);
    }

    @Test
    public void isJobRunAllowedLocalJob() {
        Job job = mock(Job.class);
        when(job.isSingleton()).thenReturn(false);
        assertThat(jobService.isJobRunAllowed(job)).isTrue();
    }

    @Test
    public void isJobRunAllowedSingletonNoActive() {
        Job job = mock(Job.class);
        when(job.isSingleton()).thenReturn(true);
        when(job.getBeanName()).thenReturn("singleton-job-1");
        when(jobDao.findJobs(eq("singleton-job-1"), eq(JobStatus.RUNNING))).thenReturn(Collections.emptyList());
        assertThat(jobService.isJobRunAllowed(job)).isTrue();
    }


    @Test
    public void isJobRunAllowedSingletonHaveActive() {
        Job job = mock(Job.class);
        when(job.isSingleton()).thenReturn(true);
        when(job.getTimeout()).thenReturn(Duration.ofHours(1));
        when(job.getBeanName()).thenReturn("singleton-job-2");
        when(jobDao.findJobs(eq("singleton-job-2"), eq(JobStatus.RUNNING)))
                .thenReturn(List.of(
                        JobEntity.builder()
                                .jobName("singleton-job-2")
                                .startedAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build()
                ));
        assertThat(jobService.isJobRunAllowed(job)).isFalse();
    }


    @Test
    public void isJobRunAllowedSingletonFailActive() {
        Job job = mock(Job.class);
        when(job.isSingleton()).thenReturn(true);
        when(job.getTimeout()).thenReturn(Duration.ofHours(1));
        when(job.getBeanName()).thenReturn("singleton-job-3");
        when(jobDao.findJobs(eq("singleton-job-3"), eq(JobStatus.RUNNING)))
                .thenReturn(List.of(
                        JobEntity.builder()
                                .id(3L)
                                .jobName("singleton-job-3")
                                .startedAt(Instant.now().minus(3, ChronoUnit.HOURS))
                                .updatedAt(Instant.now())
                                .build()
                ));
        assertThat(jobService.isJobRunAllowed(job)).isTrue();
        verify(jobDao).changeStatus(eq(3L), eq(JobStatus.FAILED), eq("Execution is timed out"), any(Instant.class));
    }

    @Test
    public void jobStarted(){
        Job job = mock(Job.class);
        when(job.isSingleton()).thenReturn(true);
        when(job.getBeanName()).thenReturn("singleton-job-4");
        when(jobDao.save(any(JobEntity.class))).thenAnswer((i) -> ((JobEntity) i.getArgument(0)).withId(3L));

        JobEntity je = jobService.jobStarted(job);

        assertThat(je.getId()).isEqualTo(3L);
        assertThat(je.getJobName()).isEqualTo("singleton-job-4");
        assertThat(je.getInstanceName()).isNotEmpty();
        assertThat(je.getStartedAt()).isNotNull();
        assertThat(je.getSingleton()).isTrue();
        assertThat(je.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(je.getError()).isNull();
    }

    @Test
    public void jobFinished(){
        JobEntity jobEntity = JobEntity.builder()
                .id(3L)
                .jobName("singleton-job-5")
                .startedAt(Instant.now())
                .status(JobStatus.RUNNING)
                .build();
        jobEntity = jobService.jobFinished(jobEntity);
        assertThat(jobEntity.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(jobEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    public void jobFailed(){
        JobEntity jobEntity = JobEntity.builder()
                .id(3L)
                .jobName("singleton-job-5")
                .startedAt(Instant.now())
                .status(JobStatus.RUNNING)
                .build();
        Throwable ex;
        try {
            throw new RuntimeException();
        } catch (Exception e){
            ex = e;
        }
        jobEntity = jobService.jobFailed(jobEntity, ex);
        assertThat(jobEntity.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(jobEntity.getUpdatedAt()).isNotNull();
        assertThat(jobEntity.getError()).isNotNull();
    }

}
