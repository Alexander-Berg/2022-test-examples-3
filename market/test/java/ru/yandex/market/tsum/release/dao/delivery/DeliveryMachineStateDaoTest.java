package ru.yandex.market.tsum.release.dao.delivery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import io.reactivex.Observable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.delivery.DeliveryMachineStateVersionService;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 15.01.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DeliveryMachineStateDaoTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DeliveryMachineStateDaoTest {
    private static final String STABLE_REVISION = "6939c75d1";

    private static final String DELIVERY_MACHINE_ID = "some-delivery-machine";

    @Autowired
    private DeliveryMachineStateDao sut;

    @Autowired
    private DeliveryMachineStateVersionService versionService;

    @Test
    public void nonExistingRevision() {
        Assert.assertNull(sut.getStableRevision(DELIVERY_MACHINE_ID));
    }

    @Test
    public void saveThenGetStableRevision() {
        sut.setStableRevision(DELIVERY_MACHINE_ID, STABLE_REVISION);
        Assert.assertEquals(STABLE_REVISION, sut.getStableRevision(DELIVERY_MACHINE_ID));
        Assert.assertEquals(
            1,
            (long) sut.getById(DELIVERY_MACHINE_ID).map(DeliveryMachineState::getVersion).get()
        );
    }

    @Test
    public void setLastUnprocessedRevision() {
        sut.setLastUnprocessedRevision(DELIVERY_MACHINE_ID, "0");
        DeliveryMachineState state = sut.getById(DELIVERY_MACHINE_ID).get();
        Assert.assertEquals("0", state.getLastUnprocessedRevision());
        Assert.assertNull(state.getLastProcessedRevision());
        Assert.assertEquals(1, (long) state.getVersion());

        sut.setLastUnprocessedRevision(DELIVERY_MACHINE_ID, "5");
        state = sut.getById(DELIVERY_MACHINE_ID).get();
        Assert.assertEquals("5", state.getLastUnprocessedRevision());
        Assert.assertNull(state.getLastProcessedRevision());
        Assert.assertEquals(2, (long) state.getVersion());
    }

    @Test
    public void setLastProcessedRevisionWhenItEqualsToUnprocessedRevision() {
        // arrange
        sut.setLastUnprocessedRevision(DELIVERY_MACHINE_ID, "0");

        // act
        sut.setLastProcessedRevision(DELIVERY_MACHINE_ID, "0");

        // assert
        DeliveryMachineState state = sut.getById(DELIVERY_MACHINE_ID).get();
        Assert.assertEquals("0", state.getLastProcessedRevision());
        Assert.assertNull(state.getLastUnprocessedRevision());
        Assert.assertEquals(2, (long) state.getVersion());
    }


    @Test
    public void setLastProcessedRevisionWhenItDoesNotEqualToUnprocessedRevision() {
        // arrange
        sut.setLastUnprocessedRevision(DELIVERY_MACHINE_ID, "5");

        // act
        sut.setLastProcessedRevision(DELIVERY_MACHINE_ID, "0");

        // assert
        DeliveryMachineState state = sut.getById(DELIVERY_MACHINE_ID).get();
        Assert.assertEquals("0", state.getLastProcessedRevision());
        Assert.assertEquals("5", state.getLastUnprocessedRevision());
        Assert.assertEquals(2L, (long) state.getVersion());
    }

    @Test
    public void testObserve() {
        Observable<Integer> versionObservable = Observable.range(1, 5)
            .doOnNext(version -> {
                // up version
                sut.setStableRevision(DELIVERY_MACHINE_ID, STABLE_REVISION);
            });

        Mockito.when(versionService.observe(DELIVERY_MACHINE_ID))
            .thenReturn(versionObservable.map(Long::new));

        List<Long> versionUpdates =
            sut.observeState(DELIVERY_MACHINE_ID).map(DeliveryMachineState::getVersion).toList().blockingGet();
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), versionUpdates);
    }

    @Test
    public void observeShouldSkipVersionDuplicates() {
        Observable<Integer> versionObservable = Observable.just(1, 1, 1, 2, 2)
            .doOnNext(version -> {
                long currentVersion = sut.getById(DELIVERY_MACHINE_ID)
                    .map(DeliveryMachineState::getVersion)
                    .orElse(0L);

                if (currentVersion < version) {
                    sut.setStableRevision(DELIVERY_MACHINE_ID, STABLE_REVISION);
                }
            });

        Mockito.when(versionService.observe(DELIVERY_MACHINE_ID))
            .thenReturn(versionObservable.map(Long::new));

        List<Long> versionUpdates =
            sut.observeState(DELIVERY_MACHINE_ID).map(DeliveryMachineState::getVersion).toList().blockingGet();
        Assert.assertEquals(Arrays.asList(1L, 2L), versionUpdates);
    }

    @Test
    public void testEnqueueTriggerRequest() {
        // просто чтобы заинсёртить стейт
        sut.setStableRevision(DELIVERY_MACHINE_ID, "100499");

        sut.enqueueTriggerRequest(DELIVERY_MACHINE_ID, "100500", "algebraic", true);

        DeliveryMachineState machineState = sut.getById(DELIVERY_MACHINE_ID).get();
        Assert.assertEquals(1, machineState.getTriggerRequests().size());

        TriggerRequest triggerRequest = machineState.getTriggerRequests().get(0);
        Assert.assertEquals("100500", triggerRequest.getRevision());
        Assert.assertEquals("algebraic", triggerRequest.getUser());
        Assert.assertEquals(true, triggerRequest.isFix());
    }

    @Test(expected = TriggerRequestConflictException.class)
    public void shouldNotInsertSecondTriggerRequest() {
        sut.insert(new DeliveryMachineState(DELIVERY_MACHINE_ID));

        sut.enqueueTriggerRequest(DELIVERY_MACHINE_ID, "100500", "algebraic", true);
        sut.enqueueTriggerRequest(DELIVERY_MACHINE_ID, "100500", "algebraic", true);
    }

    @Test(expected = TriggerRequestConflictException.class)
    public void shouldNotInsertTriggerRequestIfHotfixIsExists() {
        sut.insert(new DeliveryMachineState(DELIVERY_MACHINE_ID));

        sut.enqueueHotfix(DELIVERY_MACHINE_ID, "1", ImmutableSet.of("2", "3"), "me", PipelineType.HOTFIX, true, false,
            false);
        sut.enqueueTriggerRequest(DELIVERY_MACHINE_ID, "100500", "algebraic", true);
    }

    @Test
    public void testEnqeueHotfix() {
        sut.insert(new DeliveryMachineState(DELIVERY_MACHINE_ID));

        sut.enqueueHotfix(DELIVERY_MACHINE_ID, "1", ImmutableSet.of("2", "3"), "me", PipelineType.HOTFIX, true, false,
            false);

        DeliveryMachineState machineState = sut.getById(DELIVERY_MACHINE_ID).get();
        Assert.assertEquals(1, machineState.getHotfixesParams().size());

        HotfixParams hotfixParams = machineState.getHotfixesParams().get(0);
        Assert.assertEquals("1", hotfixParams.getBaseRevision());
        Assert.assertEquals(ImmutableSet.of("2", "3"), hotfixParams.getRevisions());
    }

    @Configuration
    @Import(TestMongo.class)
    public static class Config {
        @Autowired
        private MongoTemplate mongoTemplate;

        @Bean
        public DeliveryMachineStateDao deliveryMachineStateDao() {
            ProjectsDao projectsDao = Mockito.mock(ProjectsDao.class);
            Mockito.when(projectsDao.stream()).thenReturn(new ArrayList<ProjectEntity>().stream());
            return new DeliveryMachineStateDao(mongoTemplate, versionService(), projectsDao, 5);
        }

        @Bean
        public DeliveryMachineStateVersionService versionService() {
            return Mockito.mock(DeliveryMachineStateVersionService.class);
        }
    }
}
