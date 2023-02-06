package ru.yandex.chemodan.app.psbilling.web;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.billing.users.AbstractPaymentTest;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.OrderProcessorFacade;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.EmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.LocalizedEmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.mocks.MailSenderMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.promos.tasks.ActivatePromoTask;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.PromoHelper;
import ru.yandex.chemodan.app.psbilling.web.config.PsBillingWebServicesConfiguration;
import ru.yandex.chemodan.app.psbilling.web.services.ProductsService;
import ru.yandex.chemodan.trust.client.responses.PaymentResponse;
import ru.yandex.chemodan.trust.client.responses.PaymentStatus;
import ru.yandex.chemodan.trust.client.responses.SubscriptionResponse;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = {
        PsBillingWebServicesConfiguration.class
})
public class PleaseComeBackPromoTests extends AbstractPaymentTest {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private PromoHelper promoHelper;
    @Autowired
    private OrderProcessorFacade processor;
    @Autowired
    private BazingaTaskManagerMock bazingaTaskManagerMock;
    @Autowired
    private EmailTemplateDao emailTemplateDao;
    @Autowired
    private MailSenderMockConfiguration httpClientMock;
    @Autowired
    private UserPromoDao userPromoDao;
    @Autowired
    private ProductsService productsService;
    @Autowired
    private FeatureFlags featureFlags;
    @Autowired
    private PromoTemplateDao promoTemplateDao;

    private UserProductPrice price;
    private PromoTemplateEntity promoTemplate;
    private EmailTemplateEntity templateEntity;
    private ProductLineEntity line;
    private UserProductEntity product;

