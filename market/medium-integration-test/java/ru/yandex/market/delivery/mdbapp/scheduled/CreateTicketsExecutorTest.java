package ru.yandex.market.delivery.mdbapp.scheduled;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.queue.startrek.dto.CreateIssueForEventsDto;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailCauseType;
import ru.yandex.market.delivery.mdbapp.configuration.StartrekConfiguration;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup("/scheduled/create_tickets/setup.xml")
public class CreateTicketsExecutorTest extends AbstractMediumContextualTest {
    private static final String ADMIN_EVENT_DETAIL_URL =
        "https://lms-admin.market.yandex-team.ru/mdb/failover-counters/%d";

    private CreateTicketsExecutor executor;

    @Autowired
    private OrderEventFailoverableService orderEventFailoverableService;

    private QueueProducer<CreateIssueForEventsDto> createIssueForEventsDtoQueueProducer = mock(QueueProducer.class);

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(createIssueForEventsDtoQueueProducer);
    }

    @BeforeEach
    void setup() {
        StartrekConfiguration startrekConfiguration = new StartrekConfiguration();
        startrekConfiguration.setEnabled(true);

        executor = new CreateTicketsExecutor(
            startrekConfiguration,
            orderEventFailoverableService,
            createIssueForEventsDtoQueueProducer,
            clock
        );
    }

    @Test
    @DisplayName("Создание тикетов - для всех типов")
    @ExpectedDatabase(
        value = "/scheduled/create_tickets/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void allTypes() {
        clock.setFixed(Instant.parse("2020-10-18T15:00:00Z"), ZoneOffset.UTC);

        executor.doJob(null);

        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(10L));
        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(11L));
        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(14L));

        verify(createIssueForEventsDtoQueueProducer).enqueue(issuesDto(
            List.of(20L, 21L, 24L),
            FailCauseType.FROZEN_SERVICE
        ));

        verify(createIssueForEventsDtoQueueProducer).enqueue(issuesDto(
            List.of(30L, 31L, 34L),
            FailCauseType.INTERNAL_SERVER_ERROR
        ));
    }

    @Test
    @DisplayName("Прошло слишком мало времени для Frozen - создаем только для остальных типов")
    @ExpectedDatabase(
        value = "/scheduled/create_tickets/success_except_frozen.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void exceptFrozen() {
        clock.setFixed(Instant.parse("2020-10-18T14:55:00Z"), ZoneOffset.UTC);

        executor.doJob(null);

        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(10L));
        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(11L));
        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(14L));

        verify(createIssueForEventsDtoQueueProducer).enqueue(issuesDto(
            List.of(30L, 31L, 34L),
            FailCauseType.INTERNAL_SERVER_ERROR
        ));
    }

    @Test
    @DisplayName("Прошло слишком мало времени для Frozen и ошибки сервера - создаем только для UNKNOWN")
    @ExpectedDatabase(
        value = "/scheduled/create_tickets/success_unknown.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void onlyUnknown() {
        clock.setFixed(Instant.parse("2020-10-18T14:00:00Z"), ZoneOffset.UTC);

        executor.doJob(null);

        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(10L));
        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(11L));
        verify(createIssueForEventsDtoQueueProducer).enqueue(issueDto(14L));
    }

    @Nonnull
    private EnqueueParams<CreateIssueForEventsDto> issueDto(long eventId) {
        String title = "Failed to process order 1";
        String body = "**Order:** 1\n**Event:** " + eventId + "\n**Reason:** cause";

        CreateIssueForEventsDto dto = new CreateIssueForEventsDto(List.of(eventId), title, body);
        return EnqueueParams.create(dto);
    }

    @Nonnull
    private EnqueueParams<CreateIssueForEventsDto> issuesDto(List<Long> eventId, FailCauseType type) {
        String title = "Failed to process orders with reason " + type;

        String eventLinks = eventId.stream()
            .map(e -> String.format(ADMIN_EVENT_DETAIL_URL, e))
            .collect(Collectors.joining("\n"));

        String body = "**Events:**\n" + eventLinks;

        return EnqueueParams.create(new CreateIssueForEventsDto(eventId, title, body));
    }
}
