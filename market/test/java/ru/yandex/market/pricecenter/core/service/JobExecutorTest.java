package ru.yandex.market.pricecenter.core.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.market.pricecenter.core.dao.MetadataService;
import ru.yandex.market.reporting.common.domain.JobParameters;
import ru.yandex.market.reporting.common.domain.JobStatus;
import ru.yandex.market.reporting.common.domain.JobStatusEnum;
import ru.yandex.market.reporting.common.domain.ReportParameters;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobExecutorTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MetadataService metadataService;

    @Before
    public void setUp() {
        metadataService = mock(MetadataService.class);
        when(metadataService.addNewJob(anyString(), anyString(), any())).thenReturn("");
        when(metadataService.getJobStatus(anyString())).thenReturn(new JobStatus("mock_job_" + LocalDateTime.now(),
                JobStatusEnum.NEW, null, null));
    }

    @Test
    public void massiveEnqueueingShouldNotFail() {
        ThreadPoolExecutor taskExecutor = goodExecutor();
        JobExecutor jobExecutor = new JobExecutor(metadataService, Collections.emptyList(), taskExecutor);
        IntStream.rangeClosed(1, 50).mapToObj(i -> jobExecutor.submitJob("mockk" + i, "", jobParameters()))
                .forEachOrdered(System.out::println);
    }

    @Test
    public void massiveEnqueueingFailsWithoutAReason() {
        expectedException.expectMessage(startsWith("java.util.concurrent.CompletableFuture$AsyncRun cannot be cast to java.lang.Comparable"));
        ThreadPoolExecutor taskExecutor = badExecutor();
        JobExecutor jobExecutor = new JobExecutor(metadataService, Collections.emptyList(), taskExecutor);
        IntStream.rangeClosed(1, 50).mapToObj(i -> jobExecutor.submitJob("mockk" + i, "", jobParameters()))
                .forEachOrdered(System.out::println);
    }

    @Test
    public void massiveEnqueueingDoesNotFailWithoutAReason() {
        ThreadPoolExecutor taskExecutor = uglyExecutor();
        JobExecutor jobExecutor = new JobExecutor(metadataService, Collections.emptyList(), taskExecutor);
        IntStream.rangeClosed(1, 50).mapToObj(i -> jobExecutor.submitJob("mockk" + i, "", jobParameters()))
                .forEachOrdered(System.out::println);
    }

    private JobParameters<ReportParameters> jobParameters() {
        JobParameters<ReportParameters> jobParameters = new JobParameters<>();
        jobParameters.setParameters(new ReportParameters() {});
        return jobParameters;
    }

    private static ThreadPoolExecutor goodExecutor() {
        return taskExecutor(new LinkedBlockingQueue<>());
    }

    private static ThreadPoolExecutor badExecutor() {
        return taskExecutor(new PriorityBlockingQueue<>(10));
    }

    private static ThreadPoolExecutor uglyExecutor() {
        return taskExecutor(new PriorityBlockingQueue<>(10, (o1, o2) -> 0));
    }

    private static ThreadPoolExecutor taskExecutor(BlockingQueue<Runnable> workQueue) {
        return new ThreadPoolExecutor(1, 1,
                10L, TimeUnit.MINUTES,
                workQueue,
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("job-thread-%d")
                        .build());
    }

}
