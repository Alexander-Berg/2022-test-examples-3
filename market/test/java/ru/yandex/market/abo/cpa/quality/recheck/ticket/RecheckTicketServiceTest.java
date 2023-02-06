package ru.yandex.market.abo.cpa.quality.recheck.ticket;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.calendar.WorkHour;
import ru.yandex.market.abo.core.calendar.db.CalendarEntry;
import ru.yandex.market.abo.core.calendar.db.CalendarService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 17.09.15
 */
class RecheckTicketServiceTest extends EmptyTest {
    private static final long SHOP_ID = -1L;
    private static final RecheckTicketType TYPE = RecheckTicketType.OFFLINE;

    @Autowired
    @InjectMocks
    private RecheckTicketService recheckTicketService;

    @Mock
    CalendarService calendarService;
    @Mock
    private WorkHour workHour;

    @BeforeEach
    void init() {
        openMocks(this);
        when(workHour.add(any(), anyInt())).then(inv -> {
            Object[] args = inv.getArguments();
            return new WorkHour(calendarService).add((Date) args[0], (int) args[1]);
        });
    }

    @Test
    void testFindAll() {
        recheckTicketService.save(new RecheckTicket.Builder()
                .withShopId(SHOP_ID)
                .withType(TYPE)
                .withCheckMethod(RecheckTicketCheckMethod.PURCHASE).build());

        List<RecheckTicket> tickets = recheckTicketService.findAll(new RecheckTicketSearch.Builder()
                .shopId(SHOP_ID)
                .statuses(RecheckTicketStatus.getOpenStatuses())
                .types(TYPE)
                .build()
        );
        assertFalse(tickets.isEmpty());

        assertEquals(RecheckTicketCheckMethod.PURCHASE, tickets.get(0).getCheckMethod());
    }

    @Test
    void getExpired() {
        doReturn(new CalendarEntry(null, false, false, "пн")).when(calendarService).get(any(LocalDateTime.class));

        recheckTicketService.save(new RecheckTicket.Builder()
                .withShopId(1L)
                .withType(RecheckTicketType.CUTOFF_APPROVE)
                .withSynopsis("text").build());
        assertFalse(recheckTicketService.getExpiredTickets(RecheckTicketType.CUTOFF_APPROVE, -1).isEmpty());
    }

    @Test
    void store() {
        long id = recheckTicketService.save(
                new RecheckTicket.Builder()
                        .withShopId(SHOP_ID)
                        .withType(RecheckTicketType.CUTOFF_APPROVE).build()).getId();

        assertNotNull(recheckTicketService.get(id));

        recheckTicketService.save(new RecheckTicket());
        assertEquals(2, recheckTicketService.findAll(RecheckTicketSearch.builder().build()).size());
    }

    @Test
    void previous() {
        RecheckTicket t = recheckTicketService.save(new RecheckTicket.Builder()
                .withShopId(SHOP_ID)
                .withType(RecheckTicketType.LITE_CPC)
                .build());
        assertNull(recheckTicketService.getPrevious(t));

        t = recheckTicketService.save(new RecheckTicket.Builder()
                .withShopId(SHOP_ID)
                .withType(RecheckTicketType.LITE_TICKET_COMMON)
                .build());
        assertNotNull(recheckTicketService.getPrevious(t));
    }
}
