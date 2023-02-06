package ru.yandex.market.abo.core.ticket;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.autoorder.response.AutoOrderResponseService;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.inbox.InboxService;
import ru.yandex.market.abo.core.screenshot.ScreenshotService;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStateUpdateRequest;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.core.ticket.order.OrderService;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.util.ErrorMessageException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 17.04.19
 */
class TicketStateUpdateManagerTest {
    private static final long TICKET_ID = 1;
    private static final long SHOP_ID = 774;

    @Mock
    ScreenshotService screenshotService;
    @Mock
    ExceptionalShopsService exceptionalShopsService;
    @Mock
    AutoOrderResponseService autoOrderResponseService;
    @Mock
    TicketService ticketService;
    @Mock
    ProblemManager problemManager;
    @Mock
    AssessorService assessorService;
    @Mock
    TicketLockService ticketLockService;
    @Mock
    TicketTagService ticketTagService;
    @Mock
    OrderService orderService;
    @Mock
    TicketManager ticketManager;
    @Mock
    InboxService inboxService;
    @Mock
    TicketStateUpdateRequest request;

    @InjectMocks
    TicketStateUpdateManager ticketStateUpdateManager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(request.getTicketId()).thenReturn(TICKET_ID);
        when(ticketLockService.doInLock(anyLong(), any(Supplier.class))).thenAnswer(invocation ->
                ((Supplier<Long>) invocation.getArguments()[1]).get()
        );
    }

    @Test
    void testConcurrentModification() {
        when(ticketService.loadTicketById(TICKET_ID)).thenReturn(createTicket(TicketStatus.NEW));
        when(request.getOldStatus()).thenReturn(TicketStatus.OPEN);
        var e = assertThrows(ErrorMessageException.class, () -> ticketStateUpdateManager.updateTicketState(request));
        assertEquals("concurrent-modification", e.getMessageCode());
    }

    @Test
    void testTicketNotInInboxStatusNEW() {
        when(ticketService.loadTicketById(TICKET_ID)).thenReturn(createTicket(TicketStatus.NEW));
        when(request.getOldStatus()).thenReturn(TicketStatus.NEW);
        when(inboxService.existsTicketInInbox(TICKET_ID)).thenReturn(false);
        var e = assertThrows(ErrorMessageException.class, () -> ticketStateUpdateManager.updateTicketState(request));
        assertEquals("not-in-inbox", e.getMessageCode());
    }

    @Test
    void testTicketInInboxStatusNEW() {
        when(ticketService.loadTicketById(TICKET_ID)).thenReturn(createTicket(TicketStatus.NEW));
        when(request.getOldStatus()).thenReturn(TicketStatus.NEW);
        when(request.getCheckMethod()).thenReturn(CheckMethod.BY_SIGHT);
        when(inboxService.existsTicketInInbox(TICKET_ID)).thenReturn(true);
        assertEquals(TICKET_ID, ticketStateUpdateManager.updateTicketState(request));
    }

    private static Ticket createTicket(TicketStatus status) {
        return createTicket(status, CheckMethod.BY_SIGHT);
    }

    private static Ticket createTicket(TicketStatus status, CheckMethod checkMethod) {
        var ticket = new Ticket(createHyp(), 0L, 0, checkMethod);
        ticket.setStatus(status);
        return ticket;
    }

    private static Hypothesis createHyp() {
        var hyp = new Hypothesis(SHOP_ID, 0, 0, "", 1.0, 0, "");
        hyp.setId(TICKET_ID);
        return hyp;
    }
}
