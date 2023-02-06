package ru.yandex.market.abo.tms.ticket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.AUTO_ORDER;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.BASKET;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.DEFAULT;
import static ru.yandex.market.abo.core.ticket.model.TicketStatus.CANCELED;
import static ru.yandex.market.abo.core.ticket.model.TicketStatus.FINISHED;
import static ru.yandex.market.abo.core.ticket.model.TicketStatus.NEW;

/**
 * @author imelnikov
 */
public class TicketInboxCancelExecutorTest extends EmptyTest {

    @Autowired
    private TicketInboxCancelExecutor executor;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() {
        createTicketAndInbox(1, NEW, BASKET, 5, false); // BASKET & 20 minutes -> keep
        createTicketAndInbox(2, NEW, BASKET, 20, false); // BASKET & 20 minutes -> throw
        createTicketAndInbox(3, NEW, AUTO_ORDER, 20, false); // AO & 20 minutes -> keep
        createTicketAndInbox(4, NEW, AUTO_ORDER, 40, false); // AO & 40 minutes -> throw
        createTicketAndInbox(5, NEW, DEFAULT, 40, true); // already deleted -> don't touch
        createTicketAndInbox(6, NEW, AUTO_ORDER, 40, true); // already deleted -> don't touch


        createTicketAndInbox(7, CANCELED, BASKET, 5, false); // CANCELLED -> throw
        createTicketAndInbox(8, FINISHED, BASKET, 5, false); // FINISHED -> throw
        createTicketAndInbox(9, CANCELED, AUTO_ORDER, 5, false); // CANCELLED -> throw
        createTicketAndInbox(10, FINISHED, AUTO_ORDER, 5, false); // FINISHED -> throw
        createTicketAndInbox(11, CANCELED, BASKET, 5, true); // already deleted -> don't touch
    }

    @Test
    public void testLoadTicketsToThrowFromInbox() {
        Set<Long> ticketsToThrow = new HashSet<>(executor.loadTicketsToThrowFromInbox(10, 30));
        Set<Long> expected = new HashSet<>(Arrays.asList(2L, 4L, 7L, 8L, 9L, 10L));
        assertEquals(expected, ticketsToThrow);
    }

    private void createTicketAndInbox(int id, TicketStatus status, CheckMethod checkMethod,
                                      int minutesAgo, boolean deleted) {
        createTicket(id, status, checkMethod);
        createInboxEntry(id, minutesAgo, deleted);
    }

    private void createTicket(int id, TicketStatus status, CheckMethod checkMethod) {
        pgJdbcTemplate.update("INSERT INTO hypothesis (id, shop_id, gen_id) VALUES (?, 1, 1)", id);
        pgJdbcTemplate.update("INSERT INTO core_ticket (hyp_id, status_id, check_method_id) VALUES (?, ?, ?)",
                id, status.getId(), checkMethod.getId());
    }

    private void createInboxEntry(int ticketId, int minutesAgo, boolean deleted) {
        pgJdbcTemplate.update("INSERT INTO core_inbox (hyp_id, creation_time, ya_uid, deleted) " +
                        "VALUES (?, now() - make_interval(mins := ?), 1, ?)",
                ticketId, minutesAgo, deleted);
    }
}
