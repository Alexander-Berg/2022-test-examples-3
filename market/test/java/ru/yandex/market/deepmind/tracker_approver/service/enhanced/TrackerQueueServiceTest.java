package ru.yandex.market.deepmind.tracker_approver.service.enhanced;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.market.deepmind.tracker_approver.BaseTrackerApproverTest;
import ru.yandex.market.deepmind.tracker_approver.configuration.EnhancedTrackerApproverConfiguration;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverExecutionContext;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory;
import ru.yandex.market.tracker.tracker.MockIssues;
import ru.yandex.market.tracker.tracker.MockSession;
import ru.yandex.market.tracker.tracker.pojo.MockIssue;
import ru.yandex.startrek.client.model.Issue;

public class TrackerQueueServiceTest extends BaseTrackerApproverTest {
    private MockSession mockSession;
    private MockIssues mockIssues;
    private TrackerApproverExecutionContext executionContext;
    private TrackerQueueService trackerQueueService;

    private final OffsetDateTime dummyDateTime = OffsetDateTime.now();

    @Before
    public void setup() {
        mockSession = new MockSession() {
            @Override
            public MockIssues issues() {
                return mockIssues;
            }
        };
        mockIssues = Mockito.mock(MockIssues.class);

        executionContext = new TrackerApproverExecutionContext()
            .setThreadCount(1)
            .setMaxRetryCount(5);

        var factory = Mockito.mock(TrackerApproverFactory.class);
        Mockito.when(factory.getTypes()).thenReturn(List.of("correct type"));
        var configuration = new EnhancedTrackerApproverConfiguration(
            "test",
            "queue",
            factory,
            executionContext
        );

        trackerQueueService = new TrackerQueueService(
            configuration.getTrackerQueryBuilder(),
            ticketRepository,
            mockSession,
            configuration.getExecutionContext(),
            configuration.getTrackerApproverFactory()
        );
    }

    /**
     * Correct ticket on first execution is referring to ticket that:
     * <ul>
     * <li>has correct type</li>
     * <li>it's state is not closed</li>
     * <li>retry count < max retry count</li>
     * </ul>
     */
    @Test
    public void firstExecutionReturnsCorrectTickets() {
        //arrange
        ticketRepository.save(ticket("T1", "correct type", TicketState.ENRICHED));
        ticketRepository.save(ticket("T2", "incorrect type", TicketState.NEW)); //incorrect type
        ticketRepository.save(ticket("T3", "correct type", TicketState.CLOSED)); //state is closed
        ticketRepository.save(ticket("T4", "correct type", TicketState.PROCESSED)
            .setRetryCount(executionContext.getMaxRetryCount()) //retry count is equal to limit
        );

        //act
        var tickets = trackerQueueService.getTicketRawStatusesToProcess(
            null,
            dummyDateTime
        );

        //assert
        Assertions.assertThat(tickets)
            .usingElementComparatorOnFields("ticket", "type", "state")
            .containsExactly(ticket("T1", "correct type", TicketState.ENRICHED));
    }

    /**
     * Correct ticket on second and later executions is referring to ticket that:
     * <ul>
     * <li>present in the DB</li>
     * <li>has correct type</li>
     * <li>in the NEW state</li>
     * <li>with retry count > 0</li>
     * <li>with retry count < max retry count</li>
     * </ul>
     */
    @Test
    public void regularExecutionReturnsCorrectTickets() {
        //arrange
        Mockito.when(mockIssues.find(Mockito.anyString()))
            .thenReturn(issues("T1", "T2", "T8"));

        ticketRepository.save(ticket("T1", "correct type", TicketState.ENRICHED));
        ticketRepository.save(ticket("T2", "incorrect type", TicketState.NEW)); //incorrect type

        ticketRepository.save(ticket("T3", "correct type", TicketState.NEW)); //state is NEW
        ticketRepository.save(ticket("T4", "correct type", TicketState.PROCESSED) //not present in the issues
            .setRetryCount(0)
        );
        ticketRepository.save(ticket("T5", "correct type", TicketState.PROCESSED)
            .setRetryCount(1) //retry count is 1
        );
        ticketRepository.save(ticket("T6", "correct type", TicketState.NEW)
            .setRetryCount(1) //retry count is 1, but also state is NEW to check duplicates
        );
        ticketRepository.save(ticket("T7", "correct type", TicketState.PROCESSED)
            .setRetryCount(executionContext.getMaxRetryCount()) //retry count is equal to limit
        );

        //act
        var tickets = trackerQueueService.getTicketRawStatusesToProcess(
            dummyDateTime,
            dummyDateTime
        );

        //assert
        Assertions.assertThat(tickets)
            .usingElementComparatorOnFields("ticket", "type", "state")
            .containsExactlyInAnyOrder(
                ticket("T1", "correct type", TicketState.ENRICHED),
                ticket("T3", "correct type", TicketState.NEW),
                ticket("T5", "correct type", TicketState.PROCESSED),
                ticket("T6", "correct type", TicketState.NEW)
            );
    }

    private TrackerApproverTicketRawStatus ticket(String ticket, String type, TicketState state) {
        return new TrackerApproverTicketRawStatus(ticket, type, state);
    }

    private MockIssue issue(String ticket) {
        return new MockIssue(ticket, "", Map.of(), mockSession);
    }

    private IteratorF<Issue> issues(String... tickets) {
        return DefaultIteratorF.wrap(
            Arrays.stream(tickets)
                .map(t -> (Issue) issue(t))
                .iterator()
        );
    }
}
