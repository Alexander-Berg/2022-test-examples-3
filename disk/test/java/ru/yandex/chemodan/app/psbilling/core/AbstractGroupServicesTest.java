package ru.yandex.chemodan.app.psbilling.core;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.SneakyThrows;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.config.FeaturesSynchronize;
import ru.yandex.chemodan.app.psbilling.core.dao.AbstractDao;
import ru.yandex.chemodan.app.psbilling.core.dao.features.GroupServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.features.UserServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.features.UserServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureScope;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizableRecord;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.synchronization.feature.GroupFeatureActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.feature.UserFeatureActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupmember.GroupServiceMemberActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupmember.GroupServiceMemberActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice.GroupActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice.GroupServicesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservicefeature.GroupFeaturesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservice.UserServiceActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservice.UserServiceActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservicefeature.UserFeaturesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.util.RequestTemplate;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.misc.test.Assert;

public abstract class AbstractGroupServicesTest extends AbstractPsBillingCoreTest {
    @Autowired
    protected DirectoryClient directoryClient;
    @FeaturesSynchronize
    @Autowired
    protected RestTemplate skipTransactionsExport;
    @Autowired
    protected UserProductDao userProductDao;
    @Autowired
    protected GroupProductDao groupProductDao;
    @Autowired
    protected GroupServicesActualizationService groupServicesActualizationService;
    @Autowired
    protected GroupServiceMemberActualizationService groupServiceMemberActualizationService;
    @Autowired
    protected UserServiceActualizationService userServiceActualizationService;

    @Autowired
    protected GroupServiceMemberDao groupServiceMemberDao;
    @Autowired
    protected GroupServiceFeatureDao groupServiceFeatureDao;
    @Autowired
    protected UserServiceDao userServiceDao;
    @Autowired
    protected GroupServiceDao groupServiceDao;
    @Autowired
    protected UserServiceFeatureDao userServiceFeatureDao;
    @Autowired
    protected UserFeaturesActualizationService userFeaturesActualizationService;
    @Autowired
    protected GroupFeaturesActualizationService groupFeaturesActualizationService;
    @Autowired
    protected GroupServiceTransactionsDao groupServiceTransactionsDao;
    @Autowired
    protected ClientBalanceDao clientBalanceDao;
    @Autowired
    protected GroupServicesManager groupServicesManager;

    protected static final long UID1 = 1;
    protected static final long UID2 = 2;

