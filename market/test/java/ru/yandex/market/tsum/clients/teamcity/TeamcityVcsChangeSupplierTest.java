package ru.yandex.market.tsum.clients.teamcity;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl.ProgressBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobProgress;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState.TaskStatus.RUNNING;

@RunWith(MockitoJUnitRunner.class)
public class TeamcityVcsChangeSupplierTest {

    @Mock
    TeamcityClient teamcityClient;
    @InjectMocks
    TeamcityVcsChangeSupplier teamcityVcsChangeSupplier;

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void checkJobProgressModifiedOnVcsChangeQuerying() throws TimeoutException, InterruptedException {

        String expectedVcsVersion = "testVcsVersion";
        String expectedJobName = "testJobName";
        String expectedBuildConfigUrl = "https://testTeamcityBuildUrl";

        JobProgress testJobProgress = new JobProgress();
        ProgressBuilder testJobProgressBuilder = JobProgressContextImpl.builder(testJobProgress);

        JobProgressContext jobProgressContextMock = Mockito.mock(JobProgressContext.class);
        JobContext jobContextMock = Mockito.mock(JobContext.class);
        VcsChange vcsChangeMock = Mockito.mock(VcsChange.class);

        Queue<String> progressTexts = new LinkedList<>();

        doAnswer(invocation -> {
            Function<ProgressBuilder, ProgressBuilder> updater = invocation.getArgument(0);
            updater.apply(testJobProgressBuilder);
            progressTexts.add(testJobProgress.getText());
            return null;
        }).when(jobProgressContextMock).update(any());

        when(jobContextMock.progress()).thenReturn(jobProgressContextMock);
        when(teamcityClient.getVcsChange(eq(expectedJobName), eq(expectedVcsVersion)))
            .thenReturn(Futures.immediateFuture(vcsChangeMock));
        when(teamcityClient.getTeamcityJobConfigurationUrl(eq(expectedJobName))).thenReturn(expectedBuildConfigUrl);

        teamcityVcsChangeSupplier.waitForTeamcityVcsChangeAndGet(expectedVcsVersion, expectedJobName, jobContextMock);

        String expectedRevisionRequestMessage = String.format("Requesting revision '%s'", expectedVcsVersion);
        String expectedRevisionReceivedMessage = String.format("Revision '%s' received", expectedVcsVersion);

        TaskState taskState = testJobProgress.getTaskStates().get(Module.TEAMCITY.name() + "0");

        assertEquals(expectedRevisionRequestMessage, progressTexts.poll());
        assertEquals(expectedRevisionReceivedMessage, progressTexts.poll());
        assertEquals(RUNNING, taskState.getStatus());
        assertEquals(expectedBuildConfigUrl, taskState.getUrl());
    }
}
