package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.auth.UserInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.getDefaultMeta;

/**
 * @author mkasumov
 */
@Disabled
public class NoAuthPaymentTest extends AbstractPaymentTestBase {

    @Autowired
    private AuthService authService;
    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    @DisplayName(value = "Проверяем, что при постоплате заказа неавторизованным пользователем uid не шлется в баланс")
    public void testUnauthorizedPostPaidPayment() throws Exception {
        AuthInfo authInfo = authService.auth(
                null,
                new UserInfo("127.0.0.1", "java 1.8.0"),
                HitRateGroup.LIMIT, false);
        Long muid = authInfo.getMuid();

        Parameters params = postpaidBlueOrderParameters(123L);
        params.getOrder().getBuyer().setUid(muid);

        Order beforePay = orderCreateHelper.createOrder(params);
        shopMetaData.set(params.getShopMetaData().get(beforePay.getShopId()));

        orderStatusHelper.proceedOrderToStatus(beforePay, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        Order paidOrder = orderService.getOrder(beforePay.getId());
        order.set(paidOrder);
        paymentTestHelper.checkBalanceCallsAfterPay(beforePay, paymentTestHelper
                .createCreateCashBasketBalanceCallParams(), null);
    }

    @Test
    public void holdAndClear() throws Exception {
        createNoAuthOrder(PaymentMethod.YANDEX);
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @Test
    public void holdAndCancel() throws Exception {
        createNoAuthOrder(PaymentMethod.YANDEX);
        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.cancelPayment();
    }

    @Test
    public void payWithWalletAndCancel() throws Exception {
        createNoAuthOrder(PaymentMethod.YANDEX);
        paymentTestHelper.initAndHoldPayment(true);
        paymentTestHelper.cancelPayment(true);
    }

    @Test
    public void payWithWalletAndClear() throws Exception {
        createNoAuthOrder(PaymentMethod.YANDEX);
        paymentTestHelper.initAndHoldPayment(true);
        paymentTestHelper.clearPayment();
    }

    @Test
    public void firstFailThenClear() throws Exception {
        createNoAuthOrder(PaymentMethod.YANDEX);
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.notifyPaymentFailed(receiptId);

        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();
    }

    @Test
    public void firstFailThenCancel() throws Exception {
        createNoAuthOrder(PaymentMethod.YANDEX);
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.notifyPaymentFailed(receiptId);

        paymentTestHelper.cancelPayment();
    }


    private void createNoAuthOrder(PaymentMethod paymentMethod) {
        AuthInfo authInfo = authService.auth(
                null,
                new UserInfo("127.0.0.1", "java 1.8.0"),
                HitRateGroup.LIMIT, false);
        Long muid = authInfo.getMuid();

        Parameters parameters = new Parameters();
        parameters.getBuyer().setUid(muid);
        parameters.setColor(Color.GREEN); // Green must die
        parameters.setPaymentMethod(paymentMethod);

        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(YANDEX_MARKET);
        parameters.setDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);

        Order newOrder = orderCreateHelper.createOrder(parameters);

        Assertions.assertTrue(newOrder.isNoAuth());
        Assertions.assertEquals(muid, newOrder.getBuyer().getUid());
        Assertions.assertEquals(muid, newOrder.getBuyer().getMuid());

        shopMetaData.set(getDefaultMeta());
        shopService.updateMeta(newOrder.getShopId(), shopMetaData.get());
        order.set(newOrder);
    }
}
