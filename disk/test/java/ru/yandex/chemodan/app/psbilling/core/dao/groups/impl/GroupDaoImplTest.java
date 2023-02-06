package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.time.InstantInterval;

import static org.junit.Assert.assertTrue;

public class GroupDaoImplTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private ClientBalanceDao clientBalanceDao;

    @Test
    public void create() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1111L, PassportUid.cons(123456));
        GroupDao.InsertData insertData = GroupDao.InsertData.builder()
                .externalId("somecode")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .paymentInfo(paymentInfo)
                .clid(Option.of("1"))
                .build();
        Group group = groupDao.insert(insertData);
        Assert.assertEquals(insertData.getType(), group.getType());
        Assert.assertEquals(insertData.getExternalId(), group.getExternalId());
        Assert.assertEquals(insertData.getOwnerUid(), group.getOwnerUid());
        Assert.assertEquals(insertData.getGracePeriod(), group.getGracePeriod().get());
        Assert.assertEquals(insertData.getPaymentInfo(), group.getPaymentInfo().get());
        Assert.assertEquals(insertData.getClid(), group.getClid());
    }

    @Test
    public void findGroup() {
        Option<Group> groupO = groupDao.findGroup(GroupType.ORGANIZATION, "somecode");
        Assert.assertFalse(groupO.isPresent());

        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1111L, PassportUid.cons(123456));
        Group afterInsert = groupDao.insert(GroupDao.InsertData.builder()
                .externalId("somecode")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .paymentInfo(paymentInfo)
                .clid(Option.empty())
                .build());

        groupO = groupDao.findGroup(GroupType.FAMILY, "somecode");
        Assert.assertFalse(groupO.isPresent());

        groupO = groupDao.findGroup(GroupType.ORGANIZATION, "somecode");
        assertTrue(groupO.isPresent());
        Group group = groupO.get();
        Assert.assertEquals(afterInsert, group);
    }

    @Test
    public void findSubGroup() {
        Group parent = psBillingGroupsFactory.createGroup();
        Group child = psBillingGroupsFactory.createGroup(builder -> builder.parentGroupId(Option.of(parent.getId())));
        Option<Group> found = groupDao.findGroup(child.getType(), child.getExternalId(), parent.getId());
        Assert.assertEquals(child, found.get());
    }

    @Test
    public void paymentInfoWithoutB2BAutoBilling() {
        Group afterInsert = groupDao.insert(GroupDao.InsertData.builder()
                .externalId("somecode")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .clid(Option.empty())
                .build());

        jdbcTemplate.update("update groups set payment_info = '{\"uid\": \"12345678\", \"client_id\": 1111}'::json " +
                "where id = ?", afterInsert.getId());

        Group group = groupDao.findById(afterInsert.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void updatePaymentInfoWithB2BAutoBilling() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(1111L, PassportUid.cons(123456));
        Group afterInsert = groupDao.insert(GroupDao.InsertData.builder()
                .externalId("somecode")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .paymentInfo(paymentInfo)
                .clid(Option.empty())
                .build());
        Assert.assertFalse(afterInsert.getPaymentInfo().get().isB2bAutoBillingEnabled());

        paymentInfo = new BalancePaymentInfo(1111L, PassportUid.cons(123456), true);
        Group afterUpdate = groupDao.updatePaymentInfo(afterInsert.getId(), paymentInfo);
        Assert.assertTrue(afterUpdate.getPaymentInfo().get().isB2bAutoBillingEnabled());

        Group group = groupDao.findById(afterInsert.getId());
        Assert.assertEquals(afterUpdate.toString(), group.toString());
    }

    @Test
    public void updatePaymentInfoForGroups_multipleGroups() {
        long clientId = 1111L;
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(clientId, PassportUid.cons(123456));
        Group group1 = groupDao.insert(GroupDao.InsertData.builder()
                .externalId("somecode")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .paymentInfo(paymentInfo)
                .clid(Option.empty())
                .build());

        Group group2 = groupDao.insert(GroupDao.InsertData.builder()
                .externalId("somecode2")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .paymentInfo(paymentInfo)
                .clid(Option.empty())
                .build());

        Assert.assertFalse(group1.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(group2.getPaymentInfo().get().isB2bAutoBillingEnabled());

        groupDao.setAutoBillingForClient(clientId, true);
        Group group1AfterUpdate = groupDao.findById(group1.getId());
        Group group2AfterUpdate = groupDao.findById(group2.getId());
        Assert.assertTrue(group1AfterUpdate.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertTrue(group2AfterUpdate.getPaymentInfo().get().isB2bAutoBillingEnabled());

        groupDao.setAutoBillingForClient(clientId, false);
        group1AfterUpdate = groupDao.findById(group1.getId());
        group2AfterUpdate = groupDao.findById(group2.getId());
        Assert.assertFalse(group1AfterUpdate.getPaymentInfo().get().isB2bAutoBillingEnabled());
        Assert.assertFalse(group2AfterUpdate.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void updatePaymentInfoForGroups_oldPaymentInfoWithoutB2BAutoBilling() {
        long clientId = 1111;
        Group group = groupDao.insert(GroupDao.InsertData.builder()
                .externalId("somecode")
                .ownerUid(PassportUid.cons(1234))
                .type(GroupType.ORGANIZATION)
                .gracePeriod(Duration.standardDays(20))
                .clid(Option.empty())
                .build());

        jdbcTemplate.update("update groups " +
                "set payment_info = '{\"uid\": \"12345678\", \"client_id\": " + clientId + "}'::json " +
                "where id = ?", group.getId());

        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        groupDao.setAutoBillingForClient(clientId, true);

        Group groupAfterUpdate = groupDao.findById(group.getId());
        Assert.assertTrue(groupAfterUpdate.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void findGroupsWithClid() {
        createGroupWithService(x -> x);
        createGroupWithService(x -> x.clid(Option.of("clid1")));
        createGroupWithService(x -> x.clid(Option.of("clid2")));
        createGroupWithService(x -> x.clid(Option.of("clid2")));
        ListF<Group> groupsWithClid = groupDao.findGroupsWithClid(10000, GroupType.ORGANIZATION, Option.empty(),
                Option.empty());
        Assert.assertEquals(3, groupsWithClid.length());

    }

    @Test
    public void updateGroupNextSyncTimeIfNotChanged() {
        Group group = psBillingGroupsFactory.createGroup();
        Instant now = Instant.now();
        groupDao.updateGroupNextSyncTime(group.getId(), now);
        group = groupDao.findById(group.getId());

        Instant newNow = DateUtils.shiftTime(Duration.millis(1));
        boolean updated = groupDao.updateGroupNextSyncTimeIfNotChanged(group.getId(), group.getMembersNextSyncDt(),
                newNow);
        Assert.assertTrue(updated);

        // если грязными руками лазить в базу - можно получить бесконечный цикл апдейта организаций
        jdbcTemplate.update("update groups set members_next_sync_dt = now() where id = ?", group.getId());
        group = groupDao.findById(group.getId());
        updated = groupDao.updateGroupNextSyncTimeIfNotChanged(group.getId(), group.getMembersNextSyncDt(), newNow);
        Assert.assertTrue(updated);
    }

    @Test
    public void findUidsToCheckBillingStatus_dryRun() {
        groupDao.findUidsToCheckBillingStatus(1000, GroupType.ORGANIZATION, Option.empty());
        groupDao.findUidsToCheckBillingStatus(1000, GroupType.ORGANIZATION, uidO);
    }

    @Test
    public void findUidsToCheckBillingStatus() {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();

        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(1L,
                PassportUid.cons(1))));
        GroupService groupService1 = psBillingGroupsFactory.createGroupService(group1, groupProduct);
        groupServiceDao.updateNextBillingDate(groupService1.getId(), DateUtils.pastDateO());

        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(2L,
                PassportUid.cons(2))));
        GroupService groupService2 = psBillingGroupsFactory.createGroupService(group2, groupProduct);
        groupServiceDao.updateNextBillingDate(groupService2.getId(), DateUtils.futureDateO());

        Group group3 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(3L,
                PassportUid.cons(3))));
        GroupService groupService3 = psBillingGroupsFactory.createGroupService(group3, groupProduct);
        groupServiceDao.updateNextBillingDate(groupService3.getId(), Option.empty());

        ListF<PassportUid> uids = groupDao.findUidsToCheckBillingStatus(1000, GroupType.ORGANIZATION, Option.empty());
        Assert.assertEquals(1, uids.length());
        Assert.assertEquals(PassportUid.cons(1), uids.get(0));
    }



    @Test
    public void mixpaidClientsCount() {
        int count = groupDao.mixpaidClientsCount();
        Assert.assertEquals(0, count);

        GroupProduct hiddenPostpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID).hidden(true));
        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        GroupProduct prepaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));

        Group group1 = psBillingGroupsFactory.createGroup(1L);
        Group group2 = psBillingGroupsFactory.createGroup(1L);
        Group group3 = psBillingGroupsFactory.createGroup(2L);

        GroupService prepaidService1 = psBillingGroupsFactory.createGroupService(group1, prepaidProduct);
        psBillingGroupsFactory.createGroupService(group1, hiddenPostpaidProduct);
        count = groupDao.mixpaidClientsCount();
        Assert.assertEquals(0, count);

        psBillingGroupsFactory.createGroupService(group2, postpaidProduct);
        count = groupDao.mixpaidClientsCount();
        Assert.assertEquals(1, count);

        psBillingGroupsFactory.createGroupService(group3, prepaidProduct);
        GroupService postpaidService3 = psBillingGroupsFactory.createGroupService(group3, postpaidProduct);

        count = groupDao.mixpaidClientsCount();
        Assert.assertEquals(2, count);

        // disabled prepaid not count
        groupServicesManager.disableGroupService(prepaidService1);
        count = groupDao.mixpaidClientsCount();
        Assert.assertEquals(1, count);

        // disabled postpaid not count
        groupServicesManager.disableGroupService(postpaidService3);
        count = groupDao.mixpaidClientsCount();
        Assert.assertEquals(0, count);
    }

    @Test
    public void findGroupsWithDisabledPrepaidGsAndNoActiveGs() {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(rub));
        GroupProduct groupProductZero = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(rub).pricePerUserInMonth(BigDecimal.ZERO));
        GroupProduct groupProductHidden = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(rub).hidden(true));

        // OK!
        Group group1 = psBillingGroupsFactory.createGroup(1L);
        clientBalanceDao.createOrUpdate(1L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(7))));
        psBillingGroupsFactory.createGroupService(group1, groupProduct, Target.DISABLED);

        // There is active service
        Group group2 = psBillingGroupsFactory.createGroup(2L);
        clientBalanceDao.createOrUpdate(2L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(7))));
        psBillingGroupsFactory.createGroupService(group2, groupProduct, Target.DISABLED);
        psBillingGroupsFactory.createGroupService(group2, groupProduct, Target.ENABLED);

        // Several disabled services. OK!
        Group group3 = psBillingGroupsFactory.createGroup(3L);
        clientBalanceDao.createOrUpdate(3L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(7))));
        psBillingGroupsFactory.createGroupService(group3, groupProduct, Target.DISABLED);
        psBillingGroupsFactory.createGroupService(group3, groupProduct, Target.DISABLED);

        // Balance exhausted too recently
        Group group4 = psBillingGroupsFactory.createGroup(4L);
        clientBalanceDao.createOrUpdate(4L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(5))));
        psBillingGroupsFactory.createGroupService(group4, groupProduct, Target.DISABLED);

        // Balance exhausted too long ago
        Group group5 = psBillingGroupsFactory.createGroup(5L);
        clientBalanceDao.createOrUpdate(5L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(9))));
        psBillingGroupsFactory.createGroupService(group5, groupProduct, Target.DISABLED);

        // Product price = 0
        Group group6 = psBillingGroupsFactory.createGroup(6L);
        clientBalanceDao.createOrUpdate(6L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(7))));
        psBillingGroupsFactory.createGroupService(group6, groupProductZero, Target.DISABLED);

        // Product is hidden
        Group group7 = psBillingGroupsFactory.createGroup(7L);
        clientBalanceDao.createOrUpdate(7L, rub, BigDecimal.ZERO,
                Option.of(Instant.now().minus(Duration.standardDays(7))));
        psBillingGroupsFactory.createGroupService(group7, groupProductHidden, Target.DISABLED);

        SetF<UUID> actualGroupIds = groupDao.findGroupsWithDisabledPrepaidGsAndNoActiveGs(
                new InstantInterval(Instant.now().minus(Duration.standardDays(8)),
                        Instant.now().minus(Duration.standardDays(6))),
                Option.empty(), 7
        ).map(Group::getId).toLinkedHashSet();
        SetF<UUID> expectedGroupIds = Cf.set(group1.getId(), group3.getId());
        Assert.assertEquals(expectedGroupIds, actualGroupIds);
    }

    @Test
    public void subGroupTest() {
        Group parentGroup = psBillingGroupsFactory.createGroup();
        Group childGroup1 = psBillingGroupsFactory.createGroup(builder -> builder.parentGroupId(Option.of(parentGroup.getId())));
        Group childGroup2 = psBillingGroupsFactory.createGroup(builder -> builder.parentGroupId(Option.of(parentGroup.getId())));
        ListF<Group> children = Cf.list(childGroup1, childGroup2);

        Assert.assertTrue(parentGroup.getChildGroups().containsAllTs(children));
        Assert.assertTrue(groupDao.getChildGroups(parentGroup.getId()).containsAllTs(children));

        Assert.assertEquals(Option.empty(), parentGroup.getParentGroup());
        Assert.assertEquals(Option.empty(),groupDao.getParentGroup(parentGroup.getId()));

        for (Group child : children) {
            Assert.assertEquals(parentGroup, child.getParentGroup().get());
            Assert.assertEquals(parentGroup, groupDao.getParentGroup(child.getId()).get());
        }

    }

    private void createGroupWithService(Function<GroupDao.InsertData.InsertDataBuilder,
            GroupDao.InsertData.InsertDataBuilder> customizer) {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        Group group = psBillingGroupsFactory.createGroup(customizer);
        psBillingGroupsFactory.createGroupService(group, groupProduct);
    }
}
