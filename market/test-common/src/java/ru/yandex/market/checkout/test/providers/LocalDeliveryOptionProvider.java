package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.PickupOption;

public abstract class LocalDeliveryOptionProvider {

    public static final long RED_DELIVERY_SERVICE_ID = 2234562L;
    public static final long DROPSHIP_DELIVERY_SERVICE_ID = 8800200L;

    public static LocalDeliveryOption getRedLocalDeliveryOption(BigDecimal price) {
        return getRedLocalDeliveryOption(price, 0, 2);
    }

    public static LocalDeliveryOption getRedLocalDeliveryOption(BigDecimal price, int dayFrom, int dayTo) {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        mapLocalDeliveryOptions(localDeliveryOption, price, dayFrom, dayTo);
        return localDeliveryOption;
    }

    public static PickupOption getRedPickupOption(BigDecimal price, int dayFrom, int dayTo) {
        PickupOption pickupOption = new PickupOption();
        mapLocalDeliveryOptions(pickupOption, price, dayFrom, dayTo);
        return pickupOption;
    }

    public static LocalDeliveryOption getMarDoLocalDeliveryOption(
            long deliveryServiceId,
            Integer shipmentDay,
            Instant supplierShipmentDateTime,
            Duration packagingTime) {
        LocalDeliveryOption localDeliveryOption = new LocalDeliveryOption();
        localDeliveryOption.setDeliveryServiceId(deliveryServiceId);
        localDeliveryOption.setDayFrom(0);
        localDeliveryOption.setDayTo(2);
        localDeliveryOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
        localDeliveryOption.setPrice(new BigDecimal("100"));
        localDeliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
        localDeliveryOption.setShipmentDay(shipmentDay);
        localDeliveryOption.setSupplierShipmentDateTime(supplierShipmentDateTime);
        localDeliveryOption.setPackagingTime(packagingTime);
        return localDeliveryOption;
    }

    private static void mapLocalDeliveryOptions(LocalDeliveryOption localDeliveryOption,
                                                BigDecimal price,
                                                int dayFrom,
                                                int dayTo) {
        localDeliveryOption.setPartnerType(DeliveryOptionPartnerType.RED_MARKET.getReportName());
        localDeliveryOption.setPrice(price);
        localDeliveryOption.setDayFrom(dayFrom);
        localDeliveryOption.setDayTo(dayTo);
        localDeliveryOption.setDeliveryServiceId(RED_DELIVERY_SERVICE_ID);
        localDeliveryOption.setPaymentMethods(ImmutableSet.of(
                PaymentMethod.YANDEX.name(), PaymentMethod.CASH_ON_DELIVERY.name(),
                PaymentMethod.CARD_ON_DELIVERY.name()
        ));
    }
}
