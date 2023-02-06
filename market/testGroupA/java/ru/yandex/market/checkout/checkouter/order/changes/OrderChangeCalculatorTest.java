package ru.yandex.market.checkout.checkouter.order.changes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javolution.testing.AssertionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.DELIVERY_SERVICE;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.WAREHOUSE;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ITEMS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.order.changes.ChangesStage.OTHER;
import static ru.yandex.market.checkout.checkouter.order.changes.ChangesStage.PARTIAL_BUYBACK;
import static ru.yandex.market.checkout.checkouter.order.changes.ChangesStage.TOTAL;

class OrderChangeCalculatorTest {

    private static final Long ORDER_ID = 777L;
    private static final Currency CURRENCY = Currency.RUR;
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal(1);

    private static Order initOrder(OrderItem... items) {
        Order order = new Order();
        order.setId(ORDER_ID);
        List<OrderItem> newItems = Arrays.asList(items);
        BigDecimal total = new BigDecimal(0);
        for (OrderItem item : newItems) {
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getCount())));
        }
        order.setTotal(total);
        order.setItemsTotal(total);
        order.setBuyerItemsTotal(total);
        order.setItems(newItems);
        order.setCurrency(CURRENCY);
        order.setBuyerCurrency(CURRENCY);
        order.setExchangeRate(EXCHANGE_RATE);
        return order;
    }

    private static OrderHistoryEvent initNewOrderEvent(Long id, Order orderAfter) {
        return initEvent(id, NEW_ORDER, SYSTEM, null, orderAfter);
    }

    private static OrderHistoryEvent initItemUpdatedEvent(ClientRole clientRole,
                                                          Order orderBefore,
                                                          Order orderAfter,
                                                          OrderHistoryEvent prevEvent, Long eventId) {
        OrderHistoryEvent event = initEvent(eventId, ITEMS_UPDATED, clientRole, orderBefore, orderAfter);
        if (prevEvent == null) {
            event.setFromDate(new Date());
            event.setToDate(new Date());
            return event;
        }
        Date date;
        if (prevEvent.getFromDate() == null) {
            date = new Date();
        } else {
            date = new Date(prevEvent.getFromDate().getTime() + 1000);
        }
        prevEvent.setToDate(date);
        event.setFromDate(date);
        return event;
    }

    private static OrderHistoryEvent initEvent(Long eventId, HistoryEventType eventType, ClientRole clientRole,
                                               Order orderBefore,
                                               Order orderAfter) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(eventId);
        event.setAuthor(new ClientInfo(clientRole, null));
        event.setType(eventType);
        event.setOrderBefore(orderBefore);
        event.setOrderAfter(orderAfter);
        return event;
    }

    @Test
    @DisplayName("Подсчет изменений для одного ивента создания заказа")
    void withNewOrderEvent() {
        OrderItem item = initItem(1L, 2);
        Order order = initOrder(item);
        OrderHistoryEvent event = initNewOrderEvent(4L, order);
        assertThat(event.getFromDate()).isNull();
        assertThat(event.getToDate()).isNull();

        OrderChanges result = OrderChangeCalculator.calculate(event);

        assertChangesInfo(result, 4L);
        assertThat(result.getChangedByStage()).isEmpty();
        assertThat(result.getChangedTotal().getDeltaItemsTotal()).isEqualTo(new BigDecimal(0));
        assertThat(result.getItemsChanged()).isEmpty();
    }

    @Test
    @DisplayName("Подсчет изменений для одного ивента c полным удалением товара из заказа")
    void withItemUpdatedEvent() {
        Order orderBefore = initOrder(initItem(1L, 2), initItem(2L, 1));
        Order orderAfter = initOrder(initItem(2L, 1));
        OrderHistoryEvent event = initItemUpdatedEvent(WAREHOUSE, orderBefore, orderAfter, null, 5L);
        assertThat(event.getFromDate()).isNotNull();
        assertThat(event.getToDate()).isNotNull();

        OrderChanges result = OrderChangeCalculator.calculate(event);

        assertChangesInfo(result, 5L);
        assertThat(result.getChangedByStage()).hasSize(1);
        assertPriceChanges(result.getChangedTotal(), TOTAL, 48, 16);
        assertPriceChanges(extractOtherPriceChange(result), OTHER, 48, 16);
        assertThat(result.getItemsChanged()).hasSize(1);
        assertItemChangedContains(result, OTHER, 1L, 2, 0);
    }

    @Test
    @DisplayName("Подсчет изменений для одного ивента создания заказа произошедшего из за частичного выкупа")
    void withItemUpdatedEventOnBuyBack() {
        Order orderBefore = initOrder(initItem(1L, 2));
        Order orderAfter = initOrder(initItem(1L, 1));
        OrderHistoryEvent event = initItemUpdatedEvent(DELIVERY_SERVICE, orderBefore, orderAfter, null, 5L);

        OrderChanges result = OrderChangeCalculator.calculate(event);

        assertChangesInfo(result, 5L);
        assertThat(result.getChangedByStage()).hasSize(1);
        assertPriceChanges(result.getChangedTotal(), TOTAL, 32, 16);
        assertPriceChanges(extractBuybackChange(result), PARTIAL_BUYBACK, 32, 16);
        assertThat(result.getItemsChanged()).hasSize(1);
        assertItemChangedContains(result, PARTIAL_BUYBACK, 1L, 2, 1);
    }

    @Test
    @DisplayName("Несколько изменений без полного удаления товара")
    void changesWithPartialRemoveItem() {
        Order event1OrderAfter = initOrder(initItem(1L, 3));
        Order event2OrderAfter = initOrder(initItem(1L, 2));
        Order event3OrderAfter = initOrder(initItem(1L, 1));
        OrderHistoryEvent event1 = initNewOrderEvent(5L, event1OrderAfter);
        OrderHistoryEvent event2 = initItemUpdatedEvent(WAREHOUSE, event1OrderAfter, event2OrderAfter, event1, 6L);
        OrderHistoryEvent event3 = initItemUpdatedEvent(DELIVERY_SERVICE, event2OrderAfter, event3OrderAfter, event2,
                7L);

        OrderChanges result = OrderChangeCalculator.calculate(List.of(event1, event2, event3));

        assertChangesInfo(result, 5L, 6L, 7L);
        assertThat(result.getChangedByStage()).hasSize(2);
        assertPriceChanges(result.getChangedTotal(), TOTAL, 48, 16);
        assertPriceChanges(extractOtherPriceChange(result), OTHER, 48, 32);
        assertPriceChanges(extractBuybackChange(result), PARTIAL_BUYBACK, 32, 16);
        assertThat(result.getItemsChanged()).hasSize(2);
        assertItemChangedContains(result, OTHER, 1L, 3, 2);
        assertItemChangedContains(result, PARTIAL_BUYBACK, 1L, 2, 1);
    }

    @Test
    @DisplayName("Несколько изменений полным удалением товара")
    void changesWithFullAndPartialRemoveItem() {
        Order event1OrderAfter = initOrder(initItem(1L, 3), initItem(2L, 1));
        Order event2OrderAfter = initOrder(initItem(1L, 2));
        Order event3OrderAfter = initOrder(initItem(1L, 1));
        OrderHistoryEvent event1 = initNewOrderEvent(5L, event1OrderAfter);
        OrderHistoryEvent event2 = initItemUpdatedEvent(WAREHOUSE, event1OrderAfter, event2OrderAfter, event1, 6L);
        OrderHistoryEvent event3 = initItemUpdatedEvent(DELIVERY_SERVICE, event2OrderAfter, event3OrderAfter, event2,
                7L);

        OrderChanges result = OrderChangeCalculator.calculate(List.of(event1, event2, event3));

        assertChangesInfo(result, 5L, 6L, 7L);
        assertThat(result.getChangedByStage()).hasSize(2);
        assertPriceChanges(result.getChangedTotal(), TOTAL, 64, 16);
        assertPriceChanges(extractOtherPriceChange(result), OTHER, 64, 32);
        assertPriceChanges(extractBuybackChange(result), PARTIAL_BUYBACK, 32, 16);
        assertThat(result.getItemsChanged()).hasSize(3);
        assertItemChangedContains(result, OTHER, 1L, 3, 2);
        assertItemChangedContains(result, OTHER, 2L, 1, 0);
        assertItemChangedContains(result, PARTIAL_BUYBACK, 1L, 2, 1);
    }

    private void assertItemChangedContains(OrderChanges result, ChangesStage stage, Long itemId, int beforeCount,
                                           int afterCount) {
        OrderItemChanges itemChange = result.getItemsChanged().stream()
                .filter(change -> change.getStage() == stage && change.getItem().getId().equals(itemId))
                .findAny()
                .orElseThrow(() -> new AssertionException("Не найдено изменений для товара с id " + itemId +
                        " произошедших на стадии " + stage));
        assertThat(itemChange.getBeforeCount()).isEqualTo(beforeCount);
        assertThat(itemChange.getAfterCount()).isEqualTo(afterCount);
        assertThat(itemChange.getBeforeQuantity()).isEqualByComparingTo(BigDecimal.valueOf(beforeCount));
        assertThat(itemChange.getAfterQuantity()).isEqualByComparingTo(BigDecimal.valueOf(afterCount));
    }

    private void assertChangesInfo(OrderChanges result, Long... eventIds) {
        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getEvents()).containsExactlyInAnyOrder(eventIds);
        assertThat(result.getCurrency()).isEqualTo(CURRENCY);
        assertThat(result.getBuyerCurrency()).isEqualTo(CURRENCY);
        assertThat(result.getExchangeRate()).isEqualTo(EXCHANGE_RATE);
    }

    private OrderPricesChanges extractOtherPriceChange(OrderChanges changes) {
        return changes.getChangedByStage().stream()
                .filter(change -> change.getStage() == OTHER)
                .findAny()
                .orElseThrow(() -> new AssertionException("OrderChanges with type OTHER does not find"));
    }

    private OrderPricesChanges extractBuybackChange(OrderChanges changes) {
        return changes.getChangedByStage().stream()
                .filter(change -> change.getStage() == PARTIAL_BUYBACK)
                .findAny()
                .orElseThrow(() -> new AssertionException("OrderChanges with type PARTIAL_BUYBACK does not find"));
    }

    private void assertPriceChanges(OrderPricesChanges changedTotal, ChangesStage expectedStage, int beforeTotal,
                                    int afterTotal) {
        assertThat(changedTotal.getStage()).isEqualTo(expectedStage);
        assertThat(changedTotal.getBeforeItemsTotal()).isEqualTo(new BigDecimal(beforeTotal));
        assertThat(changedTotal.getAfterItemsTotal()).isEqualTo(new BigDecimal(afterTotal));
        assertThat(changedTotal.getDeltaBuyerItemsTotal()).isEqualTo(new BigDecimal(afterTotal - beforeTotal));
    }

    private OrderItem initItem(Long itemId, int count) {
        OrderItem item = new OrderItem();
        item.setId(itemId);
        item.setCount(count);
        item.setFeedId(item.getId());
        item.setBundleId("bundleId" + item.getId());
        item.setPrice(new BigDecimal(16));
        item.setBuyerPrice(new BigDecimal(16));
        return item;
    }
}
