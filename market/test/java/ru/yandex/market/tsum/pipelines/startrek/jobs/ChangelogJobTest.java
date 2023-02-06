package ru.yandex.market.tsum.pipelines.startrek.jobs;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.wc.SVNRevision;

import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.ResourcesJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.pipelines.startrek.config.StartCommitResource;
import ru.yandex.market.tsum.pipelines.startrek.config.StartrekPipelineConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Filippov <rolenof@yandex-team.ru>
 */
@RunWith(MockitoJUnitRunner.class)
public class ChangelogJobTest {

    @InjectMocks
    private ChangelogJob changelogJob = new ChangelogJob();

    @Mock
    private StartrekPipelineConfig startrekPipelineConfig;
    @Mock
    private StartCommitResource startCommitResource;
    @Mock
    private ArcadiaRef arcadiaRef;
    @Mock
    private RootArcadiaClient rootArcadiaClient;

    @Mock
    private JobContext context;
    @Mock
    private JobActionsContext actionsContext;
    @Mock
    private JobProgressContext progressContext;
    @Mock
    private ResourcesJobContext resourcesContext;


    @Before
    public void setUp() {
        when(context.actions()).thenReturn(actionsContext);
        when(context.progress()).thenReturn(progressContext);
        when(context.resources()).thenReturn(resourcesContext);

        when(startCommitResource.getCommit()).thenReturn("123456");
        when(arcadiaRef.getRef()).thenReturn("arcadia:/arc/trunk/tracker/separator");
    }

    @Test
    public void testEmptyChangelog() throws Exception {
        when(startrekPipelineConfig.isFailOnEmptyChangelog()).thenReturn(true);
        when(rootArcadiaClient.getChangelog(SVNRevision.create(123456L),
            SVNRevision.HEAD, 100, false,
            Collections.singletonList("/trunk/tracker/separator")))
            .thenReturn(Collections.emptyList());
        changelogJob.execute(context);

        verify(actionsContext).failJob("Changelog is empty", SupportType.NONE);
    }

    @Test
    public void testChangelog() throws Exception {
        when(rootArcadiaClient.getChangelog(SVNRevision.create(123456L),
            SVNRevision.HEAD, 100, false,
            Collections.singletonList("/trunk/tracker/separator")))
            .thenReturn(Collections.singletonList(new SVNLogEntry(ImmutableMap.of("/trunk/tracker/separator",
                new SVNLogEntryPath("/trunk/tracker/separator/app.conf", 'M', null, -1)),
                123456, "author", new Date(), "message")));

        doAnswer(invocation -> {
            ChangelogInfo changeLog = invocation.getArgument(0);
            List<ChangelogEntry> changelogEntries = changeLog.getChangelogEntries();
            assertEquals(1, changelogEntries.size());
            return null;
        }).when(resourcesContext).produce(any(ChangelogInfo.class));

        changelogJob.execute(context);

        verify(progressContext).updateText(anyString());
        verify(resourcesContext).produce(any(ChangelogInfo.class));
        verifyZeroInteractions(actionsContext);
    }
}
