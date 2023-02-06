package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.util.Set;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.delivery.mdbapp.integration.converter.PosteRestanteHouseConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PosteRestanteHouseConverterTest {
    private static final long PARTNER = 1005117;
    private PosteRestanteHouseConverter posteRestanteHouseConverter = new PosteRestanteHouseConverter(Set.of(PARTNER));

    @Test
    public void houseEnrichingSuccess() {
        assertEquals(
            "123 До востребования",
            posteRestanteHouseConverter.convert(DeliveryType.PICKUP, PARTNER, "123")
        );
    }

    @Test
    public void emptyHouseEnrichingSuccess() {
        assertEquals(
            "До востребования",
            posteRestanteHouseConverter.convert(DeliveryType.PICKUP, PARTNER, null)
        );
    }

    @Test
    public void houseIsNotEnrichedDueToDeliveryType() {
        assertEquals(
            "123",
            posteRestanteHouseConverter.convert(DeliveryType.POST, PARTNER, "123")
        );
    }

    @Test
    public void emptyHouseIsNotEnrichedDueToDeliveryType() {
        assertNull(posteRestanteHouseConverter.convert(DeliveryType.POST, PARTNER, null));
    }

    @Test
    public void houseIsNotEnrichedDueToDeliveryServiceId() {
        assertEquals(
            "123",
            posteRestanteHouseConverter.convert(DeliveryType.PICKUP, 1005118L, "123")
        );
    }
}
