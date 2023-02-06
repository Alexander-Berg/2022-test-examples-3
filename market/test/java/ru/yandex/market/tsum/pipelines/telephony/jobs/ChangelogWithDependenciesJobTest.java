package ru.yandex.market.tsum.pipelines.telephony.jobs;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.arcadia.ArcadiaCache;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.SandboxTaskRunner;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskPriority;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.startrek.config.StartCommitResource;
import ru.yandex.market.tsum.pipelines.telephony.config.ChangelogConfig;
import ru.yandex.market.tsum.release.delivery.ArcadiaVcsChange;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChangelogWithDependenciesJobTest {

    @InjectMocks
    private ChangelogWithDependenciesJob changelogJob = new ChangelogWithDependenciesJob();

    @Mock
    private ChangelogConfig config;
    @Mock
    private SandboxTaskJobConfig sandboxTaskJobConfig;
    @Mock
    private StartCommitResource startCommitResource;
    @Mock
    private ArcadiaRef arcadiaRef;
    @Mock
    private RootArcadiaClient rootArcadiaClient;
    @Mock
    private ArcadiaCache arcadiaCache;


    @Mock
    private TsumJobContext context;
    @Mock
    private JobActionsContext actionsContext;
    @Mock
    private JobProgressContext progressContext;
    @Mock
    private ResourcesJobContext resourcesContext;
    @Mock
    private PipeLaunch pipeLaunch;
    @Mock
    private SandboxClient sandboxClient;
    @Mock
    private SandboxTaskRunner taskRunner;
    @Mock
    private SandboxTask sandboxTask;
    @Mock
    private TaskResource sandboxResource;
    @Mock
    private ChangelogInfo changelogInfo;


    @Before
    public void setUp() throws TimeoutException, InterruptedException {
        when(config.getModulePath()).thenReturn("yabs/telephony/platform");
        when(sandboxTaskJobConfig.getTaskType()).thenReturn("MARKET_GENERATE_CHANGELOG_TASK");
        when(sandboxTaskJobConfig.getPriority()).thenReturn(SandboxTaskPriority.SERVICE_HIGH);
        when(context.resources()).thenReturn(resourcesContext);
        when(context.getPipeLaunch()).thenReturn(pipeLaunch);
        when(sandboxClient.newSandboxTaskRunner()).thenReturn(taskRunner);
        when(taskRunner.run()).thenReturn(sandboxTask);
        when(taskRunner.withJobTaskTags(any())).thenCallRealMethod();
        when(taskRunner.withTaskInput(any())).thenCallRealMethod();
        when(taskRunner.withListener(any())).thenCallRealMethod();
        when(taskRunner.withMaxExecutionDuration(any())).thenCallRealMethod();
        when(taskRunner.withRetryOnExceptionCount(anyInt())).thenCallRealMethod();
        when(sandboxTask.getStatus()).thenReturn("SUCCESS");
        when(sandboxClient.getResources(anyLong())).thenReturn(Arrays.asList(sandboxResource));
        when(sandboxResource.getType()).thenReturn("TSUM_JSON_RESOURCE");
        when(sandboxClient.getResource(any(), any())).thenReturn(changelogInfo);

        when(startCommitResource.getCommit()).thenReturn("123456");
    }

    @Test
    public void testEmptyChangelog() throws Exception {
        doAnswer(invocation -> {
            ChangelogInfo changeLog = invocation.getArgument(0);
            List<ChangelogEntry> changelogEntries = changeLog.getChangelogEntries();
            assertEquals(0, changelogEntries.size());
            return null;
        }).when(resourcesContext).produce(any(ChangelogInfo.class));
        changelogJob.execute(context);
        verify(resourcesContext).produce(any(ChangelogInfo.class));
    }

    @Test
    public void testChangelog() throws Exception {
        when(arcadiaCache.get(anyLong()))
            .thenReturn(
                new ArcadiaVcsChange(123456L, Instant.now(), Collections.emptyList(), "", ""));

        ChangelogEntry entry = Mockito.mock(ChangelogEntry.class);
        when(entry.extractArcadiaRevision()).thenReturn(123456L);
        when(changelogInfo.getChangelogEntries()).thenReturn(Arrays.asList(entry));

        doAnswer(invocation -> {
            ChangelogInfo changeLog = invocation.getArgument(0);
            List<ChangelogEntry> changelogEntries = changeLog.getChangelogEntries();
            assertEquals(1, changelogEntries.size());
            return null;
        }).when(resourcesContext).produce(any(ChangelogInfo.class));

        changelogJob.execute(context);

        verify(resourcesContext).produce(any(ChangelogInfo.class));
    }
}
