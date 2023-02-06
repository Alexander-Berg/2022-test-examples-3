package ru.yandex.market.sc.tms.startrek;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.startrek.StartrekService;
import ru.yandex.market.tpl.common.startrek.listener.TicketStateListener;
import ru.yandex.market.tpl.common.startrek.listener.TicketStateTransitionProcessor;
import ru.yandex.market.tpl.common.startrek.listener.TicketTransitionHandler;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;
import ru.yandex.market.tpl.common.startrek.ticket.TicketQueueType;
import ru.yandex.market.tpl.common.startrek.ticket.TicketStatusEnum;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
public class TestTicketTransitionHandler {

    private static final String INIT_STATUS = "open";
    private static final String TARGET_STATUS = "closed";
    private static final TicketQueueType QUEUE_TYPE = TicketQueueType.TESTPARENT;

    @MockBean
    Clock clock;
    @Autowired
    StartrekService startrekService;
    @Autowired
    TicketStateListener ticketStateListener;
    @Autowired
    TicketStateTransitionProcessor ticketStateTransitionProcessor;
    @MockBean
    TicketTransitionHandler ticketTransitionHandler;
    @MockBean
    Session trackerSession;

    @BeforeEach
    void init() {
        doReturn((TicketStatusEnum) () -> TARGET_STATUS).when(ticketTransitionHandler).getTargetStatus();
        doReturn(QUEUE_TYPE).when(ticketTransitionHandler).getQueue();

        when(clock.instant()).thenReturn(Instant.now());
        ticketStateTransitionProcessor.setUpdateCheckDelaySeconds(Integer.MIN_VALUE);
    }

    @Test
    void transition() {
        Issue issue = createIssue();
        StartrekTicket ticket = createTicket(issue);
        ticketStateListener.listen(ticket);
        closeIssue(issue);
        ticketStateTransitionProcessor.processAll();
        assertThatTicketHandled(ticket);
    }

    private Issue createIssue() {
        Issue issue = mock(Issue.class, RETURNS_DEEP_STUBS);
        when(issue.getKey()).thenReturn(QUEUE_TYPE.getKeyPlaceholder() + "-1");
        when(issue.getQueue().getKey()).thenReturn(QUEUE_TYPE.getKeyPlaceholder());
        when(issue.getStatus().getKey()).thenReturn(INIT_STATUS);

        Issues issues = mock(Issues.class, RETURNS_DEEP_STUBS);
        when(issues.get(eq(issue.getKey()))).thenReturn(issue);
        when(issues.find(any(String.class))).thenReturn(Cf.singletonIterator(issue));

        doReturn(issues).when(trackerSession).issues();
        return issue;
    }

    private StartrekTicket createTicket(Issue issue) {
        return startrekService.getTicket(issue);
    }

    private void closeIssue(Issue issue) {
        when(issue.getStatus().getKey()).thenReturn(TARGET_STATUS);
    }

    private void assertThatTicketHandled(StartrekTicket expectedTicket) {
        verify(ticketTransitionHandler).handleTransition(
                argThat(actualTicket -> Objects.equals(actualTicket.getKey(), expectedTicket.getKey())
                        && Objects.equals(actualTicket.getStatusKey(), TARGET_STATUS))
        );
    }

}
