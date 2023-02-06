package ru.yandex.market.tsum.cd;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.arcadia.ArcadiaCache;
import ru.yandex.market.tsum.arcadia.ArcadiaCachingHandler;
import ru.yandex.market.tsum.cd.vcs.ArcadiaQueueService;
import ru.yandex.market.tsum.cd.vcs.model.CdVcsQueueDao;
import ru.yandex.market.tsum.cd.vcs.model.CdVcsQueueEntity;
import ru.yandex.market.tsum.cd.vcs.model.VcsLastCheckEntity;
import ru.yandex.market.tsum.core.StoredObject;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.ArcadiaSettings;
import ru.yandex.market.tsum.release.dao.delivery.DeliveryMachineState;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.DirectoryRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.HaveNumberOfCommitsRule;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.LaunchRuleChecker;
import ru.yandex.market.tsum.release.dao.delivery.launch_rules.RuleGroup;
import ru.yandex.market.tsum.release.delivery.ArcadiaVcsChange;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 18/07/2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ArcadiaQueueServiceTest {

    @Mock
    private CdVcsQueueDao vcsDao;
    @Mock
    private ProjectsDao projectsDao;
    @Mock
    private CdService cdService;
    @Mock
    private ArcadiaCache arcadiaCache;

    @Captor
    private ArgumentCaptor<List<CdVcsQueueEntity>> queueSaveCaptor;

    private final LaunchRuleChecker launchRuleChecker =
        new LaunchRuleChecker(null, null, null, projectsDao);

    private ArcadiaQueueService service;

    @Before
    public void setup() {
        service = new ArcadiaQueueService(
            vcsDao, projectsDao, launchRuleChecker, cdService, arcadiaCache
        );

        List<RuleGroup> aRuleGroups = Arrays.asList(
            new RuleGroup(
                Arrays.asList(
                    new StoredObject<>(null, new DirectoryRule("/a/**"), null),
                    new StoredObject<>(null, new HaveNumberOfCommitsRule(3), null)
                )
            ),
            new RuleGroup(
                Arrays.asList(
                    new StoredObject<>(null, new DirectoryRule("/aa/**"), null),
                    new StoredObject<>(null, new HaveNumberOfCommitsRule(4), null)
                )
            )
        );
        DeliveryMachineEntity dmA = new DeliveryMachineEntity(
            "A", "A", new StoredObject<>(null, new ArcadiaSettings(), null), aRuleGroups
        );

        List<RuleGroup> bRuleGroups = Arrays.asList(
            new RuleGroup(
                Arrays.asList(
                    new StoredObject<>(null, new DirectoryRule("/b/**"), null)
                )
            )
        );
        DeliveryMachineEntity dmB = new DeliveryMachineEntity(
            "B", "B", new StoredObject<>(null, new ArcadiaSettings(), null), bRuleGroups

        );

        DeliveryMachineEntity dmC = new DeliveryMachineEntity(
            "C", "C", new StoredObject<>(null, new ArcadiaSettings(), null), bRuleGroups

        );

        when(projectsDao.list(Mockito.eq(true))).thenReturn(
            Collections.singletonList(new ProjectEntity("id", "title", Arrays.asList(dmA, dmB, dmC)))
        );

        when(vcsDao.stream()).thenReturn(
            Stream.of(
                new CdVcsQueueEntity(
                    "A",
                    new VcsLastCheckEntity(5, Instant.now(), false, aRuleGroups),
                    Arrays.asList(2L, 3L)
                ),
                new CdVcsQueueEntity(
                    "C",
                    new VcsLastCheckEntity(5, Instant.now(), true, aRuleGroups),
                    Arrays.asList(2L, 3L)
                )
            )
        );

        when(arcadiaCache.getHead()).thenReturn(
            new ArcadiaVcsChange(50L, Instant.now(), Collections.emptyList(), "", "")
        );

        when(arcadiaCache.get(Mockito.anySet())).thenReturn(
            Arrays.asList(
                new ArcadiaVcsChange(2L, Instant.now(), Arrays.asList("/a/a", "/aa/a"), "", ""),
                new ArcadiaVcsChange(3L, Instant.now(), Arrays.asList("/a/a", "/b/a"), "", "")
            )
        );

        when(cdService.getMachineStates(Mockito.anyCollection()))
            .thenReturn(
                Collections.singletonList(
                    new DeliveryMachineState("A", "2")
                )
            );

        List<ArcadiaVcsChange> changelog = Arrays.asList(
            new ArcadiaVcsChange(2L, Instant.now(), Arrays.asList("/a/a", "/a"), "", ""),
            new ArcadiaVcsChange(3L, Instant.now(), Arrays.asList("/a/a", "/a"), "", ""),
            new ArcadiaVcsChange(4L, Instant.now(), Arrays.asList("/b/a", "/bb/a"), "", ""),
            new ArcadiaVcsChange(5L, Instant.now(), Arrays.asList("/a/a", "/b/a"), "", ""),
            new ArcadiaVcsChange(6L, Instant.now(), Arrays.asList("/a/a", "/aa/a"), "", ""),
            new ArcadiaVcsChange(7L, Instant.now(), Arrays.asList("/c/a", "/a/a"), "", ""),
            new ArcadiaVcsChange(50L, Instant.now(), Arrays.asList("/d/a", "/d/a"), "", "")
        );
        doAnswer(
            invocation -> {
                changelog.forEach(
                    change -> ((ArcadiaCachingHandler) invocation.getArguments()[3]).handleLogEntry(change));
                return null;
            }
        )
            .when(arcadiaCache).getChangelog(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.any());
    }

    @Test
    public void updatesVcsQueueAndReturnsRevisionsToLaunch() {
        List<ArcadiaQueueService.RevisionToLaunch> revisionsToLaunch = service.update();
        verify(vcsDao).bulkSaveAll(queueSaveCaptor.capture(), Mockito.anyString(), Mockito.any());
        List<CdVcsQueueEntity> queues = queueSaveCaptor.getValue();
        Assert.assertEquals(3, queues.size());
        Assert.assertEquals(Arrays.asList(3L, 6L, 7L), queues.get(0).getRevisions());
        Assert.assertEquals(Arrays.asList(4L, 5L), queues.get(1).getRevisions());
        Assert.assertEquals(1, revisionsToLaunch.size());
        Assert.assertEquals("A", revisionsToLaunch.get(0).getMachineId());
        Assert.assertEquals("7", revisionsToLaunch.get(0).getRevision().toString());
        Assert.assertFalse(queues.get(2).getLastCheck().getOverflow());
    }
}
