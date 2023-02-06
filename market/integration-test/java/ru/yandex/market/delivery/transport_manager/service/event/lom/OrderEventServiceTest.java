package ru.yandex.market.delivery.transport_manager.service.event.lom;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.OptimisticLockException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.converter.LomConverter;
import ru.yandex.market.delivery.transport_manager.domain.dto.order.full.OrderDto;
import ru.yandex.market.delivery.transport_manager.domain.entity.Order;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderBindingType;
import ru.yandex.market.delivery.transport_manager.domain.enums.OrderStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.order_route.BindOrderProducer;
import ru.yandex.market.delivery.transport_manager.service.order.OrderService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class OrderEventServiceTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LomConverter lomConverter;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private OrderEventService orderEventService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private BindOrderProducer bindOrderProducer;

    @Test
    @ExpectedDatabase(value = "/service/event/db/init_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void processNewOrderEvent() {
        orderEventService.processEvents(
            createEvent(5001L, "service/event/logbroker/externalIdChanged/snapshot.json")
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/service/event/db/order_with_only_required_fields.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processNewOrderWithOnlyRequiredFieldsEvent() {
        orderEventService.processEvents(
            createEvent(5001L, "service/event/logbroker/WithOnlyRequiredFields/snapshot.json")
        );
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(value = "/service/event/db/updated_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void processUpdateOrderEvent() {
        orderEventService.processEvents(
            createEvent(5002L, "service/event/logbroker/addSegment/snapshot.json")
        );
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(
        value = "/service/event/db/updated_order_with_last_external_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processAppendedSegmentWithExternalId() {
        orderEventService.processEvents(
            createEvent(5002L, "service/event/logbroker/addSegmentWithExternalId/snapshot.json")
        );
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(
        value = "/service/event/db/updated_order_with_updated_route.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void processRouteUuidChanged() {
        orderEventService.processEvents(
            createEvent(5002L, "service/event/logbroker/routeUuidChanged/snapshot.json")
        );
        verify(bindOrderProducer).enqueue(Set.of(1L), OrderBindingType.ON_ROUTE_UPDATE, true);
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(value = "/service/event/db/updated_order_with_ff_id.xml", assertionMode = NON_STRICT_UNORDERED)
    void processUpdateOrderExternalId() {
        orderEventService.processEvents(
            createEvent(5002L, "service/event/logbroker/ffExternalIdChanged/snapshot.json")
        );
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(value = "/service/event/db/cancelled_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void processCancelOrderEvent() {
        orderEventService.processEvents(
            createEvent(5003L, "service/event/logbroker/cancelOrder/snapshot.json")
        );
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(value = "/service/event/db/init_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void skipOldEvent() {
        orderEventService.processEvents(
            createEvent(5000L, "service/event/logbroker/externalIdChanged/snapshot.json")
        );
    }

    @Test
    @ExpectedDatabase(value = "/service/event/db/order.xml", assertionMode = NON_STRICT_UNORDERED)
    void failOnParallelInsert() {
        doAnswer(invocation -> {
            transactionTemplate.execute(status -> orderService.create(getOrderMock(5001L)));
            return invocation.callRealMethod();
        }).when(lomConverter).convert(any(OrderDto.class));

        softly.assertThatThrownBy(() -> orderEventService.processEvents(
            createEvent(5000L, "service/event/logbroker/externalIdChanged/snapshot.json")
        ))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("ERROR: duplicate key value violates unique constraint \"orders_barcode_key\"");
    }

    @Test
    @DatabaseSetup("/service/event/db/init_order.xml")
    @ExpectedDatabase(value = "/service/event/db/cancelled_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void failOnParallelUpdate() {
        doAnswer(invocation -> {
            transactionTemplate.execute(status -> {
                Order oldOrder = orderService.findByBarcodes(Set.of("6734209")).stream().findFirst().orElseThrow();
                orderService.update(oldOrder, getOrderMock(5003L).setStatus(OrderStatus.CANCELLED));
                return null;
            });
            return invocation.callRealMethod();
        }).when(lomConverter).convert(any(OrderDto.class));

        softly.assertThatThrownBy(() -> orderEventService.processEvents(
            createEvent(5002L, "service/event/logbroker/externalIdChanged/snapshot.json")
        ))
            .isInstanceOf(OptimisticLockException.class)
            .hasMessage("Cannot find old version of order with id=1, logbrokerId=5001 to update");
    }

    @Nonnull
    private Map<Long, JsonNode> createEvent(long logbrokerId, String orderSnapshotPath) {
        try {
            return Map.of(
                logbrokerId,
                objectMapper.readTree(extractFileContent(orderSnapshotPath))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Order getOrderMock(long logbrokerId) {
        return new Order()
            .setBarcode("6734209")
            .setSenderId(431782L)
            .setSenderMarketId(147L)
            .setStatus(OrderStatus.PROCESSING)
            .setReturnSortingCenterId(172L)
            .setLogbrokerId(logbrokerId);
    }

}
