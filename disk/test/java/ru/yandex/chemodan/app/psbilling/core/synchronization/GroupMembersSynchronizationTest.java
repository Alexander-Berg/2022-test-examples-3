package ru.yandex.chemodan.app.psbilling.core.synchronization;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceCalculator;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.SentEmailInfoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServiceMember;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.EmailPaymentsAwareMembersTask;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice.GroupActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice.GroupServicesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.chemodan.directory.client.OrganizationNotFoundException;
import ru.yandex.inside.passport.PassportUid;

public class GroupMembersSynchronizationTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private GroupServicesActualizationService groupServicesActualizationService;
    @Autowired
    private GroupServiceMemberDao groupServiceMemberDao;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private DirectoryClient directoryClient;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private FeatureFlags featureFlags;
    @Autowired
    private ClientBalanceCalculator calculator;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private SentEmailInfoDao sentEmailInfoDao;
    @Autowired
    private EmailHelper emailHelper;
    @Autowired
    private BazingaTaskManagerMock bazingaTaskManagerMock;

    private Currency rub = Currency.getInstance("RUB");
    private Instant currentDate = new DateTime(2021, 11, 1, 0, 0, 0).toInstant();
    private Instant voidDate1Users = new DateTime(2022, 1, 1, 0, 0, 0).toInstant(); // для одного юзера денег хватит
    // на 2 месяца
    private Instant voidDate2Users = new DateTime(2021, 12, 1, 0, 0, 0).toInstant(); // для двух юзеров на 1 месяц

    @Before
    public void initMocks() {
        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
    }

    @Test
    public void shouldSetSynchronizationTaskOnGroupServiceCreation() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        psBillingGroupsFactory.createGroupService(group, product);

        Assert.assertEquals(2, bazingaTaskManagerStub.tasksWithParams.size());
        Assert.assertEquals(
                new GroupActualizationTask.Parameters(group.getId()),
                bazingaTaskManagerStub.tasksWithParams.map(x -> x._1).filter(GroupActualizationTask.class::isInstance).first().getParameters()
        );
        Assert.assertEquals(
                1,
                bazingaTaskManagerStub.tasksWithParams.map(x -> x._1).filter(EmailPaymentsAwareMembersTask.class::isInstance).size()
        );
    }

    @Test
    public void shouldSetSynchronizationTaskForExistedGroup() {
        String externalGroupIdId = UUID.randomUUID().toString();
        Group group = psBillingGroupsFactory.createGroup(b -> b.externalId(externalGroupIdId));
        bazingaTaskManagerStub.tasksWithParams.clear();

        groupServicesActualizationService.scheduleForceGroupActualization(GroupType.ORGANIZATION, externalGroupIdId);

        Assert.assertEquals(1, bazingaTaskManagerStub.tasksWithParams.size());
        Assert.assertEquals(
                new GroupActualizationTask.Parameters(group.getId()),
                bazingaTaskManagerStub.tasksWithParams.first()._1.getParameters()
        );
    }

    @Test
    public void shouldNotSetSynchronizationTaskForNotExistedGroup() {
        groupServicesActualizationService
                .scheduleForceGroupActualization(GroupType.ORGANIZATION, UUID.randomUUID().toString());

        Assert.assertEquals(0, bazingaTaskManagerStub.tasksWithParams.size());
    }

    @Test
    public void shouldAddMembersOnSynchronization() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(group, product);

        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        Assert.assertEquals(1, groupServiceMembers.size());
        Assert.assertEquals(String.valueOf(uid), groupServiceMembers.first().getUid());
        Assert.assertEquals(Target.ENABLED, groupServiceMembers.first().getTarget());
        Assert.assertEquals(SynchronizationStatus.INIT, groupServiceMembers.first().getStatus());
    }


    @Test
    public void shouldSendWelcomeEmailToNewMember() {
        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));
        featureFlags.getB2bWelcomeEmailEnabled().setValue("true");

        // Check email - first time
        Group group = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createCommonEmailFeatures(group);
        bazingaTaskManagerMock.clearTasks();
        groupServicesActualizationService.actualize(group.getId());
        String emailKey = "b2b_welcome_email";
        ListF<String> args = Cf.list("disk_space_service", "fan_mail_limit_service", "tariff_name",
                "mail_archive_text_service");
        emailHelper.checkSingleLocalizedEmailArgs(SendLocalizedEmailTask.class, emailKey, args);
        // Add record about email sending
        sentEmailInfoDao.createOrUpdate(SentEmailInfoDao.InsertData.builder()
                .emailTemplateKey(emailKey)
                .uid(Long.toString(uid))
                .build());

        // Check no email - second time
        Group group2 = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createCommonEmailFeatures(group2);
        bazingaTaskManagerMock.clearTasks();
        groupServicesActualizationService.actualize(group2.getId());
        AssertHelper.assertSize(bazingaTaskManagerMock.findTasks(SendLocalizedEmailTask.class), 0);
    }

    @Test
    public void shouldAddMembersOnSynchronizationForTwoGroupServices() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService firstGroupService = psBillingGroupsFactory.createGroupService(group, product);
        GroupService secondGroupService = psBillingGroupsFactory.createGroupService(group, product);

        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        Assert.assertEquals(2, groupServiceMembers.size());
        Assert.assertEquals(
                Cf.set(firstGroupService.getId(), secondGroupService.getId()),
                groupServiceMembers.map(GroupServiceMember::getGroupServiceId).unique()
        );
    }

    @Test
    public void shouldDisableMembersOnSynchronization() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(group, product);

        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));
        groupServicesActualizationService.actualize(group.getId());

        mockOrganizatiomMembers(Cf.set());
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        Assert.assertEquals(1, groupServiceMembers.size());
        Assert.assertEquals(String.valueOf(uid), groupServiceMembers.first().getUid());
        Assert.assertEquals(Target.DISABLED, groupServiceMembers.first().getTarget());
    }

    @Test
    public void shouldNotAddMembersOnSynchronizationIfNoGroupService() {
        Group group = psBillingGroupsFactory.createGroup();

        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        Assert.assertEquals(0, groupServiceMembers.size());
        Mockito.verifyNoMoreInteractions(directoryClient);
    }

    @Test
    public void shouldNotAddMembersOnSynchronizationIfGroupServiceIsNotActive() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(group, product, Target.DISABLED);

        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));

        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        Assert.assertEquals(0, groupServiceMembers.size());
    }

    @Test
    public void analyzeActualGroupServiceMembers() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, product);

        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();

        groupServiceMemberDao.setStatusActual(groupServiceMembers.map(GroupServiceMember::getId), Target.ENABLED);
        groupServicesActualizationService.updateGroupServicesSyncState();
        GroupService byId = groupServiceDao.findById(groupService.getId());
        Assert.assertEquals(SynchronizationStatus.ACTUAL, byId.getStatus());
        Assert.assertTrue(byId.getActualEnabledAt().isPresent());
        Assert.assertFalse(byId.getActualDisabledAt().isPresent());
    }

    @Test
    public void analyzePartiallyActualGroupServiceMembers() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, product);

        long uid1 = 1234L;
        long uid2 = 2345L;
        mockOrganizatiomMembers(Cf.set(uid1, uid2));
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        Assert.assertEquals(2, groupServiceMembers.size());

        groupServiceMemberDao.setStatusActual(Cf.list(groupServiceMembers.first().getId()), Target.ENABLED);
        groupServicesActualizationService.updateGroupServicesSyncState();
        GroupService byId = groupServiceDao.findById(groupService.getId());
        Assert.assertEquals(SynchronizationStatus.SYNCING, byId.getStatus());
    }

    @Test
    public void analyzePsBillingProcessingDisabled() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, product);

        long uid = 1234L;
        mockOrganizatiomMembers(Cf.set(uid));
        groupServicesActualizationService.actualize(group.getId());

        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, true));
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        groupServiceMemberDao.setStatusActual(groupServiceMembers.map(GroupServiceMember::getId), Target.DISABLED);

        groupServicesActualizationService.updateGroupServicesSyncState();
        GroupService byId = groupServiceDao.findById(groupService.getId());
        Assert.assertEquals(SynchronizationStatus.ACTUAL, byId.getStatus());
        Assert.assertFalse(byId.getActualEnabledAt().isPresent());
        Assert.assertTrue(byId.getActualDisabledAt().isPresent());
    }

    @Test
    public void testAnalyzingBatch() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService groupService1 = psBillingGroupsFactory.createGroupService(group, product);
        GroupService groupService2 = psBillingGroupsFactory.createGroupService(group, product);
        GroupService groupService3 = psBillingGroupsFactory.createGroupService(group, product);
        GroupService groupService4 = psBillingGroupsFactory.createGroupService(group, product);


        mockOrganizatiomMembers(Cf.set(11111L));
        groupServicesActualizationService.actualize(group.getId());

        ListF<GroupServiceMember> groupServiceMembers = groupServiceMemberDao.findAll();
        groupServiceMemberDao.setStatusActual(groupServiceMembers.map(GroupServiceMember::getId), Target.ENABLED);

        groupServicesActualizationService.updateGroupServicesSyncState(2);
        Assert.assertEquals(SynchronizationStatus.ACTUAL, groupServiceDao.findById(groupService1.getId()).getStatus());
        Assert.assertEquals(SynchronizationStatus.ACTUAL, groupServiceDao.findById(groupService2.getId()).getStatus());
        Assert.assertEquals(SynchronizationStatus.ACTUAL, groupServiceDao.findById(groupService3.getId()).getStatus());
        Assert.assertEquals(SynchronizationStatus.ACTUAL, groupServiceDao.findById(groupService4.getId()).getStatus());
    }

    @Test
    public void shouldRecalculateClientBalanceForPrepaidProductAddMembersOnSynchronization() {
        Tuple2<Group, BalancePaymentInfo> info = createDataForRecalculateTest(Cf.longList(1234, 12345),
                Cf.longList(1234), GroupPaymentType.PREPAID, Option.of(voidDate1Users));

        groupServicesActualizationService.actualize(info.get1().getId());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(info.get2().getClientId(), Currency.getInstance(
                "RUB")).get();
        Assert.assertEquals(voidDate2Users, clientBalance.getBalanceVoidAt().get());

        ListF<GroupService> groupServices =
                groupServiceDao.findActiveGroupServicesByPaymentInfoClient(info.get2().getClientId(),
                        rub);
        for (GroupService groupService : groupServices) {
            Assert.assertEquals(voidDate2Users, groupService.getNextBillingDate().orElseThrow());
        }
    }

    @Test
    public void shouldRecalculateClientBalanceForPrepaidProductRemoveMembersOnSynchronization() {
        Tuple2<Group, BalancePaymentInfo> info = createDataForRecalculateTest(Cf.longList(1234), Cf.longList(1234,
                12345), GroupPaymentType.PREPAID, Option.of(voidDate2Users));

        groupServicesActualizationService.actualize(info.get1().getId());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(info.get2().getClientId(), Currency.getInstance(
                "RUB")).get();
        Assert.assertEquals(voidDate1Users, clientBalance.getBalanceVoidAt().get());

        ListF<GroupService> groupServices =
                groupServiceDao.findActiveGroupServicesByPaymentInfoClient(info.get2().getClientId(),
                        rub);
        for (GroupService groupService : groupServices) {
            Assert.assertEquals(voidDate1Users, groupService.getNextBillingDate().orElseThrow());
        }
    }

    @Test
    public void shouldNotRecalculateClientBalanceForPrepaidProductRemoveMembersOnSynchronizationForDisabledService() {
        Tuple2<Group, BalancePaymentInfo> info = createDataForRecalculateTest(Cf.longList(1234), Cf.longList(1234,
                12345), GroupPaymentType.PREPAID, Option.of(voidDate2Users));
        GroupService groupService =
                groupServiceDao.findActiveGroupServicesByPaymentInfoClient(info.get2().getClientId(), rub).single();
        groupServicesManager.disableGroupService(groupService);

        groupServicesActualizationService.actualize(info.get1().getId());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(info.get2().getClientId(), rub).get();
        calculator.updateClientBalance(clientBalance.getClientId());
        clientBalance = clientBalanceDao.findById(clientBalance.getId());
        groupService = groupServiceDao.findById(groupService.getId());

        AssertHelper.assertTrue(clientBalance.getBalanceVoidAt().isPresent());
        AssertHelper.assertEquals(clientBalance.getBalanceVoidAt().get(), voidDate2Users);
        AssertHelper.assertEquals(groupService.getNextBillingDate().orElseThrow(), voidDate2Users);
    }

    @Test
    public void shouldNotRecalculateClientBalanceForPrepaidProductAddMembersOnSynchronizationForDisabledService() {
        Tuple2<Group, BalancePaymentInfo> info = createDataForRecalculateTest(Cf.longList(1234, 12345),
                Cf.longList(1234), GroupPaymentType.PREPAID, Option.of(voidDate1Users));
        GroupService groupService =
                groupServiceDao.findActiveGroupServicesByPaymentInfoClient(info.get2().getClientId(), rub).single();
        groupServicesManager.disableGroupService(groupService);

        groupServicesActualizationService.actualize(info.get1().getId());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(info.get2().getClientId(), rub).get();
        calculator.updateClientBalance(clientBalance.getClientId());
        clientBalance = clientBalanceDao.findById(clientBalance.getId());
        groupService = groupServiceDao.findById(groupService.getId());

        AssertHelper.assertTrue(clientBalance.getBalanceVoidAt().isPresent());
        AssertHelper.assertEquals(clientBalance.getBalanceVoidAt().get(), voidDate1Users);
        AssertHelper.assertEquals(groupService.getNextBillingDate().orElseThrow(), voidDate1Users);
    }

    @Test
    public void shouldRecalculateClientBalanceForPrepaidProductWithoutUpdateMembersOnSynchronization() {
        Tuple2<Group, BalancePaymentInfo> info = createDataForRecalculateTest(Cf.longList(1234, 12345),
                Cf.longList(1234, 12345), GroupPaymentType.PREPAID, Option.of(voidDate2Users));

        groupServicesActualizationService.actualize(info.get1().getId());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(info.get2().getClientId(), Currency.getInstance(
                "RUB")).get();
        Assert.assertEquals(voidDate2Users, clientBalance.getBalanceVoidAt().get());

        ListF<GroupService> groupServices =
                groupServiceDao.findActiveGroupServicesByPaymentInfoClient(info.get2().getClientId(),
                        rub);
        for (GroupService groupService : groupServices) {
            Assert.assertEquals(voidDate2Users, groupService.getNextBillingDate().orElseThrow());
        }
    }

    @Test
    public void shouldRecalculateClientBalanceForPostpaidProductOnSynchronization() {
        Tuple2<Group, BalancePaymentInfo> info = createDataForRecalculateTest(Cf.longList(1234, 12345),
                Cf.longList(1234), GroupPaymentType.POSTPAID, Option.empty());

        groupServicesActualizationService.actualize(info.get1().getId());

        ClientBalanceEntity clientBalance = clientBalanceDao.find(info.get2().getClientId(), Currency.getInstance(
                "RUB")).get();
        Assert.assertTrue(clientBalance.getBalanceVoidAt().isEmpty());
    }

    @Test
    public void shouldDisableAutoPayOnSynchronizationPayerIsNotAdmin() {
        featureFlags.getAutoPayDisableOnUserIsNotAdmin().setValue("true");

        long clientId = 1234;
        Group groupWherePayer = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid, true)));
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(groupWherePayer, product);

        Group groupWherePayer2 = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid, true)));
        Group groupWhereAdmin = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(4567, PassportUid.cons(456), true)));

        mockOrganizatiomMembers(Cf.set());

        Mockito.when(directoryClient
                .organizationsWhereUserIsAdmin(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(Cf.list(groupWhereAdmin.getExternalId()));

        groupServicesActualizationService.actualize(groupWherePayer.getId());

        groupWherePayer = groupDao.findById(groupWherePayer.getId());
        Assert.assertFalse(groupWherePayer.getPaymentInfo().get().isB2bAutoBillingEnabled());

        groupWherePayer2 = groupDao.findById(groupWherePayer2.getId());
        Assert.assertFalse(groupWherePayer2.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void shouldNotDisableAutoPayOnSynchronizationPayerIsAdmin() {
        featureFlags.getAutoPayDisableOnUserIsNotAdmin().setValue("true");

        long clientId = 1234;
        Group groupWherePayer = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid, true)));
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(groupWherePayer, product);

        Group groupWherePayer2 = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid, true)));

        mockOrganizatiomMembers(Cf.set());

        Mockito.when(directoryClient
                .organizationsWhereUserIsAdmin(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(Cf.list(groupWherePayer2.getExternalId()));

        groupServicesActualizationService.actualize(groupWherePayer.getId());

        groupWherePayer = groupDao.findById(groupWherePayer.getId());
        Assert.assertTrue(groupWherePayer.getPaymentInfo().get().isB2bAutoBillingEnabled());

        groupWherePayer2 = groupDao.findById(groupWherePayer2.getId());
        Assert.assertTrue(groupWherePayer2.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    private Tuple2<Group, BalancePaymentInfo> createDataForRecalculateTest(ListF<Long> actualUids,
                                                                           ListF<Long> existingUids,
                                                                           GroupPaymentType paymentType,
                                                                           Option<Instant> voidDate) {
        DateUtils.freezeTime(new DateTime(2021, 11, 1, 0, 0, 0)); // берем месяц с 30 днями

        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(123456L, "123321");
        clientBalanceDao.createOrUpdate(paymentInfo.getClientId(), rub, BigDecimal.valueOf(300),
                voidDate);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));
        GroupProduct product =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(paymentType).pricePerUserInMonth(BigDecimal.valueOf(150)));
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, product);
        groupServiceDao.updatePrepaidNextBillingDateForClient(123456L, product.getPriceCurrency(), voidDate);

        if (existingUids.isNotEmpty()) {
            groupServiceMemberDao.batchInsert(
                    existingUids.map(
                            uid -> GroupServiceMemberDao.InsertData.builder().groupServiceId(groupService.getId()).uid(String.valueOf(uid)).build()
                    ),
                    Target.ENABLED);
        }

        mockOrganizatiomMembers(actualUids.toLinkedHashSet());
        return Tuple2.tuple(group, paymentInfo);
    }

    @Test
    public void analyzeWithNotFoundOrganization() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, product);

        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenThrow(OrganizationNotFoundException.class);

        Mockito.when(directoryClient.usersInOrganization(
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyInt(),
                Mockito.any()
                )
        )
                .thenThrow(OrganizationNotFoundException.class);

        groupServicesActualizationService.actualize(group.getId());

        GroupService byId = groupServiceDao.findById(groupService.getId());
        Assert.assertEquals(SynchronizationStatus.SYNCING, byId.getStatus());
        Assert.assertEquals(Target.DISABLED, byId.getTarget());
        Assert.assertFalse(byId.getActualEnabledAt().isPresent());
        Assert.assertFalse(byId.getActualDisabledAt().isPresent());
    }

    private void mockOrganizatiomMembers(SetF<Long> set) {
        Mockito.when(directoryClient
                .usersInOrganization(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(set);
    }
}
