package ru.yandex.market.tsum.release.subscribers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.entity.project.RenderableResourceEntity;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceDao;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRefContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChange;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;

import static org.mockito.Mockito.mock;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 13.03.18
 */
@RunWith(MockitoJUnitRunner.class)
public class ReleaseProducedResourcesSubscriberTest {
    private static final ObjectId PIPE_LAUNCH_ID = new ObjectId(new Date());
    private static final String PIPE_ID = "pipeId";
    private static final String PROJECT_ID = "testProject";
    private static final String JOB_ID = "jobId";
    private static final ResourceRef RESOURCE_REF = new ResourceRef(
        new ObjectId(new Date()), "className", UUID.randomUUID()
    );
    private static final String RELEASE_ID = "releaseId";

    @Mock
    private ReleaseDao releaseDao;

    @Mock
    private ProjectsDao projectsDao;

    @Mock
    private ResourceDao resourceDao;

    @Mock
    private PipeLaunch pipeLaunch;

    @Mock
    private SourceCodeService sourceCodeService;

    private static class TestResource implements Resource {
        private int field;

        public int getField() {
            return field;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("d452e991-50e3-4827-ab2f-8115c4fa73f9");
        }
    }

    @Before
    public void setUp() {
        Mockito.when(pipeLaunch.getId()).thenReturn(PIPE_LAUNCH_ID);

        StoredResourceContainer resourceContainer = new StoredResourceContainer(
            Collections.singletonList(
                new StoredResource(
                    new ObjectId(), PIPE_ID, PIPE_LAUNCH_ID, TestResource.class.getName(), UUID.randomUUID(),
                    new Document(), Collections.emptyList()
                )
            ),
            sourceCodeService
        );

        Mockito.when(resourceDao.loadResources(Mockito.any())).thenReturn(resourceContainer);

        ProjectEntity project = new ProjectEntity(
            "projectId",
            "project title",
            Collections.singletonList(new RenderableResourceEntity("title", Resource.class, UUID.randomUUID()))
        );

        Mockito.when(projectsDao.get(PROJECT_ID)).thenReturn(project);

        Release release = mock(Release.class);
        Mockito.when(release.getId()).thenReturn(RELEASE_ID);
        Mockito.when(release.getProjectId()).thenReturn(PROJECT_ID);
        Mockito.when(releaseDao.getReleaseByPipeLaunchId(PIPE_LAUNCH_ID)).thenReturn(release);
    }

    @Test
    public void saveResourcesIfProducedAndSucceeded() {
        createJobState(StatusChange.executorSucceeded(), true);

        runSubscriber();

        checkInvocations(1);
    }

    @Test
    public void notSaveResourcesIfJobFailed() {
        createJobState(StatusChange.executorFailed(), true);

        runSubscriber();

        checkInvocations(0);
    }

    @Test
    public void notSaveResourcesIfNotProduced() {
        createJobState(StatusChange.executorSucceeded(), false);

        runSubscriber();

        checkInvocations(0);
    }

    private void createJobState(StatusChange lastStatusChange, boolean isProducedResources) {
        JobState jobState = mock(JobState.class);

        JobLaunch jobLaunch = new JobLaunch(
            0, "user42", null,
            Arrays.asList(StatusChange.queued(), StatusChange.running(), lastStatusChange)
        );
        if (isProducedResources) {
            jobLaunch.setProducedResources(
                new ResourceRefContainer(Collections.singletonList(RESOURCE_REF))
            );
        }

        Mockito.when(jobState.isProducesResources()).thenReturn(isProducedResources);
        Mockito.when(jobState.getLastStatusChangeType()).thenCallRealMethod();
        Mockito.when(jobState.getLastLaunch()).thenReturn(jobLaunch);

        Mockito.when(pipeLaunch.getJobState(JOB_ID)).thenReturn(jobState);
    }

    private void runSubscriber() {
        ReleaseProducedResourcesSubscriber subscriber = new ReleaseProducedResourcesSubscriber(
            releaseDao, projectsDao, resourceDao
        );

        subscriber.jobExecutorHasFinished(
            new FullJobLaunchId(PIPE_LAUNCH_ID.toString(), JOB_ID, 1),
            pipeLaunch
        );
    }

    private void checkInvocations(int timesWanted) {
        Mockito.verify(releaseDao, Mockito.times(timesWanted)).addDisplayedProducedResources(
            Mockito.eq(RELEASE_ID), Mockito.eq(JOB_ID), Mockito.any()
        );
        Mockito.verify(resourceDao, Mockito.times(timesWanted)).loadResources(Mockito.any());
    }
}
