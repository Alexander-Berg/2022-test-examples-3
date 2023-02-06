package ru.yandex.chemodan.app.psbilling.web.actions.groups;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupPartnerDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialUsageDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductOwnerDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServicePriceOverride;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialUsage;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupServiceTransaction;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.B2BAdminAbandonedPaymentTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.chemodan.app.psbilling.web.BaseWebTest;
import ru.yandex.chemodan.app.psbilling.web.exceptions.AccessDeniedException;
import ru.yandex.chemodan.app.psbilling.web.model.AddressPojo;
import ru.yandex.chemodan.app.psbilling.web.model.GroupTypeApi;
import ru.yandex.chemodan.app.psbilling.web.model.PayloadPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PhPaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.model.UrPaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.utils.PaymentDataUtils;
import ru.yandex.chemodan.balanceclient.model.request.CreatePersonRequest;
import ru.yandex.chemodan.balanceclient.model.response.CreateOfferResponse;
import ru.yandex.chemodan.balanceclient.model.response.FindClientResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetClientPersonsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationByIdResponse;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;
import ru.yandex.chemodan.directory.client.DirectoryUsersInfoResponse;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.eq;

public class GroupBillingActionsTest extends BaseWebTest {
    @Autowired
    private GroupBillingActions actions;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private ProductOwnerDao productOwnerDao;
    @Autowired
    private BalanceClientStub balanceClientStub;
    @Autowired
    private GroupServiceTransactionsDao groupServiceTransactionsDao;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private DirectoryClient directoryClient;
    @Autowired
    private TrialUsageDao trialUsageDao;
    @Autowired
    private GroupServicePriceOverrideDao groupServicePriceOverrideDao;
    @Autowired
    private GroupProductDao groupProductDao;
    @Autowired
    private EmailHelper emailHelper;
    @Autowired
    private GroupPartnerDao groupPartnerDao;

    final private long personId = 2L;
    final private long clientId = 1L;
    final private long contractId = 3L;
    final private PassportUid uid = PassportUid.cons(123);
    final private Option<PassportUid> uidO = Option.of(uid);

    @Test
    public void setUrPaymentData() {
        UrPaymentDataPojo paymentDataPojo = new UrPaymentDataPojo(
                "disk-devnull@yandex-team.ru",
                "+79999999999",
                "225",
                "My super organization",
                "1234567890",
                "7776665554432",
                new AddressPojo("119021", "ул. Льва Толстого, 16"),
                new AddressPojo("119021", "ул. Льва Толстого, 16"),
                false);
        mockGetClientPerson(paymentDataPojo);
        checkSetPaymentData(paymentDataPojo);
    }

    @Test
    public void setPhPaymentData() {
        PhPaymentDataPojo paymentDataPojo = new PhPaymentDataPojo("disk-devnull@yandex-team.ru", "+79999999999",
                "225", "Иванов", "Иван", "Иванович", false);
        mockGetClientPerson(paymentDataPojo);
        checkSetPaymentData(paymentDataPojo);
    }

