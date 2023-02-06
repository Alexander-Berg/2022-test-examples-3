package ru.yandex.chemodan.app.psbilling.core.admin;

import java.util.List;
import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingDBTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingOrdersFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.admin.InappMigrationService.MigrationStatus;
import ru.yandex.chemodan.app.psbilling.core.admin.task.InappMigrationTask;
import ru.yandex.chemodan.app.psbilling.core.billing.users.PaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.billing.users.UserBillingService;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.InappSubscriptionProcessor;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.dao.admin.InappMigrationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserInfoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.admin.InappMigration;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.users.UserServiceManager;
import ru.yandex.chemodan.trust.client.TrustClient;
import ru.yandex.chemodan.trust.client.responses.GetPaymentMethodsResponse;
import ru.yandex.chemodan.trust.client.responses.PaymentMethod;
import ru.yandex.commune.dynproperties.DynamicProperty;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;

@ContextConfiguration(classes = {InappMigrationServiceTest.MocksConfiguration.class})
public class InappMigrationServiceTest extends AbstractPsBillingDBTest {
    @Autowired
    InappMigrationService inappMigrationService;
    @Autowired
    PsBillingOrdersFactory ordersFactory;
    @Autowired
    PsBillingUsersFactory usersFactory;
    @Autowired
    UserProductManager userProductManager;
    @Autowired
    InappSubscriptionProcessor inappSubscriptionProcessor;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private JdbcTemplate3 jdbcTemplate;
    @Autowired
    private InappMigrationDao inappMigrationDao;
    @Autowired
    private TrustClient trustClientMock;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    TaskScheduler taskSchedulerMock;
    @Autowired
    PsBillingCoreMocksConfig psBillingCoreMocksConfig;
    @Autowired
    UserBillingService userBillingServiceMock;
    @Autowired
    UserServiceManager userServiceManager;

    private static final String INAPP_PERIOD_CODE = "mail_pro_b2c_standard100_inapp_apple_for_disk_month";
    private static final String WEB_PERIOD_CODE = "PS_BILLING_mail_pro_b2c_standard100_v20210610_month_subs";
    private static final String WEB_DISCOUNT_PERIOD_CODE = "PS_BILLING_mail_pro_b2c_standard100_start_discount_50_1m_month_subs";
    private final PassportUid uid = PassportUid.cons(1);
    private final PaymentMethod paymentMethod1 = new PaymentMethod(null, 225, "card", "visa", "pid1", "123****11",
            false);
    private final PaymentMethod paymentMethod2 = new PaymentMethod(null, 225, "card", "visa", "pid2", "123****11",
            false);
    private static final String NEW_ORDER_TRUST_ID_1 = "newOrderTrustId1";
    private static final String NEW_ORDER_TRUST_ID_2 = "newOrderTrustId2";
    private static final String INAPP_ORDER_TRUST_ID = "INAPP_ORDER_TRUST_ID";

    @Before
    public void setup() {
        Assert.assertTrue(MockUtil.isMock(userBillingServiceMock));
        taskSchedulerMock = psBillingCoreMocksConfig.getMock(TaskScheduler.class);
        inappMigrationService.setMigrationMaxPurchaseDate(new DynamicProperty<>("", Instant.now().plus(Duration.standardDays(1))));
        inappMigrationService.setMaxOrderDateForDiscount(new DynamicProperty<>("", Instant.now().minus(Duration.standardDays(1))));
    }


    @Test
    public void loadTestDirectCase() {
        loadTestImpl(MigrationStatus.SCHEDULED, Cf.list(paymentMethod1), "225");
        Mockito.verify(taskSchedulerMock, Mockito.times(1)).schedule(Mockito.any(InappMigrationTask.class), Mockito.any());
        Mockito.verify(taskSchedulerMock, Mockito.times(1))
                .scheduleLocalizedEmailTask(Mockito.eq(InappMigrationService.INAPP_MIGRATION_SCHEDULED_MAIL_KEY), Mockito.any());
    }

    @Test
    public void loadTestDiscount() {
        inappMigrationService.setMaxOrderDateForDiscount(new DynamicProperty<>("", Instant.now().plus(Duration.standardDays(1))));
        loadTestImpl(MigrationStatus.SCHEDULED, Cf.list(paymentMethod1), "225", WEB_DISCOUNT_PERIOD_CODE);
        Mockito.verify(taskSchedulerMock, Mockito.times(1)).schedule(Mockito.any(InappMigrationTask.class), Mockito.any());
        Mockito.verify(taskSchedulerMock, Mockito.times(1))
                .scheduleLocalizedEmailTask(Mockito.eq(InappMigrationService.INAPP_MIGRATION_SCHEDULED_MAIL_KEY), Mockito.any());
    }

