package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrdersCountingService;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.FillEmptyPicUrlInOrderItemPartitionTaskV2Factory;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FillEmptyPicUrlInOrderItemTaskV2Test extends AbstractWebTestBase {
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrdersCountingService ordersCountingService;

    private LocalDateTime dateFrom;

    @Autowired
    private FillEmptyPicUrlInOrderItemPartitionTaskV2Factory fillEmptyPicUrlInOrderItemPartitionTaskV2Factory;

    @BeforeEach
    public void beforeTest() {
        dateFrom = LocalDateTime.now();
    }

    @Test
    public void testSingleOrderUpdate() {
        final Order order = orderServiceHelper.createPostOrder(
                o -> {
                    o.setRgb(Color.BLUE);
                    o.getItems().forEach(orderItem -> orderItem.setPictureURL(null));
                }
        );

        final OrderItem itemToUpdate = order.getItems().iterator().next();
        mockOrderResultFromReport(order, itemToUpdate);

        fillEmptyPicUrlInOrderItemPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        final Order updatedOrder = orderService.getOrder(order.getId());
        assertEquals("someUrl", updatedOrder.getItem(itemToUpdate.getId()).getPictureURL());
        assertEquals(0, ordersCountingService.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));
    }

    @Test
    public void testMultipleItemsUpdate() {
        final Order order = orderServiceHelper.createPostOrder(o -> {
            o.addItem(OrderItemProvider.getOrderItem());
            o.setRgb(Color.BLUE);
            o.getItems().forEach(orderItem -> orderItem.setPictureURL(null));
        });

        final OrderItem itemToUpdate = order.getItems().iterator().next();
        mockOrderResultFromReport(order, itemToUpdate);

        fillEmptyPicUrlInOrderItemPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        final Order updatedOrder = orderService.getOrder(order.getId());
        assertEquals("someUrl", updatedOrder.getItem(itemToUpdate.getId()).getPictureURL());
        assertEquals(1, ordersCountingService.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));
    }

    @Test
    public void testMultipleOrdersUpdate() {
        final Order order1 = orderServiceHelper.createPostOrder(o -> o.setRgb(Color.BLUE));
        final Order order2 = orderServiceHelper.createPostOrder(o -> o.setRgb(Color.BLUE));

        final OrderItem itemToUpdate = order1.getItems().iterator().next();
        mockOrderResultFromReport(order1, itemToUpdate);
        mockOrderResultFromReport(order2);

        fillEmptyPicUrlInOrderItemPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        final Order updatedOrder = orderService.getOrder(order1.getId());
        assertEquals("someUrl", updatedOrder.getItem(itemToUpdate.getId()).getPictureURL());
        assertEquals(1, ordersCountingService.countOrdersWithEmptyPicUrlOnItem(Color.ALIVE_COLORS, dateFrom));
    }

    private void mockOrderResultFromReport(Order order, OrderItem... itemsWithPics) {
        Parameters parameters = new Parameters(order);

        if (itemsWithPics != null) {
            for (OrderItem item : order.getItems()) {
                if (Arrays.asList(itemsWithPics).contains(item)) {
                    parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setPicUrl("someUrl");
                } else {
                    parameters.getReportParameters().overrideItemInfo(item.getFeedOfferId()).setHideOffer(true);
                }
            }
        }
        reportConfigurer.mockReportPlace(MarketReportPlace.OFFER_INFO, parameters.getReportParameters());
    }
}
