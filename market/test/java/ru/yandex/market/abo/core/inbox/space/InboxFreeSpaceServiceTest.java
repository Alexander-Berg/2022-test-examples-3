package ru.yandex.market.abo.core.inbox.space;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.inbox.InboxTicketFilter;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 */
public class InboxFreeSpaceServiceTest extends EmptyTest {

    @Autowired
    private InboxFreeSpaceService inboxFreeSpaceService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private ConfigurationService aboConfigurationService;

    @BeforeEach
    public void setUp() throws Exception {
        int id = 0;
        insertTicket(++id, CheckMethod.BASKET, 1, false);
        insertTicket(++id, CheckMethod.PHONE, 1, true);
        insertTicket(++id, CheckMethod.AUTO_ORDER, 1, false);
        insertTicket(++id, CheckMethod.AUTO_ORDER, 1, false);
        insertTicket(++id, CheckMethod.AUTO_ORDER, 1, true);

        insertTicket(++id, CheckMethod.DEFAULT, 2, false);

        for (int i = 0; i < autoOrderLimit() + 1; i++) {
            insertTicket(++id, CheckMethod.AUTO_ORDER, 3, false);
        }
    }

    @Test
    public void getRegularFreeSpace() {
        assertEquals(0, inboxFreeSpaceService.getFreeSpace(1, InboxTicketFilter.ACCEPT_ALL_EXCEPT_AUTO_ORDER.checkMethods()));

        assertEquals(0, inboxFreeSpaceService.getFreeSpace(2, InboxTicketFilter.ACCEPT_ALL_EXCEPT_AUTO_ORDER.checkMethods()));

        assertEquals(1, inboxFreeSpaceService.getFreeSpace(3, InboxTicketFilter.ACCEPT_ALL_EXCEPT_AUTO_ORDER.checkMethods()));
        assertEquals(1, inboxFreeSpaceService.getFreeSpace(3, EnumSet.of(CheckMethod.PHONE)));
        assertEquals(1, inboxFreeSpaceService.getFreeSpace(3, EnumSet.of(CheckMethod.BASKET)));
        assertEquals(1, inboxFreeSpaceService.getFreeSpace(3, EnumSet.of(CheckMethod.BY_SIGHT)));
        assertEquals(1, inboxFreeSpaceService.getFreeSpace(3, EnumSet.of(CheckMethod.COMPLEX)));
    }

    @Test
    public void getAutoOrderFreeSpace() {
        EnumSet<CheckMethod> autoOrder = EnumSet.of(CheckMethod.AUTO_ORDER);

        assertEquals(autoOrderLimit() - 2, inboxFreeSpaceService.getFreeSpace(1, autoOrder));
        assertEquals(autoOrderLimit(), inboxFreeSpaceService.getFreeSpace(2, autoOrder));
        assertEquals(0, inboxFreeSpaceService.getFreeSpace(3, autoOrder));
    }

    private void insertTicket(long id, CheckMethod checkMethod, long uid, boolean deleted) {
        pgJdbcTemplate.update("INSERT INTO hypothesis (id, gen_id, shop_id) VALUES (?, 1, 1)", id);
        pgJdbcTemplate.update("INSERT INTO core_ticket (hyp_id, check_method_id, status_id) VALUES (?, ?, ?)",
                id, checkMethod.getId(), TicketStatus.NEW.getId());
        pgJdbcTemplate.update("INSERT INTO core_inbox (hyp_id, ya_uid, deleted) VALUES (?, ?, ?)",
                id, uid, deleted);
    }

    private int autoOrderLimit() {
        return aboConfigurationService.getValueAsInt(CoreConfig.INBOX_LIMIT_AUTO_ORDER.getId());
    }
}
