package ru.yandex.market.checkout.checkouter.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.trace.TraceLogHelper.awaitTraceLog;

public class ResultSizeHolderTest extends AbstractTraceLogTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ResultSizeHolderTest.class);

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void houldWriteResultSize() {
        Order order = orderServiceHelper.createOrder(Color.BLUE);

        OrderHistoryEvents events = checkouterAPI.orderHistoryEvents()
                .getOrderHistoryEvents(
                        0, 50, null, false, null,
                        OrderFilter.builder().setRgb(Color.BLUE).build()
                );

        LOG.debug("events size: {}", events.getContent().size());

        verifyTraceLog(events.getContent().size());
    }

    private void verifyTraceLog(int expected) {
        List<Map<String, String>> events = Collections.emptyList();

        events = awaitTraceLog(inMemoryAppender, events);
        assertThat("No events received", events, CoreMatchers.notNullValue());

        Map<String, String> inRecord = events.stream()
                .filter(it -> "IN".equals(it.get("type")))
                .findFirst()
                .get();

        Assertions.assertEquals(String.valueOf(expected), inRecord.get("kv.resultSize"));
    }
}
