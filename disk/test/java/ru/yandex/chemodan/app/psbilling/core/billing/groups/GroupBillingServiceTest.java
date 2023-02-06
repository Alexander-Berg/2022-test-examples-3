package ru.yandex.chemodan.app.psbilling.core.billing.groups;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectResult;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractGroupServicesTest;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.exceptions.FewMoneyForPaymentException;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.payment.PaymentChecker;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupServiceTransaction;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.chemodan.balanceclient.exception.BalanceErrorCodeException;
import ru.yandex.chemodan.balanceclient.model.request.PayRequestRequest;
import ru.yandex.chemodan.balanceclient.model.response.CheckBindingResponse;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

public class GroupBillingServiceTest extends AbstractGroupServicesTest {
    @Autowired
    private BalanceClientStub balanceClientStub;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private GroupBillingService groupBillingService;
    @Autowired
    private GroupDao groupDao;
    @Autowired
    private GroupTrustPaymentRequestDao groupTrustPaymentRequestDao;
    @Autowired
    private AmazonS3 mdsAmazonS3;
    @Autowired
    @Qualifier("balanceRestTemplate")
    private RestTemplate balanceRestTemplate;
    @Autowired
    private CardDao cardDao;
    @Autowired
    private PaymentFactory paymentFactory;
    @Autowired
    private PaymentChecker paymentChecker;

