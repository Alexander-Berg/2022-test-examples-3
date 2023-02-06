package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import java.math.BigDecimal;
import java.util.Currency;

import lombok.val;
import org.joda.time.Instant;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupBillingService;
import ru.yandex.chemodan.app.psbilling.core.config.Settings;
import ru.yandex.chemodan.app.psbilling.core.dao.cards.CardDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.chemodan.balanceclient.exception.BalanceException;
import ru.yandex.chemodan.balanceclient.model.request.CreateRequest2Item;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public abstract class BaseAutoPayTest extends AbstractPsBillingCoreTest {
    protected final Currency rub = Currency.getInstance("RUB");
    protected final PassportUid uid = PassportUid.MAX_VALUE;
    protected ArgumentCaptor<CreateRequest2Item> request2ItemArgumentCaptor;
    protected long clientId = 1L;

    @Autowired
    protected BalanceClientStub balanceClientStub;
    @Autowired
    protected ClientBalanceDao clientBalanceDao;
    @Autowired
    protected PsBillingTransactionsFactory psBillingTransactionsFactory;
    @Autowired
    protected GroupTrustPaymentRequestDao groupTrustPaymentRequestDao;
    @Autowired
    protected GroupDao groupDao;
    @Autowired
    protected PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    protected CardDao cardDao;
    @Autowired
    protected Settings settings;
    @Autowired
    protected GroupBillingService groupBillingService;
    @Autowired
    protected PaymentFactory paymentFactory;
    @Autowired
    protected GroupServicesManager groupServicesManager;
    @Autowired
    private PaymentChecker paymentChecker;
    @Autowired
    protected AutoPaymentUtils autoPaymentUtils;

    public void createGroupWithoutClientBalance() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        // no balance: ClientBalanceEntity balance = createBalance(clientId, 1);
        // no balance: clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);
    }

    public void createGroupWithoutServices() {
        autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        // no service: psBillingTransactionsFactory.createService(group,
        // no service:    new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);
    }

    public GroupService createGroupWithPostpaidServices() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.POSTPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        autoPaymentUtils.createContract(clientId);
        return psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
    }

    public void createGroupWithFarVoidBalance() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        clientBalanceDao.updateVoidAt(balance.getId(),
                Option.of(Instant.now().plus(settings.getAutoPayLeadTime()).plus(1)));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);
    }

    public void createGroupWithoutEnabledAutoPay() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        Group group = autoPaymentUtils.createGroup(clientId, false);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        autoPaymentUtils.createContract(clientId);
    }

    public void createGroupWithDefaultProduct() {
        GroupProduct product = autoPaymentUtils.createProduct(GroupPaymentType.PREPAID, 100);
        GroupProduct defaultProduct = psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(GroupPaymentType.POSTPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(0))
                        .hidden(true)
                        .skipTransactionsExport(true));
        Group group = autoPaymentUtils.createGroup(clientId, true);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        CardEntity card = autoPaymentUtils.insertPrimaryCard("card-x123");
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode()));
        ClientBalanceEntity balance = autoPaymentUtils.createBalance(clientId, 1);
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(product.getCode()), 1);
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(defaultProduct.getCode()), 1);
        clientBalanceDao.updateVoidAt(balance.getId(), Option.of(Instant.now()));
        autoPaymentUtils.createContract(clientId);
    }

    protected abstract GroupService setupCorrectData(long clientId);

    protected abstract GroupService setupCorrectData(long clientId, GroupProduct product);

    protected void insertPaymentRecord(Long clientId, PaymentInitiationType type, PaymentRequestStatus status) {
        Option<CardEntity> cardO = type == PaymentInitiationType.USER
                ? Option.empty()
                : cardDao.findAll().firstO();
        paymentFactory.insertGroupPayment(clientId, uid, x -> x
                .paymentInitiationType(type)
                .status(status)
                .cardId(cardO.map(AbstractEntity::getId)));
    }

    protected GroupTrustPaymentRequest checkLastPaymentRequest(String code) {
        GroupTrustPaymentRequest paymentRequest = getLastPaymentRequest();

        CheckRequestPaymentResponse paymentResponse = new CheckRequestPaymentResponse();
        paymentResponse.setRequestId(paymentRequest.getRequestId());
        paymentResponse.setResponseCode(code);
        balanceClientStub.addPaymentRequestStatusCheck(paymentResponse);

        paymentChecker.checkPaymentRequestStatus(paymentRequest.getRequestId());
        return groupTrustPaymentRequestDao.findByRequestId(paymentRequest.getRequestId()).get();
    }

    protected GroupTrustPaymentRequest getLastPaymentRequest() {
        return groupTrustPaymentRequestDao.findAll().sortedByDesc(AbstractEntity::getCreatedAt).first();
    }

    protected void assertBalanceCalledWithAmount(double amount) {
        Mockito.verify(balanceClientStub.getBalanceClientMock(), Mockito.times(1))
                .createRequest(Mockito.anyLong(), Mockito.anyLong(),
                        request2ItemArgumentCaptor.capture(),
                        Mockito.anyBoolean(), Mockito.any());
        AssertHelper.assertEquals(request2ItemArgumentCaptor.getValue().getQty(), amount);
        psBillingBalanceFactory.setIncomeSum(clientId, rub, amount);
    }

    protected void verifyNoPayment() {
        Mockito.verify(psBillingCoreMocksConfig.balanceClientStub().getBalanceClientMock(), Mockito.never()).payRequest(Mockito.any());
    }

    protected void setupErrorPayRequest() {
        Mockito.when(psBillingCoreMocksConfig.balanceClientStub().getBalanceClientMock().payRequest(Mockito.any()))
                .thenThrow(new BalanceException("Error"));
    }

    protected void assertErrorPayRequest(long clientId, Assert.Block block) {
        Assert.assertThrows(block, BalanceException.class);
        val recentPayments = groupTrustPaymentRequestDao.findRecentPayments(clientId, Option.empty());

        Assert.sizeIs(1, recentPayments);

        val entity = recentPayments.first();
        Assert.equals(PaymentRequestStatus.INIT, entity.getStatus());
        Assert.none(entity.getTransactionId());
    }

    @Before
    public void initialize() {
        featureFlags.getAutoPayDisableOnUserIsNotAdmin().setValue("true");
        request2ItemArgumentCaptor = ArgumentCaptor.forClass(CreateRequest2Item.class);
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list());
    }
}
