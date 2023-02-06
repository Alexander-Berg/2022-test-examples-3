package ru.yandex.market.checkout.checkouter.json;

import java.time.LocalTime;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;

public class DeliveryDatesJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        DeliveryDates deliveryDates = new DeliveryDates(
                new Date(1408888660000L),
                new Date(1478304000000L),
                LocalTime.of(3, 0, 0),
                LocalTime.of(18, 30, 0)
        );

        String json = write(deliveryDates);

        checkJson(json, "$." + Names.DeliveryDates.FROM_DATE, "24-08-2014");
        checkJson(json, "$." + Names.DeliveryDates.TO_DATE, "05-11-2016");
        checkJson(json, "$." + Names.DeliveryDates.FROM_TIME, "03:00");
        checkJson(json, "$." + Names.DeliveryDates.TO_TIME, "18:30");
    }

    @Test
    public void deserialize() throws Exception {
        String json = "{\"fromDate\":\"24-08-2014\",\"toDate\":\"05-11-2016\",\"fromTime\":\"03:00\"," +
                "\"toTime\":\"18:30\"}";

        DeliveryDates deliveryDates = read(DeliveryDates.class, json);

        Assertions.assertEquals(new Date(1408824000000L), deliveryDates.getFromDate());
        Assertions.assertEquals(new Date(1478293200000L), deliveryDates.getToDate());
        Assertions.assertEquals(LocalTime.of(3, 0), deliveryDates.getFromTime());
        Assertions.assertEquals(LocalTime.of(18, 30), deliveryDates.getToTime());
    }
}
