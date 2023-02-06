package ru.yandex.chemodan.app.psbilling.web.services;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.balance.CorpContract;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceInfo;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupBalanceService;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupBillingService;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialUsageDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.web.config.PsBillingWebServicesConfiguration;
import ru.yandex.chemodan.app.psbilling.web.exceptions.WebActionException;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationByIdResponse;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory.DEFAULT_CLIENT_ID;
import static ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem.buildActiveBalanceContract;

@ContextConfiguration(classes = {
        PsBillingWebServicesConfiguration.class,
        SubscriptionServicePrepaidTest.MocksConfiguration.class
})
public class SubscriptionServicePrepaidTest extends AbstractPsBillingCoreTest {
    public static final PassportUid TEST_UID = PassportUid.cons(3000185708L);
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private GroupBillingService groupBillingService;
    @Autowired
    private GroupBalanceService groupBalanceService;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private DirectoryClient directoryClient;
    @Autowired
    private TrialUsageDao trialUsageDao;
    @Autowired
    private GroupServicePriceOverrideDao groupServicePriceOverrideDao;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupServicesManager groupServicesManager;

    private final Currency currency = Currency.getInstance("RUB");
    private Group group;
    private GroupProduct groupProduct;


    @Test
    public void subscribeOrganization__success() {
        setup(Option.of(1000f), Option.empty());

        subscriptionService.subscribeOrganization(TEST_UID, group.getExternalId(), groupProduct.getCode(), Option.empty(), Cf.list());

        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), groupProduct.getId());
        Assert.sizeIs(1, groupServices);

        Assert.equals(Target.ENABLED, groupServices.first().getTarget());
    }

    @Test
    public void subscribeOrganization__emptyBalance() {
        setup(Option.of(0f), Option.of(0f));

        Assert.assertThrows(
                () -> subscriptionService.subscribeOrganization(TEST_UID, group.getExternalId(), groupProduct.getCode(), Option.empty(), Cf.list()),
                WebActionException.cons400("").getClass()
        );

        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), groupProduct.getId());
        Assert.sizeIs(0, groupServices);
    }

    @Test
    public void subscribeOrganizationWithTrial__emptyBalance() {
        setup(Option.of(0f), Option.of(0f));

        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod();
        GroupProduct groupProductEntity = psBillingProductsFactory.createGroupProduct(b -> b
                .singleton(true)
                .paymentType(GroupPaymentType.PREPAID)
                .trialDefinitionId(Option.of(trialDefinition.getId())));

        SubscriptionServiceTest.testSubscriptionWithTrialImpl(
                trialDefinition,
                groupProductEntity,
                psBillingProductsFactory,
                psBillingGroupsFactory,
                subscriptionService,
                trialUsageDao,
                groupServicePriceOverrideDao);
    }

    @Test
    public void subscribeOrganization__emptyBalanceWithoutCheck() {
        setup(Option.of(0f), Option.of(0f));

        subscriptionService.subscribeOrganization(
                TEST_UID,
                group.getExternalId(),
                groupProduct.getCode(),
                Option.empty(),
                SubscriptionService.SubscriptionCheckRightConfig.builder()
                        .skipValidateClientBalance(true)
                        .skipAvailableProductValidation(false)
                        .build(),
                Cf.list()
        );

        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), groupProduct.getId());
        Assert.sizeIs(1, groupServices);

        Assert.equals(Target.ENABLED, groupServices.first().getTarget());
    }

    @Test
    public void subscribeOrganization__zeroBalanceAndNotZeroBillingBalance() {
        setup(Option.of(0f), Option.of(1000f));
        subscriptionService.subscribeOrganization(TEST_UID, group.getExternalId(), groupProduct.getCode(), Option.empty(), Cf.list());

        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), groupProduct.getId());
        Assert.sizeIs(1, groupServices);

        Assert.equals(Target.ENABLED, groupServices.first().getTarget());

        Option<ClientBalanceEntity> clientBalance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(), currency);
        Assert.notEmpty(clientBalance);
        Assert.equals(currency, clientBalance.get().getBalanceCurrency());
        Assert.equals(BigDecimal.valueOf(1000f), clientBalance.get().getBalanceAmount());
    }

    @Test
    public void subscribeOrganization__emptyBalanceAndCorrectBillingBalance() {
        setup(Option.empty(), Option.of(1000f));

        subscriptionService.subscribeOrganization(TEST_UID, group.getExternalId(), groupProduct.getCode(), Option.empty(), Cf.list());

        ListF<GroupService> groupServices = groupServiceDao.find(group.getId(), groupProduct.getId());
        Assert.sizeIs(1, groupServices);

        Assert.equals(Target.ENABLED, groupServices.first().getTarget());

        Option<ClientBalanceEntity> clientBalance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(), currency);
        Assert.notEmpty(clientBalance);
        Assert.equals(currency, clientBalance.get().getBalanceCurrency());
        Assert.equals(BigDecimal.valueOf(1000f), clientBalance.get().getBalanceAmount());
    }


    @Test
    public void addAddonsTest() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons = psBillingProductsFactory.createMainsAndAddons();
        GroupProduct mainProduct = mainsAndAddons.get1().get(0);
        GroupProduct addonProduct = mainsAndAddons.get2().get(0);
        Group mainGroup = psBillingGroupsFactory.createGroup();

        ListF<String> subgroupExtIds = Cf.arrayList();
        int count = 10;
        for (int i = 0; i < count; i++) {
            subgroupExtIds.add(UUID.randomUUID().toString());
        }
        psBillingGroupsFactory.createGroupService(mainGroup, mainProduct);

        subscriptionService.subscribeOrganizationSubgroup(mainGroup.getOwnerUid(), mainGroup.getExternalId(), subgroupExtIds,
                GroupType.ORGANIZATION_USER, addonProduct.getCode(), Option.empty());
        for (String subgroupExtId : subgroupExtIds) {
            validateAddon(subgroupExtId, addonProduct, mainGroup, Target.ENABLED);
        }
    }


    @Test
    public void changeAddonTest() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons = psBillingProductsFactory.createMainsAndAddons();
        GroupProduct mainProduct = mainsAndAddons.get1().get(0);
        GroupProduct addonProduct1 = mainsAndAddons.get2().get(0);
        GroupProduct addonProduct2 = mainsAndAddons.get2().get(1);
        Group mainGroup = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createGroupService(mainGroup, mainProduct);
        String subgroupExtId = "subgroupExtId";
        subscriptionService.subscribeOrganizationSubgroup(mainGroup.getOwnerUid(), mainGroup.getExternalId(), Cf.list(subgroupExtId),
                GroupType.ORGANIZATION_USER, addonProduct1.getCode(), Option.empty());
        validateAddon(subgroupExtId, addonProduct1, mainGroup, Target.ENABLED);
        subscriptionService.subscribeOrganizationSubgroup(mainGroup.getOwnerUid(), mainGroup.getExternalId(), Cf.list(subgroupExtId),
                GroupType.ORGANIZATION_USER, addonProduct2.getCode(), Option.empty());
        validateAddon(subgroupExtId, addonProduct1, mainGroup, Target.DISABLED);
        validateAddon(subgroupExtId, addonProduct2, mainGroup, Target.ENABLED);
    }

    @Test
    public void resubscribeAddonTest() {
        Tuple2<ListF<GroupProduct>, ListF<GroupProduct>> mainsAndAddons = psBillingProductsFactory.createMainsAndAddons();
        GroupProduct mainProduct = mainsAndAddons.get1().get(0);
        GroupProduct addonProduct1 = mainsAndAddons.get2().get(0);

        Group mainGroup = psBillingGroupsFactory.createGroup();
        psBillingGroupsFactory.createGroupService(mainGroup, mainProduct);
        String subgroupExtId = "subgroupExtId";
        subscriptionService.subscribeOrganizationSubgroup(mainGroup.getOwnerUid(), mainGroup.getExternalId(), Cf.list(subgroupExtId),
                GroupType.ORGANIZATION_USER, addonProduct1.getCode(), Option.empty());

        GroupService oldService = validateAddon(subgroupExtId, addonProduct1, mainGroup, Target.ENABLED);
        groupServicesManager.disableGroupService(oldService);
        GroupService newService = subscriptionService.subscribeOrganizationSubgroup(mainGroup.getOwnerUid(),
                mainGroup.getExternalId(), Cf.list(subgroupExtId),
                GroupType.ORGANIZATION_USER, addonProduct1.getCode(), Option.empty()).get(0);
        Assert.notEquals(oldService.getId(), newService.getId());
        Assert.equals(Target.ENABLED, newService.getTarget());
    }

    @Test
    public void unsubscribeSubgroupServiceTest() {
        Group mainGroup = psBillingGroupsFactory.createGroup();
        Group subGroup = psBillingGroupsFactory.createGroup(
                builder -> builder.parentGroupId(Option.of(mainGroup.getId())));
        GroupService groupService = psBillingGroupsFactory.createGroupService(
                subGroup, psBillingProductsFactory.createGroupProduct());
        Assert.equals(Target.ENABLED, groupService.getTarget());
        subscriptionService.unsubscribeOrganization(mainGroup.getOwnerUid(), mainGroup.getExternalId(),
                groupService.getId());
        Assert.equals(Target.DISABLED, groupServiceDao.findById(groupService.getId()).getTarget());
    }

    private GroupService validateAddon(String subgroupExtId, GroupProduct addonProduct, Group mainGroup, Target expectedTarget) {
        Group subgroup = groupDao.findGroup(GroupType.ORGANIZATION_USER, subgroupExtId, mainGroup.getId()).get();
        ListF<GroupService> groupServices = groupServiceDao.find(subgroup.getId(), addonProduct.getId());
        Assert.equals(1, groupServices.size());
        GroupService groupService = groupServices.get(0);
        Assert.equals(expectedTarget, groupService.getTarget());
        return groupService;
    }


    private void setup(Option<Float> dbBalance, Option<Float> billingBalance) {
        groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID).priceCurrency(currency)
        );
        group = psBillingGroupsFactory.createGroup();

        if (dbBalance.isPresent()) {
            clientBalanceDao.insert(
                    ClientBalanceDao.InsertData.builder()
                            .clientId(group.getPaymentInfo().get().getClientId())
                            .balanceAmount(dbBalance.map(BigDecimal::valueOf).get())
                            .balanceCurrency(currency)
                            .build()
            );
        }

        if (billingBalance.isPresent()) {
            ClientBalanceInfo clientBalanceInfo = new ClientBalanceInfo(0L, currency);
            clientBalanceInfo.setIncomeSum(billingBalance.map(BigDecimal::valueOf).get());


            Mockito.when(groupBalanceService.getContractBalanceItems(DEFAULT_CLIENT_ID, true))
                    .thenReturn(Cf.list(Mockito.mock(CorpContract.class)));
            Mockito.when(groupBalanceService.getBalanceAmount(Mockito.anyLong(), Mockito.any()))
                    .thenReturn(Cf.map(currency, clientBalanceInfo));
        }
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
    public void setupDirectoryClient() {
        Mockito.when(directoryClient
                        .getOrganizationById(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationByIdResponse("dummy_id", "common"));
        Mockito.when(directoryClient
                        .getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
    }

    @Configuration
    public static class MocksConfiguration {
        @Autowired
        private PsBillingCoreMocksConfig mocksConfig;

        @Primary
        @Bean
        public GroupBillingService groupBillingService() {
            return mocksConfig.addMock(GroupBillingService.class);
        }

        @Primary
        @Bean
        public GroupBalanceService groupBalanceService() {
            return mocksConfig.addMock(GroupBalanceService.class);
        }

        @Primary
        @Bean
        public BalanceService balanceService() {
            return mocksConfig.addMock(BalanceService.class);
        }
    }
}
