package ru.yandex.market.delivery.mdbapp.components.storage.repository;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderEventsFailoverCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailCauseType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.TicketCreationStatus;

@Sql(
    value = "/data/repository/orderEventFailover/cleanup.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class OrderEventsFailoverRepositoryTest extends MockContextualTest {
    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private OrderEventsFailoverRepository repository;

    @Test
    public void testSetTicketCreationStatusConstraint() {
        OrderEventsFailoverCounter counter = new OrderEventsFailoverCounter(
            1L,
            1L,
            "",
            TicketCreationStatus.NOT_CREATED,
            FailCauseType.UNKNOWN
        );

        repository.save(counter);
        softly.assertThat(json(repository.findByEventId(1L).get())).isEqualTo(json(counter));
    }

    @Test
    public void testSetTicketCreationStatusQueuedConstraint() {
        OrderEventsFailoverCounter counter = OrderEventsFailoverCounter.queueEvent(1L, 1L, null);

        repository.save(counter);
        softly.assertThat(json(repository.findByEventId(1L).get())).isEqualTo(json(counter));
        softly.assertThat(counter.isQueued()).isTrue();
    }

    @Test(expected = Exception.class)
    public void testSetTicketCreationStatusQueuedConstraintFail() {
        OrderEventsFailoverCounter counter =
            new OrderEventsFailoverCounter(1L, 1L, "", TicketCreationStatus.NOT_CREATED, FailCauseType.UNKNOWN)
                .setQueued(true);

        repository.save(counter);
    }

    @Sql("/data/repository/orderEventFailover/failovers-retry-order.sql")
    @Test
    @Transactional
    public void testRetryOrderOneMoreTimeIfNumberOfAttemptsExceeded() {
        repository.retryOrderOneMoreTimeIfNumberOfAttemptsExceeded(1L, 5);

        softly.assertThat(repository.findAll())
            .extracting(counter -> Arrays.asList(counter.getEventId(), counter.getAttemptCount(), counter.isFixed()))
            .containsExactlyInAnyOrder(
                Arrays.asList(10L, 6, true),
                Arrays.asList(11L, 5, false),
                Arrays.asList(12L, 1, false),
                Arrays.asList(13L, 1, false),
                Arrays.asList(14L, 1, false)
            );
    }

    public <T> JsonNode json(T t) {
        return om.convertValue(t, JsonNode.class);
    }
}
