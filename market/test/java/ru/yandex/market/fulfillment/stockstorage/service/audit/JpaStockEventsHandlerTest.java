package ru.yandex.market.fulfillment.stockstorage.service.audit;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.google.common.collect.ImmutableList;
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
import ru.yandex.market.fulfillment.stockstorage.domain.dto.PayloadType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.EventAudit;
import ru.yandex.market.fulfillment.stockstorage.events.stock.StockEvent;
import ru.yandex.market.fulfillment.stockstorage.repository.EventAuditRepository;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JpaStockEventsHandlerTest {

    private final Clock fixed = Clock.fixed(LocalDateTime.of(2018, 1, 1, 0, 0)
            .atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    @Mock
    EventAuditRepository repository;
    @Mock
    SystemPropertyService systemPropertyService;

    private JpaStockEventsHandler stockEventsHandler;

    private final SoftAssertions assertions = new SoftAssertions();


    @Captor
    private ArgumentCaptor<List<EventAudit>> captor;

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
        stockEventsHandler = new JpaStockEventsHandler(repository, systemPropertyService);
    }

    @Test
    public void handle() {

        when(repository.saveAll(anyList())).thenReturn(any());
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.WRITE_TO_EVENT_AUDIT_TABLE_IN_DATABASE))
                .thenReturn(true);

        StockEvent event = createEvent();
        stockEventsHandler.handle(ImmutableList.of(event));
        verify(repository).saveAll(captor.capture());

        List<EventAudit> value = captor.getValue();
        assertions.assertThat(value).hasSize(1);
        EventAudit actual = value.get(0);
        assertions.assertThat(actual.getType()).as("Type").isEqualTo(event.getEventType());
        assertions.assertThat(actual.getPayload()).as("Payload").isEqualTo(event.getPayload());
        assertions.assertThat(actual.getTargetId()).as("Target id").isEqualTo(event.getTargetId());
        assertions.assertThat(actual.getTargetType()).as("Target type").isEqualTo(event.getPayloadType());
        assertions.assertThat(actual.getRequestId()).as("Request id").isEqualTo(event.getRequestId());
        assertions.assertThat(actual.getCreated()).as("Event date").isEqualTo(event.getEventDate());
    }

    @Test
    public void handleWithoutSave() {
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.WRITE_TO_EVENT_AUDIT_TABLE_IN_DATABASE))
                .thenReturn(false);

        StockEvent event = createEvent();
        stockEventsHandler.handle(ImmutableList.of(event));
        verify(repository, never()).saveAll(anyList());
    }

    private StockEvent createEvent() {
        return StockEvent
                .builder(LocalDateTime.now(fixed))
                .withRequestId("request_id")
                .withPayloadType(PayloadType.FREEZE)
                .withPayload(ImmutableMap.of(EventAuditFieldConstants.AMOUNT, "some"))
                .withId("id")
                .withEventType(EventType.FREEZE_FORCE_REMOVED).build();

    }
}
