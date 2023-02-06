package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public abstract class CashParametersProvider {

    public static Parameters createCashParameters(boolean freeDelivery) {
        Parameters parameters = defaultBlueOrderParameters(freeDelivery);
        parameters.setFreeDelivery(freeDelivery);
        parameters.setDeliveryServiceId(null);
        parameters.setColor(Color.BLUE);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setShopId(123L);
        return parameters;
    }

    public static Parameters createCashParameters(int count, boolean freeDelivery) {
        Parameters parameters = createCashParameters(freeDelivery);
        parameters.getOrder().getItems().forEach(i -> i.setCount(count));
        return parameters;
    }

    public static Parameters createOrderWithTwoItems(boolean freeDelivery) {
        Parameters parameters = createCashParameters(freeDelivery);
        OrderItem anotherItem = OrderItemProvider.getAnotherOrderItem();
        anotherItem.setShopSku(FulfilmentProvider.ANOTHER_TEST_SHOP_SKU);
        anotherItem.setSku(FulfilmentProvider.ANOTHER_TEST_SKU);
        anotherItem.setMsku(FulfilmentProvider.ANOTHER_TEST_MSKU);
        anotherItem.setSupplierId(FulfilmentProvider.ANOTHER_FF_SHOP_ID);
        parameters.addShopMetaData(FulfilmentProvider.ANOTHER_FF_SHOP_ID, ShopSettingsHelper.getDefaultMeta());
        parameters.getOrder().addItem(anotherItem);
        parameters.setWeight(BigDecimal.valueOf(1));
        return parameters;
    }
}