    @Test
    public void testActualGroupBillingStatus_onlyActiveContract() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 2L, true, 0, 500, Instant.now());

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_onlyActiveContractWithActDay() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 2L, true, 0, 400, Instant.now().plus(Duration.standardDays(10)));

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_onlyActiveContractWithActDayAndActSum() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 2L, true, 100, 500, Instant.now().plus(Duration.standardDays(10)));

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_withInactive() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 1L, false, 100, 100, Instant.now().plus(Duration.standardDays(50)));
        createBalanceContract(group, 2L, true, 100, 500, Instant.now().plus(Duration.standardDays(10)));

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_withSomeInactive() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 0L, false, 100, 100, Instant.now().plus(Duration.standardDays(50)));
        createBalanceContract(group, 1L, false, 100, 100, Instant.now().plus(Duration.standardDays(50)));
        createBalanceContract(group, 2L, true, 100, 500, Instant.now().plus(Duration.standardDays(10)));

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_withEmptyActiveAndSomeInactive() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 0L, false, 100, 100, Instant.now().plus(Duration.standardDays(5)));
        createBalanceContract(group, 1L, false, 100, 100, Instant.now().plus(Duration.standardDays(60)));
        createBalanceContract(group, 2L, true, 0, 0, null);

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_withActiveOnlyIncomeAndSomeInactive() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 0L, false, 100, 100, Instant.now().plus(Duration.standardDays(5)));
        createBalanceContract(group, 1L, false, 100, 100, Instant.now().plus(Duration.standardDays(10)));
        createBalanceContract(group, 2L, true, 0, 400, null);

        assertBillingStatus(group, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_onlyInactive() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        createBalanceContract(group, 0L, false, 100, 100, Instant.now().plus(Duration.standardDays(5)));
        createBalanceContract(group, 1L, false, 100, 100, Instant.now().plus(Duration.standardDays(60)));

        assertBillingStatus(group);
        assertBalanceZero(group, BigDecimal.ZERO);
    }

    @Test
    public void testActualGroupBillingStatus_someActive() {
        DateUtils.freezeTime();

        Group group = initGroupWithTransaction();

        //order is importent
        createBalanceContract(group, 1L, true, 100, 100, Instant.now().plus(Duration.standardDays(60)));
        createBalanceContract(group, 2L, true, 100, 500, Instant.now().plus(Duration.standardDays(10)));

        assertBillingStatus(group, 1L, 2L);
        assertBalanceZero(group, BigDecimal.ZERO);
    }


    @NotNull
    private Group initGroupWithTransaction() {
        PassportUid uid = PassportUid.cons(1234L);
        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(1L, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);

        // sum - 500
        groupServiceTransactionsDao.batchInsert(Cf.list(
                createGroupServiceTransaction(groupService, Instant.now().plus(Duration.standardDays(10)), 100),
                createGroupServiceTransaction(groupService, Instant.now().plus(Duration.standardDays(20)), 100),
                createGroupServiceTransaction(groupService, Instant.now().plus(Duration.standardDays(30)), 100),
                createGroupServiceTransaction(groupService, Instant.now().plus(Duration.standardDays(40)), 100),
                createGroupServiceTransaction(groupService, Instant.now().plus(Duration.standardDays(50)), 100)
        ));
        return group;
    }

    public void assertBillingStatus(Group group, Long... contractId) {
        GroupBillingStatus groupBillingStatus = groupBillingService.actualGroupBillingStatus(group);

        // check status
        // в статусе участвуют только активные контракты
        Assert.sizeIs(contractId.length, groupBillingStatus.getContracts());
        Assert.assertContainsAll(groupBillingStatus.getContracts().keys(), Cf.wrap(contractId));
    }

    public void assertBalanceZero(Group group, BigDecimal balanceExpected) {
        //check balance
        BigDecimal balanceAmount = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                        Currency.getInstance("RUB"))
                .get()
                .getBalanceAmount();

        // в балансе участвуют все контракты
        Assert.equals(balanceExpected, balanceAmount);
    }

    @Test
    public void testSuccessfulTrustFormCreation() {
        PassportUid uid = PassportUid.cons(1234L);
        Group group = createGroupWithOwner(uid, false);
        createBalanceWithDebt(group, 12345L);

        String trustFormUrl = groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "", Option.of(
                "dark"), Option.of("Тема во"), Option.empty(), Option.empty(), Option.empty()).getUrl();
        Assert.assertNotNull(trustFormUrl);
    }

    @Test
    public void testSuccessfulTrustFormCreationWithAmount__withDebt() {
        PassportUid uid = PassportUid.cons(1234L);
        Group group = createGroupWithOwner(uid, false);
        createBalanceWithDebt(group, 12345L);

        String trustFormUrl = groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "", Option.of(
                "dark"), Option.of("Тема во"), Option.empty(),
                Option.of(new Money(BigDecimal.valueOf(1000), "RUB")), Option.empty()).getUrl();
        Assert.assertNotNull(trustFormUrl);
    }

    @Test
    public void testSuccessfulTrustFormCreationWithAmount__withDebtFewMoney() {
        PassportUid uid = PassportUid.cons(1234L);
        Group group = createGroupWithOwner(uid, false);
        createBalanceContract(group, 12345L, 5000, 2000);

        Assert.assertThrows(() -> groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "",
                        Option.of("dark"), Option.of("Тема во"), Option.empty(),
                        Option.of(new Money(BigDecimal.valueOf(1000), "RUB")), Option.empty()),
                FewMoneyForPaymentException.class);
    }

    @Test
    public void testSuccessfulTrustFormCreationWithAmount__withoutDebt() {
        PassportUid uid = PassportUid.cons(1234L);
        Group group = createGroupWithOwner(uid, false);
        createBalanceWithoutDebt(group, 12345L);

        String trustFormUrl = groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "", Option.of(
                "dark"), Option.of("Тема во"), Option.empty(),
                Option.of(new Money(BigDecimal.valueOf(1000), "RUB")), Option.empty()).getUrl();
        Assert.assertNotNull(trustFormUrl);
    }

    @Test
    public void testSuccessfulTrustFormCreationWithAmount__otherCurrency() {
        PassportUid uid = PassportUid.cons(1234L);
        Group group = createGroupWithOwner(uid, false);
        createBalanceWithoutDebt(group, 12345L);

        Assert.assertThrows(
                () -> groupBillingService.getTrustPaymentFormUrl(
                        group.getOwnerUid(), group, "", Option.of("dark"), Option.of("Тема во"), Option.empty(),
                        Option.of(new Money(BigDecimal.valueOf(1000), "USD")), Option.empty()
                ),
                IllegalStateException.class
        );
    }

    @Test
    public void testSuccessfulTrustPayment() {
        PassportUid uid = PassportUid.cons(4367L);
        mockCheckBinding(UUID.randomUUID().toString());
        Group group = createGroupWithOwner(uid, false);

        checkSuccessPaymentRequest(group);

        group = groupDao.findById(group.getId());
        Assert.equals(GroupStatus.ACTIVE, group.getStatus());
    }

    @Test
    public void testSuccessfulTrustPaymentSomePayments() {
        PassportUid uid = PassportUid.cons(4367L);
        mockCheckBinding(UUID.randomUUID().toString());
        Group group = createGroupWithOwner(uid, false);

        //add some group with billing info
        createGroupWithOwner(PassportUid.cons(uid.getUid() + 1), false);

        checkSuccessPaymentRequest(group);

        group = groupDao.findById(group.getId());
        Assert.equals(GroupStatus.ACTIVE, group.getStatus());
    }

    @Test
    public void testSuccessfulTrustPayment_enableAutoBilling() {
        PassportUid uid = PassportUid.cons(4367L);
        String cardExternalId = UUID.randomUUID().toString();

        mockCheckBinding(cardExternalId);

        Group group = createGroupWithOwner(uid, false);
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        checkSuccessPaymentRequest(group);

        // автосписание было выключено - стало включенным,
        // карту добавили и сделали активной и основной
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        ListF<CardEntity> cards = cardDao.findCardsByUid(uid);
        Assert.equals(cards.size(), 1);
        Assert.equals(cards.first().getExternalId(), cardExternalId);
        Assert.equals(cards.first().getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.equals(cards.first().getStatus(), CardStatus.ACTIVE);
    }

    @Test
    public void testSuccessfulTrustPayment_enableAutoBillingExistingCard() {
        PassportUid uid = PassportUid.cons(4367L);
        String cardExternalId = UUID.randomUUID().toString();

        CardEntity card = paymentFactory.insertCard(uid, CardPurpose.B2B,
                x -> x.externalId(cardExternalId).status(CardStatus.DISABLED));

        CardEntity card2 = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.externalId(UUID.randomUUID().toString()).status(CardStatus.ACTIVE));

        CardEntity card3 = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.externalId(UUID.randomUUID().toString()).status(CardStatus.DISABLED));

        mockCheckBinding(cardExternalId);

        Group group = createGroupWithOwner(uid, false);
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        checkSuccessPaymentRequest(group);

        // автосписание было выключено - стало включенным,
        // карта, которой платили, стала активной и основной.
        // предыдущая активная основная карта стала b2b, дизейбленная осталась основной
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        card = cardDao.findById(card.getId());
        Assert.equals(card.getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.equals(card.getStatus(), CardStatus.ACTIVE);

        card2 = cardDao.findById(card2.getId());
        Assert.equals(card2.getPurpose(), CardPurpose.B2B);
        Assert.equals(card2.getStatus(), CardStatus.ACTIVE);

        card3 = cardDao.findById(card3.getId());
        Assert.equals(card3.getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.equals(card3.getStatus(), CardStatus.DISABLED);
    }

    @Test
    public void testSuccessfulTrustPayment_enableAutoBillingAlreadyEnabled() {
        PassportUid uid = PassportUid.cons(4367L);
        String cardExternalId = UUID.randomUUID().toString();
        String primaryCardExternalId = UUID.randomUUID().toString();

        CardEntity card = paymentFactory.insertCard(uid, CardPurpose.B2B,
                x -> x.externalId(cardExternalId).status(CardStatus.DISABLED));
        CardEntity primaryCard = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.externalId(primaryCardExternalId).status(CardStatus.ACTIVE));

        mockCheckBinding(cardExternalId);

        Group group = createGroupWithOwner(uid, true);
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        checkSuccessPaymentRequest(group);

        // автосписание было включено - осталось включенным,
        // основная карта осталась прежней,
        // карта, которой платили, стала активной
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        card = cardDao.findById(card.getId());
        Assert.equals(card.getPurpose(), CardPurpose.B2B);
        Assert.equals(card.getStatus(), CardStatus.ACTIVE);

        primaryCard = cardDao.findById(primaryCard.getId());
        Assert.equals(primaryCard.getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.equals(primaryCard.getStatus(), CardStatus.ACTIVE);
    }

    @Test
    public void testSuccessfulTrustPayment_enableAutoBillingAlreadyEnabled_disabledPrimaryCard() {
        PassportUid uid = PassportUid.cons(4367L);
        String disabledPrimaryCardExternalId = UUID.randomUUID().toString();
        String primaryCardExternalId = UUID.randomUUID().toString();

        CardEntity disabledPrimaryCard = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.externalId(disabledPrimaryCardExternalId).status(CardStatus.DISABLED));
        CardEntity primaryCard = paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY,
                x -> x.externalId(primaryCardExternalId).status(CardStatus.ACTIVE));

        mockCheckBinding(disabledPrimaryCardExternalId);

        Group group = createGroupWithOwner(uid, true);
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        checkSuccessPaymentRequest(group);

        // автосписание было включено - осталось включенным,
        // основная карта осталась прежней,
        // карта, которой платили, стала активной и не главной
        group = groupDao.findById(group.getId());
        Assert.assertTrue(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        CardEntity card = cardDao.findById(disabledPrimaryCard.getId());
        Assert.equals(card.getPurpose(), CardPurpose.B2B);
        Assert.equals(card.getStatus(), CardStatus.ACTIVE);

        primaryCard = cardDao.findById(primaryCard.getId());
        Assert.equals(primaryCard.getPurpose(), CardPurpose.B2B_PRIMARY);
        Assert.equals(primaryCard.getStatus(), CardStatus.ACTIVE);
    }

    @Test
    public void testSuccessfulTrustPayment_balanceReturnedError() {
        PassportUid uid = PassportUid.cons(4367L);

        mockCheckBindingFailure();

        Group group = createGroupWithOwner(uid, false);
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        checkSuccessPaymentRequest(group);

        // автосписание было выключено - осталось выключенным,
        // карту не сохранили
        group = groupDao.findById(group.getId());
        Assert.assertFalse(group.getPaymentInfo().get().isB2bAutoBillingEnabled());

        ListF<CardEntity> cards = cardDao.findCardsByUid(uid);
        Assert.equals(cards.size(), 0);
    }

    @Test
    public void testCancelledTrustPayment() {
        PassportUid uid = PassportUid.cons(2985L);
        Long contractId = 9475L;
        Group group = createGroupWithOwner(uid, false);
        createBalanceWithDebt(group, contractId);
        String requestId = groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "", Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty()).getPaymentRequestId();
        addErrorPaymentStatusForRequest(requestId);
        paymentChecker.checkPaymentRequestStatus(requestId);
        GroupTrustPaymentRequest paymentRequest = groupTrustPaymentRequestDao.findByRequestId(requestId).get();
        Assert.equals(PaymentRequestStatus.CANCELLED, paymentRequest.getStatus());
        group = groupDao.findById(group.getId());
        Assert.equals(GroupStatus.PAYMENT_REQUIRED, group.getStatus());
    }

    @Test
    public void checkFailedOrderCauseOfBalance() {
        balanceClientStub.turnOnMockitoForMethod("payRequest");
        ArgumentCaptor<PayRequestRequest> argumentCaptor = ArgumentCaptor.forClass(PayRequestRequest.class);
        Mockito.when(balanceClientStub.getBalanceClientMock().payRequest(Mockito.any())).thenThrow(RuntimeException.class);

        PassportUid uid = PassportUid.cons(4367L);
        Long contractId = 3875L;
        final Group group = createGroupWithOwner(uid, false);
        createBalanceWithDebt(group, contractId);
        Assert.assertThrows(() -> groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "",
                Option.empty(), Option.of("cool_title"), Option.empty(), Option.empty(),
                Option.empty()), RuntimeException.class);
        Mockito.verify(balanceClientStub.getBalanceClientMock(), Mockito.times(1))
                .payRequest(argumentCaptor.capture());

        String requestId = argumentCaptor.getValue().getRequestId();
        Assert.assertFalse(groupTrustPaymentRequestDao.findByRequestId(requestId).isPresent());
        paymentChecker.checkPaymentRequestStatus(requestId, "");
    }

    @Test
    public void createInvoices__contractWithDebt() {
        Group group = psBillingGroupsFactory.createGroup();
        createBalanceWithDebt(group, 567L);

        ListF<String> result = groupBillingService.createInvoices(group, Option.empty());
        Assert.sizeIs(1, result);
        Assert.equals("https://s3.yandex.net/psbilling/fund123?key=12133213", result.get(0));

        result = groupBillingService.createInvoices(group, Option.of(new Money(BigDecimal.valueOf(100), "RUB")));
        Assert.sizeIs(1, result);
        Assert.equals("https://s3.yandex.net/psbilling/fund123?key=12133213", result.get(0));
    }

    @Test
    public void createInvoices__contractWithoutDebt() {
        Group group = psBillingGroupsFactory.createGroup();
        createBalanceWithoutDebt(group, 567L);

        ListF<String> result = groupBillingService.createInvoices(group, Option.empty());
        Assert.isEmpty(result);

        result = groupBillingService.createInvoices(group, Option.of(new Money(BigDecimal.valueOf(100), "RUB")));
        Assert.sizeIs(1, result);
        Assert.equals("https://s3.yandex.net/psbilling/fund123?key=12133213", result.get(0));
    }

    @Test
    public void createInvoices__fewAmount() {
        Group group = psBillingGroupsFactory.createGroup();
        createBalanceContract(group, 567L, 600, 200);

        Assert.assertThrows(
                () -> groupBillingService.createInvoices(group, Option.of(new Money(BigDecimal.valueOf(100), "RUB"))),
                FewMoneyForPaymentException.class
        );
    }

    @Test
    public void createInvoices__otherCurrency() {
        Group group = psBillingGroupsFactory.createGroup();
        createBalanceWithoutDebt(group, 567L);

        ListF<String> result = groupBillingService.createInvoices(group, Option.of(new Money(BigDecimal.valueOf(100),
                "USD")));
        Assert.isEmpty(result);
    }

    @Test
    public void updateUidGroupsBillingStatus__clientNotFound() {
        PassportUid uid = PassportUid.cons(123);

        balanceClientStub.turnOnMockitoForMethod("getClientContracts");
        Mockito.when(balanceClientStub.getBalanceClientMock().getClientContracts(Mockito.any()))
                .thenThrow(new BalanceErrorCodeException(Cf.list("1003", "CLIENT_NOT_FOUND"),
                        "Client with ID 1349926934 not found in DB", ""));

        Group group = psBillingGroupsFactory.createGroup(
                x -> x.paymentInfo(new BalancePaymentInfo(1L, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);

        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = groupServiceDao.findById(groupService.getId());
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    private void createBalanceWithDebt(Group group, Long contractId) {
        createBalanceContract(group, contractId, 1, 1);
    }

    private void createBalanceWithoutDebt(Group group, Long contractId) {
        createBalanceContract(group, contractId, 0, 0);
    }

    private void createBalanceContract(Group group, Long contractId, int firstDebtAmount, int actSumAmount) {
        createBalanceContract(group, contractId, true, firstDebtAmount, actSumAmount, 0, Instant.now());
    }

    private void createBalanceContract(Group group, Long contractId, boolean contractActive,
                                       int actSumAmount, int clientPaymentsSum, Instant lastActDT) {
        createBalanceContract(group, contractId, contractActive, 0, actSumAmount, clientPaymentsSum, lastActDT);

    }

    private void createBalanceContract(Group group, Long contractId, boolean contractActive, int firstDebtAmount,
                                       int actSumAmount, int clientPaymentsSum, Instant lastActDT) {
        Long clientId = group.getPaymentInfo().get().getClientId();
        GetClientContractsResponseItem contract = new GetClientContractsResponseItem();
        contract.setId(contractId);
        contract.setIsActive(contractActive ? 1 : 0);
        contract.setPersonId(BalanceClientStub.DEFAULT_CONTRACT_PERSON_ID);
        contract.setServices(Cf.set(balanceService.getServiceId()));
        balanceClientStub.createOrReplaceClientContract(clientId, contract);
        GetPartnerBalanceContractResponseItem contractBalance = new GetPartnerBalanceContractResponseItem();
        contractBalance.setContractId(contractId);
        contractBalance.setCurrencyCode("RUB");
        contractBalance.setFirstDebtAmount(BigDecimal.valueOf(firstDebtAmount));
        contractBalance.setExpiredDebtAmount(BigDecimal.ZERO);
        contractBalance.setActSum(BigDecimal.valueOf(actSumAmount));
        contractBalance.setFirstDebtPaymentTermDT(DateUtils.pastDate());
        contractBalance.setClientPaymentsSum(BigDecimal.valueOf(clientPaymentsSum));
        contractBalance.setLastActDT(lastActDT);
        balanceClientStub.addContractBalance(contractBalance);
        groupDao.updateStatus(group.getId(), GroupStatus.PAYMENT_REQUIRED);
    }

    private GetPartnerBalanceContractResponseItem createNonDebtBalanceContract(Long contractId) {
        GetPartnerBalanceContractResponseItem contractBalance = new GetPartnerBalanceContractResponseItem();
        contractBalance.setContractId(contractId);
        contractBalance.setCurrencyCode("RUB");
        contractBalance.setFirstDebtAmount(BigDecimal.ZERO);
        contractBalance.setExpiredDebtAmount(BigDecimal.ZERO);
        contractBalance.setActSum(BigDecimal.ZERO);
        contractBalance.setClientPaymentsSum(BigDecimal.ZERO);
        contractBalance.setFirstDebtPaymentTermDT(null);
        return contractBalance;
    }

    private void addSuccessfulPaymentStatusForRequest(String requestId) {
        CheckRequestPaymentResponse paymentResponse = new CheckRequestPaymentResponse();
        paymentResponse.setRequestId(requestId);
        paymentResponse.setResponseCode("success");
        balanceClientStub.addPaymentRequestStatusCheck(paymentResponse);
    }

    private void addErrorPaymentStatusForRequest(String requestId) {
        CheckRequestPaymentResponse paymentResponse = new CheckRequestPaymentResponse();
        paymentResponse.setRequestId(requestId);
        paymentResponse.setResponseCode("error");
        balanceClientStub.addPaymentRequestStatusCheck(paymentResponse);
    }

    private void mockCheckBinding(String cardExternalId) {
        balanceClientStub.turnOnMockitoForMethod("checkBinding");
        CheckBindingResponse mockResponse = new CheckBindingResponse().
                withBindingResult("success").withPaymentMethodId(cardExternalId);
        Mockito.when(balanceClientStub.getBalanceClientMock().checkBinding(Mockito.any())).
                thenReturn(mockResponse);
    }

    private void mockCheckBindingFailure() {
        balanceClientStub.turnOnMockitoForMethod("checkBinding");
        CheckBindingResponse mockResponse = new CheckBindingResponse().
                withBindingResult("error");
        Mockito.when(balanceClientStub.getBalanceClientMock().checkBinding(Mockito.any())).
                thenReturn(mockResponse);
    }

    private void checkSuccessPaymentRequest(Group group) {
        Long contractId = 3875L;

        createBalanceWithDebt(group, contractId);
        String requestId = groupBillingService.getTrustPaymentFormUrl(group.getOwnerUid(), group, "", Option.empty(),
                Option.of("cool_title"), Option.empty(), Option.empty(), Option.empty()
        ).getPaymentRequestId();
        balanceClientStub.addContractBalance(createNonDebtBalanceContract(contractId));

        addSuccessfulPaymentStatusForRequest(requestId);
        paymentChecker.checkPaymentRequestStatus(requestId);
        GroupTrustPaymentRequest paymentRequest = groupTrustPaymentRequestDao.findByRequestId(requestId).get();
        Assert.equals(PaymentRequestStatus.SUCCESS, paymentRequest.getStatus());
    }

    private Group createGroupWithOwner(PassportUid uid, boolean isB2bAutoBillingEnabled) {
        return psBillingGroupsFactory.createGroup(insertDataBuilder -> insertDataBuilder
                .paymentInfo(new BalancePaymentInfo(85721L, uid, isB2bAutoBillingEnabled))
                .ownerUid(uid));
    }

    private GroupServiceTransaction createGroupServiceTransaction(GroupService groupService, Instant billingDate, int value) {
        return GroupServiceTransaction.builder()
                .billingDate(billingDate.toDateTime().toLocalDate())
                .groupServiceId(groupService.getId())
                .currency(Currency.getInstance("RUB"))
                .amount(BigDecimal.valueOf(value))
                .userSecondsCount(BigDecimal.ONE)
                .calculatedAt(Instant.now())
                .build();
    }

    @Before
    public void setupS3Mock() {
        Mockito.when(
                mdsAmazonS3.copyObject(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())
        ).thenReturn(new CopyObjectResult());

        Mockito.when(
                mdsAmazonS3.generatePresignedUrl(Mockito.any())
        ).thenReturn(UrlUtils.url("https://s3.yandex.net/psbilling/fund123?key=12133213"));
    }

    @Before
    public void setupBalanceRestTemplate() {
        ResponseEntity<String> response = new ResponseEntity<>("{\"mds_link\":\"https://s3.yandex" +
                ".net/billing/fund123\"}", HttpStatus.OK);
        Mockito.when(
                balanceRestTemplate.<String>getForEntity(Mockito.any(), Mockito.any())
        ).thenReturn(response);
    }
}
