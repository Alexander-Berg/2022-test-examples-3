package ru.yandex.market.delivery.mdbapp.components.failover;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.AbstractFailoverCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderEventsFailoverCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailCauseType;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderEventsFailoverRepository;

@DatabaseSetup("/components/failover/before/failovers_for_create_ticket.xml")
public class OrderEventFailoverableServiceTest extends AbstractMediumContextualTest {
    @Autowired
    private OrderEventFailoverableService orderEventFailoverableService;

    @Autowired
    private OrderEventsFailoverRepository failoverRepository;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2020-11-23T00:00:00.00Z"), ZoneOffset.UTC);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    public void findLastEventsForTicketCreation(FailCauseType causeType, long eventId) {
        List<OrderEventsFailoverCounter> found = orderEventFailoverableService.findLastEventsForTicketCreation(
            1,
            causeType
        );
        softly.assertThat(found)
            .hasSize(1)
            .extracting(OrderEventsFailoverCounter::getEventId)
            .containsExactlyInAnyOrder(eventId);
    }

    @Nonnull
    private static Stream<Arguments> findLastEventsForTicketCreation() {
        return Stream.of(
            Arguments.of(FailCauseType.FROZEN_SERVICE, 50L),
            Arguments.of(FailCauseType.UNKNOWN, 10L),
            Arguments.of(FailCauseType.INTERNAL_SERVER_ERROR, 70L)
        );
    }

    @Test
    public void testMarkFailedQueuedEventAsNotQueued() {
        String errText = "Some error";

        OrderHistoryEvent evt = new OrderHistoryEvent();
        evt.setId(11L);

        orderEventFailoverableService.storeError(evt, errText, new RuntimeException("error"));

        Optional<OrderEventsFailoverCounter> existing = failoverRepository.findByEventId(11L);
        softly.assertThat(existing).map(AbstractFailoverCounter::getLastFailCause).contains(errText);
        softly.assertThat(existing).map(OrderEventsFailoverCounter::isQueued).contains(false);
    }
}