    @Before
    public void init() {
        featureFlags.getPleaseComeBackPromoOnTuning().setValue("true");
        DateUtils.freezeTime(Instant.parse("2020-10-21"));
        TaskScheduler taskScheduler = psBillingCoreMocksConfig.getMock(TaskScheduler.class);
        bazingaTaskManagerMock.filterTaskQueue(x -> false);
        textsManagerMockConfig.turnMockOn();
        product = psBillingProductsFactory.createUserProduct(builder -> {
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
        line = psBillingProductsFactory.createProductLine("set", x -> x, product);

        initStubs(price);
        httpClientMock.mockHttpClientResponse(200, "OK");

        String emailKey = "please_come_back_email_product_set_request";
        templateEntity = emailTemplateDao.create(EmailTemplateDao.InsertData.builder()
                .args(Option.of(Cf.list("public_display_name", "user_promo_to_date")))
                .key(emailKey).description("please come back").build());
        emailTemplateDao.mergeLocalizations(Cf.list(
                new LocalizedEmailTemplateEntity(emailKey, "ru", "Disk:RussianTemplateCode")));
        promoTemplate = promoHelper.createUserPromo(x -> x.code("please_come_back_promo_product_set_request")
                .toDate(Option.of(DateUtils.futureDate()))
                .activationEmailTemplate(Option.of(emailKey)));
        promoTemplateDao.bindProductLines(promoTemplate.getId(), line.getId());

        Mockito.doCallRealMethod().when(taskScheduler).schedulePleaseComeBackTask(Mockito.any());
    }

    @Test
    public void activationTaskShouldBeQueued() {
        requestProductSet(userId);

        ListF<Tuple2<ActivatePromoTask, BazingaTaskManagerMock.TaskParams>> emailTasks =
                bazingaTaskManagerMock.findTasks(ActivatePromoTask.class);

        Assert.assertEquals(emailTasks.length(), 1);

        ActivatePromoTask sentTask = emailTasks.get(0)._1;
        BazingaTaskManagerMock.TaskParams taskScheduleParams = emailTasks.get(0)._2;
        ActivatePromoTask.Parameters taskParams = sentTask.getParametersTyped();

        Assert.equals("please_come_back_promo_product_set_request", taskParams.getPromoCode());
        Assert.equals(Instant.now().plus(Duration.standardDays(3)), taskScheduleParams.getDate());
    }

    @Test
    public void promoShouldBeActivated() {
        requestProductSet(userId);

        bazingaTaskManagerMock.executeTasks(applicationContext);
        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.equals(promoTemplate.getId(), userPromos.single().getPromoTemplateId());
        Assert.isTrue(userPromos.single().isActive());
    }

    @Test
    public void promoEmailShouldBeSent() {
        requestProductSet(userId);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        HttpUriRequest request = mailSenderMockConfig.verifyEmailSent();
        Assert.assertContains(request.getURI().toString(), "Disk");
        Assert.assertContains(request.getURI().toString(), "RussianTemplateCode");
        Assert.assertContains(request.getURI().toString(), userId.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void promoEmailShouldBeSent_WithAllParamsHandled() throws IOException {
        requestProductSet(userId);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        HttpPost request = mailSenderMockConfig.verifyEmailSent();
        JsonNode jsonArgs =
                new ObjectMapper().readValue(EntityUtils.toString(request.getEntity()), ObjectNode.class).get("args");
        MapF<String, Object> args = Cf.toHashMap(new JsonMapper().treeToValue(jsonArgs, Map.class));

        Assert.equals(args.keys(), templateEntity.getArgs());
        Assert.equals("21 октября 2020 года", args.getTs("user_promo_to_date"));
        Assert.equals("feelgood t.", args.getTs("public_display_name"));
    }

    @Test
    public void promoShouldNotBeActivated_WhenGotActivePromo() {
        requestProductSet(userId);

        PromoTemplateEntity globalPromo = promoHelper.createGlobalPromo();
        promoTemplateDao.bindProductLines(globalPromo.getId(), line.getId());
        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
    }

    @Test
    public void promoShouldNotBeActivated_WhenGotPaidProduct() {
        Order order = createOrder(price);
        processPaid(order);

        requestProductSet(userId);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void promoShouldNotBeActivated_WhenPromoAlreadyUsed() {
        userPromoDao.createOrUpdate(UserPromoDao.InsertData.builder()
                .uid(userId)
                .fromDate(Instant.now())
                .promoTemplateId(promoTemplate.getId())
                .promoStatusType(PromoStatusType.USED)
                .build());

        requestProductSet(userId);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.equals(PromoStatusType.USED, userPromos.single().getStatus());

        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void activationTaskShouldNotBeSent_WhenPromoAlreadyActivated() {
        UserPromoDao.InsertData insertData = UserPromoDao.InsertData.builder()
                .uid(userId)
                .fromDate(Instant.now())
                .promoTemplateId(promoTemplate.getId())
                .promoStatusType(PromoStatusType.ACTIVE)
                .build();
        userPromoDao.createOrUpdate(insertData);

        requestProductSet(userId);

        DateUtils.freezeTime(DateUtils.futureDate());
        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.equals(PromoStatusType.ACTIVE, userPromos.single().getStatus());
        Assert.equals(insertData.getFromDate(), userPromos.single().getFromDate());

        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void activationTaskShouldNotBeSent_WhenPromoExpired() {
        requestProductSet(userId);

        DateUtils.freezeTime(promoTemplate.getToDate().get().plus(1));
        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void promoShouldNotBeActivatedOnNotPaidOrder() {
        Order order = createOutdatedOrder(price);
        processNotPaid(order);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
    }

    @Test
    public void promoShouldBeActivatedOnProductSetRequestBeforeNotPaidOrder() {
        requestProductSet(userId);
        Order order = createOutdatedOrder(price);
        processNotPaid(order);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(1, userPromos.length());
        Assert.equals(promoTemplate.getId(), userPromos.single().getPromoTemplateId());
        Assert.isTrue(userPromos.single().isActive());
    }

    @Test
    public void promoShouldNotBeActivatedOnRequestWithoutPromoActivation() {
        requestProductSet(userId, false);

        bazingaTaskManagerMock.executeWhileGotTasks(applicationContext);

        ListF<UserPromoEntity> userPromos = userPromoDao.findUserPromos(userId);
        Assert.equals(0, userPromos.length());
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

        bazingaTaskManagerMock.suppressNewTasksAdd();
        processor.processByOrderId(order.getId());
        bazingaTaskManagerMock.allowNewTasksAdd();

        Order orderNew = orderDao.findById(order.getId());
        assertEquals(OrderStatus.PAID, orderNew.getStatus());
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
        Assert.equals(OrderStatus.ERROR, orderNew.getStatus());
        Assert.equals("payment_not_created", orderNew.getTrustErrorCode().get());
    }

    private void requestProductSet(PassportUid uid) {
        requestProductSet(uid, true);
    }

    private void requestProductSet(PassportUid uid, boolean promoActivation) {
        productsService.getProductSetForUpgrade(Option.of(uid),
                "set", Option.empty(), Option.empty(), Option.empty(), false, promoActivation, Option.empty(),
                Option.empty(), Cf.list());
    }
}
