package ru.yandex.chemodan.app.psbilling.core.groups;

import java.util.UUID;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupPartnerDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductOwnerDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductOwner;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class GroupsManagerTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupsManager groupsManager;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private ProductOwnerDao productOwnerDao;
    @Autowired
    private DirectoryClient directoryClient;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupPartnerDao groupPartnerDao;

    private ProductOwner owner;
    private PassportUid uid;
    private long clientId;

    @Test
    public void testNoProductsIfNoOrganizations() {
        Assert.isEmpty(groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION, Cf.list()));
        Assert.isEmpty(groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION, Cf.list("123123")));
    }

    @Test
    public void testFindActiveProductCode() {
        String externalGroupId = "1231234";
        String productCode = "product_code";

        psBillingGroupsFactory.createActualGroupService(
                psBillingGroupsFactory.createGroup(b -> b.externalId(externalGroupId)),
                psBillingProductsFactory.createGroupProduct(b -> b.code(productCode))
        );

        Assert.equals(
                Cf.set(productCode),
                groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION, Cf.list(externalGroupId))
        );
    }

    @Test
    public void testFindTwoActiveProductCodes() {
        String externalGroupId = "1231234";
        String firstProductCode = "first_product_code";
        String secondProductCode = "second_product_code";

        Group group = psBillingGroupsFactory.createGroup(b -> b.externalId(externalGroupId));
        psBillingGroupsFactory.createActualGroupService(
                group,
                psBillingProductsFactory.createGroupProduct(b -> b.code(firstProductCode))
        );
        psBillingGroupsFactory.createActualGroupService(
                group,
                psBillingProductsFactory.createGroupProduct(b -> b.code(secondProductCode))
        );

        Assert.equals(
                Cf.set(firstProductCode, secondProductCode),
                groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION, Cf.list(externalGroupId))
        );
    }

    @Test
    public void testOnlyOneActiveService() {
        String externalGroupId = "1231234";
        String firstProductCode = "first_product_code";
        String secondProductCode = "second_product_code";

        Group group = psBillingGroupsFactory.createGroup(b -> b.externalId(externalGroupId));
        psBillingGroupsFactory.createActualGroupService(
                group,
                psBillingProductsFactory.createGroupProduct(b -> b.code(firstProductCode))
        );
        psBillingGroupsFactory.createGroupService(
                group,
                psBillingProductsFactory.createGroupProduct(b -> b.code(secondProductCode))
        );

        Assert.equals(
                Cf.set(firstProductCode),
                groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION, Cf.list(externalGroupId))
        );
    }

    @Test
    public void testTwoOrganizationsWithSameProduct() {
        String firstExternalGroupId = "1231234";
        String secondExternalGroupId = "12345649";
        String productCode = "product_code";

        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(b -> b.code(productCode));
        psBillingGroupsFactory.createActualGroupService(
                psBillingGroupsFactory.createGroup(b -> b.externalId(firstExternalGroupId)),
                groupProduct
        );
        psBillingGroupsFactory.createActualGroupService(
                psBillingGroupsFactory.createGroup(b -> b.externalId(secondExternalGroupId)),
                groupProduct
        );

        Assert.equals(
                Cf.set(productCode),
                groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION,
                        Cf.list(firstExternalGroupId, secondExternalGroupId))
        );
    }

    @Test
    public void testTwoOrganizationsWithDifferentProducts() {
        String firstExternalGroupId = "1231234";
        String secondExternalGroupId = "12345649";
        String firstProductCode = "first_product_code";
        String secondProductCode = "second_product_code";

        psBillingGroupsFactory.createActualGroupService(
                psBillingGroupsFactory.createGroup(b -> b.externalId(firstExternalGroupId)),
                psBillingProductsFactory.createGroupProduct(b -> b.code(firstProductCode))
        );
        psBillingGroupsFactory.createActualGroupService(
                psBillingGroupsFactory.createGroup(b -> b.externalId(secondExternalGroupId)),
                psBillingProductsFactory.createGroupProduct(b -> b.code(secondProductCode))
        );

        Assert.equals(
                Cf.set(firstProductCode, secondProductCode),
                groupsManager.findSubscribedGroupProductCodes(GroupType.ORGANIZATION,
                        Cf.list(firstExternalGroupId, secondExternalGroupId))
        );
    }

    @Test
    public void shouldReturnNextMonthForTrials() {
        DateTimeUtils.setCurrentMillisFixed(Instant.parse("2020-04-05T21:00:00Z").getMillis());

        Group group = psBillingGroupsFactory.createGroup();
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod();
        GroupProduct trialProduct = psBillingProductsFactory
                .createGroupProduct(b -> b.trialDefinitionId(Option.of(trialDefinition.getId())));

        psBillingGroupsFactory.createGroupService(group, trialProduct);

        Assert.equals(Instant.parse("2020-06-01T21:00:00Z"), groupsManager.calculateGroupNextBillingDate(group));
    }

    @Test
    public void shouldNormalBillingDateIfHasTrialAndAnotherProduct() {
        DateTimeUtils.setCurrentMillisFixed(Instant.parse("2020-04-05T21:00:00Z").getMillis());

        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct trialProduct = psBillingProductsFactory.createGroupProduct(b -> b.code("org_disk_200gb_trial_v1"));
        GroupProduct anotherProduct = psBillingProductsFactory.createGroupProduct(b -> b.code("some_code"));
        psBillingGroupsFactory.createGroupService(group, trialProduct);
        psBillingGroupsFactory.createGroupService(group, anotherProduct);

        Assert.equals(Instant.parse("2020-05-01T21:00:00Z"), groupsManager.calculateGroupNextBillingDate(group));
    }

    @Test
    public void agreementTest() {
        DateTimeUtils.setCurrentMillisFixed(Instant.parse("2020-04-05T21:00:00Z").getMillis());

        GroupProduct defaultProduct = psBillingProductsFactory.createGroupProduct(c -> c.singleton(true));
        ProductOwner productOwner = psBillingProductsFactory.createProductOwner(Option.of(defaultProduct.getId()));

        Group group1 = psBillingGroupsFactory.createGroup();
        Group group2 = psBillingGroupsFactory.createGroup();

        // free group with accepted agreement
        groupsManager.acceptAgreementForGroup(
                group1.getOwnerUid(), group1.getType(), group1.getExternalId(), false, productOwner.getCode(),
                Option.empty());
        ListF<GroupService> groupServices = groupServiceDao.find(group1.getId(), Target.ENABLED);
        GroupService defaultService = groupServices.single();
        Assert.equals(defaultProduct.getId(), defaultService.getGroupProductId());

        ListF<String> externalIds = Cf.list(group1, group2).map(Group::getExternalId);

        ListF<Group> groupsWithAgreement = groupsManager.filterGroupsByAcceptedAgreement(
                GroupType.ORGANIZATION, externalIds, productOwner.getCode());

        Assert.hasSize(1, groupsWithAgreement);
        Assert.assertContains(groupsWithAgreement, group1);
    }

    @Test
    public void testRemovePaymentData() {
        Group group = psBillingGroupsFactory.createGroup();
        Assert.assertSome(group.getPaymentInfo());
        groupsManager.removePaymentData(group);

        Group updated = groupsManager.findGroupTrustedOrThrow(group.getType(), group.getExternalId());
        Assert.assertNone(updated.getPaymentInfo());
    }

    @Test
    public void testKeepB2bAutoBillingEnabled() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(clientId, uid, true)));

        BalancePaymentInfo newPaymentInfo = new BalancePaymentInfo(clientId, uid);
        Group updatedGroup = groupsManager.createOrUpdateOrganization(uid, group.getExternalId(), false, owner,
                newPaymentInfo, Option.empty());
        Assert.assertTrue(updatedGroup.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void testKeepB2bAutoBillingEnabled_emptyPaymentInfo() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(clientId, uid, true)));

        BalancePaymentInfo newPaymentInfo = new BalancePaymentInfo(clientId, uid);
        Group updatedGroup = groupsManager.createOrUpdateOrganization(uid, group.getExternalId(), false, owner,
                newPaymentInfo, Option.empty());
        Assert.assertTrue(updatedGroup.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void testKeepB2bAutoBillingEnabledFromOtherGroup() {
        psBillingGroupsFactory.createGroup(x -> x.paymentInfo(
                new BalancePaymentInfo(clientId, uid, true)));

        BalancePaymentInfo newPaymentInfo = new BalancePaymentInfo(clientId, uid);
        Group updatedGroup = groupsManager.createOrUpdateOrganization(uid,
                UUID.randomUUID().toString(), false, owner, newPaymentInfo, Option.empty());
        Assert.assertTrue(updatedGroup.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void allowSetPaymentInfoForNewGroup() {
        String externalId = "org_id";
        BalancePaymentInfo newPaymentInfo = new BalancePaymentInfo(clientId, uid);

        Group group = groupsManager.createOrUpdateOrganization(uid, externalId, false, owner,
                newPaymentInfo, Option.empty());

        AssertHelper.assertEquals(group.getPaymentInfo().get().getClientId(), clientId);
        AssertHelper.assertEquals(group.getPaymentInfo().get().getPassportUid(), uid);
        AssertHelper.assertEquals(group.getExternalId(), externalId);
    }

    @Test
    public void allowSetNewPaymentInfo() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));
        BalancePaymentInfo newPaymentInfo = new BalancePaymentInfo(clientId, uid);

        Group updatedGroup = groupsManager.createOrUpdateOrganization(uid, group.getExternalId(), false, owner,
                newPaymentInfo, Option.empty());

        AssertHelper.assertEquals(updatedGroup.getPaymentInfo().get().getClientId(), clientId);
        AssertHelper.assertEquals(updatedGroup.getPaymentInfo().get().getPassportUid(), uid);
    }

    @Test
    public void allowSetSamePaymentInfo() {
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(clientId, uid);
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));

        Group updatedGroup = groupsManager.createOrUpdateOrganization(uid, group.getExternalId(), false, owner,
                paymentInfo, Option.empty());

        AssertHelper.assertEquals(updatedGroup.getPaymentInfo().get().getClientId(), clientId);
        AssertHelper.assertEquals(updatedGroup.getPaymentInfo().get().getPassportUid(), uid);
    }

    @Test
    public void ForbidSetPaymentInfoUpdate() {
        PassportUid uid1 = PassportUid.cons(1);
        PassportUid uid2 = PassportUid.cons(2);
        long clientId1 = 1L;
        long clientId2 = 2L;
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId1, uid1)));

        AssertHelper.assertBadRequest(() ->
                groupsManager.createOrUpdateOrganization(uid1, group.getExternalId(), false, owner,
                        new BalancePaymentInfo(clientId1, uid2), Option.empty()));

        AssertHelper.assertBadRequest(() ->
                groupsManager.createOrUpdateOrganization(uid1, group.getExternalId(), false, owner,
                        new BalancePaymentInfo(clientId2, uid1), Option.empty()));

        AssertHelper.assertBadRequest(() ->
                groupsManager.createOrUpdateOrganization(uid1, group.getExternalId(), false, owner,
                        new BalancePaymentInfo(clientId2, uid2), Option.empty()));
    }

    @Test
    public void testCreateOrganizationPartnerClient() {
        String externalId = "org_id";
        String partnerExternalId = "partner_id";
        BalancePaymentInfo newPaymentInfo = new BalancePaymentInfo(clientId, uid);

        Mockito.when(directoryClient.getOrganizationPartnerId(externalId))
                .thenReturn(Option.of(partnerExternalId));

        Group group = groupsManager.createOrUpdateOrganization(uid, externalId, false,
                owner, newPaymentInfo, Option.empty());

        Option<String> partner = groupPartnerDao.getPartnerId(group.getId());
        Assert.equals(partnerExternalId, partner.get());
    }

    @Test
    public void testCreateOrganizationNotPartnerClient() {
        Group group = groupsManager.createOrUpdateOrganization(uid,  "org_id",
                false, owner, new BalancePaymentInfo(clientId, uid), Option.empty());

        Assert.assertEmpty(groupPartnerDao.getPartnerId(group.getId()));
    }

    @Test
    public void testCreateOrganizationPartnerClientWithoutPayment() {
        String groupExternalId = "org_id";
        String partnerExternalId = "partner_id";
        GroupType groupType = GroupType.ORGANIZATION;

        Mockito.when(directoryClient.getOrganizationPartnerId(groupExternalId))
                .thenReturn(Option.of(partnerExternalId));

        groupsManager.acceptAgreementForGroup(uid, groupType,
                groupExternalId, false, owner.getCode(), Option.empty());

        Group group = groupsManager.findGroupTrusted(groupType, groupExternalId).get();
        Option<String> partner = groupPartnerDao.getPartnerId(group.getId());
        Assert.equals(partnerExternalId, partner.get());
    }

    @Test
    public void hasPaymentInfoForOrganizationTest() {
        psBillingGroupsFactory.createGroup(b -> b.externalId("somecode").paymentInfo(null));
        Assert.isFalse(groupsManager.hasPaymentInfoForOrganization("somecode"));

        psBillingGroupsFactory.createGroup(b -> b.externalId("somecode1"));
        Assert.isTrue(groupsManager.hasPaymentInfoForOrganization("somecode1"));
    }

    @Before
    public void setup() {
        owner = productOwnerDao.create("yandex_mail", Option.empty());
        uid = PassportUid.cons(1111);
        clientId = 1L;

        Mockito.when(directoryClient.getOrganizationPartnerId(Mockito.anyString()))
                .thenReturn(Option.empty());
    }
}
