package ru.yandex.market.checkout.checkouter.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItem;

import static org.assertj.core.api.Assertions.assertThat;

class OrderHistoryEventReasonDetailsTest {

    @Test
    void convertFromOrderItemsFromEmptyList() {
        OrderHistoryEventReasonDetails reasonDetails = OrderHistoryEventReasonDetails.convertFromOrderItems(List.of());

        assertThat(reasonDetails.getMissingOrderItems()).isEmpty();
    }

    @Test
    void convertFromOrderItemsFromTwoItems() {
        List<OrderItem> items = new ArrayList<>();
        items.add(initItem(1L, 10));
        items.add(initItem(2L, 1));
        OrderHistoryEventReasonDetails reasonDetails = OrderHistoryEventReasonDetails.convertFromOrderItems(items);

        assertThat(reasonDetails.getMissingOrderItems())
                .hasSize(2)
                .extracting(ItemCount::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    private OrderItem initItem(Long itemId, int count) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setCount(count);
        return item;
    }
}
