package ru.yandex.market.tpl.billing.utils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.pvz.client.billing.dto.BillingOrderDto;
import ru.yandex.market.pvz.client.billing.dto.BillingReturnDto;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.client.model.order.OrderType;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class PvzModelFactory {
    public static BillingOrderDto order(OffsetDateTime deliveredAt) {
        return orderBuilder(deliveredAt).build();
    }

    public static BillingOrderDto.BillingOrderDtoBuilder orderBuilder(OffsetDateTime deliveredAt) {
        return BillingOrderDto.builder()
                .deliveredAt(deliveredAt)
                .deliveryServiceId(133L)
                .externalId("externalId")
                .id(1L)
                .paymentStatus("PAID")
                .paymentType("PREPAID")
                .paymentSum(BigDecimal.valueOf(100))
                .itemsSum(BigDecimal.valueOf(90))
                .pickupPointId(72L)
                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                .orderType(OrderType.CLIENT);
    }

    public static BillingReturnDto getReturn(OffsetDateTime dispatchedAt) {
        return BillingReturnDto.builder()
                .returnId("1")
                .externalOrderId("externalId")
                .dispatchedAt(dispatchedAt)
                .pickupPointId(3L)
                .build();
    }
}
