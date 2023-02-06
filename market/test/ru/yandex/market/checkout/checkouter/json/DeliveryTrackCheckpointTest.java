package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackCheckpoint;

public class DeliveryTrackCheckpointTest extends AbstractJsonHandlerTestBase {

    private static final String JSON = "{" +
            "\"id\":520220," +
            "\"country\":\"\"," +
            "\"city\":\"\"," +
            "\"location\":\"HEL01W\"," +
            "\"message\":\"【Europe Distribution Center】 Shipment transiting to destination country\"," +
            "\"checkpointStatus\":\"IN_TRANSIT\"," +
            "\"zipCode\":null," +
            "\"checkpointDate\":\"2018-01-26 11:20:00\"," +
            "\"deliveryCheckpointStatus\":30" +
            "}";

    @Test
    public void shouldDeserialize() throws IOException {
        DeliveryTrackCheckpoint checkpoint = read(DeliveryTrackCheckpoint.class, JSON);
        Assertions.assertEquals(520220, checkpoint.getId());
        Assertions.assertEquals("", checkpoint.getCountry());
        Assertions.assertEquals("", checkpoint.getCity());
        Assertions.assertEquals("HEL01W", checkpoint.getLocation());
        Assertions.assertEquals("【Europe Distribution Center】 Shipment transiting to destination country",
                checkpoint.getMessage());
        Assertions.assertEquals("IN_TRANSIT", checkpoint.getCheckpointStatus().name());
        Assertions.assertEquals(30L, checkpoint.getDeliveryCheckpointStatus().longValue());
    }
}
