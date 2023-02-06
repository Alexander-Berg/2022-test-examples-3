package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;

public class OrderHistoryEventJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        OrderHistoryEvent orderHistoryEvent = EntityHelper.getOrderHistoryEvent();

        String json = write(orderHistoryEvent);
        System.out.println(json);

        checkJson(json, Names.History.ID, 123);
        checkJson(json, Names.History.TYPE, HistoryEventType.ORDER_DELIVERY_UPDATED.name());
        checkJson(json, Names.History.AUTHOR, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, Names.History.FROM_DATE, "11-11-2017 15:00:00");
        checkJson(json, Names.History.TO_DATE, "15-11-2017 18:00:00");
        checkJson(json, Names.History.TRAN_DATE, "16-11-2017 00:00:00");
        checkJson(json, Names.History.HOST, "host");
        checkJson(json, Names.History.REFUND_ACTUAL, 12.34);
        checkJson(json, Names.History.REFUND_PLANNED, 34.56);
        checkJson(json, Names.History.ORDER_AFTER, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, Names.History.ORDER_BEFORE, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, Names.History.REFUND_EVENT, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, Names.History.RECEIPT, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, Names.History.READ_BY_USER, true);
    }

    @Test
    public void deserialize() throws Exception {
        OrderHistoryEvent orderHistoryEvent = read(OrderHistoryEvent.class, getClass().getResourceAsStream(
                "orderHistoryEvent.json"));

        Assertions.assertEquals(123L, orderHistoryEvent.getId().longValue());
        Assertions.assertEquals(HistoryEventType.ORDER_DELIVERY_UPDATED, orderHistoryEvent.getType());
        Assertions.assertNotNull(orderHistoryEvent.getAuthor());
        Assertions.assertEquals(EntityHelper.CREATION_DATE, orderHistoryEvent.getFromDate());
        Assertions.assertEquals(EntityHelper.UPDATE_DATE, orderHistoryEvent.getToDate());
        Assertions.assertEquals(EntityHelper.STATUS_EXPIRY_DATE, orderHistoryEvent.getTranDate());
        Assertions.assertEquals("host", orderHistoryEvent.getHost());
        Assertions.assertEquals(new BigDecimal("12.34"), orderHistoryEvent.getRefundActual());
        Assertions.assertEquals(new BigDecimal("34.56"), orderHistoryEvent.getRefundPlanned());
        Assertions.assertEquals(345L, orderHistoryEvent.getRefundId().longValue());
        Assertions.assertNotNull(orderHistoryEvent.getOrderAfter());
        Assertions.assertNotNull(orderHistoryEvent.getOrderBefore());
        Assertions.assertNotNull(orderHistoryEvent.getRefundEvent());
        Assertions.assertNotNull(orderHistoryEvent.getReceipt());
        Assertions.assertTrue(orderHistoryEvent.getReadByUser());

    }

}
