package ru.yandex.market.tsum.release.delivery;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.tmatesoft.svn.core.SVNLogEntry;

import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.tsum.clients.arcadia.TrunkArcadiaClient;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineStateDao;
import ru.yandex.market.tsum.release.dao.delivery.VcsPollingStateDao;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 19.02.18
 */
@RunWith(MockitoJUnitRunner.class)
public class ArcadiaPollTaskTest {
    private static final String PROJECT_ID = "test";
    private static final String STAGE_GROUP_ID = "stage-group-id";
    private static final String PIPE_ID = "pipe-id";

    @Mock
    private TrunkArcadiaClient arcadiaClient;

    @Mock
    private ProjectsDao projectsDao;

    @Mock
    private DeliveryMachineStateDao deliveryMachineStateDao;

    @Mock
    private VcsPollingStateDao vcsPollingStateDao;

    private ArcadiaPollTask sut;

    @Mock
    private MongoConverter mongoConverter;

    @Before
    public void setUp() {
        ProjectEntity projectEntity = getProject(mongoConverter);

        when(arcadiaClient.getHead()).thenReturn(new SVNLogEntry(null, 1L, "user42", new Date(), null));
        when(projectsDao.stream()).thenReturn(Stream.of(projectEntity));

        when(
            vcsPollingStateDao.getLastCheckedRevision(
                ArcadiaPollTask.ARCADIA_SVN,
                ArcadiaPollTask.ARCADIA_TRUNK
            )
        ).thenReturn(Optional.empty());

        sut = new ArcadiaPollTask(
            arcadiaClient, projectsDao, deliveryMachineStateDao, vcsPollingStateDao, new ComplicatedMonitoring()
        );
    }

    @Test
    public void thereAreNewCommits() throws Exception {
        sut.execute(mock(ExecutionContext.class));

        verify(deliveryMachineStateDao, times(1)).setLastUnprocessedRevision(STAGE_GROUP_ID, "1");
        verify(
            vcsPollingStateDao, times(1)
        ).setLastCheckedRevision(ArcadiaPollTask.ARCADIA_SVN, ArcadiaPollTask.ARCADIA_TRUNK, "1");
    }

    @Test
    public void thereAreNoNewCommits() throws Exception {
        when(
            vcsPollingStateDao.getLastCheckedRevision(
                ArcadiaPollTask.ARCADIA_SVN,
                ArcadiaPollTask.ARCADIA_TRUNK
            )
        ).thenReturn(Optional.of("1"));

        sut.execute(mock(ExecutionContext.class));

        verify(deliveryMachineStateDao, never()).setLastUnprocessedRevision(any(), any());
        verify(vcsPollingStateDao, never()).setLastCheckedRevision(any(), any(), any());
    }

    static ProjectEntity getProject(MongoConverter mongoConverter) {
        return new ProjectEntity(
            PROJECT_ID, "test title",
            Collections.singletonList(
                new DeliveryMachineEntity(DeliveryMachineSettings.builder()
                    .withArcadiaSettings()
                    .withStageGroupId(STAGE_GROUP_ID)
                    .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                    .build(), mongoConverter)
            )
        );
    }
}
