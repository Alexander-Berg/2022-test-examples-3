package ru.yandex.market.abo.cpa.quality.recheck.ticket;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.common_inbox.CommonInboxService;
import ru.yandex.market.abo.core.common_inbox.InboxType;
import ru.yandex.market.abo.core.prepay.PrepayRequestManager;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
class RecheckTicketManagerTest extends EmptyTest {
    private static final long USER_ID = 1L;
    private static final List<Long> SHOP_IDS = Arrays.asList(-2L, -3L);

    @Autowired
    private RecheckTicketManager recheckTicketManager;
    @Autowired
    private RecheckTicketService recheckTicketService;
    @Autowired
    private CommonInboxService commonInboxService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private PrepayRequestManager prepayRequestManager;

    @BeforeEach
    void setUp() {
        pgJdbcTemplate.update("DELETE FROM recheck_ticket");
        pgJdbcTemplate.update("DELETE FROM common_inbox");
    }

    @Test
    void testPassAndFailRecheckTicket() {
        // don't use real listeners
        RecheckTicketManager spyManager = spy(recheckTicketManager);
        final Map<Long, RecheckTicketStatus> ticketsForListener = new HashMap<>();
        spyManager.setListeners(Collections.singletonList((ticket) ->
                ticketsForListener.put(ticket.getId(), ticket.getStatus())));

        RecheckTicketType type = RecheckTicketType.PROMO_CPC_MODERATION;
        String text = "Тестовая проверка";

        // create
        List<RecheckTicket> tickets = createRecheckTickets(SHOP_IDS, type, text);

        // pass
        commonInboxService.putTicketToInbox(USER_ID, tickets.get(0).getId(), InboxType.RECHECK_PROMO);
        spyManager.closeTicketWithInbox(tickets.get(0), RecheckTicketStatus.PASS, USER_ID, "");

        // fail
        commonInboxService.putTicketToInbox(USER_ID, tickets.get(1).getId(), InboxType.RECHECK_PROMO);
        spyManager.closeTicketWithInbox(tickets.get(1), RecheckTicketStatus.FAIL, USER_ID, "result comment");

        List<RecheckTicket> updatedTickets = getRecheckTickets(SHOP_IDS, type);
        assertTrue(updatedTickets.stream().anyMatch(t -> t.getStatus() == RecheckTicketStatus.FAIL));
        assertTrue(updatedTickets.stream().anyMatch(t -> ticketsForListener.get(t.getId()) == RecheckTicketStatus.FAIL));

        assertTrue(updatedTickets.stream().anyMatch(t -> t.getStatus() == RecheckTicketStatus.PASS));
        assertTrue(updatedTickets.stream().anyMatch(t -> ticketsForListener.get(t.getId()) == RecheckTicketStatus.PASS));
    }

    @Test
    void releaseCreditsDraftTicketsTest() {
        var oldTicket = new RecheckTicket(SHOP_IDS.get(0), RecheckTicketType.CREDITS, RecheckTicketStatus.DRAFT, "");
        oldTicket.setCreationTime(DateUtil.asDate(LocalDateTime.now().minusDays(1)));
        var newTicket = new RecheckTicket(SHOP_IDS.get(0), RecheckTicketType.CREDITS, RecheckTicketStatus.DRAFT, "");
        newTicket.setCreationTime(new Date());
        recheckTicketService.save(oldTicket);
        recheckTicketService.save(newTicket);

        recheckTicketManager.releaseCreditsDraftTickets();

        assertEquals(RecheckTicketStatus.OPEN, recheckTicketService.get(oldTicket.getId()).getStatus());
        assertEquals(RecheckTicketStatus.DRAFT, recheckTicketService.get(newTicket.getId()).getStatus());
    }

