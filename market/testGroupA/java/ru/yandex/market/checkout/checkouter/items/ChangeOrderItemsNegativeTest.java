package ru.yandex.market.checkout.checkouter.items;

import java.util.ArrayList;
import java.util.Collection;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemsException;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static ru.yandex.market.checkout.checkouter.items.AbstractChangeOrderItemsTestBase.OrderConfig.defaultConfig;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.CANNOT_REMOVE_LAST_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.INVALID_ITEM_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEMS_ADDITION_NOT_SUPPORTED_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_DUPLICATE_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_NOT_FOUND_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException.notAllowed;
import static ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper.checkErroneousResponse;
import static ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper.reduceOneItem;
import static ru.yandex.market.checkout.util.ClientHelper.shopClientFor;

public class ChangeOrderItemsNegativeTest extends AbstractChangeOrderItemsTestBase {

    @Override
    @BeforeEach
    public void prepare() throws Exception {
        super.prepare();
        createOrder(defaultConfig().with(DeliveryType.PICKUP));
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Disabled("Теперь добавления айтемов с null == id считаются как новые")
    @DisplayName("Изменение товара в заказе без указания его id")
    //https://testpalm.yandex-team.ru/testcase/checkouter-98
    public void itemsWithoutId() throws Exception {
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        newItems.get(0).setId(null);

        checkErroneousResponse(changeOrderItems(newItems),
                new OrderItemsException(ITEM_NOT_FOUND_CODE));
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Изменение количества товара в заказе на некорректное значение")
    //https://testpalm.yandex-team.ru/testcase/checkouter-99
    public void itemsWithInvalidCount() throws Exception {
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        newItems.get(0).setCount(-1);

        checkErroneousResponse(changeOrderItems(newItems),
                new OrderItemsException(INVALID_ITEM_CODE));
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Дублирование товарных позиций при изменении состава заказа")
    //https://testpalm.yandex-team.ru/testcase/checkouter-100
    public void itemDuplicates() throws Exception {
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        newItems.add(newItems.get(0));

        checkErroneousResponse(changeOrderItems(newItems),
                new OrderItemsException(ITEM_DUPLICATE_CODE));
    }

    /**
     * Теперь добавления айтемов с null == id считаются как новые
     */
    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Увеличение количества товаров в заказе")
    //https://testpalm.yandex-team.ru/testcase/checkouter-96
    //https://testpalm.yandex-team.ru/testcase/checkouter-97
    public void cannotAddNewItems() throws Exception {
        createOrder(defaultConfig().with(PaymentType.PREPAID).with(DeliveryType.PICKUP));
        paymentHelper.payForOrder(order);
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        newItems.add(OrderItemProvider.buildOrderItem("333", (Long) null, 1));

        OrderItemsException expectedExc = new OrderItemsException(ITEMS_ADDITION_NOT_SUPPORTED_CODE);
        checkErroneousResponse(changeOrderItems(newItems), expectedExc);

        newItems = new ArrayList<>(order.getItems());
        OrderItem item = newItems.get(0);
        item.setCount(item.getCount() + 1);

        checkErroneousResponse(changeOrderItems(newItems), expectedExc);
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Удаление последнего товара в заказе")
    //https://testpalm.yandex-team.ru/testcase/checkouter-95
    public void cannotRemoveLastItem() throws Exception {
        ArrayList<OrderItem> newItems = new ArrayList<>(order.getItems());
        newItems.forEach(i -> i.setCount(0));

        OrderItemsException expectedExc = new OrderItemsException(CANNOT_REMOVE_LAST_ITEM_CODE);
        checkErroneousResponse(changeOrderItems(newItems), expectedExc);

        newItems.clear();
        checkErroneousResponse(changeOrderItems(newItems), expectedExc);
    }

    @Test
    @DisplayName("Изменение состава несуществующего заказа")
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    //https://testpalm.yandex-team.ru/testcase/checkouter-101
    public void missingOrder() throws Exception {
        long fakeOrderId = 1234321;
        checkErroneousResponse(changeOrderItems(order.getItems(), shopClientFor(order), fakeOrderId),
                new OrderNotFoundException(fakeOrderId));
    }

    @Test
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_ITEMS)
    @DisplayName("Изменение состава заказа в неподходящем статусе")
    //https://testpalm.yandex-team.ru/testcase/checkouter-103
    public void invalidOrderStatus() throws Exception {
        Collection<OrderItem> newItems = reduceOneItem(order.getItems());

        // DELIVERY
        updateOrderStatus(OrderStatus.DELIVERY);
        OrderStatusNotAllowedException expectedExc = notAllowed(order.getId(), null, null);

        // DELIVERED
        updateOrderStatus(OrderStatus.DELIVERED);
        checkErroneousResponse(changeOrderItems(newItems), expectedExc);

        // UNPAID
        newItems = reduceOneItem(order.getItems());
        checkErroneousResponse(changeOrderItems(newItems), expectedExc);
    }

    private void updateOrderStatus(OrderStatus newStatus) {
        ClientInfo clientInfo = shopClientFor(order);
        if (order.getStatus() == PENDING || order.getDelivery().getDeliveryPartnerType() != DeliveryPartnerType.SHOP) {
            clientInfo = ClientInfo.SYSTEM;

        }
        orderUpdateService.updateOrderStatus(
                order.getId(),
                newStatus,
                clientInfo
        );
        refreshOrder();
    }
}
