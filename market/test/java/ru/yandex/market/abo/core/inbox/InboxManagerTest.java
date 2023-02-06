package ru.yandex.market.abo.core.inbox;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.autoorder.callcenter.AutoOrderPhoneService;
import ru.yandex.market.abo.core.queue.QueueFilter;
import ru.yandex.market.abo.core.queue.entity.QueueTicket;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;
import ru.yandex.market.abo.core.ticket.ICheckMethod;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.gen.model.GenId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 05.10.18
 */
public class InboxManagerTest extends AbstractCoreHierarchyTest {

    private static final long SHOP_ID = 155L;
    private static final long ANOTHER_SHOP_ID = 774;
    private static final int GEN_ID = -1;

    private static final long USER_ID = -101L;
    private static final long ATTEMPT_COUNT = InboxManager.ALREADY_TAKEN_ATTEMPT_LIMIT;

    @Autowired
    @InjectMocks
    private InboxManager inboxManager;
    @Autowired
    private InboxService inboxService;
    @Mock
    private QueueFilter queueFilter;
    @Mock
    private AutoOrderPhoneService autoOrderPhoneService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getTicket() {
        long ticketId = createTicket(SHOP_ID, GEN_ID);
        QueueTicket ticket = build(ticketId, CheckMethod.DEFAULT, GEN_ID);
        when(queueFilter.getTicketsFromQueue(any())).thenReturn(Collections.singletonList(ticket));
        when(queueFilter.check(any(), anyLong(), any())).thenReturn(true);

        assertFalse(inboxService.existsTicketInInbox(USER_ID, ticketId));
        inboxManager.takeTicketsToInbox(USER_ID, InboxTicketFilter.ACCEPT_ALL_EXCEPT_AUTO_ORDER);
        assertTrue(inboxService.existsTicketInInbox(USER_ID, ticketId));
    }

    @Test
    public void testTicketsAlreadyTaken() {
        List<QueueTicket> tickets = LongStream.range(0, ATTEMPT_COUNT + 1)
                .mapToObj(id -> createTicket(SHOP_ID, GEN_ID))
                .map(ticketId -> InboxManagerTest.build(ticketId, CheckMethod.DEFAULT, GEN_ID))
                .collect(Collectors.toList());
        when(queueFilter.getTicketsFromQueue(any())).thenReturn(tickets);
        when(queueFilter.check(any(), anyLong(), any())).thenReturn(true);

        LongStream.range(0, ATTEMPT_COUNT)
                .<Collection>mapToObj(userId -> inboxManager.takeTicketsToInbox(userId, InboxTicketFilter.ACCEPT_ALL_EXCEPT_AUTO_ORDER))
                .forEach(list -> assertFalse(list.isEmpty()));
        assertTrue(inboxManager.takeTicketsToInbox(ATTEMPT_COUNT, InboxTicketFilter.ACCEPT_ALL_EXCEPT_AUTO_ORDER).isEmpty());
    }

    @Test
    void testAutoOrderPhoneInbox() {
        var inboxFilter = mockAutoOrderFilter();
        when(autoOrderPhoneService.getFreePhonesCount()).thenReturn(1);

        long ticketId = createTicket(SHOP_ID, GenId.AUTO_ORDER_PHONE);
        long anotherTicketId = createTicket(ANOTHER_SHOP_ID, GenId.AUTO_ORDER_PHONE);
        QueueTicket ticket = build(ticketId, CheckMethod.AUTO_ORDER, GenId.AUTO_ORDER_PHONE);
        QueueTicket anotherTicket = build(anotherTicketId, CheckMethod.AUTO_ORDER, GenId.AUTO_ORDER_PHONE);

        when(queueFilter.getTicketsFromQueue(any())).thenReturn(List.of(ticket, anotherTicket));
        when(queueFilter.check(any(), anyLong(), any())).thenReturn(true);

        inboxManager.takeTicketsToInbox(USER_ID, inboxFilter);

        assertTrue(inboxService.existsTicketInInbox(USER_ID, ticketId));
        assertFalse(inboxService.existsTicketInInbox(USER_ID, anotherTicketId));
    }

    private static QueueTicket build(long hypId, CheckMethod checkMethod, int genId) {
        return new QueueTicket(hypId, genId, SHOP_ID, checkMethod, Regions.RUSSIA);
    }

    private static InboxTicketFilter mockAutoOrderFilter() {
        var checkMethods = new HashSet<ICheckMethod>();
        checkMethods.add(CheckMethod.AUTO_ORDER);

        var inboxFilter = mock(InboxTicketFilter.class);
        doReturn(checkMethods).when(inboxFilter).checkMethods();
        doReturn(true).when(inboxFilter).accept(any(QueueTicket.class), anyLong());

        return inboxFilter;
    }
}
