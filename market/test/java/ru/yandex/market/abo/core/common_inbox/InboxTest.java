package ru.yandex.market.abo.core.common_inbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.inbox.InboxTicketFilter;
import ru.yandex.market.abo.core.ticket.ICheckMethod;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketSearch;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class InboxTest extends EmptyTest {

    private static final Long YA_UID = 1L;

    @Autowired
    CommonInboxService inboxService;
    @Autowired
    private CommonInboxReadOnlyService commonInboxReadOnlyService;
    @Autowired
    RecheckTicketService recheckTicketService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    AssessorService assessorService;

    @Test
    public void testTakeTickets() {
        CommonInboxService spyInboxService = spy(inboxService);
        doAnswer(inv -> {
            int cnt = (Integer) inv.getArguments()[2];
            List<Long> list = new ArrayList<>(cnt);
            for (int i = 0; i < cnt; i++) {
                list.add(i, (long) i);
            }
            return list;
        }).when(spyInboxService).getTicketsFromQueue(eq(InboxType.RECHECK_TICKET), anyLong(), anyInt());

        int freeSpace = commonInboxReadOnlyService.getInboxFreeSpace(YA_UID);
        int cnt = 2;
        List<Long> list = (List<Long>) spyInboxService.takeTicketsToInbox(YA_UID, cnt, InboxType.RECHECK_TICKET);

        assertFalse(list.isEmpty());

        assertEquals(Math.min(freeSpace, cnt), list.size());

        int updatedFreeSpace = commonInboxReadOnlyService.getInboxFreeSpace(YA_UID);

        assertEquals(freeSpace - list.size(), updatedFreeSpace);
    }

    @Test
    public void testPutAndThrowTicket() {
        long objId = RND.nextInt(1000);
        int freeSpace = commonInboxReadOnlyService.getInboxFreeSpace(YA_UID);
        inboxService.putTicketToInbox(YA_UID, objId, InboxType.RECHECK_TICKET);
        int updatedFreeSpace = commonInboxReadOnlyService.getInboxFreeSpace(YA_UID);
        assertEquals(freeSpace - 1, updatedFreeSpace);
        assertEquals(YA_UID, commonInboxReadOnlyService.getCurrentAssessor(InboxType.RECHECK_TICKET, objId));

        inboxService.throwTicketFromInbox(InboxType.RECHECK_TICKET, objId);
        assertEquals(updatedFreeSpace + 1, commonInboxReadOnlyService.getInboxFreeSpace(YA_UID));
        assertNull(commonInboxReadOnlyService.getCurrentAssessor(InboxType.RECHECK_TICKET, objId));
    }

    @Test
    public void testTake() {
        Arrays.stream(RecheckTicketType.values())
                .filter(type -> type.getInboxType() != null)
                .filter(type -> !inboxService.getFilters().containsKey(type.getInboxType()))
                .forEach(type -> {
                    long shopId = RND.nextInt();
                    recheckTicketService.save(new RecheckTicket.Builder().withShopId(shopId).withType(type).build());
                    List<RecheckTicket> tickets = recheckTicketService.findAll(
                            RecheckTicketSearch.builder().shopId(shopId).types(type).build());
                    assertFalse(tickets.isEmpty());
                    takeAndThrow(type.getInboxType());
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ticketAlreadyInInboxTest(boolean takeIntoInbox) {
        long shopId = RND.nextInt();
        var ticket = recheckTicketService.save(
                new RecheckTicket.Builder()
                        .withShopId(shopId)
                        .withType(RecheckTicketType.SUPPLIER_ASSORTMENT)
                        .build()
        );
        flushAndClear();
        long ticketId = ticket.getId();

        if (takeIntoInbox) {
            inboxService.takeTicketsToInbox(YA_UID, 1, RecheckTicketType.SUPPLIER_ASSORTMENT.getInboxType());
            flushAndClear();
            assertTrue(inboxService.ticketAlreadyInInbox(RecheckTicketType.SUPPLIER_ASSORTMENT.getInboxType(), ticketId));
        } else {
            assertFalse(inboxService.ticketAlreadyInInbox(RecheckTicketType.SUPPLIER_ASSORTMENT.getInboxType(), ticketId));
        }
    }

    public void takeAndThrow(InboxType type) {
        int inboxSize = commonInboxReadOnlyService.getCountQueues(type);
        assertTrue(inboxSize > 0);
        Collection<Long> ids = inboxService.takeTicketsToInbox(YA_UID, 1, type);
        assertFalse(ids.isEmpty());
        ids.forEach(id -> inboxService.throwTicketFromInbox(type, id));
    }

    @Test
    public void testTakeWithFilter() {
        recheckTicketService.save(
                new RecheckTicket.Builder()
                        .withShopId(774L)
                        .withType(RecheckTicketType.OFFLINE).build());
        recheckTicketService.findAll(RecheckTicketSearch.builder().build());

        InboxType type = InboxType.OFFLINE;
        Map<InboxType, InboxTicketFilter> oldFilters = inboxService.getFilters();
        try {
            Map<InboxType, InboxTicketFilter> filters = new HashMap<>();
            filters.put(type, new InboxTicketFilter() {
                @Override
                public Set<? extends ICheckMethod> checkMethods() {
                    return null;
                }

                @Nonnull
                @Override
                public Stream<Long> acceptBulk(List<Long> hypIds, long userId) {
                    return hypIds.stream();
                }
            });
            inboxService.setFilters(filters);

            takeAndThrow(type);
        } finally {
            inboxService.setFilters(oldFilters);
        }
    }

    @Test
    public void testTakeBluePremoderation() {
        long shopId = RND.nextInt();
        recheckTicketService.save(
                new RecheckTicket.Builder()
                        .withShopId(shopId)
                        .withType(RecheckTicketType.BLUE_PREMODERATION)
                        .build());
        List<RecheckTicket> tickets = recheckTicketService.findAll(
                RecheckTicketSearch.builder()
                        .shopId(shopId)
                        .types(RecheckTicketType.BLUE_PREMODERATION)
                        .build());
        assertFalse(tickets.isEmpty());
        long shopId2 = RND.nextInt();
        recheckTicketService.save(
                new RecheckTicket.Builder()
                        .withShopId(shopId2)
                        .withType(RecheckTicketType.SUPPLIER_POSTMODERATION)
                        .build());
        List<RecheckTicket> tickets2 = recheckTicketService.findAll(
                RecheckTicketSearch.builder()
                        .shopId(shopId2)
                        .types(RecheckTicketType.SUPPLIER_POSTMODERATION)
                        .build());
        assertFalse(tickets2.isEmpty());
        int inboxSize = commonInboxReadOnlyService.getCountQueues(InboxType.BLUE_PREMODERATION);
        assertEquals(2, inboxSize);
        Collection<Long> ids = inboxService.takeTicketsToInbox(YA_UID, 2, InboxType.BLUE_PREMODERATION);
        assertEquals(2, ids.size());
        ids.forEach(id -> inboxService.throwTicketFromInbox(InboxType.BLUE_PREMODERATION, id));
    }
}
