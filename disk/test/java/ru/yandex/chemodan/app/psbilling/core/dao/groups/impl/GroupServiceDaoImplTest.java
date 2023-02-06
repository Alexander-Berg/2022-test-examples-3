package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceCalculator;
import ru.yandex.chemodan.app.psbilling.core.dao.features.GroupServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServiceMember;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.util.date.DateTimeUtils;

public class GroupServiceDaoImplTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupServiceMemberDao groupServiceMemberDao;
    @Autowired
    private GroupProductDao groupProductDao;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private UserServiceDao userServiceDao;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupServiceFeatureDao groupServiceFeatureDao;
    @Autowired
    private ClientBalanceCalculator clientBalanceCalculator;

    @Test
    public void findById() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        Option<GroupService> test = groupServiceDao.findByIdO(service.getId());
        Assert.assertTrue(test.isPresent());
        Assert.assertEquals(service, test.get());
    }

    @Test
    public void find() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        ListF<GroupService> test = groupServiceDao.find(group.getId(), product.getId());
        Assert.assertEquals(1, test.size());
        Assert.assertEquals(service, test.single());

        test = groupServiceDao.find(group.getId(), Target.DISABLED, Target.ENABLED);
        Assert.assertEquals(1, test.size());
        Assert.assertEquals(service, test.single());
    }

    @Test
    public void updateExpiredNextBillingDate() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService service1 = psBillingGroupsFactory.createGroupService(group, product);
        GroupService service2 = psBillingGroupsFactory.createGroupService(group, product);

        groupServiceDao.updateNextBillingDate(service1.getId(), Option.of(DateUtils.futureDate()));
        Assert.assertEquals(DateUtils.futureDateO(), groupServiceDao.findById(service1.getId()).getNextBillingDate());
        Assert.assertEquals(Option.of(Instant.now()), groupServiceDao.findById(service2.getId()).getNextBillingDate());

        groupServiceDao.updateNextBillingDate(service2.getId(), Option.empty());
        Assert.assertEquals(DateUtils.futureDateO(), groupServiceDao.findById(service1.getId()).getNextBillingDate());
        Assert.assertEquals(Option.empty(), groupServiceDao.findById(service2.getId()).getNextBillingDate());
    }

    @Test
    public void updatePrepaidNextBillingDateForClient() {
        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(1L, "123")));
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(2L, "123")));
        GroupProduct product =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));

        GroupService service1 = psBillingGroupsFactory.createGroupService(group1, product);
        GroupService service2 = psBillingGroupsFactory.createGroupService(group2, product);

        groupServiceDao.updatePrepaidNextBillingDateForClient(
                1L, product.getPriceCurrency(), Option.of(DateUtils.futureDate()));
        Assert.assertEquals(DateUtils.futureDateO(), groupServiceDao.findById(service1.getId()).getNextBillingDate());
        Assert.assertEquals(Option.of(Instant.now()), groupServiceDao.findById(service2.getId()).getNextBillingDate());
    }

    @Test
    public void updatePrepaidNextBillingDateForClient_free() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(1L, "123")));
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        groupServiceDao.updatePrepaidNextBillingDateForClient(1L, product.getPriceCurrency(),
                Option.of(DateUtils.futureDate()));
        Assert.assertEquals(service.getNextBillingDate(),
                groupServiceDao.findById(service.getId()).getNextBillingDate());
    }

    @Test
    public void updatePrepaidNextBillingDateForClient_postpaid() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(1L, "123")));
        GroupProduct product =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));

        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        groupServiceDao.updatePrepaidNextBillingDateForClient(1L, product.getPriceCurrency(),
                Option.of(DateUtils.futureDate()));
        Assert.assertEquals(service.getNextBillingDate(),
                groupServiceDao.findById(service.getId()).getNextBillingDate());
    }

    @Test
    public void find__manyGroups() {
        Group group1 = psBillingGroupsFactory.createGroup();
        Group group2 = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService service1 = psBillingGroupsFactory.createGroupService(group1, product);
        GroupService service2 = psBillingGroupsFactory.createGroupService(group2, product);

        ListF<GroupService> foundGroupServices = groupServiceDao.find(Cf.list(group1.getId(), group2.getId()),
                Target.ENABLED);
        Assert.assertEquals(2, foundGroupServices.size());
        Assert.assertEquals(service1, foundGroupServices.find(x -> x.getId().equals(service1.getId())).get());
        Assert.assertEquals(service2, foundGroupServices.find(x -> x.getId().equals(service2.getId())).get());
    }

    @Test
    public void find__manyGroups__emptyIds() {
        Assert.assertEquals(0, groupServiceDao.find(Cf.list(), Target.ENABLED).size());
    }

    @Test
    public void updateState() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        Assert.assertTrue(groupServiceDao.setTargetToDisabled(service.getId(), Target.ENABLED));
        ListF<GroupService> test = groupServiceDao.find(group.getId(), Target.DISABLED);
        Assert.assertEquals(1, test.size());
    }

    @Test
    public void findGroupsWithNewStatuses() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        psBillingGroupsFactory.createGroupService(group, product);

        psBillingGroupsFactory.createGroupService(group, product);


        Group anotherGroup = psBillingGroupsFactory.createGroup();
        GroupProduct anotherProduct = psBillingProductsFactory.createGroupProduct();

        psBillingGroupsFactory.createGroupService(anotherGroup, anotherProduct);
        Assert.assertEquals(
                Cf.set(group.getId(), anotherGroup.getId()),
                groupServiceDao.groupIdsToSync().unique()
        );
    }

    @Test
    public void findSubgroupServices() {
        Group parent = psBillingGroupsFactory.createGroup();
        Group child = psBillingGroupsFactory.createGroup(builder -> builder.parentGroupId(Option.of(parent.getId())));
        GroupService groupService = psBillingGroupsFactory.createGroupService(child,
                psBillingProductsFactory.createGroupProduct());
        ListF<GroupService> found = groupServiceDao.find(parent.getId(), true, Target.ENABLED);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals(groupService.getId(), found.get(0).getId());
    }

    @Test
    public void shouldFindGroupWithActiveStatuses() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        psBillingGroupsFactory.createGroupService(group, product, Target.ENABLED);

        DateUtils.shiftTime(Duration.standardMinutes(1));
        Assert.assertEquals(
                Cf.set(group.getId()),
                groupServiceDao.groupIdsToSync().unique()
        );
    }

    @Test
    public void shouldFindGroupWithDisabledTargetAndInitial() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        psBillingGroupsFactory.createGroupService(group, product, Target.DISABLED);

        Assert.assertEquals(1, groupServiceDao.groupIdsToSync().size());
    }

    @Test
    public void shouldNotFindGroupWithDisabledTargetAndSync() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();

        GroupService groupService = psBillingGroupsFactory.createGroupService(group, product, Target.DISABLED);
        groupServiceDao.setStatusActual(Cf.list(groupService.getId()), Target.DISABLED);

        Assert.assertEquals(0, groupServiceDao.groupIdsToSync().size());
    }

    @Test
    public void shouldFindGroupWithNotActualGroupFeatures() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();

        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        ProductFeatureEntity productFeature =
                psBillingProductsFactory.createProductFeature(groupProduct.getUserProduct().getId(), feature);
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct, Target.ENABLED);

        groupDao.updateGroupNextSyncTime(group.getId(), DateUtils.farFutureDate());

        groupServiceFeatureDao.insert(GroupServiceFeatureDao.InsertData.builder()
                        .groupId(group.getId())
                        .groupServiceId(groupService.getId())
                        .productFeatureId(productFeature.getId())
                        .build(),
                Target.ENABLED);

        Assert.assertEquals(
                Cf.set(group.getId()),
                groupServiceDao.groupIdsToSync().unique()
        );
    }

    @Test
    public void updateFirstFeatureDisabledAt() {
        DateUtils.freezeTime();
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        groupServiceDao.updateFirstFeatureDisabledAt(Cf.list(UUID.randomUUID()));
        GroupService groupService = groupServiceDao.find(group.getId(), product.getId()).single();
        // not update cause unknown id
        Assert.assertEquals(Option.empty(), groupService.getFirstFeatureDisabledAt());

        groupServiceDao.updateFirstFeatureDisabledAt(Cf.list(service.getId()));
        groupService = groupServiceDao.find(group.getId(), product.getId()).single();
        // not update cause target = enabled
        Assert.assertEquals(Option.empty(), groupService.getFirstFeatureDisabledAt());

        groupServiceDao.setTargetToDisabled(service.getId(), Target.ENABLED);
        groupServiceDao.updateFirstFeatureDisabledAt(Cf.list(service.getId()));
        groupService = groupServiceDao.find(group.getId(), product.getId()).single();
        Assert.assertEquals(Option.of(Instant.now()), groupService.getFirstFeatureDisabledAt());
    }

    @Test
    public void getEnabledUserServicesInDayPaidIntervals_emptyServices() {
        MapF<GroupService, ListF<Interval>> paidIntervals =
                groupServiceDao.getEnabledUserServicesInDayPaidIntervals(LocalDate.now(), Cf.list());
        Assert.assertEquals(0, paidIntervals.size());
    }

    @Test
    public void getEnabledUserServicesInDayPaidIntervals() {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(1L, "321")));
        Instant now = DateUtils.freezeTime(new DateTime(2021, 11, 15, 12, 0, 0));
        DateUtils.shiftTimeBack(Duration.standardDays(2));

        GroupService groupService1 = psBillingGroupsFactory.createGroupService(group, groupProduct);
        enableService(groupService1);
        GroupService groupService2 = psBillingGroupsFactory.createGroupService(group, groupProduct);
        enableService(groupService2);

        UserServiceEntity oldEnabledSpoilerUs = createUserService(groupService2); // none
        UserServiceEntity oldEnabledUs = createUserService(groupService1);  // 2021.11.15 00:00 - 2021.11.16 00:00
        UserServiceEntity oldDisabledUs = createUserService(groupService1); // none
        disableService(oldDisabledUs);
        UserServiceEntity todayDisabledUs = createUserService(groupService1); // 2021.11.15 00:00 - 2021.11.15 12:00

        DateUtils.freezeTime(now);
        UserServiceEntity todayEnabledUs = createUserService(groupService1); // 2021.11.15 12:00 - 2021.11.16 00:00
        disableService(todayDisabledUs);

        UserServiceEntity todayEnabledDisabledUs = createUserService(groupService1); // 2021.11.15 12:00 - 2021.11.15
        // 13:00
        DateUtils.shiftTime(Duration.standardHours(1));
        disableService(todayEnabledDisabledUs);

        DateUtils.freezeTime(now.plus(Duration.standardDays(1)));
        UserServiceEntity tomorrowEnabledUs = createUserService(groupService1); // none

        MapF<GroupService, ListF<Interval>> paidIntervals =
                groupServiceDao.getEnabledUserServicesInDayPaidIntervals(now.toDateTime().toLocalDate(),
                        Cf.list(groupService1));
        Assert.assertEquals(1, paidIntervals.keys().length());
        ListF<Interval> intervals = paidIntervals.getTs(paidIntervals.keys().first());
        Assert.assertEquals(4, intervals.length());
        assertContainsInterval(intervals,
                new DateTime(2021, 11, 15, 0, 0),
                new DateTime(2021, 11, 16, 0, 0));

        assertContainsInterval(intervals,
                new DateTime(2021, 11, 15, 0, 0),
                now.toDateTime());

        assertContainsInterval(intervals,
                now.toDateTime(),
                new DateTime(2021, 11, 16, 0, 0));

        assertContainsInterval(intervals,
                new DateTime(2021, 11, 15, 12, 0),
                new DateTime(2021, 11, 15, 13, 0));
    }

    @Test
    public void findActiveServicesOnDayForClient() {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(1L, "321")));
        Group spoilerGroup = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(2L, "123")));
        Instant now = DateTimeUtils.floorTimeForTimezone(Instant.now(), DateTimeZone.getDefault())
                .plus(Duration.standardHours(12));

        DateUtils.shiftTimeBack(Duration.standardDays(2));
        GroupService oldEnabledSpoilerGs = psBillingGroupsFactory.createGroupService(spoilerGroup, groupProduct,
                Target.ENABLED);
        GroupService oldEnabledGs = psBillingGroupsFactory.createGroupService(group, groupProduct, Target.ENABLED);
        GroupService oldDisabledGs = psBillingGroupsFactory.createGroupService(group, groupProduct, Target.ENABLED);
        disableService(oldDisabledGs);
        GroupService todayDisabledGs = psBillingGroupsFactory.createGroupService(group, groupProduct, Target.ENABLED);

        DateUtils.freezeTime(now);
        GroupService todayEnabledGs = psBillingGroupsFactory.createGroupService(group, groupProduct, Target.ENABLED);
        disableService(todayDisabledGs);

        GroupService todayEnabledDisabledGs = psBillingGroupsFactory.createGroupService(group, groupProduct,
                Target.ENABLED);
        DateUtils.shiftTime(Duration.standardHours(1));
        disableService(todayEnabledDisabledGs);

        DateUtils.freezeTime(now.plus(Duration.standardDays(1)));
        GroupService tomorrowEnabledGs = psBillingGroupsFactory.createGroupService(group, groupProduct, Target.ENABLED);

        ListF<GroupService> foundServices =
                groupServiceDao.findActiveServicesOnDayForClient(now.toDateTime().toLocalDate(), 1L);

