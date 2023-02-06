package ru.yandex.market.logistics.iris.service.audit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.event.EventAuditFieldConstants;
import ru.yandex.market.logistics.iris.core.domain.event.EventType;
import ru.yandex.market.logistics.iris.model.DimensionsDTO;
import ru.yandex.market.logistics.iris.service.audit.measurement.JpaMeasurementEventsHandler;
import ru.yandex.market.logistics.iris.service.audit.measurement.MeasurementEvent;
import ru.yandex.market.logistics.iris.service.audit.measurement.MeasurementEventsListener;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class MeasurementEventsListenerTest extends AbstractContextualTest {

    private MeasurementEventsListener eventsListener;

    @Autowired
    private  JpaMeasurementEventsHandler jpaMeasurementEventsHandler;

    @Before
    public void init() {
        this.eventsListener = new MeasurementEventsListener(ImmutableList.of(jpaMeasurementEventsHandler));
    }

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/audit/measurement/1.xml", assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessHandlerEvent() {
        eventsListener.onEvent(EventWrapper.wrap(createEvents()));
    }

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/audit/measurement/2.xml", assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessHandlerDimensionsEvent() {
        eventsListener.onEvent(EventWrapper.wrap(createDimensionsEvents()));
    }

    private Collection<MeasurementEvent> createEvents() {
        return ImmutableList.of(
                MeasurementEvent.builder(LocalDateTime.now())
                        .setPartnerId("1")
                        .setPartnerSku("sku1")
                        .setPayload(ImmutableMap.of(
                            EventAuditFieldConstants.SCORE, BigDecimal.valueOf(35),
                            EventAuditFieldConstants.COMMENT, "Comment")
                        )
                        .setEventType(EventType.GET_SCORE)
                        .setRequestId("request_id")
                        .build(),
                MeasurementEvent.builder(LocalDateTime.now())
                        .setPartnerId("1")
                        .setPartnerSku("sku2")
                        .setPayload(ImmutableMap.of(
                                EventAuditFieldConstants.SCORE, BigDecimal.valueOf(17),
                                EventAuditFieldConstants.COMMENT, "")
                        )
                        .setEventType(EventType.GET_SCORE)
                        .setRequestId("request_id_2")
                        .build()
        );
    }

    private Collection<MeasurementEvent> createDimensionsEvents() {
        return ImmutableList.of(
                MeasurementEvent.builder(LocalDateTime.now())
                        .setPartnerId("1")
                        .setPartnerSku("sku1")
                        .setPayload(ImmutableMap.of(
                                EventAuditFieldConstants.DIMENSIONS,
                                DimensionsDTO.builder()
                                        .setWidth(toBigDecimal(110, 3))
                                        .setHeight(toBigDecimal(210, 3))
                                        .setLength(toBigDecimal(310, 3))
                                        .setWeightGross(toBigDecimal(1110, 3))
                                        .build(),
                                EventAuditFieldConstants.COMMENT, "Comment")
                        )
                        .setEventType(EventType.GET_SCORE)
                        .setRequestId("request_id")
                        .build(),
                MeasurementEvent.builder(LocalDateTime.now())
                        .setPartnerId("1")
                        .setPartnerSku("sku2")
                        .setPayload(ImmutableMap.of(
                                EventAuditFieldConstants.DIMENSIONS,
                                DimensionsDTO.builder()
                                        .setHeight(toBigDecimal(510, 3))
                                        .setLength(toBigDecimal(610, 3))
                                        .build(),
                                EventAuditFieldConstants.COMMENT, "")
                        )
                        .setEventType(EventType.GET_SCORE)
                        .setRequestId("request_id_2")
                        .build()
        );
    }

}
