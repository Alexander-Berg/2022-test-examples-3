package ru.yandex.market.abo.core.premod;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketMock;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.abo.core.premod.model.PremodTicketSubstatus;
import ru.yandex.market.abo.util.FakeUsers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.premod.model.PremodItemType.MONITORINGS;

/**
 * @author artemmz
 * created on 13.03.17.
 */
public class PremodManagerTest extends EmptyTestWithTransactionTemplate {

    private static final long PREMOD_TICKET_ID = 1;

    private static final long MONITORING_ITEM_ID = 2;
    private static final long USER_ID = 1337;

    protected static final Random RND = new Random();

    @InjectMocks
    private PremodManager premodManager;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodProblemService premodProblemService;
    @Mock
    private PremodItemService premodItemService;
    @Mock
    private PremodTicketListener listener;
    @Mock
    private PremodTicket premodTicket;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        premodManager.setTicketListeners(Collections.singletonList(listener));
        doNothing().when(premodTicketService).ensureTicketNotFinished(anyInt());
        doNothing().when(premodTicketService).updatePremodTicket(any());

        when(premodTicket.getId()).thenReturn(PREMOD_TICKET_ID);
        when(premodTicket.getStatus()).thenReturn(PremodTicketStatus.NEW);
        when(premodTicketService.loadPremodTicket(PREMOD_TICKET_ID)).thenReturn(premodTicket);
    }

    @Test
    void testInvokeListenersOnCreate() {
        PremodTicket ticket = new PremodTicket(0, 0, PremodCheckType.CPC_PREMODERATION);
        ticket.setId(RND.nextLong());
        premodManager.updatePremodTicket(ticket);

        verify(premodTicketService).updatePremodTicket(ticket);
        verify(listener).ticketUpdated(ticket);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInvokeListenersOnUpdate(boolean stateChanged) {
        PremodTicket ticket = fetchTicketFromDb(PremodTicketStatus.FAILED);
        if (stateChanged) {
            ticket.setSubstatus(PremodTicketSubstatus.NEED_INFO);
        }

        premodManager.updatePremodTicket(ticket);

        verify(premodTicketService).updatePremodTicket(ticket);
        verify(listener, times(stateChanged ? 1 : 0)).ticketUpdated(ticket);
    }

    private static PremodTicketMock fetchTicketFromDb(PremodTicketStatus inStatus) {
        PremodTicketMock ticket = new PremodTicketMock(inStatus);
        ticket.setId(111L);
        ticket.setShopId(RND.nextLong());
        ticket.setTestingType(PremodCheckType.CPC_PREMODERATION);
        ticket.setTryNumber(1);
        return ticket;
    }

    @Test
    void nullSubstatus() {
        PremodTicketSubstatus.getById(null);
    }

    @Test
    void failItemAndCancelAllOthersTest() {
        var monitoringItem = mockItem(MONITORING_ITEM_ID, MONITORINGS);
        when(premodItemService.loadPremodItem(MONITORING_ITEM_ID)).thenReturn(monitoringItem);
        var anotherItemWithNewStatus = mockItem(MONITORING_ITEM_ID + 1, PremodItemType.AUTOORDER, PremodItemStatus.NEW);
        var anotherItemWithFinishedStatus = mockItem(MONITORING_ITEM_ID + 2, PremodItemType.LOGO_CHECK, PremodItemStatus.PASS);
        when(premodItemService.loadPremodItemsByTicket(PREMOD_TICKET_ID))
                .thenReturn(List.of(monitoringItem, anotherItemWithFinishedStatus, anotherItemWithNewStatus));

        premodManager.failTicketByItem(USER_ID, monitoringItem);

        Stream.of(anotherItemWithNewStatus, anotherItemWithFinishedStatus).forEach(item -> {
            verify(item).setStatus(PremodItemStatus.CANCELLED);
            verify(item).setYaUid(FakeUsers.PREMOD_AUTO.getId());
            verify(premodItemService).updatePremodItem(item);
            verify(premodProblemService).deleteByItem(item.getId());
        });
        verify(monitoringItem).setStatus(PremodItemStatus.FAILED);
        verify(monitoringItem).setYaUid(USER_ID);
    }

    private static PremodItem mockItem(long id, PremodItemType type, PremodItemStatus status) {
        var item = mockItem(id, type);
        when(item.getStatus()).thenReturn(status);
        return item;
    }

    private static PremodItem mockItem(long id, PremodItemType type) {
        var item = mock(PremodItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getTicketId()).thenReturn(PREMOD_TICKET_ID);
        when(item.getType()).thenReturn(type);
        return item;
    }
}
