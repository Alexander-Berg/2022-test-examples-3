package ru.yandex.market.logistics.iris.service.audit;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.event.EventType;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.model.DimensionsDTO;
import ru.yandex.market.logistics.iris.service.audit.measurement.MeasurementEvent;
import ru.yandex.market.logistics.iris.service.audit.measurement.MeasurementEventAuditService;
import ru.yandex.market.logistics.iris.service.audit.measurement.MeasurementEventAuditServiceImpl;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class MeasurementEventAuditServiceTest extends AbstractContextualTest {

    private final String REQUEST_ID = "TestRequestId";

    private ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private MeasurementEventAuditService auditService = new MeasurementEventAuditServiceImpl(eventPublisher, Clock.systemDefaultZone());

    @Captor
    private ArgumentCaptor<EventWrapper<MeasurementEvent>> eventsCaptor;

    @Before
    public void init() {
        RequestContextHolder.createContext(REQUEST_ID);
    }

    @Test
    public void shouldSuccessLogScoreEvent() {
        auditService.asyncLogScore(ImmutableMap.of(ItemIdentifier.of("1", "sku1"), BigDecimal.valueOf(10)));

        Mockito.verify(eventPublisher, times(1)).publishEvent(eventsCaptor.capture());
        EventWrapper<MeasurementEvent> eventWrapper = eventsCaptor.getValue();

        assertSoftly(assertions -> {
            assertions.assertThat(eventWrapper).isNotNull();
            assertions.assertThat(eventWrapper.getEvents().isEmpty()).isFalse();

            Optional<MeasurementEvent> optionalEvent = eventWrapper.getEvents().stream().findFirst();
            assertions.assertThat(optionalEvent.isPresent()).isTrue();

            MeasurementEvent event = optionalEvent.get();
            assertions.assertThat(event.getPartnerId()).isEqualTo("1");
            assertions.assertThat(event.getPartnerSku()).isEqualTo("sku1");
            assertions.assertThat(event.getEventType()).isEqualTo(EventType.GET_SCORE);
            assertions.assertThat(event.getRequestId()).isEqualTo(REQUEST_ID);

            assertions.assertThat(event.getPayload()).isNotEmpty();
            assertions.assertThat(event.getPayload().get("score")).isEqualTo(BigDecimal.valueOf(10));
            assertions.assertThat(event.getPayload().get("comment")).isEqualTo("");
        });
    }

    @Test
    public void shouldSuccessLogProbabilityTwoEvent() {
        auditService.asyncLogProbability(createProbabilityData());

        Mockito.verify(eventPublisher, times(1)).publishEvent(eventsCaptor.capture());
        EventWrapper<MeasurementEvent> eventWrapper = eventsCaptor.getValue();

        assertSoftly(assertions -> {
            assertions.assertThat(eventWrapper).isNotNull();
            assertions.assertThat(eventWrapper.getEvents().size()).isEqualTo(2);

            List<MeasurementEvent> events = new ArrayList<>(eventWrapper.getEvents());

            MeasurementEvent firstEvent = events.get(0);
            assertions.assertThat(firstEvent.getPartnerId()).isEqualTo("1");
            assertions.assertThat(firstEvent.getPartnerSku()).isEqualTo("sku1");
            assertions.assertThat(firstEvent.getEventType()).isEqualTo(EventType.GET_PROBABILITY);
            assertions.assertThat(firstEvent.getRequestId()).isEqualTo(REQUEST_ID);

            assertions.assertThat(firstEvent.getPayload()).isNotEmpty();
            assertions.assertThat(firstEvent.getPayload().get("probability")).isEqualTo(BigDecimal.valueOf(10));
            assertions.assertThat(firstEvent.getPayload().get("comment")).isEqualTo("");

            MeasurementEvent secondEvent = events.get(1);
            assertions.assertThat(secondEvent.getPartnerId()).isEqualTo("1");
            assertions.assertThat(secondEvent.getPartnerSku()).isEqualTo("sku2");
            assertions.assertThat(secondEvent.getEventType()).isEqualTo(EventType.GET_PROBABILITY);
            assertions.assertThat(secondEvent.getRequestId()).isEqualTo(REQUEST_ID);

            assertions.assertThat(secondEvent.getPayload()).isNotEmpty();
            assertions.assertThat(secondEvent.getPayload().get("probability")).isEqualTo(BigDecimal.valueOf(32));
            assertions.assertThat(secondEvent.getPayload().get("comment")).isEqualTo("");
        });
    }

    @Test
    public void shouldSuccessLogDimensionsEvent() {
        final String warehouseId = "172";
        auditService.asyncLogDimensions(createDimensionsData(), warehouseId);

        Mockito.verify(eventPublisher, times(1)).publishEvent(eventsCaptor.capture());
        EventWrapper<MeasurementEvent> eventWrapper = eventsCaptor.getValue();

        assertSoftly(assertions -> {
            assertions.assertThat(eventWrapper).isNotNull();
            assertions.assertThat(eventWrapper.getEvents().size()).isEqualTo(2);

            List<MeasurementEvent> events = new ArrayList<>(eventWrapper.getEvents());

            MeasurementEvent firstEvent = events.get(0);
            assertions.assertThat(firstEvent.getPartnerId()).isEqualTo("1");
            assertions.assertThat(firstEvent.getPartnerSku()).isEqualTo("sku1");
            assertions.assertThat(firstEvent.getEventType()).isEqualTo(EventType.PUSH_DIMENSIONS);
            assertions.assertThat(firstEvent.getRequestId()).isEqualTo(REQUEST_ID);

            assertions.assertThat(firstEvent.getPayload()).isNotEmpty();
            assertions.assertThat(firstEvent.getPayload().get("dimensions")).isEqualTo(DimensionsDTO.builder()
                            .setWidth(toBigDecimal(110, 3))
                            .setHeight(toBigDecimal(210, 3))
                            .setLength(toBigDecimal(310, 3))
                            .setWeightGross(toBigDecimal(1110, 3))
                            .build());
            assertions.assertThat(firstEvent.getPayload().get("warehouseId")).isEqualTo(warehouseId);

            MeasurementEvent secondEvent = events.get(1);
            assertions.assertThat(secondEvent.getPartnerId()).isEqualTo("1");
            assertions.assertThat(secondEvent.getPartnerSku()).isEqualTo("sku2");
            assertions.assertThat(secondEvent.getEventType()).isEqualTo(EventType.PUSH_DIMENSIONS);
            assertions.assertThat(secondEvent.getRequestId()).isEqualTo(REQUEST_ID);

            assertions.assertThat(secondEvent.getPayload()).isNotEmpty();
            assertions.assertThat(secondEvent.getPayload().get("dimensions")).isEqualTo(DimensionsDTO.builder()
                    .setHeight(toBigDecimal(510, 3))
                    .setLength(toBigDecimal(610, 3))
                    .build());
            assertions.assertThat(secondEvent.getPayload().get("warehouseId")).isEqualTo(warehouseId);
        });
    }

    private Map<ItemIdentifier, BigDecimal> createProbabilityData() {
        return ImmutableMap.of(
                ItemIdentifier.of("1", "sku1"), BigDecimal.valueOf(10),
                ItemIdentifier.of("1", "sku2"), BigDecimal.valueOf(32));
    }

    private Map<ItemIdentifier, DimensionsDTO> createDimensionsData() {
        return ImmutableMap.of(
                ItemIdentifier.of("1", "sku1"), DimensionsDTO.builder()
                        .setWidth(toBigDecimal(110, 3))
                        .setHeight(toBigDecimal(210, 3))
                        .setLength(toBigDecimal(310, 3))
                        .setWeightGross(toBigDecimal(1110, 3))
                        .build(),
                ItemIdentifier.of("1", "sku2"),  DimensionsDTO.builder()
                        .setHeight(toBigDecimal(510, 3))
                        .setLength(toBigDecimal(610, 3))
                        .build());
    }
}
