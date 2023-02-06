package ru.yandex.market.checkout.checkouter.tasks.eventexport.erp;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;

/**
 * @author : poluektov
 * date: 2020-07-03.
 */
public class ErpEventExportServiceTest extends AbstractWebTestBase {
    @Autowired
    private ErpEventExportService erpEventExportService;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private JdbcTemplate erpJdbcTemplate;
    @Autowired
    private OrderPayHelper orderPayHelper;

    Order order;

    @Test
    public void testExportById() throws Exception {
        Collection<OrderHistoryEvent> orderHistoryEvents = generateSomeEvents();
        int exported = erpEventExportService.exportEventsById(orderHistoryEvents.stream()
                .map(OrderHistoryEvent::getId)
                .collect(Collectors.toSet()));
        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 1);
    }

    @Test
    public void httpMethodTest() throws Exception {
        Collection<OrderHistoryEvent> orderHistoryEvents = generateSomeEvents();

        MockHttpServletRequestBuilder requestBuilder = post("/export-events-to-erp")
                .contentType(MediaType.APPLICATION_JSON)
                .content((new JSONArray(orderHistoryEvents.stream()
                        .map(OrderHistoryEvent::getId)
                        .collect(Collectors.toList()))).toString());
        mockMvc.perform(requestBuilder);
        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 1);
    }

    private Collection<OrderHistoryEvent> generateSomeEvents() throws Exception {
        Parameters ffBlueOrder = BlueParametersProvider.defaultBlueOrderParameters();
        ffBlueOrder.setColor(Color.BLUE);
        ffBlueOrder.setSupplierTypeForAllItems(SupplierType.FIRST_PARTY);
        order = orderCreateHelper.createOrder(ffBlueOrder);
        orderPayHelper.payForOrder(order);
        return eventsGetHelper.getOrderHistoryEvents(order.getId()).getItems();
    }

    private void verifyItemsExported(Order order, Integer count) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(DISTINCT ITEM_ID) FROM COOrderItem WHERE ORDER_ID=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId()
        );
        assertThat(res, equalTo(count));
    }


    private void verifyEventExported(Order order, HistoryEventType type, Consumer<Integer> check) {
        log.info("Checking event export of type: " + type);
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM COOrderEvent WHERE ORDER_ID=? AND EVENT_TYPE=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId(),
                type.name()
        );

        check.accept(res);
    }

    private void verifyEventExported(Order order, HistoryEventType type, Matcher<Integer> matcher) {
        verifyEventExported(order, type, count -> assertThat(count, matcher));
    }


}
