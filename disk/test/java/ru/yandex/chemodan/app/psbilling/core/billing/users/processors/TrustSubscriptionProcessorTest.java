package ru.yandex.chemodan.app.psbilling.core.billing.users.processors;

import java.math.BigDecimal;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.users.AbstractPaymentTest;
import ru.yandex.chemodan.app.psbilling.core.billing.users.RefundOrderTask;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.RefundDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.RefundStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceBillingStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.users.UserService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.chemodan.trust.client.requests.OrderRequest;
import ru.yandex.chemodan.trust.client.requests.PaymentRequest;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionState;
import ru.yandex.chemodan.trust.client.responses.InstantInterval;
import ru.yandex.chemodan.trust.client.responses.PaymentResponse;
import ru.yandex.chemodan.trust.client.responses.PaymentStatus;
import ru.yandex.chemodan.trust.client.responses.SubscriptionResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class TrustSubscriptionProcessorTest extends AbstractPaymentTest {
    @Autowired
    private OrderProcessorFacade processor;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private UserServiceDao userServiceDao;
    @Autowired
    private UserProductManager userProductManager;

    @Autowired
    private PsBillingUsersFactory psBillingUsersFactory;
    @Autowired
    BazingaTaskManagerMock bazingaTaskManagerMock;
    @Autowired
    private FeatureFlags featureFlags;
    @Autowired
    private EmailHelper emailHelper;
    @Autowired
    private RefundDao refundDao;

    private UserProductPrice price;
    private String lastPaymentPurchaseToken = UUID.randomUUID().toString();

    @Before
    public void init() {
        price = createUserProductPrice("TEST", BigDecimal.TEN, CustomPeriodUnit.ONE_MONTH,
                psBillingTextsFactory.create().getId(), Option.empty(), Function.identityF(),
                x->x.packageName(Option.of("ru.yandex.disk"))
        );
        UserProduct userProduct = price.getPeriod().getUserProduct();
        psBillingProductsFactory.addUserProductToProductSet("set", userProduct.getId());
        featureFlags.getLongBillingPeriodForTrustEmailEnabled().setValue("true");
        featureFlags.getDisableStaleWebServices().setValue("true");
        DateUtils.freezeTime();
    }

    @Test
    public void testSuccessSubscription() {
        Order order = createOrder(price);

        payForOrderAndValidateService(order);
    }

    @Test
    // CHEMODAN-78219: автоматически оформлять возврат второго продукта
    public void testSuccessSubscriptionWithExistConflict() {
        UserProductEntity inappProduct =
                psBillingProductsFactory.createUserProduct(x -> x.billingType(BillingType.INAPP_APPLE));
        UserProductPrice inappProductPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(inappProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        psBillingProductsFactory.addUserProductToProductSet("set", inappProduct);
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey("set").map(ProductSetEntity::getId)).build());

        // пользователь создает заказ в вебе
        Order webOrder = createOrder(price);
        assertOrderStatus(webOrder, OrderStatus.INIT);

        // идет оформляет подписку в мобилке
        Order inappOrder = psBillingOrdersFactory.createOrUpdateInappOrder(userId, inappProductPrice.getId(),
                "inapp_order");
        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE);
        orderProcessorFacade.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);

        // и тут приходит оплата от веб заказа
        SubscriptionResponse subscriptionResponse = trustClientMockConfiguration.mockGetSubscriptionPaid();
        orderProcessorFacade.processOrder(webOrder);

        // мы его зарефандим, потому что две подписки быть не должно
        assertOrderStatus(webOrder, OrderStatus.PAID);
        assertOrderServiceNotExist(webOrder);
        Assert.equals(1, bazingaTaskManagerMock.findTasks(RefundOrderTask.class).length());
        RefundOrderTask refundOrderTask = bazingaTaskManagerMock.findTasks(RefundOrderTask.class).first()._1;
        Assert.equals(webOrder.getId().toString(), refundOrderTask.getParametersTyped().getOrderId());

        // таска на рефанд должна нормально выполняться
        bazingaTaskManagerMock.filterTaskQueue(x -> x instanceof RefundOrderTask);
        Mockito.when(trustClient.getPayment(any())).thenReturn(
                PaymentResponse.builder()
                        .paymentStatus(PaymentStatus.cleared)
                        .purchaseToken(subscriptionResponse.getPaymentIds().first())
                        .build());

        Mockito.when(trustClient.createRefund(any())).thenReturn("refund_id");
        bazingaTaskManagerMock.executeTasks(applicationContext);
    }

    @Test
    public void testWaitPayment() {
        Order order = createOrder(price);

        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().minus(Duration.standardSeconds(1)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .subscriptionPeriodCount(0)
                .subscriptionPeriod("1Y")
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.started)
                .currentAmount(BigDecimal.valueOf(123))
                .build());

        processor.processByOrderId(order.getId());

        /* sample from actual logs
        action: process order, status: started, status_code: null, status_description: null,
        order_id: 06b8d705-ce8a-4352-b0ac-203b1595faad, uid: 561672775,
        subscription: SubscriptionResponse[<some stuff inside>],
        period: 1Y, price: null, currency: None, package_name: null
         */

        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: process order, status: started, order_id: %uuid%, uid: %uid%, subscription: " +
                        "%subscription%, period: 1Y, price: 123, currency: RUB"
        );
        Order orderNew = orderDao.findById(order.getId());
        assertEquals(OrderStatus.INIT, orderNew.getStatus());
        assertFalse(orderNew.getUserServiceId().isPresent());
    }

    @Test
    public void testErrorPayment() {
        Order order = createOrder(price);

        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().minus(Duration.standardSeconds(1)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .subscriptionPeriodCount(0)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.not_authorized)
                .paymentStatusCode("not_enough_money")
                .paymentStatusDescription("unsufficient money")
                .build());

        processor.processByOrderId(order.getId());
        /* sample from logs
        action: buy_new, status: payment_failed,
        order_id: e29e0113-cdd9-41fc-a7db-087e17a5e716, uid: 4063835710,
        product_code: mail_pro_b2c_standard100_discount20_v20210525,
        period: UserProductPeriod[<the whole toStringed period here>],
        error_code: payment_timeout, error_message: timeout while waiting for payment data, package_name: null
         */
        //changed period to display the period, not the toStringed UserProductPeriod object
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: buy_new, status: payment_failed, " +
                        "order_id: %uuid%, uid: %uid%, product_code: TEST, " +
                        "period: 1M, price: 10, currency: RUB, error_code: not_enough_money, " +
                        "error_message: unsufficient money"
        );
        Order orderNew = orderDao.findById(order.getId());
        assertEquals(OrderStatus.ERROR, orderNew.getStatus());
        assertFalse(orderNew.getUserServiceId().isPresent());
        assertEquals("not_enough_money", orderNew.getTrustErrorCode().get());
        assertEquals("unsufficient money", orderNew.getTrustErrorMessage().get());
    }

    @Test
    public void testProlong() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);

        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().plus(Duration.standardDays(60)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .subscriptionPeriodCount(2)
                .subscriptionState(3)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());

        processor.processByOrderId(order.getId());
        //original from logs
        /*
        action: prolong_auto, status: success,
        order_id: 86071, uid: 405701328, product_code: mail_pro_b2c_premium1000_inapp_google,
        period: 1Y, price: 1390, currency: RUB,
        old_expiration_date:2022-06-14T21:34:15.000Z,
        new_expiration_date: 2022-06-15T21:04:39.000Z,
        package_name: ru.yandex.disk, service_billing_status: PAID
         */
        PurchaseReportingServiceMockConfiguration.assertLogLike("action: prolong_auto, status: success, " +
                "order_id: %uuid%, uid: %uid%, product_code: TEST, " +
                "period: 1M, price: 10, is_start_period: false, currency: RUB, " +
                "old_expiration_date: %date%, " +
                "new_expiration_date: %date%, " +
                "service_billing_status: PAID");

        UserServiceEntity service = userServiceDao.findById(order.getUserServiceId().get());
        validateService(order, Target.ENABLED, UserServiceBillingStatus.PAID, true);
        assertEquals(Instant.now().plus(Duration.standardDays(60)), service.getNextCheckDate().get());
        assertEquals(service.getNextCheckDate(), service.getDueDate());
    }

    @Test
    public void testGrace() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);

        Instant from = Instant.now().minus(Duration.standardHours(12));
        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().plus(Duration.standardDays(10)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .graceIntervals(Cf.list(new InstantInterval(from, from.plus(Duration.standardDays(20)))))
                .subscriptionPeriodCount(2)
                .subscriptionState(3)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());
        bazingaTaskManagerMock.clearTasks();

        processor.processByOrderId(order.getId());
        UserServiceEntity service = userServiceDao.findById(order.getUserServiceId().get());
        validateService(order, Target.ENABLED, UserServiceBillingStatus.WAIT_AUTO_PROLONG, true);
        assertEquals(Instant.now().plus(Duration.standardHours(12)),
                service.getNextCheckDate().get());
        assertEquals(Instant.now().plus(Duration.standardDays(2)).minus(Duration.standardHours(12)),
                service.getDueDate().get());

        String emailKey = "trust_in_grace";
        ListF<String> args = Cf.list("public_display_name", "subscription_end_date", "tariff_name",
                "user_product_currency", "user_product_price");
        emailHelper.checkSingleLocalizedEmailArgs(SendLocalizedEmailTask.class, emailKey, args);
    }

    @Test
    public void testGraceAtTimeRace() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);

        Instant graceEnds = Instant.now().minus(1000);
        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(graceEnds)
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .graceIntervals(Cf.list(new InstantInterval(
                        Instant.now().minus(Duration.standardDays(10)),
                        graceEnds)))
                .subscriptionPeriodCount(2)
                .subscriptionState(3)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());

        processor.processByOrderId(order.getId());
        UserServiceEntity service = userServiceDao.findById(order.getUserServiceId().get());
        validateService(order, Target.ENABLED, UserServiceBillingStatus.WAIT_AUTO_PROLONG, true);
        assertEquals(graceEnds, service.getNextCheckDate().get());
        assertEquals(Instant.now().plus(Duration.standardDays(2 - 10)),
                service.getDueDate().get());
    }

    @Test
    public void testOnHold() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);

        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().minus(Duration.standardSeconds(30)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .graceIntervals(Cf.list(new InstantInterval(
                        Instant.now().minus(Duration.standardDays(20)),
                        Instant.now().minus(Duration.standardDays(10)))))
                .holdIntervals(Cf.list(new InstantInterval(
                        Instant.now().minus(Duration.standardDays(10)),
                        Instant.now().plus(Duration.standardDays(10)))))
                .subscriptionPeriodCount(2)
                .subscriptionState(4)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());

        processor.processByOrderId(order.getId());
        validateOrderOnHold(order);
    }

    @Test
    public void restoreOnHold() {
        Order order = payForOrderAndValidateService(createOrder(price));
        trustSubscriptionProcessor.processHoldOrder(order, Instant.now(), 1);
        validateOrderOnHold(order);
        Mockito.reset(trustClient);
        order = payForOrderAndValidateService(order);
        validateService(order, Target.ENABLED, UserServiceBillingStatus.PAID, true);
    }

    @Test
    public void restoreOnHoldWithWrongHoldPeriod() {
        Order order = payForOrderAndValidateService(createOrder(price));
        trustSubscriptionProcessor.processHoldOrder(order, Instant.now(), 1);
        validateOrderOnHold(order);
        Mockito.reset(trustClient);
        InstantInterval holdInterval = new InstantInterval(Instant.now().minus(Duration.standardDays(1)),
                Instant.now().plus(Duration.standardDays(1)));
        order = processOrder(order, Cf.list(holdInterval));
        validateService(order, Target.ENABLED, UserServiceBillingStatus.PAID, true);
    }

    @Test
    public void disableStaleOrder_stale() {
        final InstantInterval holdInterval = new InstantInterval(Instant.now().minus(Duration.standardDays(100)),
                Instant.now().minus(settings.getAcceptableUserServiceCheckDateExpirationTime()));
        disableStaleOrderImpl(holdInterval, holdInterval.getFrom(), Target.DISABLED, OrderStatus.PAID);
    }

    @Test
    public void disableStaleOrder_notStaleInHold() {
        final InstantInterval holdInterval = new InstantInterval(Instant.now().minus(Duration.standardDays(100)),
                Instant.now().plus(Duration.standardDays(1)));
        disableStaleOrderImpl(holdInterval, holdInterval.getFrom(), Target.DISABLED, OrderStatus.ON_HOLD);
    }

    @Test
    public void disableStaleOrder_notStaleIsActive() {
        final InstantInterval holdInterval = new InstantInterval(Instant.now().minus(Duration.standardDays(100)),
                Instant.now().minus(settings.getAcceptableUserServiceCheckDateExpirationTime()));
        disableStaleOrderImpl(holdInterval, Instant.now().plus(Duration.standardDays(1)), Target.ENABLED, OrderStatus.PAID);
    }

    @Test
    public void disableStaleOrder_severalIntervals() {
        ListF<InstantInterval> holdIntervals = Cf.list(new InstantInterval(Instant.now().minus(Duration.standardDays(1)),
                        Instant.now().plus(Duration.standardDays(1))),
                new InstantInterval(Instant.now().minus(Duration.standardDays(100)),
                        Instant.now().minus(settings.getAcceptableUserServiceCheckDateExpirationTime())));
        disableStaleOrderImpl(holdIntervals, holdIntervals.get(0).getFrom(), Target.DISABLED,
                OrderStatus.ON_HOLD);
    }

    @Test
    public void restorePaidIfNoService() {
        Order order = payForOrderAndValidateService(createOrder(price));
        userServiceManager.disableService(order.getUserServiceId().get());
        Assert.equals(Target.DISABLED, userServiceDao.findById(order.getUserServiceId().get()).getTarget());
        processor.processByOrderId(order.getId());
        validateService(orderDao.findById(order.getId()), Target.ENABLED, UserServiceBillingStatus.PAID, true);
    }

    @Test
    public void doNotRestorePaidIfRefunded() {
        Order order = payForOrderAndValidateService(createOrder(price));
        userServiceManager.disableService(order.getUserServiceId().get());
        Assert.equals(Target.DISABLED, userServiceDao.findById(order.getUserServiceId().get()).getTarget());
        refundDao.create(RefundDao.InsertData.builder()
                .orderId(order.getId())
                .status(Option.of(RefundStatus.SUCCESS))
                .trustPaymentId("paymentId")
                .trustRefundId("refundId")
                .build());
        processor.processByOrderId(order.getId());
        validateService(orderDao.findById(order.getId()), Target.DISABLED, UserServiceBillingStatus.PAID, true);
    }

    @Test
    public void testStopAutoProlong() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);

        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().plus(Duration.standardSeconds(30)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .finishTime(Instant.now().minus(Duration.standardDays(1)))
                .subscriptionPeriodCount(2)
                .subscriptionState(4)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());

        processor.processByOrderId(order.getId());
        UserServiceEntity service = userServiceDao.findById(order.getUserServiceId().get());
        validateService(order, Target.ENABLED, UserServiceBillingStatus.PAID, false);
        assertEquals(Instant.now().plus(Duration.standardSeconds(30)), service.getNextCheckDate().get());
        assertEquals(service.getNextCheckDate(), service.getDueDate());
    }

    @Test
    public void testStopService() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);

        mockTrustClient(order, SubscriptionResponse.builder()
                .subscriptionUntil(Instant.now().minus(Duration.standardSeconds(30)))
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .finishTime(Instant.now().minus(Duration.standardDays(1)))
                .subscriptionPeriodCount(2)
                .subscriptionState(4)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build(), PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());

        processor.processByOrderId(order.getId());
        UserServiceEntity service = userServiceDao.findById(order.getUserServiceId().get());
        validateService(order, Target.DISABLED, UserServiceBillingStatus.PAID, true);
        assertEquals(SynchronizationStatus.INIT, service.getStatus());
        //не меняем дату
        assertEquals(Instant.now().plus(Duration.standardDays(30)), service.getNextCheckDate().get());
        assertEquals(service.getNextCheckDate(), service.getDueDate());
    }

    private void mockTrustClient(Order order, SubscriptionResponse subscriptionResponse,
                                 PaymentResponse paymentResponse) {
        Mockito.when(trustClient.getSubscription(any())).thenAnswer(invocation -> {
            OrderRequest request = (OrderRequest) invocation.getArguments()[0];
            assertEquals(PassportUid.cons(Long.parseLong(order.getUid())), request.getUid());
            assertEquals(order.getTrustServiceId(), request.getTrustServiceId());
            assertEquals(order.getTrustOrderId(), request.getOrderId());

            return subscriptionResponse;
        });

        Mockito.when(trustClient.getPayment(any())).thenAnswer(invocation -> {
            PaymentRequest request = (PaymentRequest) invocation.getArguments()[0];
            assertEquals(PassportUid.cons(Long.parseLong(order.getUid())), request.getUid());
            assertEquals(order.getTrustServiceId(), request.getTrustServiceId());
            assertEquals(lastPaymentPurchaseToken, request.getPurchaseToken());

            return paymentResponse;
        });
    }

    @Test
    public void testUpgrade() {
        Order order = payForOrderAndValidateService(createOrder(price));

        UserService userService = userServiceManager.findById(order.getUserServiceId().get());
        assertTrue(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.ENABLED);
        assertEquals(userService.getNextCheckDate().get(), Instant.now().plus(Duration.standardDays(30)));
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());

        Mockito.reset(trustClient);
        UserProductPrice secondUserProductPrice = createUserProductPrice("TEST2", BigDecimal.valueOf(100),
                CustomPeriodUnit.ONE_MONTH, psBillingTextsFactory.create2().getId(), Option.empty());
        addToBucket(Cf.list(price, secondUserProductPrice));

        payForOrderAndValidateService(createOrder(secondUserProductPrice), secondUserProductPrice,
                Instant.now().plus(Duration.standardDays(33)));
        /*
        sample from logs
        action: buy_new, status: success, order_id: f0e0316c-c7f0-4052-b45d-16e9c5c42ef1,
        uid: 41145159, product_code: mail_pro_b2c_premium200_v20220414_exp3,
        period: 1Y, price: 1290, currency: RUB,
        old_order_id: null, old_service_id: null,
        new_expiration_date: 2023-06-14T21:40:40.000Z, package_name: null
         */
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: order_upgraded, status: success, order_id: %uuid%, " +
                        "uid: %uid%, product_code: TEST2, " +
                        "period: 1M, price: 100, is_start_period: false, currency: RUB, " +
                        "old_order_id: %uuid%, old_service_id: %uuid%, " +
                        "new_expiration_date: %date%"
        );

        userService = userServiceManager.findById(order.getUserServiceId().get());
        assertFalse(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.DISABLED);
        assertEquals(userService.getNextCheckDate().get(), Instant.now().plus(Duration.standardDays(30)));
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());

        //тестируем что при получении нотификации по старому ордеру ничего не случится
        Mockito.reset(trustClient);
        Mockito.when(trustClient.getSubscription(any())).thenReturn(
                SubscriptionResponse.builder()
                        .subscriptionUntil(Instant.now().plus(Duration.standardDays(33)))
                        .paymentIds(Cf.list(lastPaymentPurchaseToken))
                        .subscriptionPeriodCount(2)
                        .subscriptionState(3)
                        .currentAmount(new String[][]{{"10", "RUB"}})
                        .build());

        Mockito.when(trustClient.getPayment(any())).thenReturn(
                PaymentResponse.builder()
                        .paymentStatus(PaymentStatus.cleared)
                        .build());

        processor.processByOrderId(order.getId());

        userService = userServiceManager.findById(order.getUserServiceId().get());
        assertFalse(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.DISABLED);
        assertEquals(userService.getNextCheckDate().get(), Instant.now().plus(Duration.standardDays(30)));
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());
    }

    @Test
    public void testUpgradeFromProductWithActiveTrialPeriod() {
        UserProductPrice price = createUserProductPrice("TEST1", BigDecimal.TEN, CustomPeriodUnit.ONE_MONTH,
                psBillingTextsFactory.create().getId(),
                Option.of(psBillingProductsFactory.createTrialDefinitionWithPeriod().getId()));
        Instant firstSubscriptionEndDate = Instant.now().plus(Duration.standardDays(30));

        Order order = createOrder(price);

        Mockito.when(trustClient.getSubscription(any())).thenReturn(SubscriptionResponse.builder()
                .subscriptionUntil(firstSubscriptionEndDate)
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .subscriptionPeriodCount(1)
                .subscriptionState(1)
                .build());

        Mockito.when(trustClient.getPayment(any())).thenReturn(PaymentResponse.builder()
                .paymentStatus(PaymentStatus.cleared)
                .build());

        processor.processByOrderId(order.getId());
        order = orderDao.findById(order.getId());

        // check that first service was activated with trial period
        UserService userService = userServiceManager.findById(order.getUserServiceId().get());
        assertTrue(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.ENABLED);
        assertEquals(userService.getBillingStatus().get(), UserServiceBillingStatus.FREE_PERIOD);
        assertEquals(userService.getNextCheckDate().get(), firstSubscriptionEndDate);
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());

        UserProductPrice secondUserProductPrice = createUserProductPrice("TEST2", BigDecimal.valueOf(100),
                CustomPeriodUnit.ONE_MONTH, psBillingTextsFactory.create2().getId(), Option.empty());

        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("test").userProductId(Option.of(price.getPeriod().getUserProduct().getId())).build());
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("test").userProductId(Option.of(secondUserProductPrice.getPeriod().getUserProduct().getId()))
                .build());

        DateUtils.shiftTime(Duration.standardDays(15));

        // upgrade without supplement time
        Instant upgradeSubscriptionEndTime = Instant.now().plus(Duration.standardDays(30));
        Order upgradeOrder = payForOrderAndValidateService(createOrder(secondUserProductPrice), secondUserProductPrice,
                upgradeSubscriptionEndTime);

        // check that first service was disabled
        userService = userServiceManager.findById(order.getUserServiceId().get());
        assertFalse(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.DISABLED);
        assertEquals(userService.getNextCheckDate().get(), firstSubscriptionEndDate);
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());

        // check that second service was activated
        UserService upgradeService = userServiceManager.findById(upgradeOrder.getUserServiceId().get());
        assertTrue(upgradeService.getAutoProlongEnabled());
        assertEquals(upgradeService.getTarget(), Target.ENABLED);
        // didn't add time for trial period
        assertEquals(upgradeService.getNextCheckDate().get(), upgradeSubscriptionEndTime);
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());
    }

    @Test
    public void testUpgradeFromProductInGracePeriod() {
        Order order = payForOrderAndValidateService(createOrder(price));

        Mockito.reset(trustClient);
        Mockito.when(trustClient.getSubscription(any())).thenReturn(
                SubscriptionResponse.builder()
                        .subscriptionUntil(Instant.now().plus(Duration.standardDays(10)))
                        .paymentIds(Cf.list(lastPaymentPurchaseToken))
                        .graceIntervals(Cf.list(new InstantInterval(
                                Instant.now().minus(Duration.standardDays(10)),
                                Instant.now().plus(Duration.standardDays(10)))))
                        .subscriptionPeriodCount(2)
                        .subscriptionState(3)
                        .currentAmount(new String[][]{{"10", "RUB"}})
                        .build());

        Mockito.when(trustClient.getPayment(any())).thenReturn(
                PaymentResponse.builder()
                        .paymentStatus(PaymentStatus.cleared)
                        .build());

        // move service in grace period
        processor.processByOrderId(order.getId());

        UserService userService = userServiceManager.findById(order.getUserServiceId().get());
        assertEquals(userService.getTarget(), Target.ENABLED);
        assertEquals(userService.getBillingStatus().get(), UserServiceBillingStatus.WAIT_AUTO_PROLONG);


        UserProductPrice secondUserProductPrice = createUserProductPrice("TEST2", BigDecimal.valueOf(100),
                CustomPeriodUnit.ONE_MONTH, psBillingTextsFactory.create2().getId(), Option.empty());

        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("test").userProductId(Option.of(price.getPeriod().getUserProduct().getId())).build());
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("test").userProductId(Option.of(secondUserProductPrice.getPeriod().getUserProduct().getId()))
                .build());

        // upgrade without supplement time
        Order upgradeOrder = payForOrderAndValidateService(createOrder(secondUserProductPrice), secondUserProductPrice,
                Instant.now().plus(Duration.standardDays(30)));

        // check that first service was disabled
        userService = userServiceManager.findById(order.getUserServiceId().get());
        assertFalse(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.DISABLED);

        // check that second service was activated
        UserService upgradeService = userServiceManager.findById(upgradeOrder.getUserServiceId().get());
        assertTrue(upgradeService.getAutoProlongEnabled());
        assertEquals(upgradeService.getTarget(), Target.ENABLED);
        // didn't add time for grace period
        assertEquals(upgradeService.getNextCheckDate().get(), Instant.now().plus(Duration.standardDays(30)));
        assertEquals(upgradeService.getNextCheckDate(), upgradeService.getDueDate());
    }

    @Test
    public void testRefundOnPurchaseConflictingProduct() {
        //first purchase
        Order order = payForOrderAndValidateService(createOrder(price));

        UserService userService = userServiceManager.findById(order.getUserServiceId().get());
        assertTrue(userService.getAutoProlongEnabled());
        assertEquals(userService.getTarget(), Target.ENABLED);
        assertEquals(userService.getNextCheckDate().get(), Instant.now().plus(Duration.standardDays(30)));
        assertEquals(userService.getNextCheckDate(), userService.getDueDate());

        // second purchase, create second conflicting product which is cheaper
        Mockito.reset(trustClient);
        bazingaTaskManagerStub.tasksWithParams.clear();

        UserProductPrice secondUserProductPrice = createUserProductPrice("TEST2", BigDecimal.valueOf(5),
                CustomPeriodUnit.ONE_MONTH, psBillingTextsFactory.create2().getId(), Option.empty());
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("test").userProductId(Option.of(price.getPeriod().getUserProduct().getId())).build());
        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("test").userProductId(Option.of(secondUserProductPrice.getPeriod().getUserProduct().getId()))
                .build());

        Order secondOrder = processOrder(createOrder(secondUserProductPrice));
        Assert.assertNone(secondOrder.getUserServiceId());
        assertEquals(OrderStatus.PAID, secondOrder.getStatus());
        assertTrue(bazingaTaskManagerStub.tasksWithParams.single()._1 instanceof RefundOrderTask);
        RefundOrderTask refundTask = (RefundOrderTask) bazingaTaskManagerStub.tasksWithParams.single()._1;
        assertEquals(secondOrder.getId().toString(), refundTask.getParametersTyped().getOrderId());

        // оригинальная подписка не меняется
        UserServiceEntity firstService = userServiceDao.findById(order.getUserServiceId().get());
        validateService(order, Target.ENABLED, UserServiceBillingStatus.PAID, true);
        assertEquals(Instant.now().plus(Duration.standardDays(30)), firstService.getNextCheckDate().get());
        assertEquals(firstService.getNextCheckDate(), firstService.getDueDate());
        assertEquals(order.getId(), firstService.getLastPaymentOrderId().get());
    }

    @Test
    // CHEMODAN-77685. У пользователя на самом деле может быть несколько активных подписок. Апгрейд при этом разрешаем
    public void testUpgradeConflictProduct() {
        UserProductPrice standardPrice = createUserProductPrice("standard", 10);
        UserProductPrice premiumPrice = createUserProductPrice("premium", 100);
        UserProductPrice staffPrice = createUserProductPrice("staff_1tb", 0, x -> x.billingType(BillingType.FREE));

        addToBucket(Cf.list(staffPrice, standardPrice, premiumPrice));

        Order standardOrder = processOrder(createOrder(standardPrice));
        // second service can be created cause of group product
        UserServiceEntity staffServiceEntity = psBillingUsersFactory.createUserService(userId, staffPrice);

        Order premiumOrder = payForOrderAndValidateService(createOrder(premiumPrice), premiumPrice,
                Instant.now().plus(Duration.standardDays(33)));

        UserService premiumService = userServiceManager.findById(premiumOrder.getUserServiceId().get());
        UserService standardService = userServiceManager.findById(standardOrder.getUserServiceId().get());
        UserService staffService = userServiceManager.findById(staffServiceEntity.getId());

        assertEquals(Target.ENABLED, premiumService.getTarget());
        assertEquals(Target.DISABLED, standardService.getTarget());
        assertEquals(Target.ENABLED, staffService.getTarget());
    }

    @Test
    public void findByIdAndStateTest() {
        Order order = createOrder(price);
        OrderProcessorFacade spiedProcessor = Mockito.spy(processor);
        Mockito.doNothing().when(spiedProcessor).processOrder(order);
        spiedProcessor.processByOrderIdAndState(order.getId(), OrderStatus.ERROR);
        Mockito.verify(spiedProcessor, Mockito.times(0)).processOrder(order);
        spiedProcessor.processByOrderIdAndState(order.getId(), order.getStatus());
        Mockito.verify(spiedProcessor, Mockito.times(1)).processOrder(order);
    }

    @Test
    public void testPriceForPeriodNoStartPeriod() {
        Order order = createOrder(price);
        CalculatedPrice priceForPeriod = trustSubscriptionProcessor.getPriceForPeriod(price, order);
        Assert.equals(price.getPrice(), priceForPeriod.getPrice());
        Assert.equals(false, priceForPeriod.isStartPeriod());
    }

    @Test
    public void testPriceForPeriodInStartPeriod() {
        UserProductPrice priceWithStartPeriod = createUserProductPriceWithStartPeriod();
        Order order = createOrder(priceWithStartPeriod);
        CalculatedPrice priceForPeriod = trustSubscriptionProcessor.getPriceForPeriod(priceWithStartPeriod, order);
        Assert.equals(priceWithStartPeriod.getStartPeriodPrice().get(), priceForPeriod.getPrice());
        Assert.equals(true, priceForPeriod.isStartPeriod());
    }

    @Test
    public void testPriceForPeriodAfterStartPeriod() {
        UserProductPrice priceWithStartPeriod = createUserProductPriceWithStartPeriod();
        Order order = createOrder(priceWithStartPeriod);
        orderDao.updateSubscriptionsCount(order.getId(), 10);
        CalculatedPrice priceForPeriod = trustSubscriptionProcessor.getPriceForPeriod(priceWithStartPeriod,
                orderDao.findById(order.getId()));
        Assert.equals(priceWithStartPeriod.getPrice(), priceForPeriod.getPrice());
        Assert.equals(false, priceForPeriod.isStartPeriod());
    }

    private void addToBucket(ListF<UserProductPrice> prices) {
        String bucketCode = UUID.randomUUID().toString();
        for (UserProductPrice price : prices) {
            userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                    .code(bucketCode)
                    .userProductId(Option.of(price.getPeriod().getUserProduct().getId())).build());
        }
    }

    private UserProductPrice createUserProductPrice(String code, Integer price) {
        return createUserProductPrice(code, BigDecimal.valueOf(price),
                CustomPeriodUnit.ONE_MONTH,
                psBillingTextsFactory.create().getId(), Option.empty());
    }

    private UserProductPrice createUserProductPrice(String code, Integer price,
                                                    Function<UserProductDao.InsertData.InsertDataBuilder,
                                                            UserProductDao.InsertData.InsertDataBuilder> customizer) {
        return createUserProductPrice(code, BigDecimal.valueOf(price),
                CustomPeriodUnit.ONE_MONTH,
                psBillingTextsFactory.create().getId(), Option.empty(), customizer);
    }

    private UserProductPrice createUserProductPrice(String code, BigDecimal price, CustomPeriodUnit period,
                                                    UUID tankerKey, Option<UUID> trialDefinitionId) {
        return createUserProductPrice(code, price, period, tankerKey, trialDefinitionId, Function.identityF());
    }

    private UserProductPrice createUserProductPrice(String code, BigDecimal price, CustomPeriodUnit period,
                                                    UUID tankerKey, Option<UUID> trialDefinitionId,
                                                    Function<UserProductDao.InsertData.InsertDataBuilder,
                                                            UserProductDao.InsertData.InsertDataBuilder> customizer
    ) {
        return createUserProductPrice(code, price, period, tankerKey, trialDefinitionId,customizer,
                Function.identityF());
    }

    private UserProductPrice createUserProductPrice(String code, BigDecimal price, CustomPeriodUnit period,
                                                    UUID tankerKey, Option<UUID> trialDefinitionId,
                                                    Function<UserProductDao.InsertData.InsertDataBuilder,
                                                            UserProductDao.InsertData.InsertDataBuilder> customizer,
                                                    Function<UserProductPeriodDao.InsertData.InsertDataBuilder,
                                                            UserProductPeriodDao.InsertData.InsertDataBuilder> periodCustomizer
    ) {
        UserProductEntity product = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code(code);
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(tankerKey);
            builder.singleton(true);
            builder.trustServiceId(Option.of(111));
            builder.trustSubsChargingRetryDelay(Option.of("1D"));
            builder.trustSubsChargingRetryLimit(Option.of("2D"));
            builder.trustSubsGracePeriod(Option.of("2D"));
            builder.trialDefinitionId(trialDefinitionId);
            return customizer.apply(builder);
        });
        psBillingTextsFactory.loadTranslations();
        UserProductPrice priceEntity = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(product, period, price,
                        periodCustomizer).getId()
        );
        psBillingProductsFactory.addUserProductToProductSet(UUID.randomUUID().toString(), product.getId());
        initStubs(priceEntity);
        return priceEntity;
    }

    private UserProductPrice createUserProductPriceWithStartPeriod() {
        UserProductEntity productWithStartPeriod = psBillingProductsFactory.createUserProduct();
        UserProductPrice priceWithStartPeriod =
                userProductManager.findPrice(psBillingProductsFactory.createUserProductPrices(
                        productWithStartPeriod,
                        CustomPeriodUnit.ONE_MONTH,
                        BigDecimal.TEN, CustomPeriod.fromMonths(1),
                        1,
                        BigDecimal.ONE).getId());
        psBillingProductsFactory.addUserProductToProductSet(UUID.randomUUID().toString(),
                productWithStartPeriod.getId());
        return priceWithStartPeriod;
    }

    @NotNull
    private Order payForOrderAndValidateService(Order order) {
        return payForOrderAndValidateService(order, price, Instant.now().plus(Duration.standardDays(30)));
    }

    private Order payForOrderAndValidateService(Order order, UserProductPrice price, Instant expectedEndPeriod) {
        Order orderNew = processOrder(order);
        assertTrue(orderNew.getUserServiceId().isPresent());
        UserServiceEntity service = userServiceDao.findById(orderNew.getUserServiceId().get());
        assertEquals(orderNew.getUid(), service.getUid());
        assertEquals(price.getId(), service.getProductPriceId().get());
        assertEquals(price.getPeriod().getUserProductId(), service.getUserProductId());
        assertTrue(service.getAutoProlongEnabled().get());
        assertEquals(UserServiceBillingStatus.PAID, service.getBillingStatus().get());
        assertEquals(Target.ENABLED, service.getTarget());
        assertEquals(expectedEndPeriod, service.getNextCheckDate().get());
        assertEquals(service.getNextCheckDate(), service.getDueDate());
        assertEquals(orderNew.getId(), service.getLastPaymentOrderId().get());

        return orderNew;
    }

    @NotNull
    private Order processOrder(Order order) {
        return processOrder(order, null);
    }

    @NotNull
    private Order processOrder(Order order, ListF<InstantInterval> holdInterval) {
        return processOrder(order, holdInterval, Instant.now().plus(Duration.standardDays(30)));
    }

    @NotNull
    private Order processOrder(Order order, ListF<InstantInterval> holdInterval, Instant subscriptionUntil) {
        mockTrustClient(order, SubscriptionResponse.builder()
                        .subscriptionUntil(subscriptionUntil)
                        .paymentIds(Cf.list(lastPaymentPurchaseToken))
                        .subscriptionPeriodCount(1)
                        .subscriptionState(3)
                        .holdIntervals(holdInterval)
                        .currentAmount(new String[][]{{"10", "RUB"}})
                        .build(),
                PaymentResponse.builder()
                        .paymentStatus(PaymentStatus.cleared)
                        .build());

        processor.processByOrderId(order.getId());

        return orderDao.findById(order.getId());
    }

    protected Order createOrder(UserProductPrice price) {
        initStubs(price);
        return super.createOrder(price);
    }

    private void validateOrderOnHold(Order order) {
        validateService(order, Target.DISABLED, UserServiceBillingStatus.PAID, true);

        Order heldOrder = orderDao.findById(order.getId());
        assertEquals(OrderStatus.ON_HOLD, heldOrder.getStatus());
    }

    private void validateService(UserService service,
                                 Target expectedTarget, UserServiceBillingStatus expectedBiilingStatus,
                                 boolean expectedAutoProlong) {
        assertEquals(price.getId(), service.getProductPriceId().get());
        assertEquals(price.getPeriod().getUserProductId(), service.getUserProductId());
        assertEquals(expectedAutoProlong, service.getAutoProlongEnabled());
        assertEquals(expectedBiilingStatus, service.getBillingStatus().get());
        assertEquals(expectedTarget, service.getTarget());
    }

    private void validateService(Order order, Target expectedTarget, UserServiceBillingStatus expectedBiilingStatus,
                                 boolean expectedAutoProlong) {
        UserServiceEntity service = userServiceDao.findById(order.getUserServiceId().get());
        assertEquals(order.getUid(), service.getUid());
        assertEquals(order.getId(), service.getLastPaymentOrderId().get());
        assertEquals(price.getId(), service.getProductPriceId().get());
        assertEquals(price.getPeriod().getUserProductId(), service.getUserProductId());
        assertEquals("Unexpected autoProlong state", expectedAutoProlong, service.getAutoProlongEnabled().get());
        assertEquals(expectedBiilingStatus, service.getBillingStatus().get());
        assertEquals(expectedTarget, service.getTarget());
    }

    private void disableStaleOrderImpl(InstantInterval holdInterval, Instant subscriptionUntil, Target expectedTarget,
                                       OrderStatus expectedOrderStatus) {
        disableStaleOrderImpl(Cf.list(holdInterval), subscriptionUntil, expectedTarget, expectedOrderStatus);
    }

    private void disableStaleOrderImpl(ListF<InstantInterval> list, Instant subscriptionUntil, Target expectedTarget,
                                       OrderStatus expectedOrderStatus) {
        Order order = payForOrderAndValidateService(createOrder(price));
        Mockito.reset(trustClient);
        order = processOrder(order, list, subscriptionUntil);
        validateService(order, expectedTarget, UserServiceBillingStatus.PAID, true);
        Assert.assertEquals(expectedOrderStatus, order.getStatus());
    }
}
