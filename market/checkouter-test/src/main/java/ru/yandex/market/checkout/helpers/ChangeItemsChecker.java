package ru.yandex.market.checkout.helpers;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.HistorySortingField;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.event.diff.Diff;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemsHistory;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.collections.CollectionUtils.first;
import static ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper.checkEqualItemSet;

@WebTestHelper
public class ChangeItemsChecker {

    @Autowired
    private EventService eventService;
    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;

    public void checkItemsHistory(Order order,
                                  Order orderBeforeChange,
                                  Map<OfferItemKey, Integer> itemsNewCount) throws Exception {
        PagedEvents events = eventService.getPagedOrderHistoryEvents(order.getId(), Pager.atPage(1, 1),
                new SortingInfo<>(HistorySortingField.DATE, SortingOrder.DESC),
                null, ImmutableSet.of(HistoryEventType.ITEMS_UPDATED), false, ClientInfo.SYSTEM, null);

        assertThat(events.getItems(), not(empty()));
        OrderHistoryEvent lastEvent = first(events.getItems());
        assertThat(lastEvent.getType(), Matchers.is(HistoryEventType.ITEMS_UPDATED));

        checkEqualItemSet(lastEvent.getOrderBefore().getItems(), orderBeforeChange.getItems());
        checkEqualItemSet(lastEvent.getOrderAfter().getItems(), order.getItems());

        ResultActions historyResult = changeOrderItemsHelper.getItemsHistory(order.getId(), lastEvent.getId(),
                ClientInfo.SYSTEM);
        Diff<Collection<OrderItem>> expectedDiff = new Diff<>(newArrayList(), newArrayList(), newArrayList(),
                newArrayList());
        itemsNewCount.forEach((fo, cnt) -> {
            OrderItem item = order.getItem(fo);
            if (cnt < orderBeforeChange.getItem(fo).getCount()) {
                if (cnt == 0) {
                    expectedDiff.getDeleted().add(orderBeforeChange.getItem(fo));
                } else {
                    expectedDiff.getChanged().add(item);
                }
            }
        });
        orderBeforeChange.getItems().stream()
                .filter(i -> itemsNewCount.getOrDefault(i.getOfferItemKey(), i.getCount()).intValue() == i.getCount())
                .forEach(i -> expectedDiff.getUnchanged().add(i));
        checkItemsHistoryResponse(historyResult, orderBeforeChange.getItems(), order.getItems(), expectedDiff);
    }

    private ResultActions checkItemsHistoryResponse(ResultActions result,
                                                    Collection<OrderItem> originalItems,
                                                    Collection<OrderItem> expectedItems,
                                                    Diff<Collection<OrderItem>> expectedDiff) throws Exception {
        String response = result
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OrderItemsHistory itemsHistory = testSerializationService.deserializeCheckouterObject(
                response, OrderItemsHistory.class
        );

        assertThat(itemsHistory, notNullValue());
        assertThat(itemsHistory.getBefore(), notNullValue());
        assertThat(itemsHistory.getAfter(), notNullValue());
        assertThat(itemsHistory.getDiff(), notNullValue());

        checkEqualItemSet(itemsHistory.getBefore().getContent(), originalItems);
        checkEqualItemSet(itemsHistory.getAfter().getContent(), expectedItems);
        checkEqualItemSet(itemsHistory.getDiff().getChanged().getContent(), expectedDiff.getChanged());
        checkEqualItemSet(itemsHistory.getDiff().getDeleted().getContent(), expectedDiff.getDeleted());
        checkEqualItemSet(itemsHistory.getDiff().getAdded().getContent(), expectedDiff.getAdded());
        checkEqualItemSet(itemsHistory.getDiff().getUnchanged().getContent(), expectedDiff.getUnchanged());

        return result;
    }
}
