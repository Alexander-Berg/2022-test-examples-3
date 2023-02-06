package ru.yandex.market.delivery.transport_manager.event.les;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.les.order.OrderBoundLesExportPayload;
import ru.yandex.market.delivery.transport_manager.service.les.exporter.LesOrderEventExporter;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.tm.OrderBindEvent;

public class LesOrderEventExporterTest extends AbstractContextualTest {
    @Autowired
    private LesOrderEventExporter eventExporter;

    @Autowired
    private LesProducer lesProducer;

    @Autowired
    private DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2022-01-21T10:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup({
        "/repository/order_route/transportations.xml",
        "/repository/order_route/orders.xml"
    })
    void test() {
        Mockito.when(dataFieldMaxValueIncrementer.nextLongValue()).thenReturn(22L);
        OrderBoundLesExportPayload payload = new OrderBoundLesExportPayload(1L, 3L);
        eventExporter.send(payload);

        EventPayload eventPayload = new OrderBindEvent(
            "barcode1",
            3L,
            5L,
            "TMU5",
            Instant.parse("2021-11-15T07:00:00Z"),
            Instant.parse("2021-11-15T08:00:00Z")
        );

        Event event = new Event(
            "tm",
            String.valueOf(22L),
            Instant.now(clock).toEpochMilli(),
            "ORDER_BIND",
            eventPayload,
            "Заказ привязан к отгрузке"
        );
        Mockito.verify(lesProducer).send(event, "tm_out");
    }
}
