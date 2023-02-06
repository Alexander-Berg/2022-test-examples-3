package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class CreateOrderWithMultipleItemsTest extends AbstractWebTestBase {

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-125
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с несколькими товарами")
    @Test
    public void shouldCreateOrderWithMultipleItemsAndCalculateTotalsCorrectly() throws Exception {
        Parameters parameters = new Parameters(OrderProvider.getBlueOrder((o) -> {
            List<OrderItem> items = new ArrayList<>(o.getItems());

            items.add(OrderItemProvider.orderItemBuilder()
                    .configure(OrderItemProvider::applyDefaults)
                    .offer("2")
                    .count(2)
                    .price(500)
                    .build());

            o.setItems(items);
        }));

        parameters.setDeliveryServiceId(DeliveryProvider.FF_DELIVERY_SERVICE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems(), hasSize(2));
        // 250 + 2 * 500
        Assertions.assertEquals(new BigDecimal("1250"), order.getBuyerItemsTotal());
        // 250 + 2 * 500 + 100 (доставка)
        Assertions.assertEquals(new BigDecimal("1350"), order.getBuyerTotal());

        Assertions.assertEquals(new BigDecimal("1250"), order.getItemsTotal());
        Assertions.assertEquals(new BigDecimal("1350"), order.getTotal());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-126
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с количеством больше одного")
    @Test
    public void shouldCreateOrderWithSingleItemAndMultipleCount() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setDeliveryServiceId(DeliveryProvider.FF_DELIVERY_SERVICE_ID);
        parameters.getOrder().getItems().forEach(oi -> oi.setCount(2));
        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getItems(), hasSize(1));

        OrderItem item = Iterables.getOnlyElement(order.getItems());
        Assertions.assertEquals(2L, item.getCount().longValue());

        Assertions.assertEquals(new BigDecimal("100"), order.getDelivery().getBuyerPrice());
        // 2 * 250
        Assertions.assertEquals(new BigDecimal("500"), order.getBuyerItemsTotal());
        // 2 * 250 + 10
        Assertions.assertEquals(new BigDecimal("600"), order.getBuyerTotal());
    }
}
