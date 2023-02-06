package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderChangesViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemChangesViewModel;
import ru.yandex.market.checkout.helpers.OrderChangesHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MissingItemsOrderChangesTest extends MissingItemsAbstractTest {

    @Autowired
    private OrderChangesHelper orderChangesHelper;

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции")
    void itemReducedIfRemovalAllowed() throws Exception {
        Order order = createOrderWithTwoItems();

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        OrderChangesViewModel orderChanges = orderChangesHelper.getOrderChanges(order.getId(), ClientInfo.SYSTEM);

        assertEquals(order.getId(), orderChanges.getId());
        assertEquals(order.getBuyerCurrency(), orderChanges.getBuyerCurrency());
        assertEquals(order.getCurrency(), orderChanges.getCurrency());
        assertEquals(order.getExchangeRate(), orderChanges.getExchangeRate());
        assertThat(orderChanges.getItemsChanged(), hasSize(1));

        assertNotNull(orderChanges.getChangedTotal());
        assertEquals(new BigDecimal("8020"), orderChanges.getChangedTotal().getBeforeItemsTotal());
        assertEquals(new BigDecimal("8020"), orderChanges.getChangedTotal().getBeforeBuyerItemsTotal());
        assertEquals(new BigDecimal("7243"), orderChanges.getChangedTotal().getAfterItemsTotal());
        assertEquals(new BigDecimal("7243"), orderChanges.getChangedTotal().getAfterBuyerItemsTotal());
        assertEquals(new BigDecimal("-777"), orderChanges.getChangedTotal().getDeltaBuyerItemsTotal());
        assertEquals(new BigDecimal("-777"), orderChanges.getChangedTotal().getDeltaItemsTotal());

        OrderItem minCount = getItemWithMinCount(order);

        Optional<OrderItemChangesViewModel> minCountChangedItem = orderChanges.getItemsChanged().stream()
                .filter(oi -> minCount.getId().equals(oi.getItem().getId()))
                .findAny();

        assertFalse(minCountChangedItem.isPresent());

        OrderItem maxCount = getItemWithMaxCount(order);
        OrderItemChangesViewModel maxCountChangedItem = orderChanges.getItemsChanged().stream()
                .filter(oi -> maxCount.getId().equals(oi.getItem().getId()))
                .findAny()
                .get();

        assertEquals(maxCount.getCount(), maxCountChangedItem.getBeforeCount());
        assertEquals(maxCount.getCount() - 1, maxCountChangedItem.getAfterCount());
        assertEquals(-1, maxCountChangedItem.getDeltaCount());
    }

    @Test
    @DisplayName("Успешное удаление одной позиции закза целиком")
    void itemTotallyRemovedIfRemovalAllowed() throws Exception {
        Order order = createOrderWithTwoItems();

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneTotallyMissingItem(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        OrderChangesViewModel orderChanges = orderChangesHelper.getOrderChanges(order.getId(), ClientInfo.SYSTEM);

        assertNotNull(orderChanges.getChangedTotal());
        assertEquals(new BigDecimal("8020"), orderChanges.getChangedTotal().getBeforeItemsTotal());
        assertEquals(new BigDecimal("8020"), orderChanges.getChangedTotal().getBeforeBuyerItemsTotal());
        assertEquals(new BigDecimal("7770"), orderChanges.getChangedTotal().getAfterItemsTotal());
        assertEquals(new BigDecimal("7770"), orderChanges.getChangedTotal().getAfterBuyerItemsTotal());
        assertEquals(new BigDecimal("-250"), orderChanges.getChangedTotal().getDeltaBuyerItemsTotal());
        assertEquals(new BigDecimal("-250"), orderChanges.getChangedTotal().getDeltaItemsTotal());

        OrderItem minCount = getItemWithMinCount(order);
        OrderItemChangesViewModel minCountChangedItem = orderChanges.getItemsChanged().stream()
                .filter(oi -> minCount.getId().equals(oi.getItem().getId()))
                .findAny()
                .get();

        assertEquals(minCount.getCount(), minCountChangedItem.getBeforeCount());
        assertEquals(0, minCountChangedItem.getAfterCount());
        assertEquals(-minCount.getCount(), minCountChangedItem.getDeltaCount());


        OrderItem maxCount = getItemWithMaxCount(order);
        Optional<OrderItemChangesViewModel> maxCountChangedItem = orderChanges.getItemsChanged().stream()
                .filter(oi -> maxCount.getId().equals(oi.getItem().getId()))
                .findAny();

        assertFalse(maxCountChangedItem.isPresent());
    }

}
