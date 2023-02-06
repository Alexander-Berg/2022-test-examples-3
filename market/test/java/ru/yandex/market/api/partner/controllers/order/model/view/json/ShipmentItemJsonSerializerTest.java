package ru.yandex.market.api.partner.controllers.order.model.view.json;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.order.shipment.ShipmentItem;
import ru.yandex.market.checkout.common.json.JsonWriter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.api.partner.view.json.Names.ShipmentItem.COUNT;
import static ru.yandex.market.api.partner.view.json.Names.ShipmentItem.ID;

/**
 * @author apershukov
 */
class ShipmentItemJsonSerializerTest {

    @Test
    void testSerialize() throws IOException {
        ShipmentItem item = new ShipmentItem(10, 20);

        JsonWriter jsonWriter = mock(JsonWriter.class);
        new ShipmentItemJsonSerializer().serialize(item, jsonWriter);

        verify(jsonWriter, times(1)).startObject();
        verify(jsonWriter, times(1)).setAttribute(ID, 10L);
        verify(jsonWriter, times(1)).setAttribute(COUNT, 20);
        verify(jsonWriter, times(1)).endObject();
        verifyNoMoreInteractions(jsonWriter);
    }
}
