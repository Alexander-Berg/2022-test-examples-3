package ru.yandex.market.tsum.registry.v2.tasks.updatecomponentstatus;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import junit.framework.TestCase;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.registry.v2.TestComponentBuilder;
import ru.yandex.market.tsum.registry.v2.dao.ComponentUpdateRequestDao;
import ru.yandex.market.tsum.registry.v2.dao.ComponentsDao;
import ru.yandex.market.tsum.registry.v2.dao.InstallationsDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentStatus;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentUpdateRequest;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.ReleaseState;
import ru.yandex.startrek.client.Session;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UpdateComponentStatusWorkerTest extends TestCase {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private MongoTemplate mongoTemplate;

    private ComponentsDao componentsDao;
    private ComponentUpdateRequestDao componentUpdateRequestDao;
    private ReleaseDao releaseDao;

    private Component component;
    private ComponentUpdateRequest componentUpdateRequest;
    private Release release;

    UpdateComponentStatusWorker worker;

    @Before
    public void setUp() {
        InstallationsDao installationsDao = new InstallationsDao(mongoTemplate);
        this.componentsDao = new ComponentsDao(mongoTemplate, installationsDao);
        this.releaseDao = new ReleaseDao(mongoTemplate);
        this.componentUpdateRequestDao = new ComponentUpdateRequestDao(mongoTemplate);

        String releaseId = new ObjectId().toHexString();
        release = Release.builder()
            .withId(releaseId)
            .withProjectId(new ObjectId().toHexString())
            .withPipeId(new ObjectId().toHexString())
            .withPipeLaunchId(new ObjectId().toHexString())
            .withState(ReleaseState.FINISHED)
            .build();
        releaseDao.insertRelease(release);

        component = new TestComponentBuilder()
            .withRandomName()
            .withRandomAbcSlug()
            .withStatus(ComponentStatus.IDLE)
            .build();

        componentUpdateRequest = new ComponentUpdateRequest("", component.getId(), releaseId, "", "");

        component.setUpdateRequestId(componentUpdateRequest.getId());
        componentsDao.save(component);
        componentUpdateRequestDao.save(componentUpdateRequest);

        worker = new UpdateComponentStatusWorker(componentsDao, componentUpdateRequestDao, releaseDao,
            null, null, null);
    }

    @Test
    public void testUpdateStatus_componentStatusIsCreatingAndPipelineNotFinished_nothingChanged() {
        componentsDao.updateStatus(component, ComponentStatus.CREATING);

        release.setState(ReleaseState.IN_PROGRESS);
        releaseDao.save(release);

        worker.updateStatus();

        Component updatedComponent = componentsDao.get(component.getId());

        assertEquals(ComponentStatus.CREATING, updatedComponent.getStatus());
    }

    @Test
    public void testUpdateStatus_componentStatusIsCreatingAndPipelineFinished_statusChangedToIDLE() {
        componentsDao.updateStatus(component, ComponentStatus.CREATING);

        worker.updateStatus();

        Component updatedComponent = componentsDao.get(component.getId());

        assertEquals(ComponentStatus.IDLE, updatedComponent.getStatus());
    }

    @Test
    public void testUpdateStatus_componentStatusIsUpdatingAndPipelineFinished_statusChangedToIDLE() {
        componentsDao.updateStatus(component, ComponentStatus.UPDATING);

        worker.updateStatus();

        Component updatedComponent = componentsDao.get(component.getId());

        assertEquals(ComponentStatus.IDLE, updatedComponent.getStatus());
    }

    @Test
    public void testUpdateStatus_componentStatusIsArchivingAndPipelineFinished_statusChangedToARCHIVED() {
        componentsDao.updateStatus(component, ComponentStatus.ARCHIVING);

        worker.updateStatus();

        Component updatedComponent = componentsDao.get(component.getId());

        assertEquals(ComponentStatus.ARCHIVED, updatedComponent.getStatus());
    }
}