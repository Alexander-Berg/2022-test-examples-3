package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.AutoResurrectionPayManager;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.ClientBalanceCalculator;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialDefinitionDao;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServicePriceOverride;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.PriceOverrideReason;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.groups.TrialService;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class AutoResurrectionPaymentTest extends BaseAutoPayTest {
    @Autowired
    protected AutoResurrectionPayManager payManager;
    @Autowired
    protected GroupProductDao groupProductDao;
    @Autowired
    protected ClientBalanceCalculator clientBalanceCalculator;
    @Autowired
    protected GroupServiceDao groupServiceDao;
    @Autowired
    protected GroupServicePriceOverrideDao groupServicePriceOverrideDao;
    @Autowired
    protected TrialService trialService;

    @Test
    public void successPayment() {
        setupCorrectData(clientId);
        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void failPayment() {
        setupCorrectData(clientId);
        setupErrorPayRequest();
        assertErrorPayRequest(clientId, () -> payManager.chargeResurrectionAutoPayment(clientId, rub));
    }

    @Test
    public void notEnoughFundsSequence() {
        setupCorrectData(clientId);

        ListF<Double> chargePlan = Cf.list(1.0, 0.5, 0.25);
        ListF<Double> expectedPricePlan = Cf.list(100.0, 50.0, 25.0);
        Mockito.when(settings.getAutoPayChargePlan()).thenReturn(chargePlan);

        DateUtils.unfreezeTime(); // to correct get last payment
        for (Tuple2<Double, Double> plan : settings.getAutoPayChargePlan().zip(expectedPricePlan)) {
            Double chargeCoefficient = plan._1;
            Double expectedPrice = plan._2;

            payManager.chargeResurrectionAutoPayment(clientId, rub);
            GroupTrustPaymentRequest lastPaymentRequest =
                    checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);
            Assert.equals(PaymentRequestStatus.CANCELLED, lastPaymentRequest.getStatus());
            Assert.equals(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS, lastPaymentRequest.getError().get());
            Assert.equals(chargeCoefficient, lastPaymentRequest.getPaymentPeriodCoefficient().get());

            assertBalanceCalledWithAmount(expectedPrice);

            Mockito.reset(balanceClientStub.getBalanceClientMock());
        }
    }

    @Test
    public void noChargeForGroupWithoutEnabledAutoPay() {
        setupCorrectData(clientId);
        groupDao.setAutoBillingForClient(clientId, false);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void noChargeForUnknownCard() {
        GroupService disabledService = setupCorrectData(clientId);
        CardEntity card = cardDao.findAll().single();
        UUID groupId = disabledService.getGroupId();
        balanceClientStub.reset(); // clear cards in Balance

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        verifyNoPayment();
        Assert.equals(CardStatus.DISABLED, cardDao.findById(card.getId()).getStatus());
        Assert.isFalse(groupDao.findById(groupId).getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void noChargeForExpiredCard() {
        GroupService disabledService = setupCorrectData(clientId);
        CardEntity card = cardDao.findAll().single();
        UUID groupId = disabledService.getGroupId();
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rub.getCurrencyCode(),
                true));

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        verifyNoPayment();
        Assert.equals(CardStatus.EXPIRED, cardDao.findById(card.getId()).getStatus());
        Assert.isFalse(groupDao.findById(groupId).getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void noChargeForNoPrimaryCard() {
        setupCorrectData(clientId);
        CardEntity card = cardDao.findAll().single();
        cardDao.updatePurpose(uid, card.getExternalId(), CardPurpose.B2B);

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        verifyNoPayment();
    }

    @Test
    public void noChargeForPayerIsNotAdmin() {
        setupCorrectData(clientId);

        Group groupWherePayer = autoPaymentUtils.createGroup(clientId, true);
        Group groupWhereAdmin = psBillingGroupsFactory.createGroup(x ->
                x.paymentInfo(new BalancePaymentInfo(123, PassportUid.cons(123), true)));
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(groupWhereAdmin.getExternalId()));

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        verifyNoPayment();
        groupWherePayer = groupDao.findById(groupWherePayer.getId());
        Assert.assertFalse(groupWherePayer.getPaymentInfo().get().isB2bAutoBillingEnabled());
    }

    @Test
    public void noChargeForPositiveBalance() {
        setupCorrectData(clientId);
        ClientBalanceEntity balance = clientBalanceDao.find(clientId, rub).get();
        clientBalanceDao.updateBalance(balance.getId(), BigDecimal.ONE);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void noChargeForInfiniteBalance() {
        setupCorrectData(clientId);
        ClientBalanceEntity balance = clientBalanceDao.find(clientId, rub).get();
        clientBalanceDao.updateVoidAt(balance.getId(), Option.empty());

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void noChargeForActiveService() {
        GroupService groupService = setupCorrectData(clientId);
        Group group = groupDao.findById(groupService.getGroupId());
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service("another_product"), 1);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void chargeOnRecentSuccessUserPayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.USER, PaymentRequestStatus.SUCCESS);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
        ListF<GroupTrustPaymentRequest> payments = groupTrustPaymentRequestDao.findAll();
        Assert.equals(2, payments.size());
    }

    @Test
    public void noChargeOnRecentSuccessAutoPayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.AUTO, PaymentRequestStatus.SUCCESS);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void ignoreFarSuccessPayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.AUTO, PaymentRequestStatus.SUCCESS);
        DateUtils.shiftTime(settings.getAutoPayRecentSuccessPaymentsLookupTime().plus(1));

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void noChargeOnRecentActivePayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.AUTO, PaymentRequestStatus.INIT);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void ignoreFarActivePayment() {
        setupCorrectData(clientId);

        insertPaymentRecord(clientId, PaymentInitiationType.USER, PaymentRequestStatus.INIT);
        DateUtils.shiftTime(settings.getAcceptableInitOrdersCheckDateExpirationTime().plus(1));

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void ignoreFarCanceledPayment() {
        setupCorrectData(clientId);
        payManager.chargeResurrectionAutoPayment(clientId, rub);
        checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);

        DateUtils.shiftTime(settings.getAutoPayRecentCanceledPaymentsLookupTime().plus(1));
        Mockito.reset(balanceClientStub.getBalanceClientMock());

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void noChargeForTooFrequentTry() {
        setupCorrectData(clientId);

        DateUtils.unfreezeTime(); // to correct get last payment
        for (int i = 0; i < settings.getAutoPayChargePlan().size(); i++) {
            payManager.chargeResurrectionAutoPayment(clientId, rub);
            checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);
        }

        Mockito.reset(balanceClientStub.getBalanceClientMock());
        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment(); // too early

        DateUtils.shiftTime(settings.getAutoResurrectMinRetryIntervalMinutes().plus(1));
        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void successWithSimilarCurrency() {
        setupCorrectData(clientId);
        Currency rur = Currency.getInstance("RUR");
        psBillingBalanceFactory.createContract(clientId, 2L, rur);
        CardEntity card = cardDao.findAll().single();
        balanceClientStub.addBoundCard(uid, new BalanceClientStub.Card(card.getExternalId(), rur.getCurrencyCode()));

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void failedPaymentNoRetry() {
        setupCorrectData(clientId);

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        checkLastPaymentRequest("error");
        Mockito.reset(balanceClientStub.getBalanceClientMock());

        // no retry for failed payment
        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void retryAfterRetrySequence() {
        setupCorrectData(clientId);

        DateUtils.unfreezeTime(); // to correct get last payment
        for (int i = 0; i < settings.getAutoPayChargePlan().size(); i++) {
            payManager.chargeResurrectionAutoPayment(clientId, rub);
            checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);
        }

        Mockito.reset(balanceClientStub.getBalanceClientMock());
        payManager.chargeResurrectionAutoPayment(clientId, rub);
        verifyNoPayment();
    }

    @Test
    public void successWithDefaultProduct() {
        GroupService groupService = setupCorrectData(clientId);
        Group group = groupDao.findById(groupService.getGroupId());
        autoPaymentUtils.mockOrgsWhereUserIsAdmin(Cf.list(group.getExternalId()));
        psBillingGroupsFactory.createDefaultProductService(group);

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        assertBalanceCalledWithAmount(100);
    }

    @Test
    public void resurrect() {
        GroupService disabledService = setupCorrectData(clientId);
        GroupProductEntity product = groupProductDao.findById(disabledService.getGroupProductId());
        CardEntity card = cardDao.findAll().single();
        UUID groupId = disabledService.getGroupId();

        payManager.chargeResurrectionAutoPayment(clientId, rub);

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
        AssertHelper.assertEquals(paymentRequest.getGroupServicesInfo().get().toString(),
                String.format("{\"activate_services\":[{\"group_id\":\"%s\",\"products\":[{\"code\":\"%s\"}]}]}",
                        groupId, product.getCode()));
        Assert.equals(Option.empty(), paymentRequest.getError());
        AssertHelper.assertEquals(paymentRequest.getMoney().get().getAmount(), 100);
        AssertHelper.assertEquals(paymentRequest.getMoney().get().getCurrency(), rub);

        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService newService = groupServicesManager.find(groupId, Target.ENABLED).single();
        AssertHelper.notEquals(newService.getId(), disabledService.getId());
        AssertHelper.assertEquals(newService.getGroupProductId(), disabledService.getGroupProductId());
    }

    @Test
    public void severalResurrections() {
        GroupService disabledService1 = setupCorrectData(clientId);
        GroupService disabledService2 = setupCorrectData(clientId);
        UUID groupId1 = disabledService1.getGroupId();
        UUID groupId2 = disabledService2.getGroupId();

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        assertBalanceCalledWithAmount(200);
        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService newService1 = groupServicesManager.find(groupId1, Target.ENABLED).single();
        GroupService newService2 = groupServicesManager.find(groupId2, Target.ENABLED).single();

        AssertHelper.assertEquals(newService1.getGroupProductId(), disabledService1.getGroupProductId());
        AssertHelper.assertEquals(newService2.getGroupProductId(), disabledService2.getGroupProductId());
    }

    @Test
    public void resurrectServiceWithTrial() {
        TrialDefinitionEntity trialDefinition = trialService.insert(TrialDefinitionDao.InsertData.builder()
                .price(BigDecimal.valueOf(50))
                .endDate(DateUtils.futureDateO())
                .hidden(true)
                .type(TrialType.UNTIL_DATE)
                .singleUsageComparisonKey(Option.of("uniq"))
                .build());
        GroupProduct product = psBillingProductsFactory.createGroupProduct(x ->
                x.paymentType(GroupPaymentType.PREPAID).pricePerUserInMonth(BigDecimal.valueOf(100))
                        .trialDefinitionId(Option.of(trialDefinition.getId())));

        GroupService disabledService = autoPaymentUtils.createCorrectDataForResurrection(clientId, product);
        UUID groupId = disabledService.getGroupId();
        GroupServicePriceOverride oldOverride = getSinglePriceOverride(disabledService);

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        assertBalanceCalledWithAmount(50);
        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService newService = groupServicesManager.find(groupId, Target.ENABLED).single();
        AssertHelper.notEquals(newService.getId(), disabledService.getId());
        AssertHelper.assertEquals(newService.getGroupProductId(), disabledService.getGroupProductId());

        GroupServicePriceOverride override = getSinglePriceOverride(newService);
        AssertHelper.assertEquals(override.getStartDate(), Instant.now());
        AssertHelper.assertEquals(override.getEndDate(), oldOverride.getEndDate());
        AssertHelper.assertEquals(override.getPricePerUserInMonth(), oldOverride.getPricePerUserInMonth());
        AssertHelper.assertEquals(override.isHidden(), oldOverride.isHidden());
        AssertHelper.assertTrue(override.getTrialUsageId().isPresent());
        AssertHelper.assertEquals(override.getTrialUsageId(), oldOverride.getTrialUsageId());
        AssertHelper.assertEquals(override.getReason(), oldOverride.getReason());
    }

    @Test
    public void resurrectServiceWithGift() {
        GroupService disabledService = setupCorrectData(clientId);
        UUID groupId = disabledService.getGroupId();
        GroupServicePriceOverride oldOverride = createGift(disabledService, 50, x -> x);

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        assertBalanceCalledWithAmount(50);
        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService newService = assertCreatedService(groupId, disabledService);
        assertServiceSingleOverride(newService, oldOverride);
    }

    @Test
    public void resurrectSeveralServicesWithGift() {
        GroupService disabledService1 = setupCorrectData(clientId);
        GroupService disabledService2 = setupCorrectData(clientId);
        UUID groupId1 = disabledService1.getGroupId();
        UUID groupId2 = disabledService2.getGroupId();
        GroupServicePriceOverride oldOverride1 = createGift(disabledService1, 150,
                x -> x.startDate(DateUtils.pastDate()).endDate(DateUtils.futureDateO()));
        GroupServicePriceOverride oldOverride2 = createGift(disabledService2, 150,
                x -> x.startDate(DateUtils.pastDate()).endDate(DateUtils.futureDateO()));

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        assertBalanceCalledWithAmount(150 + 150);
        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService newService1 = assertCreatedService(groupId1, disabledService1);
        assertServiceSingleOverride(newService1, oldOverride1);

        GroupService newService2 = assertCreatedService(groupId2, disabledService2);
        assertServiceSingleOverride(newService2, oldOverride2);
    }

    @Test
    public void resurrectServiceWithSeveralGifts() {
        GroupService disabledService = setupCorrectData(clientId);
        UUID groupId = disabledService.getGroupId();
        createGift(disabledService, 10,
                x -> x.startDate(DateUtils.farPastDate()).endDate(DateUtils.pastDateO()));
        GroupServicePriceOverride oldOverride1 = createGift(disabledService, 20,
                x -> x.startDate(DateUtils.pastDate()).endDate(DateUtils.futureDateO()));
        GroupServicePriceOverride oldOverride2 = createGift(disabledService, 30,
                x -> x.startDate(DateUtils.futureDate()).endDate(DateUtils.farFutureDateO()));

        payManager.chargeResurrectionAutoPayment(clientId, rub);

        assertBalanceCalledWithAmount(20);
        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService newService = assertCreatedService(groupId, disabledService);
        ListF<GroupServicePriceOverride> overrides = getPriceOverrides(newService);
        AssertHelper.assertSize(overrides, 2);
        assertServiceOverride(overrides.get(0), oldOverride1);
        assertServiceOverride(overrides.get(1), oldOverride2);
    }

    @Test
    public void noChargeIfDisabledManually() {
        // TODO
    }

    @Test
    public void noResurrectionOnActiveService() {
        GroupService disabledService = setupCorrectData(clientId);
        Group group = groupDao.findById(disabledService.getGroupId());

        payManager.chargeResurrectionAutoPayment(clientId, rub);
        GroupService conflictService = psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service("some_other_product"), 1);

        assertBalanceCalledWithAmount(100);

        checkLastPaymentRequest(CheckRequestPaymentResponse.SUCCESS_PAYMENT_STATUS);

        GroupService existService = groupServicesManager.find(group.getId(), Target.ENABLED).single();
        AssertHelper.assertEquals(existService.getId(), conflictService.getId());
    }

    private GroupServicePriceOverride createGift(GroupService disabledService, int price,
                                                 Function<GroupServicePriceOverrideDao.InsertData.InsertDataBuilder,
                                                         GroupServicePriceOverrideDao.InsertData.InsertDataBuilder> customizer) {
        return groupServicePriceOverrideDao.insert(
                customizer.apply(GroupServicePriceOverrideDao.InsertData.builder()
                                .pricePerUserInMonth(BigDecimal.valueOf(price))
                                .startDate(DateUtils.pastDate())
                                .endDate(DateUtils.futureDateO())
                                .groupServiceId(disabledService.getId())
                                .hidden(false)
                                .reason(PriceOverrideReason.GIFT)
                                .trialUsageId(Option.empty()))
                        .build());
    }

    private GroupServicePriceOverride getSinglePriceOverride(GroupService service) {
        ListF<GroupServicePriceOverride> overrides = getPriceOverrides(service);
        AssertHelper.assertSize(overrides, 1);
        return overrides.single();
    }

    private ListF<GroupServicePriceOverride> getPriceOverrides(GroupService service) {
        return groupServicePriceOverrideDao.findByGroupServices(Cf.list(service.getId())).getTs(service.getId())
                .sorted(Comparator.comparing(GroupServicePriceOverride::getStartDate));
    }

    private GroupService assertCreatedService(UUID groupId, GroupService disabledService) {
        GroupService newService = groupServicesManager.find(groupId, Target.ENABLED).single();
        AssertHelper.notEquals(newService.getId(), disabledService.getId());
        AssertHelper.assertEquals(newService.getGroupProductId(), disabledService.getGroupProductId());
        return newService;
    }

    private void assertServiceSingleOverride(GroupService newService, GroupServicePriceOverride oldOverride) {
        GroupServicePriceOverride override = getSinglePriceOverride(newService);
        assertServiceOverride(override, oldOverride);
    }

    private void assertServiceOverride(GroupServicePriceOverride newOverride, GroupServicePriceOverride oldOverride) {
        Instant expectedStartDate = oldOverride.getStartDate().isBeforeNow() ? Instant.now() :
                oldOverride.getStartDate();
        AssertHelper.assertEquals(newOverride.getStartDate(), expectedStartDate);
        AssertHelper.assertEquals(newOverride.getEndDate(), oldOverride.getEndDate());
        AssertHelper.assertEquals(newOverride.getPricePerUserInMonth(), oldOverride.getPricePerUserInMonth());
        AssertHelper.assertEquals(newOverride.isHidden(), oldOverride.isHidden());
        AssertHelper.assertEquals(newOverride.getTrialUsageId(), oldOverride.getTrialUsageId());
        AssertHelper.assertEquals(newOverride.getReason(), oldOverride.getReason());
    }

    protected GroupService setupCorrectData(long clientId) {
        return autoPaymentUtils.createCorrectDataForResurrection(clientId);
    }

    protected GroupService setupCorrectData(long clientId, GroupProduct product) {
        return autoPaymentUtils.createCorrectDataForResurrection(clientId, product);
    }

    public void before() {
        super.before();
        featureFlags.getAutoResurrectionPayAutoOn().setValue("true");
    }
}
