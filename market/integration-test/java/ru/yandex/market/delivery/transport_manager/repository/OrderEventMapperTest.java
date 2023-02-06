package ru.yandex.market.delivery.transport_manager.repository;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderEvent;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderEventId;
import ru.yandex.market.delivery.transport_manager.repository.mappers.OrderEventMapper;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class OrderEventMapperTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderEventMapper orderEventMapper;

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    void getById() throws IOException {
        OrderEvent orderEvent = orderEventMapper.getById(1L);
        assertThatModelEquals(newOrderEvent(), orderEvent);
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    void findReadyForEnqueueIds() {
        List<OrderEventId> orderEventIds = orderEventMapper.findReadyForEnqueueIds();
        softly.assertThat(orderEventIds).hasSize(1);
        assertThatModelEquals(new OrderEventId(1L, 101L, 5001L), orderEventIds.get(0));
    }

    @Test
    @ExpectedDatabase(value = "/repository/order_event/after/order_event.xml", assertionMode = NON_STRICT_UNORDERED)
    void persist() throws IOException {
        Long id = orderEventMapper.persist(newOrderEvent());
        softly.assertThat(id).isNotNull();
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    void findByIds() throws IOException {
        List<OrderEvent> orderEvents = orderEventMapper.findByIds(Set.of(1L));
        softly.assertThat(orderEvents).hasSize(1);
        assertThatModelEquals(newOrderEvent(), orderEvents.get(0));
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    void findByIds_empty() {
        List<OrderEvent> orderEvents = orderEventMapper.findByIds(Set.of());
        softly.assertThat(orderEvents).hasSize(0);
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    @ExpectedDatabase(value = "/repository/order_event/after/no_events.xml", assertionMode = NON_STRICT_UNORDERED)
    void delete() {
        orderEventMapper.delete(Set.of(1L));
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    @ExpectedDatabase(value = "/repository/order_event/after/order_event.xml", assertionMode = NON_STRICT_UNORDERED)
    void deleteNothing() {
        orderEventMapper.delete(Set.of());
        orderEventMapper.delete(Set.of(2L));
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    @ExpectedDatabase(
        value = "/repository/order_event/after/order_event_queued.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void setQueued() {
        orderEventMapper.setQueued(Set.of(1L), true);
    }

    @Test
    @DatabaseSetup("/repository/order_event/before/order_event.xml")
    @ExpectedDatabase(value = "/repository/order_event/after/order_event.xml", assertionMode = NON_STRICT_UNORDERED)
    void setQueuedNothing() {
        orderEventMapper.setQueued(Set.of(), true);
        orderEventMapper.setQueued(Set.of(2L), true);
    }

    private OrderEvent newOrderEvent() throws IOException {
        JsonNode snapshot = objectMapper.readTree(
            extractFileContent("service/event/logbroker/externalIdChanged/snapshot.json")
        );
        return new OrderEvent()
            .setLogbrokerId(5001L)
            .setLomOrderId(101L)
            .setSnapshot(snapshot);
    }

}
