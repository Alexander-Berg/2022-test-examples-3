package ru.yandex.market.abo.cpa.quality.recheck.premod;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.prepay.PrepayRequestManager;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketManager;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;
import ru.yandex.market.api.cpa.yam.dto.RequestShopsInfoDTO;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.core.application.PartnerApplicationStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 26/02/2020.
 */
class PartnerRequestTicketManagerTest {
    private static final long TICKET_ID = 4312341;
    private static final long USER_ID = 123123;
    private static final long SOURCE_ID = 3242342;
    private static final long SHOP_ID = 437567342;

    @InjectMocks
    PartnerRequestTicketManager partnerRequestTicketManager;
    @Mock
    RecheckTicketManager recheckTicketManager;
    @Mock
    PrepayRequestManager prepayRequestTicketManager;
    @Mock
    CheckouterAPI checkouterClient;
    @Mock
    RecheckTicket recheckTicket;
    @Mock
    RequestShopsInfoDTO requestShopsInfoDTO;
    @Mock
    RequestShopsInfoDTO.Data requestData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(recheckTicketManager.getRecheckTicket(TICKET_ID)).thenReturn(recheckTicket);
        when(recheckTicket.getSourceId()).thenReturn(SOURCE_ID);
        when(recheckTicket.getType()).thenReturn(RecheckTicketType.BLUE_PREMODERATION);
        when(prepayRequestTicketManager.getPrepayRequestShops(SOURCE_ID)).thenReturn(requestShopsInfoDTO);
        when(requestShopsInfoDTO.getData()).thenReturn(List.of(requestData));
        when(requestData.getDatasourceId()).thenReturn(SHOP_ID);

    }

    @ParameterizedTest
    @CsvSource({"BLUE_PREMODERATION", "SUPPLIER_POSTMODERATION"})
    void testCloseTicket(RecheckTicketType ticketType) {
        when(recheckTicket.getType()).thenReturn(ticketType);

        PartnerApplicationStatus status = PartnerApplicationStatus.COMPLETED;
        String comment = "whatever";
        partnerRequestTicketManager.closeTicket(TICKET_ID, USER_ID, status, comment);

        verify(prepayRequestTicketManager).updateRequest(SOURCE_ID, status, comment, USER_ID);
        verify(recheckTicketManager).closeTicketWithInbox(
                recheckTicket, PartnerRequestTicketManager.MBI_TO_RECHECK_STATUSES.get(status), USER_ID, comment
        );
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void testTicketForFrozenStatus(boolean hasOrders) {
        mockCheckouterAnswer(hasOrders);
        String comment = "whatever";

        partnerRequestTicketManager.closeTicket(TICKET_ID, USER_ID, PartnerApplicationStatus.FROZEN, comment);

        if (hasOrders) {
            verify(prepayRequestTicketManager).freeze(SOURCE_ID, List.of(SHOP_ID), comment, USER_ID);
        } else {
            verify(prepayRequestTicketManager).updateRequest(
                    SOURCE_ID, PartnerApplicationStatus.DECLINED, comment, USER_ID
            );
        }
    }

    private void mockCheckouterAnswer(boolean hasOrders) {
        if (hasOrders) {
            var orderMock = mock(Order.class);
            when(checkouterClient.getOrdersByShop(any(), eq(SHOP_ID)))
                    .thenReturn(new PagedOrders(List.of(orderMock), new Pager()));
        } else {
            when(checkouterClient.getOrdersByShop(any(), eq(SHOP_ID)))
                    .thenReturn(new PagedOrders(Collections.emptyList(), new Pager()));
        }
    }
}
