package ru.yandex.market.tsum.release.delivery;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketCommit;
import ru.yandex.market.tsum.clients.bitbucket.models.BitbucketUser;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineState;
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
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.01.18
 */
public class BitbucketPollTaskTest {
    private static final String PROJECT_ID = "test";
    private static final String STAGE_GROUP_ID = "stage-group-id";
    private static final String PIPE_ID = "pipe-id";
    private static final String BITBUCKET_PROJECT_NAME = "market-infra";
    private static final String REPOSITORY_NAME = "test-pipeline";
    private static final String REPOSITORY_FULL_NAME = BITBUCKET_PROJECT_NAME + "/" + REPOSITORY_NAME;
    private static final String MAIN_BRANCH = "master";
    private static final String TEAMCITY_USER = "teamcity";

    private BitbucketClient bitbucketClient;
    private ProjectsDao projectsDao;
    private DeliveryMachineStateDao deliveryMachineStateDao;
    private VcsPollingStateDao vcsPollingStateDao;

    private BitbucketPollTask sut;
    private ComplicatedMonitoring complicatedMonitoring;

    @Before
    public void setUp() {
        ProjectEntity projectEntity = getProject();

        bitbucketClient = mock(BitbucketClient.class);
        Iterator<BitbucketCommit> commitIterator = Stream.of(
            commit("3", "user42"), commit("2", "user42"), commit("1", "user42")
        ).iterator();

        when(bitbucketClient.getCommitIterator(any(), any(), any())).thenReturn(commitIterator);

        projectsDao = mock(ProjectsDao.class);
        when(projectsDao.stream()).thenReturn(Stream.of(projectEntity));

        vcsPollingStateDao = mock(VcsPollingStateDao.class);
        when(vcsPollingStateDao.getLastCheckedRevision(REPOSITORY_FULL_NAME, MAIN_BRANCH)).thenReturn(Optional.empty());

        deliveryMachineStateDao = mock(DeliveryMachineStateDao.class);
        complicatedMonitoring = new ComplicatedMonitoring();

        sut = new BitbucketPollTask(
            bitbucketClient, projectsDao, deliveryMachineStateDao, vcsPollingStateDao, complicatedMonitoring
        );
    }

    @Test
    public void thereAreNewCommits() throws Exception {
        DeliveryMachineState deliveryMachineState = new DeliveryMachineState(STAGE_GROUP_ID, null);
        when(deliveryMachineStateDao.getById(STAGE_GROUP_ID)).thenReturn(Optional.of(deliveryMachineState));

        sut.execute(mock(ExecutionContext.class));

        verify(deliveryMachineStateDao, times(1)).setLastUnprocessedRevision(STAGE_GROUP_ID, "3");
        verify(vcsPollingStateDao, times(1)).setLastCheckedRevision(REPOSITORY_FULL_NAME, MAIN_BRANCH, "3");
    }

    @Test
    public void thereAreNoNewCommits() throws Exception {
        DeliveryMachineState deliveryMachineState = new DeliveryMachineState(STAGE_GROUP_ID, null);
        when(deliveryMachineStateDao.getById(STAGE_GROUP_ID)).thenReturn(Optional.of(deliveryMachineState));

        when(vcsPollingStateDao.getLastCheckedRevision(REPOSITORY_FULL_NAME, MAIN_BRANCH)).thenReturn(Optional.of("3"));

        sut.execute(mock(ExecutionContext.class));

        verify(deliveryMachineStateDao, never()).setLastUnprocessedRevision(any(), any());
        verify(vcsPollingStateDao, never()).setLastCheckedRevision(any(), any(), any());
    }

    @Test
    public void shouldIgnoreTeamcityCommits() throws Exception {
        Iterator<BitbucketCommit> commitIterator = Stream.of(
            commit("3", TEAMCITY_USER), commit("2", TEAMCITY_USER), commit("1", "user42")
        ).iterator();
        when(bitbucketClient.getCommitIterator(any(), any(), any())).thenReturn(commitIterator);

        DeliveryMachineState deliveryMachineState = new DeliveryMachineState(STAGE_GROUP_ID, null);
        when(deliveryMachineStateDao.getById(STAGE_GROUP_ID)).thenReturn(Optional.of(deliveryMachineState));

        when(vcsPollingStateDao.getLastCheckedRevision(REPOSITORY_FULL_NAME, MAIN_BRANCH)).thenReturn(Optional.of("1"));

        sut.execute(mock(ExecutionContext.class));

        verify(deliveryMachineStateDao, never()).setLastUnprocessedRevision(any(), any());
        verify(vcsPollingStateDao, never()).setLastCheckedRevision(any(), any(), any());
    }

    static ProjectEntity getProject() {
        ProjectEntity result = new ProjectEntity();
        result.setId(PROJECT_ID);
        result.setTitle("test title");
        result.setDeliveryMachines(
            Collections.singletonList(
                new DeliveryMachineEntity(
                    DeliveryMachineSettings.builder()
                        .withBitbucketSettings(BITBUCKET_PROJECT_NAME, REPOSITORY_NAME, MAIN_BRANCH)
                        .withStageGroupId(STAGE_GROUP_ID)
                        .withPipeline(PIPE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                        .build(),
                    mock(MongoConverter.class)
                )
            )
        );

        return result;
    }

    private static BitbucketCommit commit(String revision, String userName) {
        BitbucketCommit bitbucketCommit = Mockito.mock(BitbucketCommit.class);
        Mockito.when(bitbucketCommit.getId()).thenReturn(revision);

        BitbucketUser user = Mockito.mock(BitbucketUser.class);
        Mockito.when(user.getName()).thenReturn(userName);
        Mockito.when(bitbucketCommit.getAuthor()).thenReturn(user);

        Mockito.when(bitbucketCommit.getMessage()).thenReturn("message");

        Mockito.when(bitbucketCommit.getCommitterTimestampMillis()).thenReturn(new Date().getTime());
        Mockito.when(bitbucketCommit.getCommitter()).thenReturn(user);

        return bitbucketCommit;
    }
}
