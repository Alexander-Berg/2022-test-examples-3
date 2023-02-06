package ru.yandex.market.checkout.checkouter.pay;

import java.util.Objects;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

/**
 * Created by asafev on 19/10/2017.
 */
public class FulfilmentPaymentTest extends AbstractPaymentTestBase {

    private static final String TEST_SHOP_SKU = "testShopSKU";
    private static final long FF_SHOP_ID = 667L;

    private Order order;
    @Autowired
    private OrderPayHelper paymentHelper;
    @Autowired
    private ShopService shopService;

    @Override
    @BeforeEach
    public void createUnpaidOrder() {
        trustMockConfigurer.mockWholeTrust();
        Parameters parameters = defaultBlueOrderParameters();
        // Виртуальный магазин на постоплате
        parameters.addShopMetaData(parameters.getOrder().getShopId(), ShopSettingsHelper.getPostpayMeta());
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order = orderCreateHelper.createOrder(parameters);

        checkOrderInDb(order);
        assertThat("Order has status different from UNPAID: " + order.getStatus(), order.getStatus(), equalTo(UNPAID));
    }

    @Tag(Tags.FULFILMENT)
    @DisplayName("Оплата ФФ заказа под залогином")
    @Test
    public void createAndPayFulfilmentOrder() {
        Payment payment = paymentHelper.payForOrderWithoutNotification(order);
        assertThat(payment.getUid(), notNullValue());
        validateThatPayBasketEventContainsXUid();
        paymentHelper.notifyPayment(payment);
    }

    @Test
    public void createAndPayWithDisabledShop() {
        shopService.updateMeta(FF_SHOP_ID, ShopSettingsHelper.getPostpayMeta());
        paymentHelper.payForOrder(order);
    }

    @Test
    public void createAndPayWithWrongVirtualShop() {
        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getIncorrectOldPrepayMeta());
        paymentHelper.payForOrder(order);
    }

    private void checkOrderInDb(Order order) {
        assertThat(order.getId(), notNullValue());
        assertThat(order.isFulfilment(), is(true));
        Order orderFromDb = orderService.getOrder(order.getId());
        assertThat(orderFromDb.isFulfilment(), is(true));
        assertThat(
                orderFromDb.getItems().stream()
                        .allMatch(orderItem -> FulfilmentProvider.TEST_SKU.equals(orderItem.getSku())
                                && Objects.equals(FulfilmentProvider.TEST_MSKU, orderItem.getMsku())
                                && TEST_SHOP_SKU.equals(orderItem.getShopSku())
                                && orderItem.getSupplierId() != null),
                is(true)
        );
    }
}