    @Test
    public void loadTestNoPaymentMethods() {
        loadTestImpl(MigrationStatus.ERROR_NO_PAYMENT_METHODS, Cf.list(), "225");
        Mockito.verifyZeroInteractions(taskSchedulerMock);
    }

    @Test
    public void loadTestNoRegion() {
        loadTestImpl(MigrationStatus.ERROR_NO_REGION, Cf.list(paymentMethod1), null);
        Mockito.verifyZeroInteractions(taskSchedulerMock);
    }

    @Test
    public void loadTestWrongRegion() {
        loadTestImpl(MigrationStatus.ERROR_NOT_AVAILABLE_REGION, Cf.list(paymentMethod1), "11111");
        Mockito.verifyZeroInteractions(taskSchedulerMock);
    }

    @Test
    public void loadTestAfterMaxDate() {
        inappMigrationService.setMigrationMaxPurchaseDate(new DynamicProperty<>("", Instant.now().minus(Duration.standardDays(1))));
        prepareLoadTest();
        inappMigrationService.loadMigrationCandidates(false);
        Assert.assertTrue(inappMigrationDao.findAll().isEmpty());
    }

    @Test
    public void subscribeTestDirectCase() {
        loadTestDirectCase();
        subscribeTestImpl(MigrationStatus.MIGRATED, OrderStatus.PAID, OrderStatus.PAID,
                InappMigrationService.INAPP_MIGRATION_SUCCEED_MAIL_KEY);
    }

    @Test
    public void subscribeTestNoMoreHold() {
        loadTestDirectCase();
        orderDao.unHoldOrder(orderDao.findByTrustOrderId(INAPP_ORDER_TRUST_ID).get().getId());
        subscribeTestImpl(MigrationStatus.SUBSCRIPTION_CANCELED_NOT_ON_HOLD, OrderStatus.PAID, OrderStatus.PAID, null);
    }

    @Test
    public void subscribeTestActiveSubscriptionAppears() {
        loadTestDirectCase();
        UserServiceEntity userService = usersFactory.createUserService(Target.ENABLED, ordersFactory.createOrder(uid));
        try {
            subscribeTestImpl(MigrationStatus.SUBSCRIPTION_CANCELED_ACTIVE_SUBSCRIPTION_APPEARS, OrderStatus.PAID,
                    OrderStatus.PAID, null);
        } finally {
            //cleanup
            userServiceManager.disableService(userService.getId());
        }
    }

    @Test
    public void subscribeTestNoPaymentExists() {
        loadTestDirectCase();
        Mockito.when(trustClientMock.getPaymentMethods(Mockito.any())).thenReturn(new GetPaymentMethodsResponse(Cf.list(), null));
        subscribeTestImpl(MigrationStatus.SUBSCRIPTION_FAILED_NO_PAYMENT_METHODS, OrderStatus.PAID, OrderStatus.PAID,
                InappMigrationService.INAPP_MIGRATION_FAILED_MAIL_KEY);
    }

    @Test
    public void subscribeTestAllPaymentsFailed() {
        loadTestDirectCase();
        subscribeTestImpl(MigrationStatus.SUBSCRIPTION_FAILED_ALL_PAYMENT_METHODS_FAILED, OrderStatus.ERROR,
                OrderStatus.ERROR, InappMigrationService.INAPP_MIGRATION_FAILED_MAIL_KEY);
    }

    @Test
    public void subscribeTestFirstAttemptFailedButSecondOk() {
        loadTestDirectCase();
        Mockito.when(trustClientMock.getPaymentMethods(Mockito.any()))
                .thenReturn(new GetPaymentMethodsResponse(Cf.list(paymentMethod1, paymentMethod2), null));
        subscribeTestImpl(MigrationStatus.MIGRATED, OrderStatus.ERROR, OrderStatus.PAID,
                InappMigrationService.INAPP_MIGRATION_SUCCEED_MAIL_KEY);
    }

    @Test
    public void doNotSubscribeNotScheduled() {
        loadTestDirectCase();
        InappMigration inappMigration = inappMigrationDao.findAll().get(0);
        inappMigrationDao.setStatus(inappMigration.getId(), MigrationStatus.INIT);
        subscribeTestImpl(MigrationStatus.INIT, OrderStatus.ERROR, OrderStatus.PAID,null);
    }

