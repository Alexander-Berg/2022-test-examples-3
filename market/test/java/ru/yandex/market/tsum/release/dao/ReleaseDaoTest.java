package ru.yandex.market.tsum.release.dao;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.mongodb.client.result.DeleteResult;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 24.05.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReleaseDaoTest {
    private static final String PROJECT_ID = "testId";
    private static final String PIPE_ID = "pipe id";
    private static final String DELIVERY_MACHINE_ID = "delivery-machine-id";
    private final String releaseTitle = "test title";
    private final String pipeLaunchId = "test launch id";
    private final int testInt = 42;
    private final String testString = "forty two";
    private final String pipeId = "pipe id";

    private final PipeLaunch pipeLaunch = new PipeLaunch();

    private final List<Resource> resources = Arrays.asList(
        new TestResourceInt(testInt),
        new TestResourceString(testString)
    );

    @Autowired
    MongoTemplate mongoTemplate;

    private ReleaseDao releaseDao;
    private Release firstRelease;
    private Release secondRelease;
    private Release thirdRelease;

    private final SourceCodeService sourceCodeService = mock(SourceCodeService.class);

    @Before
    public void setUp() throws Exception {
        releaseDao = new ReleaseDao(mongoTemplate);
    }

    private void saveInitialReleaseMocks() {
        Date date = new Date();
        firstRelease = createRelease(
            "1",
            DELIVERY_MACHINE_ID,
            new Date(date.getTime() + 1),
            new Date(date.getTime() + 3).toInstant()
        );

        secondRelease = createRelease(
            "2",
            DELIVERY_MACHINE_ID,
            new Date(date.getTime() + 2),
            new Date(date.getTime() + 2).toInstant()
        );

        thirdRelease = createRelease(
            "3",
            DELIVERY_MACHINE_ID,
            new Date(date.getTime() + 3),
            new Date(date.getTime() + 1).toInstant()
        );

        releaseDao.insertRelease(firstRelease);
        releaseDao.insertRelease(secondRelease);
        releaseDao.insertRelease(thirdRelease);
    }

    @Test
    public void insertRelease() throws Exception {
        Release release = createRelease();

        releaseDao.insertRelease(release);

        Release gotRelease = releaseDao.getLastRelease(PROJECT_ID, pipeId);
        List<Resource> gotResources = new ArrayList<>();
        for (StoredResource storedResource : gotRelease.getResources()) {
            gotResources.add(storedResource.instantiate(mongoTemplate.getConverter(), sourceCodeService));
        }


        checkReleaseAndResources(gotRelease, gotResources);
    }

    @Test
    public void addDisplayedProducedResources() {
        String jobId = "jobId";
        Release release = createRelease();


        releaseDao.insertRelease(release);
        releaseDao.addDisplayedProducedResources(
            release.getId(), jobId,
            resources.stream().map(
                r -> StoredResource.fromResource(r, pipeLaunch, mongoTemplate.getConverter())
            ).collect(Collectors.toList())
        );

        Release gotRelease = releaseDao.getLastRelease(PROJECT_ID, pipeId);
        List<Resource> gotResources = new ArrayList<>();
        for (StoredResource storedResource : gotRelease.getJobIdToDisplayedProducedResources().get(jobId)) {
            gotResources.add(storedResource.instantiate(mongoTemplate.getConverter(), sourceCodeService));
        }


        checkReleaseAndResources(gotRelease, gotResources);
    }

    @Test
    public void testGetPreviousRunningRelease_WhenPreviousReleaseExists() {
        saveInitialReleaseMocks();
        Assert.assertEquals(
            thirdRelease.getId(),
            releaseDao.getPreviousRunningRelease(secondRelease).getId()
        );
    }

    @Test
    public void testGetPreviousRunningRelease_WhenPreviousReleaseDoesNotExist() {
        saveInitialReleaseMocks();
        releaseDao.finishRelease(thirdRelease.getId(), new Date());

        Assert.assertNull(
            releaseDao.getPreviousRunningRelease(secondRelease)
        );
    }

    private Release createRelease() {
        return Release.builder()
            .withProjectId(PROJECT_ID)
            .withPipeId(pipeId)
            .withTitle(releaseTitle)
            .withPipeLaunchId(pipeLaunchId)
            .withResources(
                resources.stream().map(
                    r -> StoredResource.fromResource(r, pipeLaunch, mongoTemplate.getConverter())
                ).collect(Collectors.toList())
            )
            .withTriggeredBy("testUser")
            .build();
    }

    private void checkReleaseAndResources(Release gotRelease, List<Resource> gotResources) {
        Assert.assertEquals(PROJECT_ID, gotRelease.getProjectId());
        Assert.assertEquals(releaseTitle, gotRelease.getTitle());
        Assert.assertEquals(pipeLaunchId, gotRelease.getPipeLaunchIds().get(0));
        Assert.assertEquals(testInt, (int) ((TestResourceInt) gotResources.get(0)).getInteger());
        Assert.assertEquals(testString, ((TestResourceString) gotResources.get(1)).getString());
    }

    private static class TestResourceInt implements Resource {
        private final Integer integer;

        @PersistenceConstructor
        @JsonCreator
        TestResourceInt(Integer integer) {
            this.integer = integer;
        }

        public Integer getInteger() {
            return integer;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("7bc490d9-1356-4f0b-a02c-1847947c3ae3");
        }
    }

    private static class TestResourceString implements Resource {
        private final String string;

        @PersistenceConstructor
        @JsonCreator
        TestResourceString(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("88553d00-4cb9-4266-83ff-e55a01b575e7");
        }
    }

    @Test
    public void getReleases_ShouldNotIncludeFinishedWithNothingToRelease() {
        saveInitialReleaseMocks();

        releaseDao.setFinishing(secondRelease.getId(), FinishCause.nothingToRelease());
        releaseDao.setFinishing(firstRelease.getId(), FinishCause.nothingToRelease());
        releaseDao.finishRelease(firstRelease.getId(), new Date());

        List<Release> releases = releaseDao.getReleases(PROJECT_ID, true);
        Assert.assertEquals(toIdList(thirdRelease, secondRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_AllInclusive() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, thirdRelease.getId(), true, firstRelease.getId(), true,
            null, true, ReleaseDao.OrderBy.CREATED_DATE_DESC
        );

        Assert.assertEquals(toIdList(thirdRelease, secondRelease, firstRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_ExclusiveFrom() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, thirdRelease.getId(), false, firstRelease.getId(), true,
            null, true, ReleaseDao.OrderBy.CREATED_DATE_DESC
        );

        Assert.assertEquals(toIdList(secondRelease, firstRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_ExclusiveTo() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, thirdRelease.getId(), true, firstRelease.getId(), false,
            null, true, ReleaseDao.OrderBy.CREATED_DATE_DESC
        );

        Assert.assertEquals(toIdList(thirdRelease, secondRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_GetAllUpToParticularRelease() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, thirdRelease.getId(), true, null, false,
            null, true, ReleaseDao.OrderBy.CREATED_DATE_DESC
        );

        Assert.assertEquals(toIdList(thirdRelease, secondRelease, firstRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_GetLastNReleases() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, null, false, null, false,
            2, true, ReleaseDao.OrderBy.CREATED_DATE_DESC
        );

        Assert.assertEquals(toIdList(thirdRelease, secondRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_GetLastNReleases_OrderedByCommitDate() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, null, false, null, false,
            2, true, ReleaseDao.OrderBy.COMMIT_DATE_DESC
        );

        Assert.assertEquals(toIdList(firstRelease, secondRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_GetFromRelease_OrderedByCommitDate() {
        saveInitialReleaseMocks();

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, firstRelease.getId(), false, null, false,
            2, true, ReleaseDao.OrderBy.COMMIT_DATE_DESC
        );

        Assert.assertEquals(toIdList(secondRelease, thirdRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_GetFromRelease_OrderedByCommitDate_WithHotfix() {
        saveInitialReleaseMocks();

        Release hotfixRelease = createRelease(
            "4",
            DELIVERY_MACHINE_ID,
            new Date(thirdRelease.getCreatedDate().getTime() + 1),
            secondRelease.getCommit().getCreatedDate()
        );
        releaseDao.insertRelease(hotfixRelease);

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, hotfixRelease.getId(), false, null, false,
            2, true, ReleaseDao.OrderBy.COMMIT_DATE_DESC
        );

        Assert.assertEquals(toIdList(secondRelease, thirdRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_GetToRelease_OrderedByCommitDate_WithHotfix() {
        saveInitialReleaseMocks();

        Release hotfixRelease = createRelease(
            "4",
            DELIVERY_MACHINE_ID,
            new Date(thirdRelease.getCreatedDate().getTime() + 1),
            secondRelease.getCommit().getCreatedDate()
        );

        releaseDao.insertRelease(hotfixRelease);

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, null, false, secondRelease.getId(), false,
            2, true, ReleaseDao.OrderBy.COMMIT_DATE_DESC
        );

        Assert.assertEquals(toIdList(firstRelease, hotfixRelease), toIdList(releases));
    }

    @Test
    public void getReleasesByDeliveryMachineId_ShouldNotIncludeFinishedWithNothingToRelease() {
        saveInitialReleaseMocks();

        releaseDao.setFinishing(secondRelease.getId(), FinishCause.nothingToRelease());
        releaseDao.setFinishing(firstRelease.getId(), FinishCause.nothingToRelease());
        releaseDao.finishRelease(firstRelease.getId(), new Date());

        List<Release> releases = releaseDao.getReleasesByDeliveryMachineId(
            DELIVERY_MACHINE_ID, thirdRelease.getId(), true, firstRelease.getId(), true,
            null, true, ReleaseDao.OrderBy.CREATED_DATE_DESC
        );

        Assert.assertEquals(toIdList(thirdRelease, secondRelease), toIdList(releases));
    }

    @Test
    public void cleanOldReleasesBeforeDateWithFinishCause() {
        saveInitialReleaseMocks();

        Instant dateBefore = Instant.now().minus(Duration.ofDays(30));

        Release freshRelease = createRelease("1", DELIVERY_MACHINE_ID, new Date(), Instant.now());

        Release notOldEnoughRelease = createRelease(
            "2",
            DELIVERY_MACHINE_ID,
            Date.from(dateBefore.plusMillis(42)),
            dateBefore.plusMillis(42));

        Release oldRelease = createRelease(
            "3",
            DELIVERY_MACHINE_ID,
            Date.from(dateBefore.minusMillis(42)),
            dateBefore);

        Release oldButNotFinishedRelease = createRelease(
            "4",
            DELIVERY_MACHINE_ID,
            Date.from(dateBefore.minusMillis(42)),
            dateBefore);


        releaseDao.insertRelease(freshRelease);
        releaseDao.insertRelease(notOldEnoughRelease);
        releaseDao.insertRelease(oldButNotFinishedRelease);
        releaseDao.insertRelease(oldRelease);

        releaseDao.setFinishing(freshRelease.getId(), FinishCause.nothingToRelease());
        releaseDao.setFinishing(notOldEnoughRelease.getId(), FinishCause.nothingToRelease());
        releaseDao.setFinishing(oldRelease.getId(), FinishCause.nothingToRelease());

        DeleteResult deleteResult = releaseDao.cleanBeforeWithCause(dateBefore, FinishCause.nothingToRelease());

        assertEquals(1, deleteResult.getDeletedCount());
        assertNotNull(releaseDao.getRelease(freshRelease.getId()));
        assertNotNull(releaseDao.getRelease(notOldEnoughRelease.getId()));
        assertNotNull(releaseDao.getRelease(oldButNotFinishedRelease.getId()));
        assertNull(releaseDao.getRelease(oldRelease.getId()));
    }

    private List<String> toIdList(Release... releaseList) {
        return Arrays.stream(releaseList).map(Release::getId).collect(Collectors.toList());
    }

    private List<String> toIdList(List<Release> releaseList) {
        return releaseList.stream().map(Release::getId).collect(Collectors.toList());
    }

    private Release createRelease(String revision, String deliveryMachineId, Date createdDate, Instant commitDate) {
        DeliveryPipelineParams deliveryPipelineParams = new DeliveryPipelineParams(
            revision, null, null
        );

        PipeLaunch emptyPipeLaunch = new PipeLaunch();

        return Release.builder()
            .withProjectId(PROJECT_ID)
            .withPipeId(PIPE_ID)
            .withTitle("#" + revision)
            .withPipeLaunchId(new ObjectId().toString())
            .withResources(
                Collections.singletonList(
                    StoredResource.fromResource(deliveryPipelineParams, emptyPipeLaunch, mongoTemplate.getConverter())
                )
            )
            .withTriggeredBy("testUser")
            .withDeliveryMachineId(deliveryMachineId)
            .withCommit(deliveryPipelineParams.getRevision(), commitDate)
            .withCreatedDate(createdDate)
            .build();
    }
}
