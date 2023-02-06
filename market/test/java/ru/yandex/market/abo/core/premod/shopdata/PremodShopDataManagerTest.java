package ru.yandex.market.abo.core.premod.shopdata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.shopdata.model.PremodShopChangeInfo;
import ru.yandex.market.abo.core.premod.shopdata.model.PremodShopData;
import ru.yandex.market.abo.core.premod.shopdata.model.PremodShopDataType;
import ru.yandex.market.abo.core.premod.shopdata.service.PremodShopDataService;
import ru.yandex.market.abo.core.premod.shopdata.service.PremodShopDataTypeLoader;
import ru.yandex.market.abo.core.storage.json.shop.change.JsonPremodShopChangeInfoService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 10.04.2020
 */
class PremodShopDataManagerTest {

    private static final long SHOP_ID = 123L;
    private static final long NEW_TICKET_ID = 111111L;
    private static final long TICKET_ID = 111110L;

    private static final String CHANGED_REQUSITES = "3214567890123";
    private static final String REQUSITES = "1234567890123";

    private static final Map<PremodShopDataType, PremodShopData> CHANGED_PREMOD_SHOP_DATA =
            premodShopRequisitesDataByType(NEW_TICKET_ID, CHANGED_REQUSITES);

    @InjectMocks
    private PremodShopDataManager premodShopDataManager;

    @Mock
    private List<PremodShopDataTypeLoader> dataTypeLoaders;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodShopDataService premodShopDataService;
    @Mock
    private JsonPremodShopChangeInfoService jsonPremodShopChangeInfoService;

    @Mock
    private PremodTicket newTicket;
    @Mock
    private PremodTicket previousTicket;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        mockTicket(newTicket, NEW_TICKET_ID);
        mockTicket(previousTicket, TICKET_ID);

        when(premodTicketService.getPreviousTicket(newTicket)).thenReturn(previousTicket);

        when(premodShopDataService.getShopDataByTicket(previousTicket))
                .thenReturn(List.of(premodShopRequisitesData(TICKET_ID, REQUSITES)));
    }

    @Test
    void checkShopChangeWhenPreviousTicketNotExists() {
        when(premodTicketService.getPreviousTicket(newTicket)).thenReturn(null);
        premodShopDataManager.checkShopChange(newTicket, CHANGED_PREMOD_SHOP_DATA);
        verify(premodShopDataService, never()).getShopDataByTicket(any());
        verify(jsonPremodShopChangeInfoService).saveIfNotEmpty(NEW_TICKET_ID, Collections.emptyMap());
    }

    @Test
    void checkShopChangeWhenPreviousPremodDataNotExists() {
        when(premodShopDataService.getShopDataByTicket(previousTicket)).thenReturn(Collections.emptyList());
        premodShopDataManager.checkShopChange(newTicket, CHANGED_PREMOD_SHOP_DATA);
        verify(jsonPremodShopChangeInfoService).saveIfNotEmpty(NEW_TICKET_ID, Collections.emptyMap());
    }

    @Test
    void checkShopChangeWhenDataNotChanged() {
        premodShopDataManager.checkShopChange(newTicket, premodShopRequisitesDataByType(NEW_TICKET_ID, REQUSITES));
        verify(jsonPremodShopChangeInfoService).saveIfNotEmpty(NEW_TICKET_ID, Collections.emptyMap());
    }

    @Test
    void checkShopChangeWhenDataChanged() {
        premodShopDataManager.checkShopChange(newTicket, CHANGED_PREMOD_SHOP_DATA);
        verify(jsonPremodShopChangeInfoService).saveIfNotEmpty(
                NEW_TICKET_ID, Map.of(PremodShopDataType.REQUISITES, new PremodShopChangeInfo(REQUSITES, CHANGED_REQUSITES))
        );
    }

    private static void mockTicket(PremodTicket ticket, long ticketId) {
        when(ticket.getShopId()).thenReturn(SHOP_ID);
        when(ticket.getId()).thenReturn(ticketId);
    }

    private static Map<PremodShopDataType, PremodShopData> premodShopRequisitesDataByType(long ticketId, String dataValue) {
        return Map.of(PremodShopDataType.REQUISITES, premodShopRequisitesData(ticketId, dataValue));
    }

    private static PremodShopData premodShopRequisitesData(long ticketId, String dataValue) {
        return new PremodShopData(ticketId, PremodShopDataType.REQUISITES, dataValue);
    }
}