    private void subscribeTestImpl(MigrationStatus expectedStatus, OrderStatus firstOrderResult,
                                   OrderStatus secondOrderResult,
                                   String expectedMail) {
        Mockito.reset(taskSchedulerMock);
        InappMigration inappMigration = inappMigrationDao.findAll().get(0);
        Order mockOrder = Mockito.mock(Order.class);
        Mockito.when(mockOrder.getTrustOrderId()).thenReturn(NEW_ORDER_TRUST_ID_1, NEW_ORDER_TRUST_ID_2);
        Mockito.when(userBillingServiceMock.initPayment(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.eq(false), Mockito.eq(true), Mockito.eq(true)))
        .thenReturn(new PaymentInfo(mockOrder, ""));
        MapF<String, OrderStatus> orderStatuses = Cf.map(NEW_ORDER_TRUST_ID_1, firstOrderResult,
                NEW_ORDER_TRUST_ID_2, secondOrderResult);
        Mockito.doAnswer((invocation) -> {
            String orderId = invocation.getArgument(0);
            ordersFactory.createOrUpdateOrder(uid,
                    userProductManager.findPeriodByCode(WEB_PERIOD_CODE).getPrices().get(0).getId(), orderId,
                    builder -> builder.status(Option.of(orderStatuses.getTs(orderId))));
            return null;
        }).when(userBillingServiceMock).checkOrder(NEW_ORDER_TRUST_ID_1);


        ordersFactory.createOrUpdateOrder(uid,
                userProductManager.findPeriodByCode(WEB_PERIOD_CODE).getPrices().get(0).getId(), NEW_ORDER_TRUST_ID_2,
                builder -> builder.status(Option.of(secondOrderResult)));

        inappMigrationService.subscribe(inappMigration);
        inappMigration = inappMigrationDao.findAll().get(0);
        System.out.println(inappMigration);
        Assert.assertTrue(inappMigration.getStatus().startsWith(expectedStatus.name()));
        if (expectedMail != null)
            Mockito.verify(taskSchedulerMock, Mockito.times(1))
                    .scheduleLocalizedEmailTask(Mockito.eq(expectedMail), Mockito.any());
        else
            Mockito.verifyZeroInteractions(taskSchedulerMock);
    }

    private void prepareLoadTest() {
        UUID priceId = userProductManager.findPeriodByCode(INAPP_PERIOD_CODE).getPrices().get(0).getId();
        Order order = ordersFactory.createOrUpdateOrder(
                uid, priceId, INAPP_ORDER_TRUST_ID, builder -> builder.type(OrderType.INAPP_SUBSCRIPTION));
        UserServiceEntity userService = usersFactory.createUserService(Target.DISABLED, order);
        orderDao.onSuccessfulOrderPurchase(order.getId(), Option.of(userService.getId()), 1);
        orderDao.holdOrder(order.getId());
        Mockito.reset(trustClientMock);
        jdbcTemplate.update("UPDATE user_services set actual_disabled_at = now() where id = ?", userService.getId());
        jdbcTemplate.update("delete from " + inappMigrationDao.getTableName());
    }


    private void loadTestImpl(MigrationStatus expectedStatus, List<PaymentMethod> paymentMethods, String regionId) {
        loadTestImpl(expectedStatus, paymentMethods, regionId, WEB_PERIOD_CODE);
    }

    private void loadTestImpl(MigrationStatus expectedStatus, List<PaymentMethod> paymentMethods, String regionId,
                              String expectedCode) {
        prepareLoadTest();
        Mockito.when(trustClientMock.getPaymentMethods(Mockito.any())).thenReturn(new GetPaymentMethodsResponse(paymentMethods, null));
        userInfoDao.createOrUpdate(UserInfoDao.InsertData.builder()
                .uid(uid)
                .regionId(Option.ofNullable(regionId))
                .build());
        inappMigrationService.loadMigrationCandidates(false);
        InappMigration migration = inappMigrationDao.findAll().get(0);
        System.out.println(migration);
        Assert.assertTrue(migration.getStatus().startsWith(expectedStatus.name()));
        Assert.assertEquals(migration.getTargetProductPeriodCode().get(), expectedCode);
    }

    @Configuration
    public static class MocksConfiguration {
        @Autowired
        private PsBillingCoreMocksConfig mocksConfig;

        @Primary
        @Bean
        public UserBillingService userBillingService() {
            return mocksConfig.addMock(UserBillingService.class);
        }
    }
}