    @Before
    public void initMocks() {
        Mockito.when(directoryClient.usersInOrganization(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(Cf.set(UID1, UID2));
        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
        Mockito.when(skipTransactionsExport.exchange(
                Mockito.anyString(), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).thenReturn(new ResponseEntity<>("", HttpStatus.ACCEPTED));
    }

    @SneakyThrows
    protected void actualizeFeature(Group group, FeatureEntity feature, int userCount) {
        ListF<Long> uids = Cf.range(0, userCount).map(x -> (long) x + 1);
        Mockito.when(directoryClient.usersInOrganization(Mockito.eq(group.getExternalId()),
                        Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(uids.toLinkedHashSet());
        actualizeFeature(group, feature);
    }

    @SneakyThrows
    protected void actualizeFeature(Group group, FeatureEntity feature) {
        groupServicesActualizationService.actualize(group.getId());

        groupServiceMemberDao.findRecordsInInitStatus()
                .forEach(groupServiceMemberActualizationService::actualizeGroupServiceMember);

        userServiceDao.findRecordsInInitStatus().forEach(userServiceActualizationService::actualizeUserService);


        userServiceFeatureDao.findAll().forEach(userServiceFeature ->
                userFeaturesActualizationService.synchronize(userServiceFeature.getUid(), feature.getId())
        );

        groupServiceMemberActualizationService.updateGroupServiceMembersSyncState();

        userServiceActualizationService.updateUserServicesSyncState();

        groupServiceMemberActualizationService.updateGroupServiceMembersSyncState();

        groupServicesActualizationService.updateGroupServicesSyncState();
    }

    protected void enableAndCheck(Group group, GroupService groupService, boolean snoozed) {
        UserProduct userProduct = userProductManager.findById(
                groupProductDao.findById(groupService.getGroupProductId()).getUserProductId());

        boolean hasGroupFeatures =
                userProduct.getFeatures().stream().anyMatch(x -> x.getScope().equals(FeatureScope.GROUP));

        assertTaskCountAndExecute(GroupActualizationTask.class, 1);

        ListF<UUID> groupServiceMembers = groupServiceMemberDao.findRecordsInInitStatus();
        Assert.equals(2, groupServiceMembers.size());

        if (hasGroupFeatures) {
            ListF<GroupServiceFeature> groupFeatures =
                    groupServiceFeatureDao.findInInitByParentId(groupService.getId());
            Assert.equals(1, groupFeatures.size());

            assertTaskCountAndExecute(GroupFeatureActualizationTask.class, 1);
            verifyExchangeForUrl("http:/some.yandex.ru/activate/" + group.getExternalId());

            ensureEnabled(groupFeatures.map(AbstractEntity::getId), groupServiceFeatureDao,
                    snoozed ? SynchronizationStatus.SNOOZING : SynchronizationStatus.ACTUAL);
        }

        assertTaskCountAndExecute(GroupServiceMemberActualizationTask.class, 2);

        groupServiceMembers.forEach(gsmId -> {
            Assert.assertSome(groupServiceMemberDao.findById(gsmId).getUserServiceId());
        });

        ListF<UUID> userServices = userServiceDao.findRecordsInInitStatus();
        Assert.equals(2, userServices.size());

        assertTaskCountAndExecute(UserServiceActualizationTask.class, 2);

        ListF<UserServiceFeature> userServiceFeatures = userServiceFeatureDao.findAll();
        Assert.equals(2, userServiceFeatures.size());
        userServiceFeatures.forEach(f -> {
            Assert.equals(Target.ENABLED, f.getTarget());
            Assert.equals(SynchronizationStatus.INIT, f.getStatus());
            Assert.assertNone(f.getActualEnabledAt());
            Assert.assertNone(f.getActualDisabledAt());
        });

        assertTaskCountAndExecute(UserFeatureActualizationTask.class, 2);

        verifyExchangeForUrl("http:/some.yandex.ru/activate/" + UID1);
        verifyExchangeForUrl("http:/some.yandex.ru/activate/" + UID2);
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        userServiceFeatures = userServiceFeatureDao.findAll();
        Assert.equals(2, userServiceFeatures.size());

        ensureEnabled(userServiceFeatures.map(AbstractEntity::getId), userServiceFeatureDao,
                snoozed ? SynchronizationStatus.SNOOZING : SynchronizationStatus.ACTUAL);

        // ensure nothing strange happen
        userServiceActualizationService.updateUserServicesSyncState();
        groupServiceMemberActualizationService.updateGroupServiceMembersSyncState();
        groupServicesActualizationService.updateGroupServicesSyncState();

        ensureEnabled(userServices, userServiceDao,
                snoozed ? SynchronizationStatus.SNOOZING : SynchronizationStatus.ACTUAL);

        ensureEnabled(groupServiceMembers, groupServiceMemberDao,
                snoozed ? SynchronizationStatus.SNOOZING : SynchronizationStatus.ACTUAL);

        ensureEnabled(Cf.list(groupService.getId()), groupServiceDao, SynchronizationStatus.ACTUAL);
    }

    protected void disableAndCheck(Group group, GroupService groupService,
                                   boolean wasSnoozed) {
        UserProduct userProduct = userProductManager.findById(
                groupProductDao.findById(groupService.getGroupProductId()).getUserProductId());
        boolean hasGroupFeatures =
                userProduct.getFeatures().stream().anyMatch(x -> x.getScope().equals(FeatureScope.GROUP));

        assertTaskCountAndExecute(GroupActualizationTask.class, 1);

        ListF<UUID> groupServiceMembers = groupServiceMemberDao.findRecordsInInitStatus();
        Assert.equals(2, groupServiceMembers.size());

        if (hasGroupFeatures) {
            ListF<GroupServiceFeature> groupFeatures =
                    groupServiceFeatureDao.findInInitByParentId(groupService.getId());
            Assert.equals(1, groupFeatures.size());

            assertTaskCountAndExecute(GroupFeatureActualizationTask.class, 1);

            if (!wasSnoozed) {
                verifyExchangeForUrl("http:/some.yandex.ru/deactivate/" + group.getExternalId());
            }

            ensureAllActualAndDisabled(groupFeatures.map(AbstractEntity::getId), groupServiceFeatureDao, wasSnoozed);
        }

        assertTaskCountAndExecute(GroupServiceMemberActualizationTask.class, 2);

        groupServiceMembers.forEach(gsmId -> {
            Assert.assertSome(groupServiceMemberDao.findById(gsmId).getUserServiceId());
        });

        ListF<UUID> userServices = userServiceDao.findRecordsInInitStatus();
        Assert.equals(2, userServices.size());

        assertTaskCountAndExecute(UserServiceActualizationTask.class, 2);

        ListF<UserServiceFeature> userServiceFeatures = userServiceFeatureDao.findAll();
        Assert.equals(2, userServiceFeatures.size());
        userServiceFeatures.forEach(f -> {
            Assert.equals(Target.DISABLED, f.getTarget());
            Assert.equals(SynchronizationStatus.INIT, f.getStatus());
            if (wasSnoozed) {
                Assert.assertNone(f.getActualEnabledAt());

            } else {
                Assert.assertSome(f.getActualEnabledAt());
            }
            Assert.assertNone(f.getActualDisabledAt());
        });

        assertTaskCountAndExecute(UserFeatureActualizationTask.class, 2);

        if (!wasSnoozed) {
            verifyExchangeForUrl("http:/some.yandex.ru/deactivate/" + UID1);
            verifyExchangeForUrl("http:/some.yandex.ru/deactivate/" + UID2);
        }
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        userServiceFeatures = userServiceFeatureDao.findAll();
        Assert.equals(2, userServiceFeatures.size());

        ensureAllActualAndDisabled(userServiceFeatures.map(AbstractEntity::getId), userServiceFeatureDao, wasSnoozed);

        // ensure nothing strange happen
        userServiceActualizationService.updateUserServicesSyncState();
        groupServiceMemberActualizationService.updateGroupServiceMembersSyncState();
        groupServicesActualizationService.updateGroupServicesSyncState();

        ensureAllActualAndDisabled(userServices, userServiceDao, wasSnoozed);
        ensureAllActualAndDisabled(groupServiceMembers, groupServiceMemberDao, wasSnoozed);
        ensureAllActualAndDisabled(Cf.list(groupService.getId()), groupServiceDao, false);
    }

    protected <T extends SynchronizableRecord> void ensureAllActualAndDisabled(ListF<UUID> objects,
                                                                               AbstractDao<T> dao, boolean wasSnoozed) {
        objects.forEach(id -> {
            T m = dao.findById(id);
            Assert.equals(Target.DISABLED, m.getTarget());
            Assert.equals(SynchronizationStatus.ACTUAL, m.getStatus());
            if (wasSnoozed) {
                Assert.none(m.getActualEnabledAt());
            } else {
                Assert.assertSome(m.getActualEnabledAt());
            }
            Assert.assertSome(m.getActualDisabledAt());
        });
    }

    protected void assertTaskCountAndExecute(Class<? extends OnetimeTask> taskClass, int count) {
        Assert.equals(count, bazingaTaskManagerStub.findTasks(taskClass).size());
        bazingaTaskManagerStub.executeTasks(taskClass);
    }

    protected <T extends SynchronizableRecord> void ensureAllActualAndEnabled(ListF<UUID> objects, AbstractDao<T> dao) {
        objects.forEach(id -> {
            T m = dao.findById(id);
            Assert.equals(Target.ENABLED, m.getTarget());
            Assert.equals(SynchronizationStatus.ACTUAL, m.getStatus());
            Assert.assertSome(m.getActualEnabledAt());
            Assert.assertNone(m.getActualDisabledAt());
        });
    }


    protected <T extends SynchronizableRecord> void ensureEnabled(ListF<UUID> objects, AbstractDao<T> dao,
                                                                  SynchronizationStatus status) {
        objects.forEach(id -> {
            T m = dao.findById(id);
            Assert.equals(Target.ENABLED, m.getTarget());
            Assert.equals(status, m.getStatus());
            if (status == SynchronizationStatus.ACTUAL) {
                Assert.assertSome(m.getActualEnabledAt());
            } else {
                Assert.assertNone(m.getActualEnabledAt());
            }
            Assert.assertNone(m.getActualDisabledAt());
        });
    }

    protected void verifyExchangeForUrl(String s) {
        Mockito.verify(skipTransactionsExport).exchange(
                Mockito.eq(s), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        );
    }

    protected Option<RequestTemplate> post(String s) {
        return Option.of(new RequestTemplate(HttpMethod.POST, s, Option.empty(), Option.empty()));
    }

    protected void mockPostOk(String url) {
        mockPostOk(url, "");
    }

    protected void mockPostOk(String url, String response) {
        HttpStatus statusCode = HttpStatus.OK;
        mockPost(url, response, statusCode);
    }

    protected void mockPost(String url, String response, HttpStatus statusCode) {
        Mockito.when(skipTransactionsExport.exchange(
                url, Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            if (statusCode.is5xxServerError()) {
                throw new HttpServerErrorException(statusCode, "", response.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);
            }
            if (statusCode.is4xxClientError()) {
                throw new HttpClientErrorException(statusCode, "", response.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);
            }
            return new ResponseEntity<>(response, statusCode);
        });
    }
}
