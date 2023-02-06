package ru.yandex.market.abo.core.shop;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.entity.ShopPlacement;
import ru.yandex.market.abo.api.entity.check.PartnerShopChecks;
import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 19.07.17.
 */
class CheckCountProviderTest extends AbstractCoreHierarchyTest {
    private static final long SHOP_ID = 774L;
    private static final int TICKETS_CNT = 2;
    @Autowired
    private CheckCountProvider checkCountProvider;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        IntStream.range(0, TICKETS_CNT).forEach(i -> generateClosedTicket());
    }

    @Test
    void checkCount() {
        Date fromDate = DateUtils.addDays(NOW, -30);

        assertEquals(TICKETS_CNT, checkCountProvider.getCheckCount(SHOP_ID, ShopPlacement.CPC, fromDate));
        assertEquals(TICKETS_CNT, checkCountProvider.getCheckCount(SHOP_ID, ShopPlacement.CPA, fromDate));
    }

    @Test
    void shopChecks() {
        Date from = DateUtils.addDays(NOW, -30);
        Arrays.stream(ShopPlacement.values()).forEach(placement -> {
            PartnerShopChecks checks = checkCountProvider.getShopCheckStats(SHOP_ID, placement, from, new Date());
            assertEquals(TICKETS_CNT, checks.getAllChecks());
        });
    }

    private void generateClosedTicket() {
        long ticketId = createTicket(SHOP_ID, 1);
        Ticket ticket = ticketService.loadTicketById(ticketId);
        ticket.setStatus(TicketStatus.FINISHED);
        ticketService.saveTicket(ticket, tagService.createTag(0));
        entityManager.flush();
        entityManager.clear();
    }
}
