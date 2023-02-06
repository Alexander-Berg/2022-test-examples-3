package ru.yandex.market.abo.core.ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.assessor.model.Assessor;
import ru.yandex.market.abo.core.common_inbox.CommonInboxInfo;
import ru.yandex.market.abo.core.common_inbox.InboxType;
import ru.yandex.market.abo.core.inbox.InboxService;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicket;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketFactory;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.market.abo.core.ticket.model.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 06.10.2019
 */
class TicketManagerUnitTest {
    private static final long TICKET_ID = 1;

    @InjectMocks
    TicketManager ticketManager;
    @Mock
    InboxService inboxService;
    @Mock
    AssessorService assessorService;
    @Mock
    StartrekTicketManager startrekTicketManager;
    @Mock
    TicketService ticketService;
    @Mock
    Ticket ticket;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(assessorService.loadStaffLogin(anyLong())).thenReturn(Optional.empty());
        when(assessorService.getAssessor(anyLong())).thenReturn(mock(Assessor.class));

        var inboxMock = mock(CommonInboxInfo.class);
        when(inboxMock.isFinished()).thenReturn(false);
        when(inboxService.getCommonInboxInfo(eq(InboxType.CORE_TICKET), eq(TICKET_ID))).thenReturn(inboxMock);

        when(ticket.getId()).thenReturn(TICKET_ID);
    }

    @Test
    void notifyAboutOpenStatusTimeoutTest_stTicketNotExists() {
        when(ticketService.loadTicketsWithOpenStatusTimeout()).thenReturn(List.of(ticket));
        when(startrekTicketManager.getTickets(
                eq(List.of(TICKET_ID)),
                eq(StartrekTicketReason.TICKET_OPEN_STATUS_TIMEOUT))
        ).thenReturn(List.of());

        ticketManager.notifyAboutOpenStatusTimeout();

        var createStTicketArgument = ArgumentCaptor.forClass(StartrekTicketFactory.class);
        verify(startrekTicketManager).createTicket(createStTicketArgument.capture());
        assertEquals(TICKET_ID, createStTicketArgument.getValue().getSourceId());
        assertEquals(StartrekTicketReason.TICKET_OPEN_STATUS_TIMEOUT, createStTicketArgument.getValue().getTicketReason());
    }

    @ParameterizedTest
    @CsvSource({"true, -1", "false, +1"})
    void notifyAboutOpenStatusTimeoutTest_stTicketNot(boolean needToCreate, long stTicketCreationTimeDiff) {
        var ticketModificationTime = LocalDateTime.now().minusMonths(1);
        var stTicketCreationTime = ticketModificationTime.plusHours(stTicketCreationTimeDiff);

        when(ticket.getModificationTime()).thenReturn(DateUtil.asDate(ticketModificationTime));
        when(ticketService.loadTicketsWithOpenStatusTimeout()).thenReturn(List.of(ticket));

        var stTicket = mock(StartrekTicket.class);
        when(stTicket.getSourceId()).thenReturn(TICKET_ID);
        when(stTicket.getCreationTime()).thenReturn(DateUtil.asDate(stTicketCreationTime));

        when(startrekTicketManager.getTickets(
                eq(List.of(TICKET_ID)),
                eq(StartrekTicketReason.TICKET_OPEN_STATUS_TIMEOUT))
        ).thenReturn(List.of(stTicket));

        ticketManager.notifyAboutOpenStatusTimeout();

        verify(startrekTicketManager, needToCreate ? times(1) : never()).createTicket(any());
    }
}