//        Assert.assertEquals(4, foundServices.length());
        assertContains(foundServices, oldEnabledGs);
        assertContains(foundServices, todayDisabledGs);
        assertContains(foundServices, todayEnabledGs);
        assertContains(foundServices, todayEnabledDisabledGs);

        assertNotContains(foundServices, oldEnabledSpoilerGs);
        assertNotContains(foundServices, oldDisabledGs);
        assertNotContains(foundServices, tomorrowEnabledGs);
    }

    @Test
    public void findActiveGroupServicesByPaymentType_emptyList() {
        ListF<GroupService> groups = groupServiceDao.findActiveGroupServicesByPaymentType(Cf.list(),
                GroupPaymentType.POSTPAID);
        Assert.assertEquals(0, groups.length());
    }

    @Test
    public void getMistakenlyActiveServiceCount() {
        assertMistakenlyActiveServiceCount(Duration.ZERO, 0);

        Group group = psBillingGroupsFactory.createGroup(1L);
        GroupProduct prepaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        GroupProduct prepaidHiddenProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID).hidden(true));
        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));

        psBillingGroupsFactory.createGroupService(group, prepaidProduct);
        assertMistakenlyActiveServiceCount(Duration.ZERO, 0);

        clientBalanceDao.createOrUpdate(1L, prepaidProduct.getPriceCurrency(), BigDecimal.ZERO, Option.empty());
        assertMistakenlyActiveServiceCount(Duration.ZERO, 0);

        clientBalanceDao.createOrUpdate(1L, Currency.getInstance("USD"), BigDecimal.ZERO, DateUtils.farPastDateO());
        assertMistakenlyActiveServiceCount(Duration.ZERO, 0);

        psBillingGroupsFactory.createGroupService(group, prepaidHiddenProduct);
        clientBalanceDao.createOrUpdate(1L, prepaidProduct.getPriceCurrency(), BigDecimal.ZERO,
                DateUtils.farPastDateO());
        assertMistakenlyActiveServiceCount(Duration.ZERO, 1);

        Duration serviceAndVoidAtDiff = new Duration(DateUtils.farPastDate(), Instant.now());
        assertMistakenlyActiveServiceCount(serviceAndVoidAtDiff.plus(1), 0);
        assertMistakenlyActiveServiceCount(serviceAndVoidAtDiff, 1);

        psBillingGroupsFactory.createGroupService(group, prepaidProduct);
        psBillingGroupsFactory.createGroupService(group, postpaidProduct);
        assertMistakenlyActiveServiceCount(serviceAndVoidAtDiff, 2);
    }

    @Test
    public void findLastDisabledServices() {
        Mockito.when(psBillingCoreMocksConfig.directoryClientMock().usersInOrganization(Mockito.any(),
                        Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(Cf.set(uid.getUid()));

        Group group1 = psBillingGroupsFactory.createGroup(1L);
        Group group2 = psBillingGroupsFactory.createGroup(2L);
        clientBalanceDao.createOrUpdate(1L, rub, BigDecimal.ZERO, Option.of(Instant.now()));
        clientBalanceDao.createOrUpdate(1L, usd, BigDecimal.ZERO, Option.of(Instant.now()));
        clientBalanceDao.createOrUpdate(2L, rub, BigDecimal.ZERO, Option.of(Instant.now()));

        GroupProduct postPaidProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID).priceCurrency(rub));
        GroupProduct prePaidProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(rub));
        GroupProduct prePaidFreeProductRub1 =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(rub)
                        .pricePerUserInMonth(BigDecimal.ZERO));
        GroupProduct prePaidFreeProductRub2 =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(rub)
                        .skipTransactionsExport(true));
        GroupProduct prePaidProductUsd =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(usd));

        GroupService postPaidServiceRub = psBillingGroupsFactory.createGroupService(group1, postPaidProductRub);
        GroupService prePaidServiceUsd = psBillingGroupsFactory.createGroupService(group1, prePaidProductUsd);
        GroupService prePaidServiceRub1 = psBillingGroupsFactory.createGroupService(group1, prePaidProductRub);
        GroupService prePaidServiceRub2 = psBillingGroupsFactory.createGroupService(group2, prePaidProductRub);
        GroupService prePaidFreeServiceRub1 = psBillingGroupsFactory.createGroupService(group1, prePaidFreeProductRub1);
        GroupService prePaidFreeServiceRub2 = psBillingGroupsFactory.createGroupService(group1, prePaidFreeProductRub2);

        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, rub), 0);

        disableService(postPaidServiceRub);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, rub), 0);

        disableService(prePaidServiceUsd);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, rub), 0);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, usd), 1);

        disableService(prePaidServiceRub1);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, rub), 1);

        disableService(prePaidServiceRub2);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, rub), 1);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(2L, rub), 1);

        disableService(prePaidFreeServiceRub1);
        disableService(prePaidFreeServiceRub2);
        AssertHelper.assertSize(groupServiceDao.findLastDisabledPrepaidServices(1L, rub), 1);
    }

    private void assertMistakenlyActiveServiceCount(Duration duration, int expectedCount) {
        int count = groupServiceDao.getMistakenlyActiveServiceCount(duration);
        Assert.assertEquals(expectedCount, count);
    }

    private void assertContainsInterval(ListF<Interval> intervals, DateTime start, DateTime end) {
        Assert.assertTrue(String.format("interval from %s to %s not found. exist: %s", start, end, intervals),
                intervals.find(x -> x.getStart().toInstant().equals(start.toInstant())
                        && x.getEnd().toInstant().equals(end.toInstant())).isPresent());
    }

    private UserServiceEntity createUserService(GroupService groupService) {
        GroupServiceMember member = psBillingGroupsFactory.createGroupServiceMember(groupService);
        GroupProductEntity groupProduct = groupProductDao.findById(groupService.getGroupProductId());
        UserServiceEntity userService = psBillingUsersFactory.createUserService(groupProduct.getUserProductId(),
                x -> x.uid(member.getUid()));
        enableService(userService);
        groupServiceMemberDao.updateUserServiceId(member.getId(), userService.getId());
        return userService;
    }

    private void assertContains(ListF<GroupService> services, GroupService gs) {
        Assert.assertTrue(services.find(x -> x.getId().equals(gs.getId())).isPresent());
    }

    private void assertNotContains(ListF<GroupService> services, GroupService gs) {
        Assert.assertFalse(services.find(x -> x.getId().equals(gs.getId())).isPresent());
    }

    private void enableService(GroupService gs) {
        groupServiceDao.getParentSynchronizableRecordDaoHelper().setStatusActual(Cf.list(gs.getId()), Target.ENABLED);
    }

    private void disableService(GroupService gs) {
        groupServiceDao.setTargetToDisabled(gs.getId(), Target.ENABLED);
        groupServiceDao.getParentSynchronizableRecordDaoHelper().setStatusActual(Cf.list(gs.getId()), Target.DISABLED);
        groupServiceDao.updateFirstFeatureDisabledAt(Cf.list(gs.getId()));
        clientBalanceCalculator.updateVoidDate(gs);
    }

    private void enableService(UserServiceEntity gs) {
        userServiceDao.setTargetState(Cf.list(gs.getId()), Target.ENABLED);
        userServiceDao.getParentSynchronizableRecordDaoHelper().setStatusActual(Cf.list(gs.getId()), Target.ENABLED);
    }

    private void disableService(UserServiceEntity gs) {
        userServiceDao.setTargetState(Cf.list(gs.getId()), Target.DISABLED);
        userServiceDao.getParentSynchronizableRecordDaoHelper().setStatusActual(Cf.list(gs.getId()), Target.DISABLED);
        userServiceDao.updateFirstFeatureDisabledAt(Cf.list(gs.getId()));
    }
}
