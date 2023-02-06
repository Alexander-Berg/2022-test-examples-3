package ru.yandex.market.deliverycalculator.workflow.util.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.daas.jsonb.DeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtoDeliveryOptionConverterTest {

    private ProtoDeliveryOptionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ProtoDeliveryOptionConverter();
    }

    @Test
    void convertWithoutShopDeliveryCost() {
        final DeliveryOption actual = converter.convert(DeliveryCalcProtos.DeliveryOption.newBuilder()
                .setDeliveryCost(1).setMinDaysCount(2).setMaxDaysCount(3).setOrderBefore(4).build());

        final DeliveryOption expected = new DeliveryOption(1, 2, 3, 4, null);

        assertEquals(expected, actual);
    }

    @Test
    void convertWithShopDeliveryCost() {
        final DeliveryOption actual = converter.convert(DeliveryCalcProtos.DeliveryOption.newBuilder()
                .setDeliveryCost(1).setMinDaysCount(2).setMaxDaysCount(3).setOrderBefore(4).setShopDeliveryCost(5).build());

        final DeliveryOption expected = new DeliveryOption(1, 2, 3, 4, 5L);

        assertEquals(expected, actual);
    }
}