    @Nonnull
    private List<RecheckTicket> createRecheckTickets(List<Long> shopIds, RecheckTicketType type, String text) {
        Map<Long, RecheckTicket> mbiShops = shopIds.stream()
                .collect(Collectors.toMap(Function.identity(), shopId -> new RecheckTicket(shopId, type, text)));

        recheckTicketManager.createAndCancelByMbi(mbiShops, Collections.emptyList(), type, RecheckTicket::getShopId);
        List<RecheckTicket> tickets = getRecheckTickets(shopIds, type);
        assertEquals(shopIds.size(), tickets.size());
        tickets.forEach(t -> {
            assertEquals(RecheckTicketStatus.OPEN, t.getStatus());
            RecheckTicket ticket = recheckTicketService.get(t.getId());
            assertTrue(ticket.getSourceId() == null || ticket.getSourceId() == 0);
        });
        return tickets;
    }

    @Test
    void testCreateAndCancelRecheckTickets() {
        RecheckTicketType type = RecheckTicketType.PROMO_CPC_MODERATION;
        String text = "Тестовая проверка";

        // create
        List<RecheckTicket> tickets = createRecheckTickets(SHOP_IDS, type, text);

        // cancel
        recheckTicketManager.createAndCancelByMbi(Collections.emptyMap(), tickets, type, RecheckTicket::getShopId);
        tickets = getRecheckTickets(SHOP_IDS, type);
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == RecheckTicketStatus.CANCEL));
    }

    @Test
    void testCancelBluePremodRecheckTickets() {
        RecheckTicketType type = RecheckTicketType.BLUE_PREMODERATION;
        String text = "Тестовая проверка";

        List<RecheckTicket> tickets = createRecheckTickets(SHOP_IDS, type, text);

        recheckTicketManager.createAndCancelByMbi(Collections.emptyMap(), tickets, type, RecheckTicket::getShopId);
        tickets = getRecheckTickets(SHOP_IDS, type);
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == RecheckTicketStatus.CANCEL));
    }

    @Test
    void needCancelOnSynchronization() {
        var ticket = new RecheckTicket.Builder()
                .withShopId(1L)
                .withType(RecheckTicketType.BLUE_PREMODERATION)
                .withSourceId(1L)
                .build();
        // заявка есть
        assertFalse(recheckTicketManager.needCancelOnSynchronization(ticket));

        // заявки нет
        ticket.setSourceId(2L);
        assertTrue(recheckTicketManager.needCancelOnSynchronization(ticket));
    }

    @Test
    void testCancelSupplierPostmoderationRecheckTickets() {
        long withPrepayRequestId = SHOP_IDS.get(0);
        when(prepayRequestManager.prepayRequestOrNull(withPrepayRequestId, null))
                .thenReturn(new PrepayRequestDTO());

        RecheckTicketType type = RecheckTicketType.SUPPLIER_POSTMODERATION;
        String text = "Тестовая проверка";

        List<RecheckTicket> tickets = createRecheckTickets(SHOP_IDS, type, text);
        tickets.forEach(t -> t.setSourceId(t.getShopId()));

        recheckTicketManager.createAndCancelByMbi(Collections.emptyMap(), tickets, type, RecheckTicket::getShopId);
        tickets = getRecheckTickets(SHOP_IDS, type);
        assertTrue(tickets.stream()
                .filter(t -> t.getShopId() != withPrepayRequestId)
                .allMatch(t -> t.getStatus() == RecheckTicketStatus.CANCEL));
        assertSame(tickets.stream()
                .filter(t -> t.getShopId() == withPrepayRequestId)
                .findFirst()
                .get()
                .getStatus(), RecheckTicketStatus.OPEN);
    }

    private List<RecheckTicket> getRecheckTickets(List<Long> shopIds, RecheckTicketType type) {
        return shopIds.stream()
                .flatMap(shopId -> recheckTicketService.findAll(
                        new RecheckTicketSearch.Builder()
                                .types(type)
                                .shopId(shopId)
                                .build()).stream())
                .collect(Collectors.toList());
    }
}
