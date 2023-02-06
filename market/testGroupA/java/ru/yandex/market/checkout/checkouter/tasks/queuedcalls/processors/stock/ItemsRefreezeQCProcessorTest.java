package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.stock;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.tasks.refreeze.ItemsRefreezeQCMessage;
import ru.yandex.market.checkout.checkouter.tasks.refreeze.ItemsRefreezeService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCObjectType.ORDER;
import static ru.yandex.market.checkout.checkouter.tasks.refreeze.ItemsRefreezeServiceImpl.MAPPER;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

public class ItemsRefreezeQCProcessorTest extends AbstractWebTestBase {

    @Autowired
    private ItemsRefreezeQCProcessor itemsRefreezeQCProcessor;
    @Autowired
    private ItemsRefreezeService itemsRefreezeService;
    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void qcProcessedSuccessfully() throws JsonProcessingException {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        var itemsRefreezeQCMessage = buildItemRefreezeMessage(order);

        itemsRefreezeQCProcessor.process(createQueuedCallExecution(itemsRefreezeQCMessage));
    }

    @Test
    public void putMessageToQC() throws IOException {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        var itemsRefreezeQCMessage = buildItemRefreezeMessage(order);

        transactionTemplate.execute(ts -> {
            itemsRefreezeService.prepareMessageForRefreeze(itemsRefreezeQCMessage);
            return null;
        });
        var qcs = queuedCallService.findQueuedCalls(ORDER, order.getId());
        Assertions.assertFalse(qcs.isEmpty());
        var qc = qcs.stream().findFirst().orElse(null);
        Assertions.assertNotNull(qc);
        var parsedItemsRefreezeMessage = MAPPER.readValue(qc.getPayload(), ItemsRefreezeQCMessage.class);
        Assertions.assertEquals(itemsRefreezeQCMessage.getItems().size(), parsedItemsRefreezeMessage.getItems().size());
    }

    private QueuedCallProcessor.QueuedCallExecution createQueuedCallExecution(ItemsRefreezeQCMessage message)
            throws JsonProcessingException {
        var payload = MAPPER.writeValueAsString(message);
        return new QueuedCallProcessor.QueuedCallExecution(
                message.getOrderId(),
                payload,
                0,
                Instant.now(),
                message.getOrderId());
    }

    private ItemsRefreezeQCMessage buildItemRefreezeMessage(Order order) {
        OrderItem orderItem = OrderItemProvider.getOrderItem();
        orderItem.setFitFreezed(1);
        orderItem.setFulfilmentWarehouseId(MOCK_SORTING_CENTER_HARDCODED);
        orderItem.setSku("123");
        orderItem.setMsku(123L);
        orderItem.setShopSku("321");
        orderItem.setSupplierId(234L);

        return new ItemsRefreezeQCMessage(order.getId(), List.of(orderItem.getId()), 1L);
    }
}
