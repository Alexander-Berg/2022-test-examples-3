package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment;

import java.util.Set;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;

import static org.junit.Assert.assertEquals;

public class FulfillmentPosteRestanteConverterTest {
    private FulfillmentPosteRestanteConverter fulfillmentPosteRestanteConverter =
        new FulfillmentPosteRestanteConverter(Set.of(145L));

    @Test
    public void testPatronymicTransformation() {
        assertEquals(
            "Иванович До востребования",
            fulfillmentPosteRestanteConverter.convertMiddleName(DeliveryType.PICKUP, Set.of(145L), "Иванович")
        );
    }

    @Test
    public void testPatronymicTransformationIsIgnoredDueToWarehouse() {
        assertEquals(
            "Иванович",
            fulfillmentPosteRestanteConverter.convertMiddleName(DeliveryType.PICKUP, Set.of(4L), "Иванович")
        );
    }

    @Test
    public void testPatronymicTransformationIsIgnoredDueToDeliveryType() {
        assertEquals(
            "Дмитриевич",
            fulfillmentPosteRestanteConverter.convertMiddleName(DeliveryType.POST, Set.of(145L, 4L), "Дмитриевич")
        );
    }

    @Test
    public void testDeliveryTypeTransformation() {
        assertEquals(
            DeliveryType.POST,
            fulfillmentPosteRestanteConverter.convertDeliveryType(DeliveryType.PICKUP, Set.of(145L))
        );
    }

    @Test
    public void testDeliveryTypeTransformationIsIgnoredDueToWarehouse() {
        assertEquals(
            DeliveryType.PICKUP,
            fulfillmentPosteRestanteConverter.convertDeliveryType(DeliveryType.PICKUP, Set.of(4L))
        );
    }
}
