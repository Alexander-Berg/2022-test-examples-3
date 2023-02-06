package ru.yandex.market.api.partner.controllers.order.model.view;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.order.model.OrderItemResponseDTO;
import ru.yandex.market.api.partner.controllers.order.model.view.json.OrderItemJsonSerializer;
import ru.yandex.market.api.partner.controllers.order.model.view.xml.OrderItemXmlSerializer;
import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

/**
 * @see OrderItemXmlSerializer
 * @see OrderItemJsonSerializer
 */
public class OrderItemSerializerTest extends BaseOldSerializationTest {

    /**
     * warehouseId, partnerId не должны передаваться в ответах ПАПИ (в OrderDTO)
     * т.к. это могут быть подменённые id-шники при миграции
     * тикет MBI-62704
     */
    @Test
    void shouldNotSerializeWarehouseIdSupplierId() {
        OrderItem orderItem = new OrderItem();
        orderItem.setSupplierId(1L);
        orderItem.setWarehouseId(2);

        OrderItemResponseDTO orderItemResponseDTO = new OrderItemResponseDTO(orderItem, null,
                Collections.emptyList(), Collections.emptyList());

        getChecker().testSerialization(
                orderItemResponseDTO,
                "{}",
                "<item/>");
    }
}
