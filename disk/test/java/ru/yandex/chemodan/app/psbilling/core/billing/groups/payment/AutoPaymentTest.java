package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import java.util.Currency;

import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.AutoPayManager;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class AutoPaymentTest extends BaseAutoPayTest {
    @Autowired
    private AutoPayManager autoPayManager;

    @Test
    public void noChargeForGroupWithoutClientBalance() {
        createGroupWithoutClientBalance();

        Assert.assertThrows(() -> autoPayManager.chargeRegularAutoPayment(clientId, rub), IllegalStateException.class);
        verifyNoPayment();
    }

    @Test
    public void noChargeForGroupWithoutServices() {
        createGroupWithoutServices();

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void noChargeForGroupWithPostpaidServices() {
        createGroupWithPostpaidServices();

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void noChargeForUnknownCard() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        // No bound card in balance: balanceClientStub.addBoundCard(uid, card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
        Assert.equals(CardStatus.DISABLED, cardDao.findById(card.getId()).getStatus());
        Assert.isFalse(groupDao.findById(group.getId()).getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void noChargeForExpiredCard() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode(),
                true));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
        Assert.equals(CardStatus.EXPIRED, cardDao.findById(card.getId()).getStatus());
        Assert.isFalse(groupDao.findById(group.getId()).getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void noChargeForNoPrimaryCard() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertCard("card-x123", CardPurpose.B2B);
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);

        verifyNoPayment();
    }

    @Test
    public void noChargeForPayerIsNotAdmin() {
        setupCorrectData(clientId);

        Group groupWherePayer = autoPaymentUtils.createGroup(clientId, true);
        Group groupWhereAdmin = psBillingGroupsFactory.createGroup(x ->
                x.paymentInfo(new BalancePaymentInfo(123, PassportUid.cons(123), true)));
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(groupWhereAdmin.getExternalId()));

        autoPayManager.chargeRegularAutoPayment(clientId, rub);

        verifyNoPayment();
        groupWherePayer = groupDao.findById(groupWherePayer.getId());
        Assert.assertFalse(groupWherePayer.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void noChargeForFarVoidBalance() {
        createGroupWithFarVoidBalance();

        autoPayManager.chargeRegularAutoPayment(clientId, rub);

        verifyNoPayment();
    }

    @Test
    public void noChargeForGroupWithoutEnabledAutoPay() {
        createGroupWithoutEnabledAutoPay();

        autoPayManager.chargeRegularAutoPayment(clientId, rub);

        verifyNoPayment();
    }

    @Test
    public void successFirstCharge() {
        setupCorrectData(clientId);
        CardEntity card = cardDao.findAll().single();

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);

        ListF<GroupTrustPaymentRequest> payments = groupTrustPaymentRequestDao.findAll();
        Assert.equals(1, payments.size());
        GroupTrustPaymentRequest paymentRequest = payments.get(0);
        Assert.equals(PaymentRequestStatus.INIT, paymentRequest.getStatus());
        Assert.equals(card.getId(), paymentRequest.getCardId().get());
        Assert.equals(clientId, paymentRequest.getClientId());
        Assert.equals(String.valueOf(uid.getUid()), paymentRequest.getOperatorUid());
        Assert.equals(PaymentInitiationType.AUTO, paymentRequest.getPaymentInitiationType());
        Assert.equals(Option.of(1.0), paymentRequest.getPaymentPeriodCoefficient());
        Assert.equals(Option.empty(), paymentRequest.getGroupServicesInfo());
        Assert.equals(Option.empty(), paymentRequest.getError());
        AssertHelper.assertEquals(paymentRequest.getMoney().get().getAmount(), 100);
        AssertHelper.assertEquals(paymentRequest.getMoney().get().getCurrency(), rub);

        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

    }

    @Test
    public void failPayment() {
        setupCorrectData(clientId);
        setupErrorPayRequest();
        assertErrorPayRequest(clientId, () -> autoPayManager.chargeRegularAutoPayment(clientId, rub));
    }

    @Test
    public void successWithSimilarCurrency() {
        Currency rur = Currency.getInstance("RUR");
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123" + clientId);
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rur.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        psBillingBalanceFactory.createContract(clientId, 2L, rur);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);

        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void noChargeOnRecentSuccessAutoPayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.AUTO, PaymentRequestStatus.SUCCESS);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void chargeOnRecentSuccessUserPayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.USER, PaymentRequestStatus.SUCCESS);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
        ListF<GroupTrustPaymentRequest> payments = groupTrustPaymentRequestDao.findAll();
        Assert.equals(2, payments.size());
    }

    @Test
    public void ignoreFarSuccessPayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.AUTO, PaymentRequestStatus.SUCCESS);

        DateUtils.shiftTime(settings.getAutoPayRecentSuccessPaymentsLookupTime().plus(1));

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
        ListF<GroupTrustPaymentRequest> payments = groupTrustPaymentRequestDao.findAll();
        Assert.equals(2, payments.size());
    }

    @Test
    public void noChargeOnRecentActivePayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.AUTO, PaymentRequestStatus.INIT);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void ignoreFarActivePayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.USER, PaymentRequestStatus.INIT);

        DateUtils.shiftTime(settings.getAcceptableInitOrdersCheckDateExpirationTime().plus(1));

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
        ListF<GroupTrustPaymentRequest> payments = groupTrustPaymentRequestDao.findAll();
        Assert.equals(2, payments.size());
    }

    @Test
    public void ignoreFarCanceledPayment() {
        setupCorrectData(clientId);
        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);

        DateUtils.shiftTime(settings.getAutoPayRecentCanceledPaymentsLookupTime().plus(1));
        Mockito.reset(balanceClientStub.getBalanceClientMock());

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void notEnoughFundsSequence_noFunds() {
        notEnoughFundsSequenceBase(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);
    }

    @Test
    public void notEnoughFundsSequence_limitExceeded() {
        notEnoughFundsSequenceBase(CheckRequestPaymentResponse.LIMIT_EXCEEDED);
    }

    private void notEnoughFundsSequenceBase(String error) {
        setupCorrectData(clientId);

        ListF<Double> chargePlan = Cf.list(1.0, 0.5, 0.25);
        ListF<Double> expectedPricePlan = Cf.list(100.0, 50.0, 25.0);
        Mockito.when(settings.getAutoPayChargePlan()).thenReturn(chargePlan);

        DateUtils.unfreezeTime(); // to correct get last payment
        for (Tuple2<Double, Double> plan : settings.getAutoPayChargePlan().zip(expectedPricePlan)) {
            Double chargeCoefficient = plan._1;
            Double expectedPrice = plan._2;

            autoPayManager.chargeRegularAutoPayment(clientId, rub);
            GroupTrustPaymentRequest lastPaymentRequest =
                    checkLastPaymentRequest(error);
            Assert.equals(PaymentRequestStatus.CANCELLED, lastPaymentRequest.getStatus());
            Assert.equals(error, lastPaymentRequest.getError().get());
            Assert.equals(chargeCoefficient, lastPaymentRequest.getPaymentPeriodCoefficient().get());

            assertBalanceCalledWithAmount(expectedPrice);

            Mockito.reset(balanceClientStub.getBalanceClientMock());
        }
    }

    @Test
    public void failedPaymentNoRetry() {
        setupCorrectData(clientId);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);

        checkLastPaymentRequest("error");
        Mockito.reset(balanceClientStub.getBalanceClientMock());

        // no retry for failed payment
        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void retryAfterRetrySequence() {
        setupCorrectData(clientId);

        DateUtils.unfreezeTime(); // to correct get last payment
        for (int i = 0; i < settings.getAutoPayChargePlan().size(); i++) {
            autoPayManager.chargeRegularAutoPayment(clientId, rub);
            checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);
        }

        Mockito.reset(balanceClientStub.getBalanceClientMock());
        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void successWithDefaultProduct() {
        createGroupWithDefaultProduct();

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void successWithOverride() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        GroupService service = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);

        // override actual even after void
        psBillingGroupsFactory.createServicePriceOverrides(service, 50, DateUtils.pastDate(),
                Instant.now().plus(settings.getAutoPayLeadTime().plus(1)));

        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        autoPaymentUtils.createContract(clientId);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(50);
    }

    @Test
    public void successWithTrial() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        GroupService service = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);

        // override not actual even after void
        psBillingGroupsFactory.createServicePriceOverrides(service, 0, DateUtils.pastDate(),
                Instant.now().plus(settings.getAutoPayLeadTime().minus(1)));

        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        autoPaymentUtils.createContract(clientId);

        autoPayManager.chargeRegularAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    protected GroupService setupCorrectData(long clientId) {
        return autoPaymentUtils.createDataForAutoPay(clientId);
    }

    protected GroupService setupCorrectData(long clientId, GroupProduct product) {
        return autoPaymentUtils.createDataForAutoPay(clientId, product);
    }
}
