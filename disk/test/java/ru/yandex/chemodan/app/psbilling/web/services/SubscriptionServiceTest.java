package ru.yandex.chemodan.app.psbilling.web.services;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.BucketContentDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialUsageDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServicePriceOverride;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.PriceOverrideReason;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialUsage;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductOwner;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupsManager;
import ru.yandex.chemodan.app.psbilling.core.products.BucketContentManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.web.config.PsBillingWebServicesConfiguration;
import ru.yandex.chemodan.app.psbilling.web.model.AddressPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.model.UrPaymentDataPojo;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationByIdResponse;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.chemodan.test.ConcurrentTestUtils;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory.DEFAULT_CLIENT_ID;
import static ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem.buildActiveBalanceContract;

@ContextConfiguration(classes = {
        PsBillingWebServicesConfiguration.class,
        SubscriptionServiceTest.BalanceMockConfiguration.class
})
public class SubscriptionServiceTest extends AbstractPsBillingCoreTest {

    public static final PassportUid TEST_UID = PassportUid.cons(3000185708L);
    public static final String PRODUCT_CODE = "org_disk_2tb_v1";
    public static final String CONFLICTING_PRODUCT_CODE = "org_disk_200gb_trial_v1";

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private GroupsManager groupsManager;
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private TrialUsageDao trialUsageDao;
    @Autowired
    private GroupServicePriceOverrideDao groupServicePriceOverrideDao;
    @Autowired
    private BucketContentDao bucketContentDao;
    @Autowired
    private BucketContentManager bucketContentManager;
    @Autowired
    private DirectoryClient directoryClient;
    @Autowired
    private ProductLineDao productLineDao;
    @Autowired
    private FeatureFlags featureFlags;
    @Autowired
    private JdbcTemplate3 jdbcTemplate3;

    @Test
    @Ignore
    public void createNewPerson() {
        subscriptionService.subscribeOrganization(TEST_UID, "1111", PRODUCT_CODE,
                Option.empty(), Option.of(buildPaymentData()), Option.empty(), Option.empty(), Cf.list());
    }

    @Test
    @Ignore
    public void useExistingPerson() {
        subscriptionService.subscribeOrganization(TEST_UID, "1111", PRODUCT_CODE,
                Option.of("82801340"), Option.empty(), Option.empty(), Option.empty(), Cf.list());
    }