    private void checkSetPaymentData(PaymentDataPojo paymentDataPojo) {
        productOwnerDao.create("yandex_mail", Option.empty());
        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, contractId, Currency.getInstance("RUB"),
                        x -> x.withPersonId(personId)), x -> x);

        String groupExternalId = "111";
        actions.setPaymentData(PassportUidOrZero.fromUid(123), groupExternalId, Option.empty(), "yandex_mail",
                Option.empty(), Option.of(paymentDataPojo));
        Option<Group> createdGroup = groupDao.findGroup(GroupType.ORGANIZATION, groupExternalId);
        Assert.assertTrue(createdGroup.isPresent());
        Assert.assertTrue(createdGroup.get().getPaymentInfo().isPresent());
        AssertHelper.assertEquals(createdGroup.get().getPaymentInfo().get().getClientType().value(), paymentDataPojo.getType());

        ListF<ClientBalanceEntity> balances = clientBalanceDao.findByGroup(createdGroup.get());
        AssertHelper.assertSize(balances, 1);
        ClientBalanceEntity balance = balances.single();
        AssertHelper.assertEquals(balance.getBalanceAmount(), BigDecimal.ZERO);
    }

    @Test
    public void buy_balanceClientCreated() {
        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, contractId, Currency.getInstance("RUB"),
                        x -> x.withPersonId(personId)), x -> x);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.POSTPAID));

        GroupService groupService = buy(group, groupProduct);

        ListF<ClientBalanceEntity> balances = clientBalanceDao.findByGroupService(groupService);
        AssertHelper.assertSize(balances, 1);
        ClientBalanceEntity balance = balances.single();
        AssertHelper.assertEquals(balance.getBalanceAmount(), BigDecimal.ZERO);
    }

    @Test
    public void unsubscribe__checkTransactions() {
        LocalDate date = new LocalDate(2021, 11, 20);
        DateUtils.freezeTime(date);
        PassportUid uid = PassportUid.cons(12345);

        psBillingTransactionsFactory.setupMocks();
        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, 111, Currency.getInstance("RUB")),
                x -> x.withClientPaymentsSum(BigDecimal.valueOf(300))
        );

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.pricePerUserInMonth(BigDecimal.valueOf(300))
        );

        GroupService groupService = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()), 2);
        DateUtils.shiftTime(Duration.standardHours(12));

        bazingaTaskManagerStub.clearTasks();

        actions.unsubscribe(uid.toUidOrZero(), group.getExternalId(), groupService.getId());
        bazingaTaskManagerStub.executeTasks();

        ListF<GroupServiceTransaction> transactions = groupServiceTransactionsDao.findGroupTransactions(date,
                group.getId());
        Assert.sizeIs(1, transactions);
        Assert.equals(0, BigDecimal.valueOf(10).compareTo(transactions.first().getAmount()));

        ClientBalanceEntity clientBalance = clientBalanceDao.find(clientId, Currency.getInstance("RUB")).get();
        Assert.equals(0, BigDecimal.valueOf(290).compareTo(clientBalance.getBalanceAmount()));
    }

    @Test
    public void testUnsubscribeForPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x -> x
                .paymentType(GroupPaymentType.PREPAID));
        GroupService groupService = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()), 2);

        Assert.assertThrows(() -> actions.unsubscribe(PassportUidOrZero.fromUid(uid),
                        group.getExternalId(), groupService.getId()),
                A3ExceptionWithStatus.class);
    }

    @Test
    public void autoBillingEnabled_buyRegularProduct() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID));
        psBillingBalanceFactory.createBalance(clientId, groupProduct.getPriceCurrency(), 100);
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY);

        buy(group, groupProduct);

        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void autoBillingDisabled_noCard() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x
                        .paymentType(GroupPaymentType.PREPAID));
        psBillingBalanceFactory.createBalance(clientId, groupProduct.getPriceCurrency(), 100);

        buy(group, groupProduct);

        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void autoBillingDisabled_gotPostpaidServices() {
        Group group1 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x -> x
                .paymentType(GroupPaymentType.PREPAID));
        psBillingGroupsFactory.createGroupService(group1, postpaidProduct);
        psBillingBalanceFactory.createBalance(clientId, groupProduct.getPriceCurrency(), 100);
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY);

        buy(group2, groupProduct);

        group2 = groupDao.findById(group2.getId());
        Assert.assertFalse(group2.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void autoBillingDisabled_buyTrialProduct_success() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .duration(Option.of(15))
                .durationMeasurement(Option.of(CustomPeriodUnit.ONE_DAY)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x -> x
                .paymentType(GroupPaymentType.PREPAID)
                .trialDefinitionId(Option.of(trialDefinition.getId())));
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY);

        buy(group, groupProduct);

        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void testB2BAdminAbandonedPaymentTask() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, contractId, Currency.getInstance("RUB"),
                        x -> x.withPersonId(personId)), x -> x);
        balanceClientStub.addContractBalance(new GetPartnerBalanceContractResponseItem()
                .withContractId(contractId)
                .withCurrencyCode("RUB")
                .withActSum(new BigDecimal(68))
                .withClientPaymentsSum(new BigDecimal(43)));
        psBillingGroupsFactory.createCommonEmailFeatures(group);
        featureFlags.getB2bAdminAbandonedPaymentEmailEnabled().setValue("true");
        String groupProductCode = groupProductDao.findAll().get(0).getCode();

        actions.getTrustPaymentFormV2(GroupTypeApi.ORGANIZATION,
                group.getExternalId(), PassportUidOrZero.fromUid(uid), "redirect-url", Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.of(groupProductCode), new PayloadPojo());

        // Check email
        String emailKey = "b2b_admin_abandoned_payment";
        ListF<String> args = Cf.list("uid", "disk_space_product", "tariff_name", "mail_archive_text_product",
                "fan_mail_limit_product");
        emailHelper.checkSingleLocalizedEmailArgs(B2BAdminAbandonedPaymentTask.class, emailKey, args);
    }

    @Before
    public void setupFeatureFlags() {
        featureFlags.getHotFixGroupValidationEnabled().resetValue();
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
    public void setupBalanceClient() {
        balanceClientStub.turnOnMockitoForMethod("findClient");
        balanceClientStub.turnOnMockitoForMethod("createPerson");
        balanceClientStub.turnOnMockitoForMethod("createOffer");
        balanceClientStub.turnOnMockitoForMethod("getClientPersons");

        Mockito.when(balanceClientStub.getBalanceClientMock().findClient(Mockito.any())).thenReturn(Cf.list(new FindClientResponseItem().withClientId(clientId)));
        Mockito.when(balanceClientStub.getBalanceClientMock().createPerson(Mockito.any())).thenReturn(personId);
        Mockito.when(balanceClientStub.getBalanceClientMock().createOffer(Mockito.any())).thenReturn(new CreateOfferResponse().withId(3L));

        UrPaymentDataPojo urPaymentDataPojo = new UrPaymentDataPojo();
        urPaymentDataPojo.setCountryCode("225");
        mockGetClientPerson(urPaymentDataPojo);
    }

    private void mockGetClientPerson(PaymentDataPojo paymentDataPojo) {
        CreatePersonRequest createPersonRequest = paymentDataPojo.toCorePaymentData().toCreatePersonRequest();
        GetClientPersonsResponseItem person = new GetClientPersonsResponseItem();
        person.setId(personId);
        BeanUtils.copyProperties(createPersonRequest, person);
        Mockito.when(balanceClientStub.getBalanceClientMock().getClientPersons(Mockito.any()))
                .thenReturn(Cf.list(person));
    }

    @Test
    public void buyWithTrial() {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        TrialDefinitionEntity trial = psBillingProductsFactory.createTrialDefinitionWithPeriod();
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID)
                        .trialDefinitionId(Option.of(trial.getId())));

        buy(group, groupProduct);

        ListF<TrialUsage> trialUsages = trialUsageDao.findTrialUsages(Option.of(group.getId()), uidO,
                trial.getSingleUsageComparisonKey().get());
        AssertHelper.assertEquals(trialUsages.size(), 1);

        trialUsages = trialUsageDao.findTrialUsages(Option.empty(), uidO, trial.getSingleUsageComparisonKey().get());
        AssertHelper.assertEquals(trialUsages.size(), 1);
    }

    @Test
    public void buyWithTrial_edu() {
        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(true, false));

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        TrialDefinitionEntity trial = psBillingProductsFactory.createTrialDefinitionWithPeriod();
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.PREPAID)
                        .trialDefinitionId(Option.of(trial.getId())));

        GroupService service = buy(group, groupProduct);

        assertTrialUsageCount(Option.of(group), Option.empty(), trial, 1);
        assertTrialUsageCount(Option.empty(), uidO, trial, 0);
        assertOverridesCount(service, 1);

        DateUtils.shiftTime(trial.getDuration().get().toDurationFrom(Instant.now()).plus(1));
        groupServicesManager.disableGroupService(service);

        // same group- no more trial usage
        service = buy(group, groupProduct);
        assertOverridesCount(service, 0);

        // new trial for new group with same uid
        Group group2 = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        service = buy(group2, groupProduct);
        assertTrialUsageCount(Option.of(group), Option.empty(), trial, 1);
        assertTrialUsageCount(Option.empty(), uidO, trial, 0);
        assertOverridesCount(service, 1);
    }

    @Test
    public void testCheckPermission() {
        featureFlags.getHotFixGroupValidationEnabled().setValue(Boolean.TRUE.toString());
        final String orgId = "BAD_ID";
        final PassportUidOrZero uid = PassportUidOrZero.fromUid(1111);

        Mockito
                .when(directoryClient.getUserInfo(eq(uid.toUid()), eq(orgId)))
                .thenReturn(Option.of(new DirectoryUsersInfoResponse(uid.getUid(), false)));


        Assert.assertThrows(() -> actions.setUrPaymentData(uid, orgId, Option.empty(), "",
                        Option.empty(), Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.setPhPaymentData(uid, orgId, Option.empty(), "",
                        Option.empty(), Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.setPaymentData(uid, orgId, Option.empty(), "",
                        Option.empty(), Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.buyUr(uid, orgId, "", Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), Cf.list()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.buyPh(uid, orgId, "", Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), Cf.list()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.buy(uid, orgId, "", Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), Cf.list()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.unsubscribe(uid, orgId, null),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.getOrganizationPaymentDataV1(uid, Option.of(orgId)),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.getOrganizationPaymentDataV2(uid, Option.of(orgId)),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.getGroupInvoices(uid, GroupTypeApi.ORGANIZATION, orgId, Option.empty(),
                        Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.getGroupInvoices(uid, GroupTypeApi.ORGANIZATION, orgId, Option.empty(),
                        Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.getTrustPaymentForm(GroupTypeApi.ORGANIZATION, orgId, uid, "",
                        Option.empty(),
                        Option.empty()),
                AccessDeniedException.class);
        Assert.assertThrows(() -> actions.getTrustPaymentFormV2(GroupTypeApi.ORGANIZATION, orgId, uid, "",
                        Option.empty(),
                        Option.empty(), Option.empty(), Option.empty(), Option.empty(), null),
                AccessDeniedException.class);
    }

    @Test
    public void testGetTrustPaymentFormForPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");

        Assert.assertThrows(() -> actions.getTrustPaymentForm(GroupTypeApi.ORGANIZATION,
                        group.getExternalId(), PassportUidOrZero.fromUid(uid), "",
                        Option.empty(), Option.empty()),
                A3ExceptionWithStatus.class);

        Assert.assertThrows(() -> actions.getTrustPaymentFormV2(GroupTypeApi.ORGANIZATION,
                        group.getExternalId(), PassportUidOrZero.fromUid(uid), "",
                        Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                        Option.empty(), null),
                A3ExceptionWithStatus.class);
    }

    @Test
    public void testBuyForPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x -> x
                .paymentType(GroupPaymentType.PREPAID));
        Assert.assertThrows(() -> actions.buy(PassportUidOrZero.fromUid(uid),
                        group.getExternalId(), groupProduct.getCode(), Option.empty()),
                A3ExceptionWithStatus.class);
    }

    @Test
    public void testBuyWithPaymentDataForPartnerClient() {
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        groupPartnerDao.insertPartnerInfoIfNotExists(group.getId(), "123");
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x -> x
                .paymentType(GroupPaymentType.PREPAID));
        Assert.assertThrows(() -> actions.buy(PassportUidOrZero.fromUid(uid),
                        group.getExternalId(), groupProduct.getCode(),
                        Option.of(PaymentDataUtils.buildPaymentDataId(personId, clientId, uid)),
                        Option.empty(), Option.empty(), Option.empty(), Cf.list()),
                A3ExceptionWithStatus.class);
    }

    private GroupService buy(Group group, GroupProduct groupProduct) {
        actions.buy(PassportUidOrZero.fromUid(uid), group.getExternalId(), groupProduct.getCode(),
                Option.of(PaymentDataUtils.buildPaymentDataId(personId, clientId, uid)), Option.empty(),
                Option.empty(), Option.empty(), Cf.list());
        Option<GroupService> groupServiceO = groupServicesManager.find(group.getId(), Target.ENABLED).firstO();
        Assert.isTrue(groupServiceO.isPresent());
        return groupServiceO.get();
    }

    private void assertTrialUsageCount(Option<Group> group, Option<PassportUid> uid, TrialDefinitionEntity trial,
                                       int count) {
        ListF<TrialUsage> trialUsages = trialUsageDao.findTrialUsages(group.map(AbstractEntity::getId), uid,
                trial.getSingleUsageComparisonKey().get());
        AssertHelper.assertEquals(trialUsages.size(), count);
    }

    private void assertOverridesCount(GroupService groupService, int count) {
        MapF<UUID, ListF<GroupServicePriceOverride>> overrides =
                groupServicePriceOverrideDao.findByGroupServices(Cf.list(groupService.getId()));
        AssertHelper.assertEquals(overrides.size(), count);
    }

    @Before
    public void setup() {
        Mockito.when(directoryClient.getOrganizationPartnerId(Mockito.anyString()))
                .thenReturn(Option.empty());
    }
}
