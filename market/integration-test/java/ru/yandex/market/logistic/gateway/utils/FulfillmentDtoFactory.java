package ru.yandex.market.logistic.gateway.utils;

import java.math.BigDecimal;
import java.util.Collections;

import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Consignment;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Inbound;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

public class FulfillmentDtoFactory {

    private FulfillmentDtoFactory() {
        throw new AssertionError();
    }

    public static Inbound createInbound() {
        return createInbound("111");
    }

    public static Inbound createInbound(String yandexId) {
        return new Inbound.InboundBuilder(
            createResourceId(yandexId),
            InboundType.CROSSDOCK,
            Collections.singletonList(createConsignment()),
            createDateTimeInterval()
        ).build();
    }

    public static ResourceId createResourceId(String yandexId) {
        return ResourceId.builder().setYandexId(yandexId).build();
    }

    public static Consignment createConsignment() {
        return new Consignment.ConsignmentBuilder(createResourceId("111"), createItem()).build();
    }

    public static Item createItem() {
        return new Item.ItemBuilder(
            "ItemName", 1,
            BigDecimal.TEN,
            CargoType.DANGEROUS_CARGO,
            Collections.singletonList(CargoType.DANGEROUS_CARGO)
        ).build();
    }

    public static DateTimeInterval createDateTimeInterval() {
        return DateTimeInterval.fromFormattedValue("2019-02-14T00:00:00+03:00/2019-02-21T00:00:00+03:00");
    }
}