    @Test
    public void testPsBillingDisabledFeature() {
        Mockito.when(directoryClient.getOrganizationById(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationByIdResponse("108", "portal"));
        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
        Assert.assertThrows(
                () -> subscriptionService.subscribeOrganization(TEST_UID, "108", PRODUCT_CODE, Option.of("82801340"),
                        Option.empty(), Option.empty(), Option.empty(), Cf.list()),
                A3ExceptionWithStatus.class, e -> e.getErrorName().equals("paid_product_not_available"));
    }

    @Before
    public void setupDirectoryClient() {
        Mockito.when(directoryClient
                        .getOrganizationById(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationByIdResponse("dummy_id", "common"));
        Mockito.when(directoryClient
                        .getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
    }

    @Before
    public void setupBalanceService() {
        Mockito.when(balanceService.findClient(TEST_UID)).thenReturn(Option.of(DEFAULT_CLIENT_ID));
        Mockito.when(balanceService.getClientContracts(DEFAULT_CLIENT_ID))
                .thenReturn(Cf.list(buildActiveBalanceContract()));
        Mockito.when(balanceService.getActiveContract(DEFAULT_CLIENT_ID))
                .thenReturn(Option.of(buildActiveBalanceContract()));
    }

    @Before
    public void setup() {
        featureFlags.getCheckAvailableGroupProduct().setValue("false");
    }

    @Test
    @Repeat(5)
    public void singletonProductParallelCreation() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct groupProductEntity = psBillingProductsFactory.createGroupProduct(b -> b.singleton(true));

        ConcurrentTestUtils.testConcurrency(() -> () -> {
            subscriptionService.subscribeOrganization(TEST_UID, group.getExternalId(), groupProductEntity.getCode(),
                    Option.empty(), Cf.list());
        });

        ListF<GroupService> groupServices = groupServiceDao.find(group.getId());
        assertEquals(1, groupServices.size());
    }

    @Test
    public void testConflictingProductsDisabling() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct groupProductEntity1 =
                psBillingProductsFactory.createGroupProduct(b -> b.singleton(true).code(PRODUCT_CODE));
        GroupProduct groupProductEntity2 =
                psBillingProductsFactory.createGroupProduct(b -> b.singleton(true).code(CONFLICTING_PRODUCT_CODE));

        bucketContentDao.addRowBucket("group1", PRODUCT_CODE);
        bucketContentDao.addRowBucket("group1", CONFLICTING_PRODUCT_CODE);
        bucketContentManager.refresh();

        GroupService groupService1 = subscriptionService
                .subscribeOrganization(TEST_UID, group.getExternalId(), groupProductEntity1.getCode(), Option.empty()
                        , Cf.list());
        GroupService groupService2 = subscriptionService
                .subscribeOrganization(TEST_UID, group.getExternalId(), groupProductEntity2.getCode(), Option.empty()
                        , Cf.list());

        GroupService updatedService1 = groupServiceDao.findById(groupService1.getId());
        assertEquals(Target.DISABLED, updatedService1.getTarget());
    }

    @Test
    public void testRereadBucketContent() throws InterruptedException {
        psBillingProductsFactory.createGroupProduct(b -> b.singleton(true).code(PRODUCT_CODE));
        psBillingProductsFactory.createGroupProduct(b -> b.singleton(true).code(CONFLICTING_PRODUCT_CODE));

        bucketContentManager.refresh();
        assertEquals(bucketContentManager.getBuckets().size(), 0);

        bucketContentDao.addRowBucket("group2", PRODUCT_CODE);
        bucketContentDao.addRowBucket("group2", CONFLICTING_PRODUCT_CODE);

        Thread.sleep(2000);

        assertEquals(bucketContentManager.getBuckets().size(), 1);
    }

    @Test
    public void testDefaultProductCreation() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct defaultGroupProductEntity =
                psBillingProductsFactory.createGroupProduct(b -> b.singleton(true));
        ProductOwner productOwner =
                psBillingProductsFactory.createProductOwner(Option.of(defaultGroupProductEntity.getId()));
        GroupProduct groupProductEntity = psBillingProductsFactory.createGroupProduct(b -> {
            UserProductEntity userProduct =
                    psBillingProductsFactory.createUserProduct(c -> c.productOwnerId(productOwner.getId()));
            return b.userProduct(userProduct.getId());
        });

        GroupService groupService = subscriptionService
                .subscribeOrganization(TEST_UID, group.getExternalId(), groupProductEntity.getCode(), Option.empty(),
                        Cf.list());
        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), Target.ENABLED);
        assertEquals(2, groupServices.size());
        GroupService defaultGroupService =
                groupServices.filterNot(gs -> gs.getId().equals(groupService.getId())).single();
        assertEquals(defaultGroupProductEntity.getId(), defaultGroupService.getGroupProductId());
    }

    @Test
    public void agreementsTest() {
        DateTimeUtils.setCurrentMillisFixed(Instant.parse("2020-04-05T21:00:00Z").getMillis());

        ProductOwner productOwner = psBillingProductsFactory.getOrCreateProductOwner();

        GroupProduct groupProductEntity =
                psBillingProductsFactory.createGroupProduct(b -> b.singleton(true).code(PRODUCT_CODE));

        Group group = psBillingGroupsFactory.createGroup();

        // group with subscription
        subscriptionService
                .subscribeOrganization(TEST_UID, group.getExternalId(), groupProductEntity.getCode(), Option.empty(),
                        Cf.list());

        ListF<String> externalIds = Cf.list(group.getExternalId());

        ListF<Group> groupsWithAgreement = groupsManager.filterGroupsByAcceptedAgreement(
                GroupType.ORGANIZATION, externalIds, productOwner.getCode());

        Assert.hasSize(1, groupsWithAgreement);
        Assert.assertContains(groupsWithAgreement, group);
    }

    public static void testSubscriptionWithTrialImpl(
            TrialDefinitionEntity trialDefinition,
            GroupProduct groupProduct,
            PsBillingProductsFactory psBillingProductsFactory,
            PsBillingGroupsFactory psBillingGroupsFactory,
            SubscriptionService subscriptionService,
            TrialUsageDao trialUsageDao,
            GroupServicePriceOverrideDao groupServicePriceOverrideDao
    ) {
        Group group = psBillingGroupsFactory.createGroup();

        DateUtils.freezeTime();

        GroupService groupService = subscriptionService
                .subscribeOrganization(TEST_UID, group.getExternalId(), groupProduct.getCode(), Option.empty(),
                        Cf.list());

        ListF<TrialUsage> trialUsages = trialUsageDao.findTrialUsages(
                Option.of(group.getId()),
                Option.of(TEST_UID),
                trialDefinition.getSingleUsageComparisonKey().get()
        );
        TrialUsage trialUsage = trialUsages.single();
        assertTrialUsage(trialDefinition, group, trialUsage, DateTime.now().plusMonths(1).toInstant());

        assertGroupServicePriceOverride(trialDefinition, groupServicePriceOverrideDao, groupService, trialUsage,
                DateTime.now().plusMonths(1).toInstant());
    }

    private static void assertGroupServicePriceOverride(TrialDefinitionEntity trialDefinition,
                                                        GroupServicePriceOverrideDao groupServicePriceOverrideDao,
                                                        GroupService groupService, TrialUsage trialUsage,
                                                        Instant endDate) {
        MapF<UUID, ListF<GroupServicePriceOverride>> priceOverrides =
                groupServicePriceOverrideDao.findByGroupServices(Cf.list(groupService.getId()));
        assertEquals(1, priceOverrides.size());
        assertTrue(priceOverrides.getO(groupService.getId()).isPresent());
        ListF<GroupServicePriceOverride> overrides = priceOverrides.getO(groupService.getId()).get();
        assertEquals(1, overrides.size());
        GroupServicePriceOverride override = overrides.single();
        assertEquals(trialDefinition.getPrice(), override.getPricePerUserInMonth());
        assertEquals(PriceOverrideReason.TRIAL, override.getReason());
        assertEquals(trialDefinition.isHidden(), override.isHidden());
        assertEquals(groupService.getId(), override.getGroupServiceId());
        assertEquals(Option.of(trialUsage.getId()), override.getTrialUsageId());
        assertEquals(groupService.getCreatedAt(), override.getStartDate());
        assertEquals(endDate, override.getEndDate().get());
    }

    private static void assertTrialUsage(TrialDefinitionEntity trialDefinition, Group group, TrialUsage trialUsage,
                                         Instant endDate) {
        assertEquals(TEST_UID, trialUsage.getActivatedByUid().get());
        assertEquals(endDate, trialUsage.getEndDate());
        assertEquals(group.getId(), trialUsage.getGroupId().get());
        assertEquals(trialDefinition.getId(), trialUsage.getTrialDefinitionId());
    }

    @Test
    public void testSubscriptionWithTrial() {
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod();
        GroupProduct groupProductEntity = psBillingProductsFactory.createGroupProduct(b -> b
                .singleton(true)
                .trialDefinitionId(Option.of(trialDefinition.getId()))
                .code(PRODUCT_CODE));

        testSubscriptionWithTrialImpl(
                trialDefinition,
                groupProductEntity,
                psBillingProductsFactory,
                psBillingGroupsFactory,
                subscriptionService,
                trialUsageDao,
                groupServicePriceOverrideDao);
    }

    @Test
    public void testSubscriptionPromoWithTrialWithoutKey() {
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .singleUsageComparisonKey(Option.empty())
        );

        GroupProduct groupProduct = createProductInPromo(trialDefinition, x -> x
                .applicationArea(PromoApplicationArea.GLOBAL_B2B)
                .applicationType(PromoApplicationType.MULTIPLE_TIME)
        );

        Group group = psBillingGroupsFactory.createGroup();

        //делаем подписку на продукт
        GroupService groupService = subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                Cf.list()
        );

        ListF<TrialUsage> trialUsages = trialUsageDao.findByTrialDefinitionAndGroup(
                trialDefinition.getId(),
                group.getId()
        );

        TrialUsage trialUsage = trialUsages.single();
        assertTrialUsage(trialDefinition, group, trialUsage, DateTime.now().plusMonths(1).toInstant());

        assertGroupServicePriceOverride(trialDefinition, groupServicePriceOverrideDao, groupService, trialUsage,
                DateTime.now().plusMonths(1).toInstant());

        //отменяем серввис
        groupServicesManager.disableGroupService(groupService);

        DateUtils.freezeTime(DateTime.now().plusDays(1).toInstant());

        Instant endTime = DateTime.now().plusMonths(1).minusDays(1).toInstant();
        //вернулись обратно
        GroupService returnGroupService = subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                Cf.list()
        );

        ListF<TrialUsage> trialUsagesNew = trialUsageDao.findByTrialDefinitionAndGroup(
                trialDefinition.getId(),
                group.getId()
        );

        TrialUsage trialUsageNew = trialUsagesNew.single();
        Assert.equals(trialUsage, trialUsageNew, "Reuse trialUsages is important");
        assertTrialUsage(trialDefinition, group, trialUsageNew, endTime);

        assertGroupServicePriceOverride(trialDefinition, groupServicePriceOverrideDao, returnGroupService,
                trialUsageNew, endTime);
    }

    @Test
    public void testSubscriptionWithTrialWithoutKeyWithoutReuseWithoutPromo() {
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .singleUsageComparisonKey(Option.empty())
        );

        GroupProduct groupProduct = createProductInPromo(trialDefinition, x -> x
                .applicationArea(PromoApplicationArea.GLOBAL_B2B)
                .applicationType(PromoApplicationType.ONE_TIME)
        );

        Group group = psBillingGroupsFactory.createGroup();

        //делаем подписку на продукт
        GroupService groupService = subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                Cf.list()
        );

        ListF<TrialUsage> trialUsages = trialUsageDao.findByTrialDefinitionAndGroup(
                trialDefinition.getId(),
                group.getId()
        );

        TrialUsage trialUsage = trialUsages.single();
        assertTrialUsage(trialDefinition, group, trialUsage, DateTime.now().plusMonths(1).toInstant());

        assertGroupServicePriceOverride(trialDefinition, groupServicePriceOverrideDao, groupService, trialUsage,
                DateTime.now().plusMonths(1).toInstant());

        //отменяем серввис
        groupServicesManager.disableGroupService(groupService);

        DateUtils.freezeTime(DateTime.now().plusMonths(2).toInstant());
        //вернулись обратно
        GroupService returnGroupService = subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                Cf.list()
        );

        ListF<TrialUsage> trialUsagesNew = trialUsageDao.findByTrialDefinitionAndGroup(
                trialDefinition.getId(),
                group.getId()
        );

        TrialUsage trialUsageNew = trialUsagesNew.single();
        Assert.equals(trialUsage, trialUsageNew, "Reuse trialUsages is important");

        MapF<UUID, ListF<GroupServicePriceOverride>> priceOverrides =
                groupServicePriceOverrideDao.findByGroupServices(Cf.list(returnGroupService.getId()));

        Assert.isEmpty(priceOverrides);
    }

    @NotNull
    private GroupProduct createProductInPromo(
            TrialDefinitionEntity trialDefinition,
            Function<PromoTemplateDao.InsertData.InsertDataBuilder, PromoTemplateDao.InsertData.InsertDataBuilder> promo
    ) {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(b -> b
                .singleton(true)
                .trialDefinitionId(Option.of(trialDefinition.getId()))
                .code(PRODUCT_CODE));

        ProductLineEntity line = psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(groupProduct));

        PromoTemplateEntity testPromo = psBillingPromoFactory.createPromo(promo);

        psBillingPromoFactory.bindToProductLines(testPromo.getId(), line.getId());
        return groupProduct;
    }

    @Test
    public void testSubscriptionPromoWithTrialWithoutKeyReuseAfterFullUse() {
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .singleUsageComparisonKey(Option.empty())
        );

        GroupProduct groupProduct = createProductInPromo(trialDefinition, x -> x
                .applicationArea(PromoApplicationArea.GLOBAL_B2B)
                .applicationType(PromoApplicationType.MULTIPLE_TIME)
                .toDate(Option.empty())
        );

        Group group = psBillingGroupsFactory.createGroup();

        //делаем подписку на продукт
        GroupService groupService = subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                Cf.list()
        );

        ListF<TrialUsage> trialUsages = trialUsageDao.findByTrialDefinitionAndGroup(
                trialDefinition.getId(),
                group.getId()
        );

        Instant endTime = DateTime.now().plusMonths(1).toInstant();

        TrialUsage trialUsage = trialUsages.single();
        assertTrialUsage(trialDefinition, group, trialUsage, endTime);

        assertGroupServicePriceOverride(trialDefinition, groupServicePriceOverrideDao, groupService, trialUsage,
                endTime);

        //отменяем серввис
        groupServicesManager.disableGroupService(groupService);

        DateUtils.freezeTime(DateTime.now().plusMonths(5).toInstant());

        Instant newEndTime = DateTime.now().plusMonths(1).toInstant();
        //вернулись обратно
        GroupService returnGroupService = subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                Cf.list()
        );

        ListF<TrialUsage> trialUsagesNew = trialUsageDao.findByTrialDefinitionAndGroup(
                trialDefinition.getId(),
                group.getId()
        );

        Assert.hasSize(2, trialUsagesNew);

        TrialUsage trialUsageNew = trialUsagesNew.maxBy(AbstractEntity::getCreatedAt);
        Assert.notEquals(trialUsage, trialUsageNew, "New trialUsages is important");
        assertTrialUsage(trialDefinition, group, trialUsageNew, newEndTime);

        assertGroupServicePriceOverride(trialDefinition, groupServicePriceOverrideDao, returnGroupService,
                trialUsageNew, newEndTime);
    }

    @Test
    public void subscribe_unknownProduct() {
        featureFlags.getCheckAvailableGroupProduct().setValue("true");
        GroupProduct unavailableProduct1 = createProductLine("productLineSelectorFactory.unavailableSelector()", 1);
        GroupProduct availableProduct = createProductLine("productLineSelectorFactory.availableSelector()", 2);
        GroupProduct unavailableProduct2 = createProductLine("productLineSelectorFactory.availableSelector()", 3);
        Group group = psBillingGroupsFactory.createGroup();

        subscriptionService.subscribeOrganization(PassportUid.MAX_VALUE, group.getExternalId(),
                availableProduct.getCode(), Option.empty(), Cf.list());
        checkGroupServices(true, group, unavailableProduct1);
        checkGroupServices(true, group, unavailableProduct2);
        checkGroupServices(false, group, availableProduct);

        Assert.assertThrows(
                () -> subscriptionService.subscribeOrganization(PassportUid.MAX_VALUE, group.getExternalId(),
                        unavailableProduct1.getCode(), Option.empty(), Cf.list()),
                A3ExceptionWithStatus.class
        );
        checkGroupServices(true, group, unavailableProduct1);
        checkGroupServices(true, group, unavailableProduct2);
        checkGroupServices(false, group, availableProduct);

        Assert.assertThrows(
                () -> subscriptionService.subscribeOrganization(PassportUid.MAX_VALUE, group.getExternalId(),
                        unavailableProduct2.getCode(), Option.empty(), Cf.list()),
                A3ExceptionWithStatus.class
        );
        checkGroupServices(true, group, unavailableProduct1);
        checkGroupServices(true, group, unavailableProduct2);
        checkGroupServices(false, group, availableProduct);
    }

    @Test
    public void subscribe_unknownProductWithoutFeature() {
        featureFlags.getCheckAvailableGroupProduct().setValue("false");
        GroupProduct unavailableProduct1 = createProductLine("productLineSelectorFactory.unavailableSelector()", 1);
        GroupProduct availableProduct = createProductLine("productLineSelectorFactory.availableSelector()", 2);
        GroupProduct unavailableProduct2 = createProductLine("productLineSelectorFactory.availableSelector()", 3);
        Group group = psBillingGroupsFactory.createGroup();

        bucketContentManager.refresh();

        subscriptionService.subscribeOrganization(PassportUid.MAX_VALUE, group.getExternalId(),
                availableProduct.getCode(), Option.empty(), Cf.list());
        checkGroupServices(true, group, unavailableProduct1);
        checkGroupServices(true, group, unavailableProduct2);
        checkGroupServices(false, group, availableProduct);

        subscriptionService.subscribeOrganization(PassportUid.MAX_VALUE, group.getExternalId(),
                unavailableProduct1.getCode(), Option.empty(), Cf.list());
        checkGroupServices(false, group, unavailableProduct1);
        checkGroupServices(true, group, unavailableProduct2);
        checkGroupServices(true, group, availableProduct);

        subscriptionService.subscribeOrganization(PassportUid.MAX_VALUE, group.getExternalId(),
                unavailableProduct2.getCode(), Option.empty(), Cf.list());
        checkGroupServices(true, group, unavailableProduct1);
        checkGroupServices(false, group, unavailableProduct2);
        checkGroupServices(true, group, availableProduct);
    }

    @Test
    public void testRevokeGroupSubscription() {
        Group group = psBillingGroupsFactory.createGroup();

        GroupProduct product = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).
                        hidden(false).
                        skipTransactionsExport(true));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()),
                1);

        GroupProduct defaultProduct = psBillingProductsFactory.createGroupProduct(c -> c.singleton(true));
        ProductOwner productOwner = psBillingProductsFactory.createProductOwner(Option.of(defaultProduct.getId()));
        // пришлось так костылять, т.к. у userProduct в defaultProduct owner становится другим.
        // без костыля не обойтись, т.к. получается, что при создании продукта уже нужен owner, а при создании
        // owner'а уже нужен продукт.
        jdbcTemplate3.update("update user_products set product_owner_id = ? where id = ?",
                productOwner.getId(), defaultProduct.getUserProduct().getId());
        GroupService defaultService = psBillingGroupsFactory.createGroupService(group, defaultProduct);

        Assert.equals(2, groupServiceDao.find(group.getId(), Target.ENABLED).size());

        subscriptionService.revokeGroupSubscription(group.getExternalId(), Option.empty());
        ListF<GroupService> services = groupServiceDao.find(group.getId(), Target.ENABLED);
        Assert.equals(1, services.size());
        Assert.equals(defaultService.getId(), services.first().getId());
    }

    @Test
    public void testRevokeGroupSubscription_byServiceId() {
        Group group = psBillingGroupsFactory.createGroup();

        GroupProduct product = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).
                        hidden(false).
                        skipTransactionsExport(true));
        GroupService service = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()),
                1);

        GroupProduct product2 = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).
                        hidden(false).
                        skipTransactionsExport(true));
        GroupService service2 = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product2.getCode()),
                1);

        Assert.equals(2, groupServiceDao.find(group.getId(), Target.ENABLED).size());

        subscriptionService.revokeGroupSubscription(group.getExternalId(), Option.of(service.getId()));
        ListF<GroupService> services = groupServiceDao.find(group.getId(), Target.ENABLED);
        Assert.equals(1, services.size());
        Assert.equals(service2.getId(), services.first().getId());
    }


    private void checkGroupServices(boolean empty, Group group, GroupProduct groupProduct) {
        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), groupProduct.getId());
        Assert.sizeIs(empty ? 0 : 1, groupServices.filter(gs -> gs.getTarget().equals(Target.ENABLED)));
    }

    private GroupProduct createProductLine(String selectorBeanEL, int orderNum) {
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        ProductLineEntity productLine = psBillingProductsFactory.createProductLine("productSet", x -> x
                .orderNum(orderNum).selectorBeanEL(selectorBeanEL)
        );
        productLineDao.bindGroupProducts(productLine.getId(), Cf.list(groupProduct.getId()));
        psBillingProductsFactory.addGroupProductsToBucket("bucket", groupProduct.getCode());
        return groupProduct;
    }

    private static PaymentDataPojo buildPaymentData() {
        return new UrPaymentDataPojo("abc@y-t.ru", "89269992233", "225", "blablalba", "7743013901", "12345",
                buildAddress("legal"), buildAddress("post"), false);
    }

    private static AddressPojo buildAddress(String key) {
        return new AddressPojo("1430" + key, "льва толстого, 16, комн " + key);
    }

    @Configuration
    public static class BalanceMockConfiguration {
        @Autowired
        private PsBillingCoreMocksConfig mocksConfig;

        @Primary
        @Bean
        public BalanceService balanceService() {
            return mocksConfig.addMock(BalanceService.class);
        }
    }
}
