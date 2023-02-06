package ru.yandex.market.checkout.checkouter.controller.archive;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArchiveGetOrdersTest extends AbstractArchiveWebTestBase {

    @Test
    void checkGetOrdersOrdering() throws IOException {
        checkouterProperties.setArchiveAPIEnabled(true);

        freezeTime();
        Order firstOrder = archiveOrder(createBlueOrder());
        moveArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE);

        jumpToFuture(4, ChronoUnit.DAYS);
        Order secondOrder = archiveOrder(createBlueOrder());

        jumpToFuture(8, ChronoUnit.DAYS);
        Order thirdOrder = archiveOrder(createBlueOrder());

        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        //get all records
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE}).withArchived(true).build();
        PagedOrders pagedOrders = client.getOrders(clientInfo, request);
        assertThat(pagedOrders.getItems(), hasSize(3));
        //check pager
        checkPager(pagedOrders.getPager(), 3, 1, 10);

        //check list ordering
        List<Order> orders = (List<Order>) pagedOrders.getItems();
        assertEquals(thirdOrder.getId(), orders.get(0).getId());
        assertEquals(secondOrder.getId(), orders.get(1).getId());
        assertEquals(firstOrder.getId(), orders.get(2).getId());

        int count = client.getOrdersCount(clientInfo, request);
        assertEquals(3, count);

        getNthPage(clientInfo, 1, thirdOrder.getId());
        getNthPage(clientInfo, 2, secondOrder.getId());
        getNthPage(clientInfo, 3, firstOrder.getId());
    }

    private void getNthPage(RequestClientInfo clientInfo, int page, long expectedOrderId) {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE}).withArchived(true).withPageInfo(Pager.atPage(page, 1)).build();
        PagedOrders orders = client.getOrders(clientInfo, request);
        assertThat(orders.getItems(), hasSize(1));
        assertEquals(expectedOrderId, orders.getItems().iterator().next().getId());

        checkPager(orders.getPager(), 3, page, 1);

        int count = client.getOrdersCount(clientInfo, request);
        assertEquals(3, count);
    }

    @Test
    void checkGetOrdersWithEqualCreatedDate() throws IOException {
        checkouterProperties.setArchiveAPIEnabled(true);

        LocalDateTime createdDate = LocalDateTime.now(getClock());

        Order orderInArchive = archiveOrder(createBlueOrder());
        setOrderCreatedDate(orderInArchive, createdDate);
        moveArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE);

        Order orderInBasic = archiveOrder(createBlueOrder());
        setOrderCreatedDate(orderInBasic, createdDate);

        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(new Color[]{Color.BLUE}).withArchived(true).build();
        PagedOrders pagedOrders = client.getOrders(clientInfo, request);
        assertThat(pagedOrders.getItems(), hasSize(2));
        //check pager
        checkPager(pagedOrders.getPager(), 2, 1, 10);

        //check list ordering
        List<Order> orders = (List<Order>) pagedOrders.getItems();
        assertEquals(orderInBasic.getId(), orders.get(0).getId());
        assertEquals(orderInArchive.getId(), orders.get(1).getId());

        int count = client.getOrdersCount(clientInfo, request);
        assertEquals(2, count);
    }

    private void checkPager(Pager actualPager, int total, int page, int pageSize) {
        Pager expectedPager = Pager.build(Pager.atPage(page, pageSize), total);
        assertEquals(expectedPager, actualPager);
    }

    private void setOrderCreatedDate(Order order, LocalDateTime createdAt) {
        transactionTemplate.execute(ts -> masterJdbcTemplate.update("update orders set created_at=? where id=?",
                createdAt, order.getId()));
    }
}
