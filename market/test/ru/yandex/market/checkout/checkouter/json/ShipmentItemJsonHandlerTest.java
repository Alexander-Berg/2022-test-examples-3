package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

public class ShipmentItemJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{\"itemId\":123,\"count\":234}";

        ParcelItem shipmentItem = read(ParcelItem.class, json);

        Assertions.assertEquals(123L, shipmentItem.getItemId().longValue());
        Assertions.assertEquals(234, shipmentItem.getCount().intValue());
    }

    @Test
    public void serialize() throws Exception {
        ParcelItem shipmentItem = EntityHelper.getShipmentItem();

        String json = write(shipmentItem);
        System.out.println(json);

        checkJson(json, "$.itemId", 123);
        checkJson(json, "$.count", 234);
    }

}
