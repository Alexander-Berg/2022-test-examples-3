package ru.yandex.market.pvz.tms.executor.startrek;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.tms.test.TransactionlessEmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.startrek.QuickStartWorkflowStatus;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.domain.TicketState;
import ru.yandex.market.tpl.common.startrek.domain.TicketStateRepository;
import ru.yandex.market.tpl.common.startrek.listener.TicketStateListener;
import ru.yandex.market.tpl.common.startrek.listener.TicketStateTransitionProcessor;
import ru.yandex.market.tpl.common.startrek.listener.TicketTransitionHandler;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.market.tpl.common.startrek.ticket.TicketQueueType;
import ru.yandex.market.tpl.common.startrek.ticket.TicketStatusEnum;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        FetchStartrekTicketsExecutor.class,
        FetchStartrekTicketsExecutorTest.MockTicketTransitionHandler.class
})
@TransactionlessEmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FetchStartrekTicketsExecutorTest {

    private static final String QUEUE = TicketQueueType.TESTPARENT.getKeyPlaceholder();
    private static final String CHANGED_TICKET = QUEUE + "-1";
    private static final String UNCHANGED_TICKET = QUEUE + "-2";

    private static final String DEFAULT_STATUS = QuickStartWorkflowStatus.OPENED.getStatusKey();

    private final FetchStartrekTicketsExecutor executor;
    private final TicketStateRepository ticketStateRepository;
    private final TicketStateTransitionProcessor ticketStateTransitionProcessor;
    private final TicketStateListener ticketStateListener;
    private final StartrekService startrekService;
    private final Session trackerSession;
    private final TestableClock clock;

    private Issue changedIssue;
    private Issue unchangedIssue;

    private StartrekTicket changedTicket;
    private StartrekTicket unchangedTicket;

    @BeforeEach
    void prepare() {
        changedIssue = mock(Issue.class, RETURNS_DEEP_STUBS);
        unchangedIssue = mock(Issue.class, RETURNS_DEEP_STUBS);

        changedTicket = startrekService.getTicket(changedIssue);
        unchangedTicket = startrekService.getTicket(unchangedIssue);

        when(changedIssue.getKey()).thenReturn(CHANGED_TICKET);
        when(changedIssue.getQueue().getKey()).thenReturn(QUEUE);
        when(changedIssue.getStatus().getKey()).thenReturn(DEFAULT_STATUS);

        when(unchangedIssue.getKey()).thenReturn(UNCHANGED_TICKET);
        when(unchangedIssue.getQueue().getKey()).thenReturn(QUEUE);
        when(unchangedIssue.getStatus().getKey()).thenReturn(DEFAULT_STATUS);

        when(changedIssue.update(any())).thenReturn(changedIssue);
    }

    @Test
    void testUpdateChangedTickets() {
        clock.setFixed(Instant.now().plus(1, ChronoUnit.DAYS), clock.getZone());
        ticketStateTransitionProcessor.setUpdateCheckDelaySeconds(Integer.MIN_VALUE);

        ticketStateListener.listen(changedTicket);
        ticketStateListener.listen(unchangedTicket);

        String query = "(Key: \"TSTPARENT-1\" AND Status: !\"open\") OR (Key: \"TSTPARENT-2\" AND Status: !\"open\")";
        when(trackerSession.issues().find(eq(query)))
                .thenReturn(DefaultIteratorF.wrap(List.of(changedIssue).listIterator()));

        when(changedIssue.getStatus().getKey()).thenReturn(QuickStartWorkflowStatus.IN_PROGRESS.getStatusKey());

        executor.doRealJob(null);

        verify(changedIssue, times(1)).update(any());

        assertThat(ticketStateRepository.findAllByOpenedIsTrueAndUpdatedAtLessThanOrderByKey(clock.instant()))
                .containsExactlyInAnyOrder(
                        TicketState.builder()
                                .key(CHANGED_TICKET)
                                .opened(true)
                                .status(QuickStartWorkflowStatus.IN_PROGRESS.getStatusKey())
                                .build(),

                        TicketState.builder()
                                .key(UNCHANGED_TICKET)
                                .opened(true)
                                .status(QuickStartWorkflowStatus.OPENED.getStatusKey())
                                .build()
                );

    }

    @Component
    public static class MockTicketTransitionHandler implements TicketTransitionHandler {

        @Override
        public TicketQueueType getQueue() {
            return TicketQueueType.TESTPARENT;
        }

        @Override
        public TicketStatusEnum getTargetStatus() {
            return QuickStartWorkflowStatus.IN_PROGRESS;
        }

        @Override
        public void handleTransition(StartrekTicket ticket) {
            ticket.doUpdate();
        }

    }

}
