package ru.yandex.market.checkout.checkouter.client;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckouterClientGetAccessibleOrdersTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void testGetAccessibleOrderIds() {
        Order testOrder = orderServiceHelper.createPostOrder();
        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE})
                .withOrderIds(new Long[]{testOrder.getId()})
                .build();
        var actualResult = client.getAccessibleOrderIds(request, ClientRole.USER, testOrder.getBuyer().getUid());
        Assertions.assertEquals(Collections.singleton(testOrder.getId()), actualResult);
    }

    @Test
    public void testGetAccessibleOrderIds2OrdersAndDifferentBuyer() {
        var firstOrder = orderServiceHelper.createPostOrder();
        var secondOrder = orderServiceHelper.createPostOrder(order -> order.setBuyer(firstOrder.getBuyer()));
        // different buyer
        var thirdOrder = orderServiceHelper.createPostOrder(order ->
                order.setBuyer(BuyerProvider.getDefaultBuyer(125L))
        );

        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE})
                .withOrderIds(new Long[]{firstOrder.getId(), secondOrder.getId(), thirdOrder.getId()})
                .build();
        var actualResult = client.getAccessibleOrderIds(request, ClientRole.USER,
                firstOrder.getBuyer().getUid());
        var expectedIds = Set.of(firstOrder.getId(), secondOrder.getId());
        Assertions.assertEquals(expectedIds, actualResult);
    }

    @Test
    public void testGetAccessibleOrderIdsByOneColor() {
        var firstOrder = orderServiceHelper.createPostOrder();
        var secondOrder = orderServiceHelper.createPostOrder(order -> {
            order.setBuyer(firstOrder.getBuyer());
            order.setRgb(Color.WHITE);
        });
        // different buyer
        var thirdOrder = orderServiceHelper.createPostOrder(order ->
                order.setBuyer(BuyerProvider.getDefaultBuyer(125L))
        );

        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE})
                .withOrderIds(new Long[]{firstOrder.getId(), secondOrder.getId(), thirdOrder.getId()})
                .build();
        var actualResult = client.getAccessibleOrderIds(request, ClientRole.USER,
                firstOrder.getBuyer().getUid());
        var expectedIds = Collections.singleton(firstOrder.getId());
        Assertions.assertEquals(expectedIds, actualResult);
    }

    @Test
    public void testGetAccessibleOrderIdsBy2Colors() {
        var firstOrder = orderServiceHelper.createPostOrder();
        var secondOrder = orderServiceHelper.createPostOrder(order -> {
            order.setBuyer(firstOrder.getBuyer());
            order.setRgb(Color.WHITE);
        });

        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE, Color.WHITE})
                .withOrderIds(new Long[]{firstOrder.getId(), secondOrder.getId()})
                .build();
        var actualResult = client.getAccessibleOrderIds(request, ClientRole.USER,
                firstOrder.getBuyer().getUid());
        var expectedIds = Set.of(firstOrder.getId(), secondOrder.getId());
        Assertions.assertEquals(expectedIds, actualResult);
    }

    @Test
    public void testGetAccessibleOrderIdsByAllColors() {
        var firstOrder = orderServiceHelper.createPostOrder();
        var secondOrder = orderServiceHelper.createPostOrder(order -> {
            order.setBuyer(firstOrder.getBuyer());
            order.setRgb(Color.WHITE);
        });

        var request = OrderSearchRequest.builder()
                .withOrderIds(new Long[]{firstOrder.getId(), secondOrder.getId()})
                .build();
        var actualResult = client.getAccessibleOrderIds(request, ClientRole.USER,
                firstOrder.getBuyer().getUid());
        var expectedIds = Set.of(firstOrder.getId(), secondOrder.getId());
        Assertions.assertEquals(expectedIds, actualResult);
    }

    @Test
    public void testGetAccessibleOrderIdWithoutClientId() {
        Order testOrder = orderServiceHelper.createPostOrder();
        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE})
                .withOrderIds(new Long[]{testOrder.getId()})
                .build();
        assertThrows(
                ErrorCodeException.class,
                () -> client.getAccessibleOrderIds(request, ClientRole.USER, null)
        );
    }

    @Test
    public void testGetAccessibleOrderIdsWithoutClientRole() {
        Order testOrder = orderServiceHelper.createPostOrder();
        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE})
                .withOrderIds(new Long[]{testOrder.getId()})
                .build();
        assertThrows(
                ErrorCodeException.class,
                () -> client.getAccessibleOrderIds(request, null, testOrder.getBuyer().getUid())
        );
    }

    @Test
    public void testGetAccessibleOrderIdsWithUnknownRole() {
        Order testOrder = orderServiceHelper.createPostOrder();
        var request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE})
                .withOrderIds(new Long[]{testOrder.getId()})
                .build();
        assertThrows(
                ErrorCodeException.class,
                () -> client.getAccessibleOrderIds(request, ClientRole.UNKNOWN, testOrder.getBuyer().getUid())
        );
    }
}
