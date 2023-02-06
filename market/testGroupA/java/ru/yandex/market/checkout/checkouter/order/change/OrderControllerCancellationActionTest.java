package ru.yandex.market.checkout.checkouter.order.change;

import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;

import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

public class OrderControllerCancellationActionTest extends AbstractOrderCancelationActionTest {

    private static final Set<OrderStatus> CANCELLATION_ORDER_STATUSES = Sets.newLinkedHashSet(PROCESSING);

    public static Stream<Arguments> parameterizedTestData() {

        return CANCELLATION_ORDER_STATUSES.stream()
                .map(status -> new Object[]{status})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    //MARKETCHECKOUT-10153 - negative case
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void cancellationOfProcessingOrdersShouldntPassToStockUnfreeze(OrderStatus orderStatus)
            throws StockStorageFreezeNotFoundException {
        Order order = generateOrder(orderStatus);
        AtomicBoolean clientPass = new AtomicBoolean(false);

        StockStorageOrderClient stockStorageOrderClient = mockStockServiceClient((orderId, isCanceled) -> {
            Assertions.assertEquals(order.getId().toString(), orderId);
            Assertions.assertFalse(isCanceled);
            clientPass.set(true);
        });
        stockStorageService.setStockStorageOrderClient(stockStorageOrderClient);

        Assertions.assertTrue(CANCELLATION_ORDER_STATUSES.contains(order.getStatus()), MessageFormat.format(
                "order status must be one of ({0}) but pass as {1}", CANCELLATION_ORDER_STATUSES,
                order.getStatus()));

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.CUSTOM);

        Assertions.assertEquals(orderService.getOrder(order.getId()).getStatus(), OrderStatus.CANCELLED);
        Assertions.assertTrue(clientPass.get());
    }
}
