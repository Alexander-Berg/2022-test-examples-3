package ru.yandex.market.checkout.checkouter.items;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.checkout.checkouter.items.AbstractChangeOrderItemsTestBase.OrderConfig.defaultConfig;
import static ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper.checkSuccessResponse;

//ignored until 'remove items' is refactored and supported in production
@Disabled
public class IncreasePostpaidPendingOrderItemsTest extends AbstractChangeOrderItemsTestBase {

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;

    @BeforeEach
    public void initOrder() throws Exception {
        createOrder(defaultConfig()
                .withColor(Color.BLUE)
                .with(OrderAcceptMethod.WEB_INTERFACE)
                .fulfillment(true));
        stockStorageConfigurer.mockOkForRefreeze();
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Увеличение количества товаров в заказе")
    //https://testpalm.yandex-team.ru/testcase/checkouter-96
    public void canAddNewItems() throws Exception {
        List<FeedOfferId> oldOrderItems = order.getItems().stream().map(OrderItem::getFeedOfferId)
                .collect(Collectors.toList());
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        OrderItem newItem = changeOrderItemsHelper.addNewItem(order);
        newItems.add(newItem);
        checkSuccessResponse(changeOrderItems(newItems));

        refreshOrder();
        OrderItem orderItem = order.getItems().stream()
                .filter(oi -> !oldOrderItems.contains(oi.getFeedOfferId()))
                .findAny()
                .orElse(null);
        assertThat(orderItem, notNullValue());
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Добавление товара только с обязательными полями")
    //https://testpalm.yandex-team.ru/testcase/checkouter-96
    public void canAddNewItemWithMinimumFields() throws Exception {
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        OrderItem newItem = changeOrderItemsHelper.addBareItem(order);
        newItems.add(newItem);

        checkSuccessResponse(changeOrderItems(newItems));

        refreshOrder();
        assertThat(order.getItem(newItem.getFeedOfferId()), notNullValue());
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Увеличение количества позиции в заказе")
    //https://testpalm.yandex-team.ru/testcase/checkouter-97
    public void canIncreaseItemCount() throws Exception {
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());

        OrderItem item = newItems.get(0);
        int expectedCount = item.getCount() + 1;
        item.setCount(expectedCount);

        checkSuccessResponse(changeOrderItems(newItems));

        refreshOrder();
        OrderItem orderItem = order.getItem(item.getId());
        assertThat(orderItem, notNullValue());
        assertThat(orderItem, hasProperty("count", equalTo(expectedCount)));
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Одновременно уменьшить количество товаров и добавить новых в заказ")
    public void canAddNewItemsWithPromo() throws Exception {
        List<FeedOfferId> oldOrderItems = order.getItems().stream().map(OrderItem::getFeedOfferId)
                .collect(Collectors.toList());
        List<OrderItem> newItems = new ArrayList<>(order.getItems());
        // Добавляем товар
        OrderItem newItem = changeOrderItemsHelper.addNewItem(order);
        newItems.add(newItem);
        // Уменьшаем количество товара
        OrderItem reducedItem = newItems.get(0);
        int expectedCount = reducedItem.getCount() - 1;
        reducedItem.setCount(expectedCount);

        checkSuccessResponse(changeOrderItems(newItems));

        refreshOrder();
        OrderItem orderItem = order.getItem(reducedItem.getId());
        assertThat(orderItem, notNullValue());
        assertThat(orderItem, hasProperty("count", equalTo(expectedCount)));

        orderItem = order.getItems().stream()
                .filter(oi -> !oldOrderItems.contains(oi.getFeedOfferId()))
                .findAny()
                .orElse(null);
        assertThat(orderItem, notNullValue());
    }

}
