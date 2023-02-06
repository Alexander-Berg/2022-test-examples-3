package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.util.function.Supplier;

import com.google.common.primitives.Longs;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ItemInfo.Fulfilment;

/**
 * Created by asafev on 13/09/2017.
 */
@Deprecated
public final class FulfilmentProvider {

    public static final long TEST_MSKU = 123456789L;
    public static final String TEST_SKU = String.valueOf(TEST_MSKU);
    public static final String TEST_SHOP_SKU = "testShopSKU";
    public static final Long FF_SHOP_ID = 667L;
    public static final Integer TEST_WAREHOUSE_ID = 1234;

    public static final long ANOTHER_TEST_MSKU = 987654321L;
    public static final String ANOTHER_TEST_SKU = String.valueOf(ANOTHER_TEST_MSKU);
    public static final String ANOTHER_TEST_SHOP_SKU = "anotherTestShopSKU";
    public static final Long ANOTHER_FF_SHOP_ID = 668L;

    public static final long OTHER_TEST_MSKU = 543219876L;
    public static final String OTHER_TEST_SKU = String.valueOf(ANOTHER_TEST_MSKU);
    public static final String OTHER_TEST_SHOP_SKU = "otherTestShopSKU";

    private FulfilmentProvider() {
    }

    @Deprecated
    public static void addFulfilmentFields(OrderItem orderItem, String sku, String shopSku, Long shopId) {
        addBlueFields(orderItem, sku, shopSku, shopId);
        if (orderItem.getWarehouseId() == null) {
            orderItem.setWarehouseId(1);
        }
    }

    @Deprecated
    public static void addBlueFields(OrderItem orderItem, String sku, String shopSku, Long shopId) {
        orderItem.setSku(sku);
        orderItem.setMsku(Longs.tryParse(sku));
        orderItem.setShopSku(shopSku);
        orderItem.setSupplierId(shopId);
    }

    @Deprecated
    public static void fulfilmentize(Order order) {
        order.setFulfilment(true);
        order.getItems().forEach(FulfilmentProvider::addFulfilmentFields);
    }

    public static OrderItem buildFulfilmentItem(String offerId, Long ffShopId, String shopSku, String sku) {
        OrderItem item = OrderItemProvider.buildOrderItem(offerId);
        item.setSupplierId(ffShopId);
        item.setShopSku(shopSku);
        item.setSku(sku);
        item.setWarehouseId(1);
        return item;
    }

    /**
     * @deprecated use {@link BlueParametersProvider#defaultBlueOrderParameters(Order, Buyer)} instead
     */
    @Deprecated
    public static Parameters defaultFulfilmentParameters(Order order, Buyer buyer) {
        Parameters parameters = fulfilmentParametersWithoutFFDelivery(PaymentMethod.YANDEX,
                () -> new Parameters(buyer, order));
        FFDeliveryProvider.setFFDeliveryParameters(parameters);
        parameters.setDeliveryServiceId(null);
        return parameters;
    }

    @Deprecated
    public static Parameters defaultFulfilmentParameters(MultiCart multiOrder, Buyer buyer) {
        Parameters parameters = fulfilmentParametersWithoutFFDelivery(PaymentMethod.YANDEX,
                () -> new Parameters(buyer, multiOrder));
        FFDeliveryProvider.setFFDeliveryParameters(parameters);
        parameters.setDeliveryServiceId(null);
        return parameters;
    }

    @Deprecated
    private static Parameters fulfilmentParametersWithoutFFDelivery(PaymentMethod method,
                                                                    Supplier<Parameters> parametersSupplier) {
        Parameters parameters = parametersSupplier.get();
        parameters.setupFulfillment(new Fulfilment(FF_SHOP_ID, TEST_SKU, TEST_SHOP_SKU));
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setPaymentMethod(method);
        parameters.setWeight(BigDecimal.valueOf(1));
        parameters.setDimensions("10", "10", "10");
        // Виртуальный магазин
        parameters.addShopMetaData(parameters.getOrder().getShopId(),
                ShopSettingsHelper.createCustomNewPrepayMeta(FF_SHOP_ID.intValue()));
        // Реальный fulfilment магазин
        parameters.addShopMetaData(FF_SHOP_ID, ShopSettingsHelper.createCustomNewPrepayMeta(FF_SHOP_ID.intValue()));
        return parameters;
    }

    @Deprecated
    private static void addFulfilmentFields(OrderItem orderItem) {
        addFulfilmentFields(orderItem, TEST_SKU, TEST_SHOP_SKU, FF_SHOP_ID);
    }
}
