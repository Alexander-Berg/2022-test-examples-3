package ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_FOUND;

public class ItemsRemovalDtoParsingTest extends AbstractJsonHandlerTestBase {

    @Test
    public void orderEditRequestParsingTest() throws IOException {
        ItemInfo item1 = new ItemInfo(1, "11", 10);
        ItemInfo item2 = new ItemInfo(2, "22", 20);
        MissingItemsNotification removalRequest = new MissingItemsNotification(true, List.of(item1, item2),
                ITEMS_NOT_FOUND);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(removalRequest);

        OrderEditRequest parsedRequest = read(OrderEditRequest.class, write(editRequest));
        MissingItemsNotification parsedRemovalRequest = parsedRequest.getMissingItemsNotification();

        assertEquals(removalRequest.isAlreadyRemovedByWarehouse(),
                parsedRemovalRequest.isAlreadyRemovedByWarehouse());
        assertEquals(new HashSet<>(removalRequest.getRemainedItems()),
                new HashSet<>(parsedRemovalRequest.getRemainedItems()));
    }

    @Test
    public void changeRequestParsingTest() throws IOException {
        Order order = OrderProvider.getBlueOrder();
        order.setId(1L);
        Parcel parcel = new Parcel();
        parcel.setOrderId(order.getId());
        parcel.setParcelItems(order.getItems().stream()
                .map(ParcelItem::new)
                .collect(Collectors.toList()));
        order.getDelivery().setParcels(List.of(parcel));

        ItemsRemovalChangeRequestPayload payload = new ItemsRemovalChangeRequestPayload(
                order.getItems(),
                order.getDelivery().getParcels(),
                null
        );

        ItemsRemovalChangeRequestPayload parsedPayload =
                (ItemsRemovalChangeRequestPayload) read(AbstractChangeRequestPayload.class, write(payload));

        OrderItem orderItem = payload.getUpdatedItems().iterator().next();
        OrderItem parsedOrderItem = parsedPayload.getUpdatedItems().iterator().next();
        assertEquals(orderItem.getSupplierId(), parsedOrderItem.getSupplierId());
        assertEquals(orderItem.getShopSku(), parsedOrderItem.getShopSku());
        assertEquals(orderItem.getCount(), parsedOrderItem.getCount());

        ParcelItem parcelItem = payload.getUpdatedParcels().iterator().next()
                .getParcelItems().iterator().next();
        ParcelItem parsedParcelItem = parsedPayload.getUpdatedParcels().iterator().next()
                .getParcelItems().iterator().next();
        assertEquals(parcelItem.getItemId(), parsedParcelItem.getItemId());
        assertEquals(parcelItem.getCount(), parsedParcelItem.getCount());
    }
}
