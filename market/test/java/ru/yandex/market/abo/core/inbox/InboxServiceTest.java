package ru.yandex.market.abo.core.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 * @date 25.05.17
 */
public class InboxServiceTest extends AbstractCoreHierarchyTest {

    private static final long USER_ID = -101L;

    @Autowired
    private InboxService inboxService;

    private long TICKET_ID = 1L;

    @BeforeEach
    public void setup() {
        TICKET_ID = createTicket(155L, -1);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void putTheSameTicketTwice() {
        assertTrue(inboxService.putTicketToInbox(TICKET_ID, USER_ID));
        assertFalse(inboxService.putTicketToInbox(TICKET_ID, USER_ID));
    }

    @Test
    public void removeInbox() {
        inboxService.putTicketToInbox(TICKET_ID, USER_ID);
        inboxService.throwTicketFromInbox(TICKET_ID);

        assertTrue(inboxService.putTicketToInbox(TICKET_ID, USER_ID));
    }

    @Test
    public void checkInbox() {
        assertFalse(inboxService.existsTicketInInbox(USER_ID, TICKET_ID));
        assertFalse(inboxService.existsTicketInInbox(TICKET_ID));
        inboxService.putTicketToInbox(TICKET_ID, USER_ID);
        assertTrue(inboxService.existsTicketInInbox(USER_ID, TICKET_ID));
        assertTrue(inboxService.existsTicketInInbox(TICKET_ID));
    }
}
