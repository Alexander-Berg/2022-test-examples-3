package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl.balance;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.util.BatchFetchingUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;

public class ClientBalanceDaoTest extends AbstractPsBillingCoreTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientBalanceDaoTest.class);

    @Autowired
    private ClientBalanceDao dao;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private GroupProductDao groupProductDao;
    @Autowired
    private PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    private PaymentFactory paymentFactory;
    @Autowired
    private GroupTrustPaymentRequestDao groupTrustPaymentRequestDao;

    private ClientBalanceDao.InsertData otherBalanceData;
    private ClientBalanceEntity otherBalance;

    private final Long clientId = 1L;
    private final Currency rub = Currency.getInstance("RUB");
    private final Currency usd = Currency.getInstance("USD");

    @Before
    public void before() {
    }

    private void assertOtherBalanceNotChanged() {
        otherBalance = dao.findById(otherBalance.getId());
        Assert.assertNotNull(otherBalance.getId());
        Assert.assertEquals(otherBalanceData.getClientId(), otherBalance.getClientId());
        Assert.assertEquals(otherBalanceData.getBalanceAmount(), otherBalance.getBalanceAmount());
        Assert.assertEquals(otherBalanceData.getBalanceCurrency(), otherBalance.getBalanceCurrency());
        Assert.assertEquals(otherBalanceData.getBalanceVoidAt(), otherBalance.getBalanceVoidAt());
        Assert.assertEquals(otherBalanceData.getLastInvoiceAt(), otherBalance.getLastInvoiceAt());
    }

    @Test
    public void insert() {
        ClientBalanceDao.InsertData toInsert = ClientBalanceDao.InsertData.builder()
                .clientId(1L)
                .balanceAmount(BigDecimal.ONE)
                .balanceCurrency(Currency.getInstance("RUB"))
                .balanceVoidAt(Option.of(DateUtils.futureDate()))
                .lastInvoiceAt(Option.of(DateUtils.farFutureDate()))
                .build();
        ClientBalanceEntity inserted = dao.insert(toInsert);
        Assert.assertNotNull(inserted.getId());
        Assert.assertEquals(toInsert.getClientId(), inserted.getClientId());
        Assert.assertEquals(toInsert.getBalanceAmount(), inserted.getBalanceAmount());
        Assert.assertEquals(toInsert.getBalanceCurrency(), inserted.getBalanceCurrency());
        Assert.assertEquals(toInsert.getBalanceVoidAt(), inserted.getBalanceVoidAt());
        Assert.assertEquals(Instant.now(), inserted.getBalanceUpdatedAt());
        Assert.assertEquals(Instant.now(), inserted.getCreatedAt());
        Assert.assertEquals(toInsert.getLastInvoiceAt(), inserted.getLastInvoiceAt());
        Assert.assertEquals(1, dao.findAll().length());
    }

    @Test
    public void find() {
        ClientBalanceEntity balance = createBalance();
        ClientBalanceEntity foundBalance = dao.find(clientId, rub).get();
        Assert.assertEquals(balance, foundBalance);
    }

    @Test
    public void find__notFound() {
        Option<ClientBalanceEntity> foundBalance = dao.find(clientId, rub);
        Assert.assertTrue(foundBalance.isEmpty());
    }

    @Test
    public void findByGroup() {
        Group group = createGroupWithBilling(clientId, false);
        ClientBalanceEntity balanceRub = createBalance(Currency.getInstance("RUB"));
        ClientBalanceEntity balanceUsd = createBalance(Currency.getInstance("USD"));

        ListF<ClientBalanceEntity> foundBalances = dao.findByGroup(group);

        Assert.assertEquals(Cf.list(balanceRub, balanceUsd), foundBalances);
    }

    @Test
    public void findByGroupService() {
        Group group = createGroupWithBilling(1L, false);
        GroupProduct groupProduct1 =
                psBillingProductsFactory.createGroupProduct(x -> x.priceCurrency(Currency.getInstance("RUB")));
        GroupProduct groupProduct2 =
                psBillingProductsFactory.createGroupProduct(x -> x.priceCurrency(Currency.getInstance("USD")));
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct1);
        psBillingGroupsFactory.createGroupService(group, groupProduct2);

        createBalance(Currency.getInstance("RUB"));
        createBalance(Currency.getInstance("USD"));

        Option<ClientBalanceEntity> foundBalanceO = dao.findByGroupService(groupService);
        Assert.assertTrue(foundBalanceO.isPresent());
        Assert.assertEquals("RUB", foundBalanceO.get().getBalanceCurrency().getCurrencyCode());
    }

    @Test
    public void updateBalance() {
        ClientBalanceEntity balance = createBalance();
        dao.updateBalance(balance.getId(), BigDecimal.valueOf(123));

        balance = dao.findById(balance.getId());
        Assert.assertEquals(BigDecimal.valueOf(123), balance.getBalanceAmount());
        Assert.assertEquals(Instant.now(), balance.getBalanceUpdatedAt());
    }

    @Test
    public void updateInvoiceDate() {
        createOtherBalance();

        ClientBalanceEntity balance = createBalance();
        ClientBalanceEntity updatedBalance = dao.updateInvoiceDate(balance.getId(), Option.of(Instant.now()));

        Assert.assertEquals(Instant.now(), updatedBalance.getLastInvoiceAt().get());
        Assert.assertEquals(balance.getBalanceUpdatedAt(), updatedBalance.getBalanceUpdatedAt());

        updatedBalance = dao.updateInvoiceDate(balance.getId(), Option.empty());

        Assert.assertEquals(Option.empty(), updatedBalance.getLastInvoiceAt());
        assertOtherBalanceNotChanged();
    }

    private void createOtherBalance() {
        otherBalanceData = ClientBalanceDao.InsertData.builder()
                .clientId(9999L)
                .balanceAmount(BigDecimal.valueOf(9999))
                .balanceCurrency(Currency.getInstance("RUB"))
                .balanceVoidAt(Option.of(DateUtils.farPastDate()))
                .lastInvoiceAt(Option.of(DateUtils.pastDate()))
                .build();
        otherBalance = dao.insert(otherBalanceData);
    }

    @Test
    public void updateVoidAt() {
        ClientBalanceEntity balance = createBalance();
        ClientBalanceEntity updatedBalance = dao.updateVoidAt(balance.getId(), Option.of(Instant.now()));

        Assert.assertEquals(Instant.now(), updatedBalance.getBalanceVoidAt().get());
        Assert.assertEquals(balance.getBalanceUpdatedAt(), updatedBalance.getBalanceUpdatedAt());

        updatedBalance = dao.updateVoidAt(balance.getId(), Option.empty());
        Assert.assertEquals(Option.empty(), updatedBalance.getBalanceVoidAt());
    }

    @Test
    public void update() {
        createOtherBalance();
        ClientBalanceEntity balance = createBalance();
        dao.createOrUpdate(balance.getClientId(), balance.getBalanceCurrency(), BigDecimal.TEN,
                Option.of(Instant.now()));

        balance = dao.findById(balance.getId());
        Assert.assertEquals(Instant.now(), balance.getBalanceVoidAt().get());
        Assert.assertEquals(Instant.now(), balance.getBalanceUpdatedAt());
        Assert.assertEquals(BigDecimal.TEN, balance.getBalanceAmount());
        assertOtherBalanceNotChanged();
    }

    @Test
    public void findObsoleteBalanceClients() {
        Instant now = Instant.now();
        Instant balanceStaleThreshold = now.minus(Duration.standardDays(1));
        Instant invoiceStaleThreshold = now.minus(Duration.standardDays(7));

        psBillingBalanceFactory.createBalance(1L, "RUB", now.minus(Duration.standardDays(2)));
        psBillingBalanceFactory.createBalance(1L, "USD", now.minus(Duration.standardDays(2)));

        psBillingBalanceFactory.createBalance(2L, "RUB", now.minus(Duration.standardDays(2)));
        psBillingBalanceFactory.createBalance(3L, "USD", now);
        psBillingBalanceFactory.createBalance(4L, "RUB", now.minus(Duration.standardHours(12)));

        ClientBalanceEntity balance =
                psBillingBalanceFactory.createBalance(5L, "USD", now.minus(Duration.standardHours(12)));
        dao.updateInvoiceDate(balance.getId(), Option.of(now.minus(Duration.standardDays(2))));
        balance =
                psBillingBalanceFactory.createBalance(6L, "USD", now.minus(Duration.standardHours(12)));
        dao.updateInvoiceDate(balance.getId(), Option.of(now.minus(Duration.standardDays(8))));

        ListF<Long> clientsToCheck = dao.findObsoleteBalanceClients(balanceStaleThreshold, invoiceStaleThreshold,
                Option.empty(), 1000);
        Assert.assertEquals(3, clientsToCheck.length());

        Long clientId = dao.findObsoleteBalanceClients(balanceStaleThreshold, invoiceStaleThreshold, Option.empty(),
                1).get(0);
        Assert.assertEquals(1L, clientId.longValue());

        clientId = dao.findObsoleteBalanceClients(balanceStaleThreshold, invoiceStaleThreshold, Option.of(clientId),
                1).get(0);
        Assert.assertEquals(2L, clientId.longValue());

        clientId = dao.findObsoleteBalanceClients(balanceStaleThreshold, invoiceStaleThreshold, Option.of(clientId),
                1).get(0);
        Assert.assertEquals(5L, clientId.longValue());
    }

    @Test
    public void countNotActualUpdatedBefore() {
        int count = dao.countNotActualUpdatedBefore(Duration.standardHours(0));
        Assert.assertEquals(0, count);

        createBalance(Currency.getInstance("RUB"));
        DateUtils.shiftTime(Duration.standardHours(1));

        createBalance(Currency.getInstance("USD"));
        DateUtils.shiftTime(Duration.standardHours(1));

        count = dao.countNotActualUpdatedBefore(Duration.standardHours(0));
        Assert.assertEquals(2, count);

        count = dao.countNotActualUpdatedBefore(Duration.standardHours(1));
        Assert.assertEquals(1, count);

        count = dao.countNotActualUpdatedBefore(Duration.standardHours(2));
        Assert.assertEquals(0, count);
    }

    @Test
    public void prepaidDebtsCount() {
        Assert.assertEquals(0, dao.prepaidDebtsCount());

        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        GroupProduct prepaidProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        GroupProduct prepaidProductUsd =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .priceCurrency(Currency.getInstance("USD")));
        Group group1 = psBillingGroupsFactory.createGroup(1L);
        Group group2 = psBillingGroupsFactory.createGroup(2L);
        Group group3 = psBillingGroupsFactory.createGroup(3L);
        Group group4 = psBillingGroupsFactory.createGroup(4L);

        psBillingGroupsFactory.createGroupService(group1, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group1, prepaidProductUsd);

        psBillingGroupsFactory.createGroupService(group2, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group2, prepaidProductUsd);

        psBillingGroupsFactory.createGroupService(group3, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group4, postpaidProduct);

        psBillingBalanceFactory.createBalance(1, "RUB", 0);
        psBillingBalanceFactory.createBalance(1, "USD", 1);
        psBillingBalanceFactory.createBalance(2, "RUB", -0.1);
        psBillingBalanceFactory.createBalance(2, "USD", -1);
        psBillingBalanceFactory.createBalance(3, "RUB", -1);
        psBillingBalanceFactory.createBalance(4, "RUB", 1);

        Assert.assertEquals(3, dao.prepaidDebtsCount());
    }

    @Test
    public void findClientsForAutoPay() {
        GroupProduct postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        GroupProduct prepaidProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        GroupProduct prepaidProductUsd =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .priceCurrency(usd));
        GroupProduct prepaidFreeProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .skipTransactionsExport(true));
        Group group1 = createGroupWithBilling(1L, true);
        Group group2 = createGroupWithBilling(2L, true);
        Group group3 = createGroupWithBilling(3L, false);
        Group group4 = createGroupWithBilling(4L, true);
        Group group5 = createGroupWithBilling(5L, true);
        Group group61 = createGroupWithBilling(6L, true);
        Group group62 = createGroupWithBilling(6L, true);
        Group group7 = createGroupWithBilling(7L, true);
        Group group8 = createGroupWithBilling(8L, true);

        Instant threshold = Instant.now().plus(settings.getAutoPayLeadTime());
        Instant afterThreshold = Instant.now().plus(settings.getAutoPayLeadTime().plus(1));

        // expected to find. ok data for 2 currency
        psBillingGroupsFactory.createGroupService(group1, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group1, prepaidProductUsd);
        psBillingBalanceFactory.createBalance(1, rub, 0, threshold);
        psBillingBalanceFactory.createBalance(1, usd, 0, threshold);

        // not expected to find. all services are disabled
        psBillingGroupsFactory.createGroupService(group2, prepaidProductRub, Target.DISABLED);
        psBillingBalanceFactory.createBalance(2, rub, 0, threshold);

        // not expected to find. auto payment disabled
        psBillingGroupsFactory.createGroupService(group3, prepaidProductRub);
        psBillingBalanceFactory.createBalance(3, rub, 0, threshold);
        insertCard(group3, CardPurpose.B2B_PRIMARY, CardStatus.DISABLED);
        insertCard(group3, CardPurpose.B2B_PRIMARY, CardStatus.EXPIRED);
        insertCard(group3, CardPurpose.B2B, CardStatus.ACTIVE);

        // not expected to find. active postpaid service
        psBillingGroupsFactory.createGroupService(group4, postpaidProduct);
        psBillingBalanceFactory.createBalance(4, rub, 0, threshold);

        // not expected to find. all services are free
        psBillingGroupsFactory.createGroupService(group5, prepaidFreeProductRub);
        psBillingBalanceFactory.createBalance(5, rub, 0, threshold);

        // expected to find single client-currency pair. ok data. 2 groups with 1 client_id
        psBillingGroupsFactory.createGroupService(group61, prepaidProductRub);
        psBillingGroupsFactory.createGroupService(group62, prepaidProductRub);
        psBillingBalanceFactory.createBalance(6, rub, 0, threshold);

        // not expected to find. negative balance
        psBillingGroupsFactory.createGroupService(group7, prepaidProductRub);
        psBillingBalanceFactory.createBalance(7, rub, -1, threshold);

        // not expected to find. void date is too far
        psBillingGroupsFactory.createGroupService(group8, prepaidProductRub);
        psBillingBalanceFactory.createBalance(8, rub, 0, afterThreshold);

        ListF<Tuple2<Long, Currency>> clientsAndCurrency = BatchFetchingUtils.collectBatchedEntities(
                (batchSize, lastData) ->
                        dao.findClientsForAutoPay(threshold, batchSize, lastData),
                Function.identityF(), 1, logger);

        Assert.assertEquals(3, clientsAndCurrency.size());
        Assert.assertEquals(Tuple2.tuple(1L, rub), clientsAndCurrency.get(0));
        Assert.assertEquals(Tuple2.tuple(1L, usd), clientsAndCurrency.get(1));
        Assert.assertEquals(Tuple2.tuple(6L, rub), clientsAndCurrency.get(2));
    }

    @Test
    public void findClientsForAutoResurrectionPay() {
        GroupProduct postpaidProduct = psBillingProductsFactory.createPostpaidProduct(rub);
        GroupProduct prepaidProductRub = psBillingProductsFactory.createPrepaidProduct(rub);
        GroupProduct prepaidProductUsd = psBillingProductsFactory.createPrepaidProduct(usd);
        GroupProduct prepaidFreeProductRub =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .skipTransactionsExport(true));
        Group group1 = createGroupWithBilling(1L, true);
        Group group2 = createGroupWithBilling(2L, true);
        Group group3 = createGroupWithBilling(3L, false);
        Group group4 = createGroupWithBilling(4L, true);
        Group group5 = createGroupWithBilling(5L, true);
        Group group61 = createGroupWithBilling(6L, true);
        Group group62 = createGroupWithBilling(6L, true);
        Group group7 = createGroupWithBilling(7L, true);
        Group group8 = createGroupWithBilling(8L, true);
        Group group9 = createGroupWithBilling(9L, true);
        Group group10 = createGroupWithBilling(10L, true);

        Instant threshold = Instant.now().minus(settings.getAutoResurrectMaxResurrectTimeMinutes());
        Instant beforeThreshold = Instant.now().minus(settings.getAutoResurrectMaxResurrectTimeMinutes().plus(1));

        // expected to find. ok data for 2 currency
        GroupService groupService1rub = psBillingGroupsFactory.createGroupService(group1, prepaidProductRub);
        psBillingBalanceFactory.createBalance(1, rub, 0, threshold);
        disableService(groupService1rub);
        GroupService groupService1usd = psBillingGroupsFactory.createGroupService(group1, prepaidProductUsd);
        psBillingBalanceFactory.createBalance(1, usd, 0, threshold);
        disableService(groupService1usd);

        // not expected to find. service is active
        psBillingGroupsFactory.createGroupService(group2, prepaidProductRub);
        psBillingBalanceFactory.createBalance(2, rub, 0, threshold);

        // not expected to find. auto payment disabled
        GroupService groupService3rub = psBillingGroupsFactory.createGroupService(group3, prepaidProductRub);
        psBillingBalanceFactory.createBalance(3, rub, 0, threshold);
        disableService(groupService3rub);

        // not expected to find. active postpaid service
        GroupService groupService4postpaid = psBillingGroupsFactory.createGroupService(group4, postpaidProduct);
        psBillingBalanceFactory.createBalance(4, rub, 0, threshold);
        disableService(groupService4postpaid);

        // not expected to find. all services are free
        GroupService groupService5free = psBillingGroupsFactory.createGroupService(group5, prepaidFreeProductRub);
        psBillingBalanceFactory.createBalance(5, rub, 0, threshold);
        disableService(groupService5free);

        // expected to find single client-currency pair. ok data. 2 groups with 1 client_id
        GroupService groupService61 = psBillingGroupsFactory.createGroupService(group61, prepaidProductRub);
        GroupService groupService62 = psBillingGroupsFactory.createGroupService(group62, prepaidProductRub);
        psBillingBalanceFactory.createBalance(6, rub, 0, threshold);
        disableService(groupService61);
        disableService(groupService62);

        // not expected to find. positive balance
        GroupService groupService7rub = psBillingGroupsFactory.createGroupService(group7, prepaidProductRub);
        psBillingBalanceFactory.createBalance(7, rub, 1, threshold);
        disableService(groupService7rub);

        // not expected to find. void date is too far
        GroupService groupService8rub = psBillingGroupsFactory.createGroupService(group8, prepaidProductRub);
        psBillingBalanceFactory.createBalance(8, rub, 0, beforeThreshold);
        disableService(groupService8rub);

        // not expected to find. too frequent call for unknown charge error
        GroupService groupService9rub = psBillingGroupsFactory.createGroupService(group9, prepaidProductRub);
        psBillingBalanceFactory.createBalance(9, rub, 0, threshold);
        disableService(groupService9rub);
        paymentFactory.insertGroupPayment(9, uid,
                x -> x.paymentInitiationType(PaymentInitiationType.AUTO)
                        .status(PaymentRequestStatus.CANCELLED)
                        .error(Option.of("unknown")));

        // not expected to find. too frequent call for not enough funds error
        GroupService groupService10rub = psBillingGroupsFactory.createGroupService(group10, prepaidProductRub);
        psBillingBalanceFactory.createBalance(10, rub, 0, threshold);
        disableService(groupService10rub);
        paymentFactory.insertGroupPayment(10, uid,
                x -> x.paymentInitiationType(PaymentInitiationType.AUTO)
                        .status(PaymentRequestStatus.CANCELLED)
                        .error(Option.of(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS))
                        .paymentCoefficient(settings.getAutoPayChargePlan().lastO()));

        DateUtils.shiftTime(settings.getAutoResurrectMinRetryIntervalMinutes());

        ListF<Tuple2<Long, Currency>> clientsAndCurrency = getClientsAndCurrency(threshold);

        Assert.assertEquals(3, clientsAndCurrency.size());
        Assert.assertEquals(Tuple2.tuple(1L, rub), clientsAndCurrency.get(0));
        Assert.assertEquals(Tuple2.tuple(1L, usd), clientsAndCurrency.get(1));
        Assert.assertEquals(Tuple2.tuple(6L, rub), clientsAndCurrency.get(2));

        DateUtils.shiftTime(Duration.standardSeconds(1));

        clientsAndCurrency = getClientsAndCurrency(threshold);

        Assert.assertEquals(5, clientsAndCurrency.size());
        Assert.assertEquals(Tuple2.tuple(1L, rub), clientsAndCurrency.get(0));
        Assert.assertEquals(Tuple2.tuple(1L, usd), clientsAndCurrency.get(1));
        Assert.assertEquals(Tuple2.tuple(6L, rub), clientsAndCurrency.get(2));
        Assert.assertEquals(Tuple2.tuple(9L, rub), clientsAndCurrency.get(3));
        Assert.assertEquals(Tuple2.tuple(10L, rub), clientsAndCurrency.get(4));
    }

    @Test
    public void findClientsForAutoResurrectionPay_limitExceeded() {
        GroupProduct prepaidProductRub = psBillingProductsFactory.createPrepaidProduct(rub);
        Group group1 = createGroupWithBilling(1L, true);
        Group group2 = createGroupWithBilling(2L, true);

        Instant threshold = Instant.now().minus(settings.getAutoResurrectMaxResurrectTimeMinutes());

        // not expected to find. too frequent call for limit error
        GroupService groupService1rub = psBillingGroupsFactory.createGroupService(group1, prepaidProductRub);
        psBillingBalanceFactory.createBalance(1, rub, 0, threshold);
        disableService(groupService1rub);
        paymentFactory.insertGroupPayment(1, uid,
                x -> x.paymentInitiationType(PaymentInitiationType.AUTO)
                        .status(PaymentRequestStatus.CANCELLED)
                        .error(Option.of(CheckRequestPaymentResponse.LIMIT_EXCEEDED))
                        .paymentCoefficient(settings.getAutoPayChargePlan().lastO()));

        // expected to find. Only first try has happend
        GroupService groupService2rub = psBillingGroupsFactory.createGroupService(group2, prepaidProductRub);
        psBillingBalanceFactory.createBalance(2, rub, 0, threshold);
        disableService(groupService2rub);
        paymentFactory.insertGroupPayment(2, uid,
                x -> x.paymentInitiationType(PaymentInitiationType.AUTO)
                        .status(PaymentRequestStatus.CANCELLED)
                        .error(Option.of(CheckRequestPaymentResponse.LIMIT_EXCEEDED))
                        .paymentCoefficient(settings.getAutoPayChargePlan().firstO()));

        DateUtils.shiftTime(settings.getAutoResurrectMinRetryIntervalMinutes());
        ListF<Tuple2<Long, Currency>> clientsAndCurrency = getClientsAndCurrency(threshold);

        Assert.assertEquals(1, clientsAndCurrency.size());
        Assert.assertEquals(Tuple2.tuple(2L, rub), clientsAndCurrency.get(0));
    }

    private ListF<Tuple2<Long, Currency>> getClientsAndCurrency(Instant threshold) {
        return BatchFetchingUtils.collectBatchedEntities(
                (batchSize, lastData) ->
                        dao.findClientsForAutoResurrectionPay(threshold,
                                Instant.now().minus(settings.getAutoResurrectMinRetryIntervalMinutes()),
                                settings.getAutoPayChargePlan().last(), batchSize, lastData),
                Function.identityF(), 1, logger);
    }


    private ClientBalanceEntity createBalance() {
        return createBalance(rub);
    }

    private ClientBalanceEntity createBalance(Currency currency) {
        ClientBalanceDao.InsertData toInsert = ClientBalanceDao.InsertData.builder()
                .clientId(clientId)
                .balanceAmount(BigDecimal.ONE)
                .balanceCurrency(currency)
                .build();
        return dao.insert(toInsert);
    }

    private Group createGroupWithBilling(long clientId, boolean autoBilling) {
        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid,
                autoBilling)));
        paymentFactory.insertCard(group.getPaymentInfo().get().getPassportUid(), CardPurpose.B2B_PRIMARY);
        return group;
    }

    private void disableService(GroupService groupService) {
        Group group = groupDao.findById(groupService.getGroupId());
        GroupProductEntity groupProduct = groupProductDao.findById(groupService.getGroupProductId());
        ClientBalanceEntity balance = dao.find(group.getPaymentInfo().get().getClientId(),
                groupProduct.getPriceCurrency()).get();
        groupServiceDao.updateNextBillingDate(groupService.getId(), balance.getBalanceVoidAt());
        groupServiceDao.setTargetToDisabled(groupService.getId(), Target.ENABLED);
    }

    private void insertCard(Group group, CardPurpose purpose, CardStatus status) {
        paymentFactory.insertCard(group.getPaymentInfo().get().getPassportUid(), purpose, x -> x.status(status));
    }
}
