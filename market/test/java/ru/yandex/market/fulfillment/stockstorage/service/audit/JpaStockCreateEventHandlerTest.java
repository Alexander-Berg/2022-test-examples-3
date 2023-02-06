package ru.yandex.market.fulfillment.stockstorage.service.audit;


import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.EventType;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FulfillmentFeedId;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.PayloadType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.events.stock.StockEvent;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcFeedIdRepository;


import static org.mockito.Mockito.verify;

public class JpaStockCreateEventHandlerTest {

    private final Clock fixed = Clock.fixed(LocalDateTime.of(2018, 1, 1, 0, 0)
            .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    @Mock
    private JdbcFeedIdRepository repository;

    private JpaStockCreateEventHandler stockEventsHandler;

    private final SoftAssertions assertions = new SoftAssertions();


    @Captor
    private ArgumentCaptor<List<FulfillmentFeedId>> captor;

    @AfterEach
    public void tearDown() {
        assertions.assertAll();
    }

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeEach
    public void setUp() {
        stockEventsHandler = new JpaStockCreateEventHandler(repository);
    }

    @Test
    public void shouldHandle() {
        Collection<StockEvent> events = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            events.add(createStockEvent(i, (int) i, EventType.SKU_CREATED));
        }
        stockEventsHandler.handle(events);
        verify(repository).insertAll(captor.capture());
        List<FulfillmentFeedId> value = captor.getValue();
        assertions.assertThat(value.size()).as("Insert size").isEqualTo(events.size());
    }

    @Test
    public void shouldNotHandleWrongEventType() {
        Collection<StockEvent> events = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            events.add(createStockEvent(i, (int) i, EventType.FREEZE_FORCE_REMOVED));
        }
        events.add(createStockEvent(1, 1, EventType.SKU_CREATED));
        stockEventsHandler.handle(events);
        verify(repository).insertAll(captor.capture());
        List<FulfillmentFeedId> value = captor.getValue();
        assertions.assertThat(value.size()).as("Insert size").isEqualTo(1);
    }

    @Test
    public void shouldInsertOnlyUniquePairs() {
        Collection<StockEvent> events = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            events.add(createStockEvent(1, 1, EventType.SKU_CREATED));
        }
        stockEventsHandler.handle(events);
        verify(repository).insertAll(captor.capture());
        List<FulfillmentFeedId> value = captor.getValue();
        assertions.assertThat(value.size()).as("Insert size").isEqualTo(1);
    }

    @Test
    public void shouldNotInsertEventsWithBrokenPayload() {
        Collection<StockEvent> events = new ArrayList<>();
        for (long i = 0; i < 10; i++) {
            events.add(createStockEvent(i, (int) i, EventType.SKU_CREATED));
        }
        events.add(getStockEventWithWrongPayload());
        stockEventsHandler.handle(events);
        verify(repository).insertAll(captor.capture());
        List<FulfillmentFeedId> value = captor.getValue();
        assertions.assertThat(value.size()).as("Insert size").isEqualTo(10);
    }

    private StockEvent createStockEvent(long warehouseId, int vendorId, EventType eventType) {
        UnitId unitId = new UnitId();
        unitId.setVendorId(warehouseId);
        unitId.setWarehouseId(vendorId);
        return StockEvent
                .builder(LocalDateTime.now(fixed))
                .withRequestId("request_id")
                .withPayloadType(PayloadType.SKU)
                .withPayload(ImmutableMap.of(EventAuditFieldConstants.UNIT_ID, unitId))
                .withId("id")
                .withEventType(eventType).build();
    }

    private StockEvent getStockEventWithWrongPayload() {
        return StockEvent
                .builder(LocalDateTime.now(fixed))
                .withRequestId("request_id")
                .withPayloadType(PayloadType.SKU)
                .withPayload(ImmutableMap.of(EventAuditFieldConstants.UNIT_ID, new Object()))
                .withId("id")
                .withEventType(EventType.SKU_CREATED).build();
    }
}
