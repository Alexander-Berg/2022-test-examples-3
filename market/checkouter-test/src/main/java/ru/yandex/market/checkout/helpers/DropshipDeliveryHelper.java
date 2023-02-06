package ru.yandex.market.checkout.helpers;

import java.math.BigDecimal;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static java.util.Collections.singletonList;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.PICKUP_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;

@WebTestHelper
public class DropshipDeliveryHelper {

    public static final long DROPSHIP_SHOP_ID = 5331L;
    public static final int DROPSHIP_WAREHOUSE_ID = 4321;
    public static final int SUPER_DROPSHIP_WAREHOUSE_ID = 1888;
    public static final long WIDTH = 10;
    public static final long HEIGHT = 20;
    public static final long DEPTH = 30;

    private final OrderCreateHelper orderCreateHelper;

    public DropshipDeliveryHelper(OrderCreateHelper orderCreateHelper) {
        this.orderCreateHelper = orderCreateHelper;
    }

    public Order createDropshipOrder() {
        return orderCreateHelper.createOrder(getDropshipPrepaidParameters());
    }

    public Order createDropshipPostpaidOrder() {
        return orderCreateHelper.createOrder(getDropshipPostpaidParameters(prepareOrderItem(), DeliveryType.DELIVERY));
    }

    public static Parameters getDropshipPrepaidParameters() {
        return getDropshipPrepaidParameters(DeliveryType.DELIVERY);
    }

    public static Parameters getDropshipPrepaidParameters(BigDecimal deliveryPrice) {
        return getDropshipPrepaidParameters(prepareOrderItem(), DeliveryType.DELIVERY, deliveryPrice);
    }

    public static Parameters getDropshipPrepaidParameters(DeliveryType deliveryType) {
        return getDropshipPrepaidParameters(prepareOrderItem(), deliveryType);
    }

    public static Parameters getDropshipPrepaidParameters(OrderItem orderItem, DeliveryType deliveryType) {
        return getDropshipParametersInner(orderItem, deliveryType, PaymentMethod.YANDEX, DELIVERY_PRICE);
    }

    public static Parameters getDropshipPrepaidParameters(OrderItem orderItem, DeliveryType deliveryType,
                                                          BigDecimal deliveryPrice) {
        return getDropshipParametersInner(orderItem, deliveryType, PaymentMethod.YANDEX, deliveryPrice);
    }

    public static Parameters getDropshipPostpaidParameters() {
        return getDropshipPostpaidParameters(DeliveryType.DELIVERY);
    }

    public static Parameters getDropshipPostpaidParameters(DeliveryType deliveryType) {
        return getDropshipPostpaidParameters(prepareOrderItem(), deliveryType);
    }

    public static Parameters getDropshipPostpaidParameters(OrderItem orderItem, DeliveryType deliveryType) {
        return getDropshipParametersInner(orderItem, deliveryType, PaymentMethod.CASH_ON_DELIVERY, DELIVERY_PRICE);
    }

    @Nonnull
    private static OrderItem prepareOrderItem() {
        OrderItem orderItem = OrderItemProvider.getOrderItem();
        orderItem.setCount(3);
        orderItem.setQuantity(BigDecimal.valueOf(3));

        FulfilmentProvider.addBlueFields(orderItem, FulfilmentProvider.TEST_SKU, FulfilmentProvider.TEST_SHOP_SKU,
                DROPSHIP_SHOP_ID);
        orderItem.setWarehouseId(DROPSHIP_WAREHOUSE_ID);
        return orderItem;
    }

    private static Parameters getDropshipParametersInner(OrderItem orderItem,
                                                         DeliveryType deliveryType,
                                                         PaymentMethod paymentMethod,
                                                         BigDecimal deliveryPrice) {
        Order request = OrderProvider.getBlueOrder(o -> {
            o.setItems(Lists.newArrayList(orderItem));
            o.setShopId(DROPSHIP_SHOP_ID);
        });

        Parameters parameters = new Parameters(request);
        parameters.setColor(Color.BLUE);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setDeliveryType(deliveryType);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setPaymentMethod(paymentMethod);

        parameters.addShopMetaData(DROPSHIP_SHOP_ID, ShopSettingsHelper.getDefaultMeta());

        parameters.getBuiltMultiCart().getCarts()
                .forEach(c -> c.setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress()));
        parameters.setDimensions(String.valueOf(WIDTH), String.valueOf(HEIGHT), String.valueOf(DEPTH));
        parameters.getReportParameters().setIgnoreStocks(false);
        parameters.getReportParameters().setDeliveryPartnerTypes(
                singletonList(DeliveryPartnerType.YANDEX_MARKET.name())
        );
        ItemInfo itemInfo = parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId());
        itemInfo.setAtSupplierWarehouse(true);
        itemInfo
                .setFulfilment(new ItemInfo.Fulfilment(DROPSHIP_SHOP_ID, FulfilmentProvider.TEST_SKU,
                        FulfilmentProvider.TEST_SHOP_SKU, null, false));

        if (deliveryType == DeliveryType.DELIVERY) {
            parameters.setDeliveryServiceId(LocalDeliveryOptionProvider.DROPSHIP_DELIVERY_SERVICE_ID);
            parameters.getReportParameters().setActualDelivery(
                    ActualDeliveryProvider.builder()
                            .addDelivery(LocalDeliveryOptionProvider.DROPSHIP_DELIVERY_SERVICE_ID,
                                    2, deliveryPrice)
                            .build()
            );
        }

        if (deliveryType == DeliveryType.PICKUP) {
            parameters.setDeliveryServiceId(PICKUP_SERVICE_ID);
            parameters.getReportParameters().setActualDelivery(
                    ActualDeliveryProvider.builder()
                            .addPickup(PICKUP_SERVICE_ID, 2, Collections.singletonList(12312303L))
                            .build()
            );
        }
        return parameters;
    }
}
