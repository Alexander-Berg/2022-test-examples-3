package ru.yandex.market.tsum.pipelines.common.jobs.sandbox;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxTaskId;

public class SandboxReleaseJobTest {
    @Mock
    private SandboxClient sandboxClient;

    @InjectMocks
    SandboxReleaseJob sut = new SandboxReleaseTestJob();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void finishImmediatelyIfNoMatchingResourceTypePassed() throws Exception {
        sut.setConfig(
            SandboxReleaseJobConfig.builder(SandboxReleaseType.TESTING)
                .withSandboxResourceType("requiredResourceType")
                .build()
        );

        sut.setSandboxTaskIds(
            Collections.singletonList(new SandboxTaskId("type1", 1L, "someResourceType"))
        );

        sut.execute(new TestJobContext());
        Mockito.verifyZeroInteractions(sandboxClient);
    }

    @Test
    public void pollSandboxResourceRelease() throws Exception {
        sut.setConfig(
            SandboxReleaseJobConfig.builder(SandboxReleaseType.TESTING).build()
        );
        sut.setTsumSandboxUrl("https://sandbox.yandex-team.ru");

        long id = 1;
        SandboxTaskId taskId = new SandboxTaskId("testType", id, "testResourceType");

        Mockito.when(sandboxClient.getTask(taskId.getId()))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_SUCCESS_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASING_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASED_STATUS));

        sut.pollSandboxResourceRelease(new TestJobContext(), taskId);

        Mockito.verify(sandboxClient, Mockito.times(3)).getTask(id);
    }

    private static class SandboxReleaseTestJob extends SandboxReleaseJob {
        @Override
        protected Poller.PollerBuilder<SandboxTask> createPoller() {
            return super.createPoller().allowIntervalLessThenOneSecond(true).interval(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("b1aa84c7-fa78-4df3-8181-fa01df059b22");
        }
    }

}
