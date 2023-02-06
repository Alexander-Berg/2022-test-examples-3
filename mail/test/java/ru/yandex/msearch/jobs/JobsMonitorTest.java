package ru.yandex.msearch.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.logger.DevNullLogger;
import ru.yandex.test.util.TestBase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JobsMonitorTest extends TestBase {
    private static final Duration CHECK_INTERVAL = Duration.ofMillis(100);
    private JobsManager jobsManagerMock;
    private JobsMonitor jobsMonitor;
    private Path jobsPath;

    @Before
    public void setUp() throws Exception {
        jobsManagerMock = mock(JobsManager.class);
        jobsPath = Files.createTempDirectory("jobiks-");
        jobsMonitor = new JobsMonitor(jobsManagerMock, jobsPath, CHECK_INTERVAL, DevNullLogger.INSTANCE);
        jobsMonitor.start();
    }

    @After
    public void tearDown() {
        jobsMonitor.stop();
    }

    @Test
    public void jobsMonitorTriggersOnJobikCreation() throws IOException {
        jobsPath.resolve("new_job.jobik").toFile().createNewFile();
        verifyJobIsScheduled(1);
    }

    @Test
    public void jobsMonitorSkipsProcessedJobs() throws IOException {
        jobsPath.resolve("job1.jobik").toFile().createNewFile();
        verifyJobIsScheduled(1);
        jobsPath.resolve("job2.jobik").toFile().createNewFile();
        verifyJobIsScheduled(1);
    }

    @Test
    public void jobsMonitorSkipsNonJobikFiles() throws IOException {
        jobsPath.resolve("job1.jobik").toFile().createNewFile();
        jobsPath.resolve("iamjobik.xlsx").toFile().createNewFile();
        verifyJobIsScheduled(1);
    }

    @Test
    public void jobsMonitorProcessesMultipleJobiks() throws IOException {
        jobsPath.resolve("job1.jobik").toFile().createNewFile();
        jobsPath.resolve("job2.jobik").toFile().createNewFile();
        verifyJobIsScheduled(2);
    }

    private void verifyJobIsScheduled(int expectedScheduledJobsCount) {
        long expectedMaxExecutionTime = CHECK_INTERVAL.plusSeconds(2).toMillis();
        verify(
                jobsManagerMock,
                timeout(expectedMaxExecutionTime).times(expectedScheduledJobsCount)
        ).addJob(any(File.class));
    }
}
