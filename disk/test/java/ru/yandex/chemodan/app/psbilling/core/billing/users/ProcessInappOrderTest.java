package ru.yandex.chemodan.app.psbilling.core.billing.users;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductSetEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.trust.client.TrustException;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionState;
import ru.yandex.inside.passport.PassportUid;

public class ProcessInappOrderTest extends AbstractPaymentTest {
    private static PassportUid userId = PassportUid.MAX_VALUE;

    UserProductEntity inappProduct;
    UserProductPrice inappProductPrice;
    UserProductEntity webProduct;
    UserProductPrice webProductPrice;

    @Before
    public void Init() {
        inappProduct = psBillingProductsFactory.createUserProduct(x -> x.billingType(BillingType.INAPP_APPLE));
        inappProductPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(inappProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        psBillingProductsFactory.addUserProductToProductSet("set", inappProduct);

        webProduct = psBillingProductsFactory.createUserProduct(x -> x.billingType(BillingType.TRUST));
        webProductPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(webProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        psBillingProductsFactory.addUserProductToProductSet("set", webProduct);

        userProductBucketDao.addToBucket(UserProductBucketDao.InsertData.builder()
                .code("someCode").productSetId(productSetDao.findByKey("set").map(ProductSetEntity::getId)).build());
    }

    @Test
    // CHEMODAN-78001: Пользователь подключил подписку повторно
    // такая ситуация уже практически невозможна, но отголоски прошлых подписок нас преследуют CHEMODAN-78202
    public void allowToBuySecondProductWhenWasOnHold() {
        // пользователь купил продукт в appstore
        Order inappOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, inappProductPrice.getId(), "inapp_order");

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);

        // затем подписка ушла в холд
        mockGetInappSubscription(inappOrder, InappSubscriptionState.ON_HOLD);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.ON_HOLD);
        assertOrderServiceStatus(inappOrder, Target.DISABLED);

        // пользователь купил веб подписку
        Order webOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, webProductPrice.getId(), "web_order");
        trustClientMockConfiguration.mockGetSubscriptionPaid();
        trustSubscriptionProcessor.processOrder(webOrder);
        assertOrderStatus(webOrder, OrderStatus.PAID);
        assertOrderServiceStatus(webOrder, Target.ENABLED);

        // а затем подписка из эпстора вернулась в paid
        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);

        // веб подписку при этом не трогаем. Если что, сапорт рефандит ее
        assertOrderStatus(webOrder, OrderStatus.PAID);
        assertOrderServiceStatus(webOrder, Target.ENABLED);
    }


    @Test
    // CHEMODAN-77991: Order не может выйти из статуса ON_HOLD
    public void orderUnHold_Active() {
        Order inappOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, inappProductPrice.getId(), "inapp_order");

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ON_HOLD);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.ON_HOLD);
        assertOrderServiceStatus(inappOrder, Target.DISABLED);

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);
    }

    @Test
    // CHEMODAN-77991: Order не может выйти из статуса ON_HOLD
    public void orderUnHold_Finished() {
        Order inappOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, inappProductPrice.getId(), "inapp_order");

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ON_HOLD);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.ON_HOLD);
        assertOrderServiceStatus(inappOrder, Target.DISABLED);

        mockGetInappSubscription(inappOrder, InappSubscriptionState.FINISHED);
        inappSubscriptionProcessor.processOrder(inappOrder);
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.DISABLED);
    }

    @Test
    public void shouldResyncOldSubscriptions() {
        Order inappOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, inappProductPrice.getId(), "inapp_order");

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE, x -> x.syncTime(Instant.now()));
        inappSubscriptionProcessor.processOrder(inappOrder);
        Mockito.verify(trustClientMockConfiguration.getMock(), Mockito.times(0))
                .resyncInappSubscription(Mockito.any());

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE, x -> x.syncTime(DateUtils.farPastDate()));
        inappSubscriptionProcessor.processOrder(inappOrder);
        Mockito.verify(trustClientMockConfiguration.getMock(), Mockito.times(1))
                .resyncInappSubscription(Mockito.any());
    }

    @Test
    public void shouldFinishExpiredSubscriptions() {
        Order inappOrder = createActiveSubscription();

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE,
                x -> x.syncTime(DateUtils.farPastDate())
                        .storeExpirationTime(DateUtils.farFutureDate())
                        .subscriptionUntil(DateUtils.farFutureDate()));
        trustClientMockConfiguration.mockResyncError(new TrustException(Option.of(HttpStatus.BAD_REQUEST), "error",
                "play_subscription_not_found", null));
        inappSubscriptionProcessor.processOrder(inappOrder);
        Mockito.verify(trustClientMockConfiguration.getMock(), Mockito.times(1))
                .resyncInappSubscription(Mockito.any());
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.DISABLED);
    }

    @Test
    public void shouldFinishExpiredInvalidReceiptSubscriptions() {
        Order inappOrder = createActiveSubscription();

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE,
                x -> x.syncTime(DateUtils.farPastDate())
                        .subscriptionUntil(DateUtils.farPastDate()));
        trustClientMockConfiguration.mockResyncError(new TrustException(Option.of(HttpStatus.BAD_REQUEST), "error",
                "invalid_receipt", null));
        inappSubscriptionProcessor.processOrder(inappOrder);
        Mockito.verify(trustClientMockConfiguration.getMock(), Mockito.times(1))
                .resyncInappSubscription(Mockito.any());
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.DISABLED);
    }

    @Test
    public void shouldNotFinishNotExpiredInvalidReceiptSubscriptions() {
        Order inappOrder = createActiveSubscription();

        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE,
                x -> x.syncTime(DateUtils.farPastDate())
                        .subscriptionUntil(DateUtils.futureDate()));
        trustClientMockConfiguration.mockResyncError(new TrustException(Option.of(HttpStatus.BAD_REQUEST), "error",
                "invalid_receipt", null));
        inappSubscriptionProcessor.processOrder(inappOrder);
        Mockito.verify(trustClientMockConfiguration.getMock(), Mockito.times(1))
                .resyncInappSubscription(Mockito.any());
        assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);
    }

    @Test
    public void updateOrderSyncDateOnProcessing() {
        Order order = psBillingOrdersFactory.createOrUpdateOrder(userId, inappProductPrice.getId(), "inapp_order");
        Assert.assertEquals(Option.empty(), order.getInappSynchronizationDate());
        Instant syncTime = Instant.now();
        mockGetInappSubscription(order, InappSubscriptionState.ACTIVE,
                x -> x.syncTime(syncTime)
                        .storeExpirationTime(DateUtils.farFutureDate())
                        .subscriptionUntil(DateUtils.farFutureDate()));
        inappSubscriptionProcessor.processOrder(order);
        Order processedOrder = orderDao.findById(order.getId());
        Assert.assertEquals(syncTime, processedOrder.getInappSynchronizationDate().get());
    }

    private Order createActiveSubscription() {
        Order inappOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, inappProductPrice.getId(), "inapp_order");
        mockGetInappSubscription(inappOrder, InappSubscriptionState.ACTIVE,
                x -> x.syncTime(Instant.now())
                        .storeExpirationTime(DateUtils.farFutureDate())
                        .subscriptionUntil(DateUtils.farFutureDate()));
        inappSubscriptionProcessor.processOrder(inappOrder);
        inappOrder = assertOrderStatus(inappOrder, OrderStatus.PAID);
        assertOrderServiceStatus(inappOrder, Target.ENABLED);

        return inappOrder;
    }
}
