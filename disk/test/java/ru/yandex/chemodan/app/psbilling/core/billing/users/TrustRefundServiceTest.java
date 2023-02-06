package ru.yandex.chemodan.app.psbilling.core.billing.users;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Refund;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.trust.client.responses.SubscriptionResponse;
import ru.yandex.chemodan.trust.client.responses.TrustRefundStatus;

import static org.mockito.ArgumentMatchers.any;

public class TrustRefundServiceTest extends AbstractPaymentTest {
    @Autowired
    private FeatureFlags featureFlags;
    @Autowired
    private UserServiceDao userServiceDao;
    @Autowired
    private UserProductManager userProductManager;
    @Autowired
    private TrustRefundService trustRefundService;

    @Test
    public void refundTrialProduct() {
        TrialDefinitionEntity trial = psBillingProductsFactory.createTrialDefinitionWithPeriod();
        UserProductEntity userProduct =
                psBillingProductsFactory.createUserProduct(x -> x.trialDefinitionId(Option.of(trial.getId())));
        UserProductPrice price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES).getId());

        Order order = psBillingOrdersFactory.createOrUpdateOrder(userId, price.getId(), "trust_id");

        trustClientMockConfiguration.mockGetSubscriptionTrial();
        trustSubscriptionProcessor.processOrder(order);
        order = assertOrderStatus(order, OrderStatus.PAID);
        assertOrderServiceStatus(order, Target.ENABLED);

        Option<Refund> refund = trustRefundService.refundLastOrderPayment(order, "refund");
        Assert.assertFalse(refund.isPresent());
        assertOrderServiceStatus(order, Target.DISABLED);
    }

    @Test
    // по идее надо делать нормально CHEMODAN-78225: обработка refund-ов проапгрейженых заказов
    // но пока, чтобы иметь возможность рефандить заказы руками, допускаем дефанд заказа в статусе UPGRADE
    public void refundUpgradedOrder() {
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct();
        UserProductPrice price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES).getId());

        Order order = psBillingOrdersFactory.createOrUpdateOrder(userId, price.getId(), "trust_id");

        SubscriptionResponse subscriptionResponse = trustClientMockConfiguration.mockGetSubscriptionPaid();
        trustSubscriptionProcessor.processOrder(order);

        order = psBillingOrdersFactory.createOrUpdateOrder(order, x -> x.status(Option.of(OrderStatus.UPGRADED)));

        trustClientMockConfiguration.mockPaymentOk(subscriptionResponse.getPaymentIds().first());
        Mockito.when(trustClient.createRefund(any())).thenReturn("refund_id");
        Option<Refund> refund = trustRefundService.refundLastOrderPayment(order, "refund");
        Assert.assertTrue(refund.isPresent());
    }

    @Test
    public void refundUpgradeOrder() {
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct();
        UserProductPrice price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES,
                        BigDecimal.ONE).getId());

        UserProductEntity expensiveUserProduct = psBillingProductsFactory.createUserProduct();
        UserProductPrice expensivePrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(expensiveUserProduct, CustomPeriodUnit.TEN_MINUTES,
                        BigDecimal.TEN).getId());
        psBillingProductsFactory.addProductsToBucket("bucket", userProduct.getId(), expensiveUserProduct.getId());

        Order order = psBillingOrdersFactory.createOrUpdateOrder(userId, price.getId(), "trust_id_1");

        SubscriptionResponse subscriptionResponse = trustClientMockConfiguration.mockGetSubscriptionPaid();
        trustSubscriptionProcessor.processOrder(order);

        Order expensiveOrder = psBillingOrdersFactory.createOrUpdateOrder(userId, expensivePrice.getId(), "trust_id_2");
        trustSubscriptionProcessor.processOrder(expensiveOrder);
        expensiveOrder = assertOrderStatus(expensiveOrder, OrderStatus.PAID);
        order = assertOrderStatus(order, OrderStatus.UPGRADED);

        trustClientMockConfiguration.mockPaymentOk(subscriptionResponse.getPaymentIds().first());
        Mockito.when(trustClient.createRefund(any())).thenReturn("refund_id");
        Option<Refund> refund = trustRefundService.refundLastOrderPayment(expensiveOrder, "refund");
        Assert.assertTrue(refund.isPresent());

        trustClientMockConfiguration.mockRefund(refund.get().getTrustRefundId(), TrustRefundStatus.success);
        trustRefundService.checkRefund(refund.get().getId());
        /*
        sample from actual logs
        action: refund, status: success, order_id: a61f7d1a-7175-451c-bb5e-9eea757fb8f8,
        refund_id: 62a907b04f5c6e029196e5d7, uid: 1005710826,
        product_code: mail_pro_b2c_premium1000_v20220414_exp3,
        period: UserProductPeriod[<the whole tostringed period>],
        price: 2290, currency: RUB, package_name: null
         */
        //changed product period to actual period
        PurchaseReportingServiceMockConfiguration.assertLogLike(
                "action: unsubscribe, status: success, order_id: trust_id_2, uid: %uid%, " +
                        "product_code: %uuid%, period: 600S, price: 10, currency: RUB",
                "action: refund, status: success, order_id: trust_id_2, refund_id: refund_id, uid: %uid%, " +
                        "product_code: %uuid%, period: 600S, price: 10, currency: RUB"
        );

        bazingaTaskManagerStub.filterTaskQueue(t -> t instanceof CheckOrderTask);
        bazingaTaskManagerStub.executeTasks(applicationContext);

        order = assertOrderStatus(order, OrderStatus.PAID);
        Option<UserServiceEntity> userServiceO = userServiceDao.findByIdO(order.getUserServiceId().get());
        Assert.assertTrue(userServiceO.isPresent());
        Assert.assertEquals(Target.ENABLED, userServiceO.get().getTarget());

        expensiveOrder = assertOrderStatus(expensiveOrder, OrderStatus.PAID);
        userServiceO = userServiceDao.findByIdO(expensiveOrder.getUserServiceId().get());
        Assert.assertTrue(userServiceO.isPresent());
        Assert.assertEquals(Target.DISABLED, userServiceO.get().getTarget());
    }
}
