package ru.yandex.chemodan.app.psbilling.core.promos;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.billing.users.AbstractPaymentTest;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.OrderProcessorFacade;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.LocalizedEmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.mail.EventMailType;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.BaseEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.trust.client.responses.PaymentResponse;
import ru.yandex.chemodan.trust.client.responses.PaymentStatus;
import ru.yandex.chemodan.trust.client.responses.SubscriptionResponse;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public class AbandonedCartTests extends AbstractPaymentTest {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private OrderProcessorFacade processor;
    @Autowired
    private BazingaTaskManagerMock bazingaTaskManagerMock;
    @Autowired
    private EmailTemplateDao emailTemplateDao;
    @Autowired
    private HttpClient httpClientMock;

    private UserProductPrice price;

    @Before
    public void init() {
        DateUtils.freezeTime();
        TaskScheduler taskScheduler = psBillingCoreMocksConfig.getMock(TaskScheduler.class);
        bazingaTaskManagerMock.filterTaskQueue(x -> false);
        UserProductEntity product = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code("TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.singleton(false);
            builder.trustServiceId(Option.of(111));
            builder.trustSubsChargingRetryDelay(Option.of("1D"));
            builder.trustSubsChargingRetryLimit(Option.of("2D"));
            builder.trustSubsGracePeriod(Option.of("2D"));

            return builder;
        });
        psBillingTextsFactory.loadTranslations();
        price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(product, CustomPeriodUnit.TEN_MINUTES).getId());
        psBillingProductsFactory.addUserProductToProductSet("set", product.getId());

        initStubs(price);
        mailSenderMockConfig.mockHttpClientResponse(200, "OK");

        String emailKey = "abandoned_cart";
        emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .key(emailKey).description("abandoned cart").build());
        emailTemplateDao.mergeLocalizations(Cf.list(
                new LocalizedEmailTemplateEntity(emailKey, "ru", "Disk:RussianTemplateCode")));
        Mockito.doCallRealMethod().when(taskScheduler).scheduleAbandonedCartEmailTask(Mockito.any(), Mockito.any());
    }

    @Test
    public void abandonedCartEmailShouldBeSent() {
        Order order = createOutdatedOrder(price);
        processNotPaid(order);

        ListF<Tuple2<SendLocalizedEmailTask, BazingaTaskManagerMock.TaskParams>> emailTasks =
                findBazingaAbandonedTasksWithParams();
        Assert.assertEquals(emailTasks.length(), 1);

        SendLocalizedEmailTask sentTask = emailTasks.get(0)._1;
        BazingaTaskManagerMock.TaskParams taskScheduleParams = emailTasks.get(0)._2;
        BaseEmailTask.Parameters taskParams = sentTask.getParametersTyped();

        Assert.equals("abandoned_cart", taskParams.getEmailKey());
        Assert.equals(Instant.now().plus(Duration.standardHours(1)), taskScheduleParams.getDate());
    }

    @Test
    public void abandonedCartEmailCooldown() throws IOException {
        Order order = createOutdatedOrder(price);
        processNotPaid(order);
        Assert.equals(1, findBazingaAbandonedTasks().length());

        bazingaTaskManagerMock.executeTasks(applicationContext);
        Assert.equals(0, findBazingaAbandonedTasks().length());

        Mockito.verify(httpClientMock, Mockito.times(1))
                .execute(any(), Mockito.any(ResponseHandler.class));
        Mockito.clearInvocations(httpClientMock);

        order = createOutdatedOrder(price);
        processNotPaid(order);
        Assert.equals(1, findBazingaAbandonedTasks().length());

        // email not sent cause of email cooldown
        bazingaTaskManagerMock.executeTasks(applicationContext);
        Mockito.verify(httpClientMock, Mockito.never())
                .execute(any(), Mockito.any(ResponseHandler.class));
        Assert.equals(0, findBazingaAbandonedTasks().length());

        // cooldown passed
        DateUtils.freezeTime(Instant.now().plus(Duration.standardDays(1)).plus(1));
        order = createOutdatedOrder(price);
        processNotPaid(order);
        Assert.equals(1, findBazingaAbandonedTasks().length());
        bazingaTaskManagerMock.executeTasks(applicationContext);
        Mockito.verify(httpClientMock, Mockito.times(1))
                .execute(any(), Mockito.any(ResponseHandler.class));
        Mockito.clearInvocations(httpClientMock);
    }

    @Test
    public void abandonedCartEmailNotQueued_ForPaidOrder() {
        Order order = createOrder(price);
        processPaid(order);
        Assert.equals(0, findBazingaAbandonedTasks().length());
    }

    @Test
    public void abandonedCartEmailNotQueued_ForPaidUser() {
        Order order = createOrder(price);
        processPaid(order);

        order = createOutdatedOrder(price);
        processNotPaid(order);
        Assert.equals(0, findBazingaAbandonedTasks().length());
    }

    @Test
    public void abandonedCartEmailNotQueued_ForUserWithActiveOrder() {
        Order order = createOutdatedOrder(price);
        createOutdatedOrder(price);
        createOrder(price);

        processNotPaid(order);
        Assert.equals(0, findBazingaAbandonedTasks().length());
    }

    @Test
    public void abandonedCartEmailNotSent_ForPaidUser() throws IOException {
        Order order = createOutdatedOrder(price);

        processNotPaid(order);
        Assert.equals(1, findBazingaAbandonedTasks().length());

        order = createOrder(price);
        processPaid(order);

        bazingaTaskManagerMock.executeTasks(applicationContext);
        Mockito.verify(httpClientMock, Mockito.never())
                .execute(any(), Mockito.any(ResponseHandler.class));
    }

    @Test
    public void abandonedCartEmailNotSent_ForUserWithActiveOrder() throws IOException {
        Order order = createOutdatedOrder(price);

        processNotPaid(order);
        Assert.equals(1, findBazingaAbandonedTasks().length());

        createOrder(price);

        bazingaTaskManagerMock.executeTasks(applicationContext);
        Mockito.verify(httpClientMock, Mockito.never())
                .execute(any(), Mockito.any(ResponseHandler.class));
    }

    private void processNotPaid(Order order) {
        Mockito.when(trustClient.getSubscription(any()))
                .thenAnswer(invocation ->
                        SubscriptionResponse.builder()
                                .subscriptionUntil(Instant.now().minus(Duration.standardSeconds(1)))
                                .paymentIds(Cf.list())
                                .subscriptionPeriodCount(0)
                                .currentAmount(new String[][]{{"10", "RUB"}})
                                .build());

        processor.processByOrderId(order.getId());

        Order orderNew = orderDao.findById(order.getId());
        assertEquals(OrderStatus.ERROR, orderNew.getStatus());
        assertEquals("payment_not_created", orderNew.getTrustErrorCode().get());
        filterWrongTasks();
    }

    private void processPaid(Order order) {
        String lastPaymentPurchaseToken = UUID.randomUUID().toString();
        Mockito.when(trustClient.getSubscription(any()))
                .thenAnswer(invocation -> SubscriptionResponse.builder()
                        .subscriptionUntil(Instant.now().plus(Duration.standardDays(30)))
                        .paymentIds(Cf.list(lastPaymentPurchaseToken))
                        .subscriptionPeriodCount(1)
                        .subscriptionState(3)
                        .currentAmount(new String[][]{{"10", "RUB"}})
                        .build());

        Mockito.when(trustClient.getPayment(any()))
                .thenAnswer(invocation ->
                        PaymentResponse.builder()
                                .paymentStatus(PaymentStatus.cleared)
                                .build());

        processor.processByOrderId(order.getId());

        Order orderNew = orderDao.findById(order.getId());
        assertEquals(OrderStatus.PAID, orderNew.getStatus());
        filterWrongTasks();
    }

    private ListF<Tuple2<SendLocalizedEmailTask, BazingaTaskManagerMock.TaskParams>> findBazingaAbandonedTasksWithParams() {
        return bazingaTaskManagerMock.tasksWithParams
                .filter(x -> x._1 instanceof SendLocalizedEmailTask)
                .map(x -> Tuple2.tuple((SendLocalizedEmailTask) x._1, x._2));
    }

    private ListF<SendLocalizedEmailTask> findBazingaAbandonedTasks() {
        return findBazingaAbandonedTasksWithParams().map(x -> x._1);
    }

    private void filterWrongTasks() {
        bazingaTaskManagerMock.filterTaskQueue(x -> x instanceof SendLocalizedEmailTask
                && Arrays.stream(EventMailType.values()).noneMatch(
                eventType -> eventType.toString()
                        .equals(((SendLocalizedEmailTask) x).getParametersTyped().getEmailKey())));
    }
}
