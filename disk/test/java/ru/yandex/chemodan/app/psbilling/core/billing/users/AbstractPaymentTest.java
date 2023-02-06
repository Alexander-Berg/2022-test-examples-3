package ru.yandex.chemodan.app.psbilling.core.billing.users;

import java.math.BigDecimal;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.InappSubscriptionProcessor;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.OrderProcessorFacade;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.TrustSubscriptionProcessor;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductLineDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductSetDao;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.mocks.TrustClientMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.services.balance.BalanceServiceTest;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.users.UserService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.trust.client.InappStoreType;
import ru.yandex.chemodan.trust.client.TrustClient;
import ru.yandex.chemodan.trust.client.requests.CreateOrderRequest;
import ru.yandex.chemodan.trust.client.requests.CreatePaymentRequest;
import ru.yandex.chemodan.trust.client.requests.PaymentRequest;
import ru.yandex.chemodan.trust.client.responses.InappSubscription;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionState;
import ru.yandex.chemodan.trust.client.responses.PaymentResponse;
import ru.yandex.inside.passport.PassportUid;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public abstract class AbstractPaymentTest extends AbstractPsBillingCoreTest {
    protected static final PassportUid userId = PassportUid.cons(BalanceServiceTest.TEST_UID);

    @Autowired
    protected TrustClient trustClient;
    @Autowired
    protected UserBillingService billingService;
    @Autowired
    protected ProductLineDao productLineDao;
    @Autowired
    protected ProductSetDao productSetDao;
    @Autowired
    protected UserProductManager userProductManager;
    @Autowired
    protected InappSubscriptionProcessor inappSubscriptionProcessor;
    @Autowired
    protected TrustSubscriptionProcessor trustSubscriptionProcessor;
    @Autowired
    protected OrderProcessorFacade orderProcessorFacade;
    @Autowired
    protected TrustClientMockConfiguration trustClientMockConfiguration;

    @After
    public void after() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    protected void initStubs(UserProductPrice price) {
        Mockito.reset(trustClient);
        Mockito.when(trustClient.createSubscription(any())).thenAnswer(invocation -> {
            CreateOrderRequest request = (CreateOrderRequest) invocation.getArguments()[0];
            assertEquals(Integer.valueOf(111), request.getTrustServiceId());
            assertEquals("127.0.0.1", request.getUserIp());
            assertEquals("225", request.getRegionId());
            assertEquals(price.getPeriod().getTrustProductId(true), request.getProductId());
            assertEquals(userId, request.getUid());
            assertTrue(((String) request.getDeveloperPayload().get("tariff_name")).startsWith("Тестовый ключ"));

            return request.getOrderId();
        });

        Mockito.when(trustClient.createPayment(any())).thenAnswer(invocation -> {
            CreatePaymentRequest request = (CreatePaymentRequest) invocation.getArguments()[0];
            assertEquals("127.0.0.1", request.getUserIp());
            assertEquals("ru", request.getDomainSuffix());
            assertEquals(Integer.valueOf(111), request.getTrustServiceId());
            MatcherAssert.assertThat(request.getNotificationUrl(),
                    allOf(startsWith("https://ps-billing-web.qloud.dst.yandex.net/v1/orders/"), endsWith("/notify")));
            MatcherAssert.assertThat(request.getReturnPath(), startsWith("http://ya.ru/"));
            assertEquals(userId, request.getUid());
            assertEquals("desktop/form", request.getTemplateTag());
            assertEquals("trust_web_page", request.getPaymentMethod());
            assertEquals(1, request.getOrders().size());

            return "unique_purchase_token";
        });

        Mockito.when(trustClient.startPayment(any())).thenAnswer(invocation -> {
            PaymentRequest request = (PaymentRequest) invocation.getArguments()[0];
            assertEquals("127.0.0.1", request.getUserIp());
            assertEquals("225", request.getRegionId());
            assertEquals(userId, request.getUid());
            assertEquals(Integer.valueOf(111), request.getTrustServiceId());
            assertEquals("unique_purchase_token", request.getPurchaseToken());

            return PaymentResponse.builder()
                    .paymentUrl("http://some.unique/url")
                    .amount(BigDecimal.ZERO)
                    .build();
        });
        textsManagerMockConfig.turnMockOn();
    }

    protected Order createOrder(UserProductPrice price) {
        return createOrder(price.getPeriod().getTrustProductId(true), Option.of(price.getCurrencyCode()));
    }

    protected Order createOrder(String trustProductId, Option<String> currency) {
        PaymentInfo paymentInfo = billingService.initPayment(userId,
                "127.0.0.1", "ru", "http://ya.ru/!ORDER_ID!", TrustFormTemplate.DESKTOP,
                trustProductId,
                Option.of("ru"), currency, true, Option.empty(), true);
        assertEquals(paymentInfo.getPaymentUrl(), "http://some.unique/url");

        return paymentInfo.getOrder();
    }

    protected Order createOutdatedOrder(UserProductPrice price) {
        Duration shift = Duration.standardDays(1);
        DateUtils.shiftTimeBack(shift);
        Order order = createOrder(price);
        DateUtils.shiftTime(shift);
        return order;
    }

    protected void mockGetInappSubscription(
            Order order, InappSubscriptionState state) {
        mockGetInappSubscription(order, state, x -> x);
    }

    protected void mockGetInappSubscription(
            Order order, InappSubscriptionState state,
            Function<InappSubscription.InappSubscriptionBuilder, InappSubscription.InappSubscriptionBuilder> customizer) {
        UserProductPrice price = userProductManager.findPrice(order.getUserProductPriceId());
        InappStoreType storeType;
        switch (price.getPeriod().getUserProduct().getBillingType()) {
            case INAPP_GOOGLE:
                storeType = InappStoreType.GOOGLE_PLAY;
                break;
            case INAPP_APPLE:
                storeType = InappStoreType.APPLE_APPSTORE;
                break;
            default:
                throw new IllegalStateException();
        }
        trustClientMockConfiguration.mockGetInappSubscription(x ->
                customizer.apply(
                        x.uid(order.getUid())
                                .storeType(storeType)
                                .productId(price.getPeriod().getCode())
                                .subscriptionId(order.getTrustOrderId())
                                .state(state)));
    }

    protected Order assertOrderStatus(Order order, OrderStatus status) {
        order = orderDao.findById(order.getId());
        Assert.assertEquals(status, order.getStatus());
        return order;
    }

    protected UserService assertOrderServiceStatus(Order order, Target status) {
        order = orderDao.findById(order.getId());
        UserService service = userServiceManager.findById(order.getUserServiceId().get());
        Assert.assertEquals(status, service.getTarget());
        return service;
    }

    protected void assertOrderServiceNotExist(Order order) {
        order = orderDao.findById(order.getId());
        Assert.assertFalse(order.getUserServiceId().isPresent());
    }
}
