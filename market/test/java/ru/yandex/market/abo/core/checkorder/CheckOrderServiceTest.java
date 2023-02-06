package ru.yandex.market.abo.core.checkorder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


/**
 * @author artemmz
 * @date 23/10/2019.
 */
class CheckOrderServiceTest {
    @InjectMocks
    CheckOrderService checkOrderService;
    @Mock
    MultiCart multiCart;
    @Mock
    Order order;
    @Mock
    OrderItem orderItem;
    @Mock
    CreateOrderParam createOrderParam;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(multiCart.getCarts()).thenReturn(List.of(order));
        when(order.getItems()).thenReturn(List.of(orderItem));
        when(createOrderParam.getRgb()).thenReturn(Color.BLUE);
    }

    @Test
    void testCheckCountAndDelivery() throws CheckOrderCreationException {
        assertErrorType(CheckOrderScenarioErrorType.EMPTY_CART,
                () -> checkOrderService.checkCountAndDelivery(new MultiCart(), null));

        assertErrorType(CheckOrderScenarioErrorType.EMPTY_DELIVERY,
                () -> checkOrderService.checkCountAndDelivery(multiCart, createOrderParam));

        doReturn(List.of(new Delivery())).when(order).getDeliveryOptions();
        assertErrorType(CheckOrderScenarioErrorType.NO_AVAILABLE_ITEMS,
                () -> checkOrderService.checkCountAndDelivery(multiCart, createOrderParam));

        when(orderItem.getCount()).thenReturn(1);
        when(orderItem.getDelivery()).thenReturn(true);
        checkOrderService.checkCountAndDelivery(multiCart, createOrderParam);
    }


    @Test
    void extractFailureSubCodes() {
        Set<OrderFailure.SubCode> subcodes1 = new HashSet();
        subcodes1.add(OrderFailure.SubCode.DELIVERY);
        subcodes1.add(OrderFailure.SubCode.COUNT);
        subcodes1.add(null);
        var failure1 = new OrderFailure(new Order(),
                OrderFailure.Code.OUT_OF_DATE,
                subcodes1,
                "details1"
        );

        Set<OrderFailure.SubCode> subcodes2 = null;
        var failure2 = new OrderFailure(new Order(),
                OrderFailure.Code.OUT_OF_DATE,
                subcodes2,
                "details2"
        );

        Set<OrderFailure.SubCode> subcodes3 = new HashSet();
        subcodes3.add(OrderFailure.SubCode.DELIVERY);
        subcodes3.add(OrderFailure.SubCode.PRICE);
        subcodes3.add(null);
        var failure3 = new OrderFailure(new Order(),
                OrderFailure.Code.OUT_OF_DATE,
                subcodes3,
                "details3"
        );

        MultiCart cart = new MultiCart();
        cart.setCartFailures(List.of(failure1, failure2, failure3));

        assertEquals(
                Set.of(OrderFailure.SubCode.DELIVERY, OrderFailure.SubCode.COUNT, OrderFailure.SubCode.PRICE),
                CheckOrderService.extractFailureSubCodes(cart)
        );
    }

    private void assertErrorType(CheckOrderScenarioErrorType expected, CheckOrderRunnable runnable) {
        try {
            runnable.run();
        } catch (CheckOrderCreationException ex) {
            assertEquals(expected, ex.getScenarioError().getErrorType());
        }
    }

    private interface CheckOrderRunnable {
        void run() throws CheckOrderCreationException;
    }

}
