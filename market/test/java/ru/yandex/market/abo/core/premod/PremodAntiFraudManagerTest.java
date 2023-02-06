package ru.yandex.market.abo.core.premod;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudRule;
import ru.yandex.market.abo.core.antifraud.yt.YtPremodAntiFraudManager;
import ru.yandex.market.abo.core.antifraud.yt.model.AntiFraudScoringResult;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.storage.json.premod.antifraud.JsonPremodAntiFraudResultService;
import ru.yandex.market.abo.util.monitoring.SharedMonitoringUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.02.2020
 */
class PremodAntiFraudManagerTest extends EmptyTestWithTransactionTemplate {

    private static final long TICKET_ID = 12345678L;
    private static final long SHOP_ID = 123L;

    @InjectMocks
    private PremodAntiFraudManager premodAntiFraudManager;

    @Mock
    private PremodManager premodManager;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodItemService premodItemService;
    @Mock
    private YtPremodAntiFraudManager ytPremodAntiFraudManager;
    @Mock
    private JsonPremodAntiFraudResultService jsonPremodAntiFraudResultService;
    @Mock
    private SharedMonitoringUnit antiFraudItemExpiredMonitoring;

    @Mock
    private PremodItem antiFraudItem;
    @Mock
    private PremodTicket ticket;
    @Mock
    private AntiFraudScoringResult antiFraudScoringResult;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(premodItemService.loadPremodItemsByStatusAndType(PremodItemStatus.NEWBORN, PremodItemType.ANTI_FRAUD))
                .thenReturn(List.of(antiFraudItem));
        when(premodItemService.loadPremodItemByTicketIdAndType(TICKET_ID, PremodItemType.ANTI_FRAUD))
                .thenReturn(antiFraudItem);

        when(antiFraudItem.getTicketId()).thenReturn(TICKET_ID);

        when(premodTicketService.loadTicketsByIds(any())).thenReturn(List.of(ticket));

        when(ticket.getShopId()).thenReturn(SHOP_ID);
        when(ticket.getId()).thenReturn(TICKET_ID);

        when(ytPremodAntiFraudManager.loadAntiFraudResults())
                .thenReturn(List.of(antiFraudScoringResult));

        when(antiFraudScoringResult.getShopId()).thenReturn(SHOP_ID);
    }

    @Test
    void processPremodAntiFraudWhenDoNotExistNewItems() {
        when(premodTicketService.loadTicketsByIds(any())).thenReturn(Collections.emptyList());
        premodAntiFraudManager.processPremodAntiFraud();
        verify(premodManager, never()).updatePremodItem(any(PremodItem.class));
    }

    @Test
    void processPremodAntiFraudWithExpiredItem() {
        when(premodItemService.loadPremodItemsByStatusAndTypeCreatedBefore(
                eq(PremodItemStatus.NEWBORN), eq(PremodItemType.ANTI_FRAUD), any(LocalDateTime.class))
        ).thenReturn(List.of(antiFraudItem));
        premodAntiFraudManager.processExpiredItems();
        verify(antiFraudItem).setStatus(PremodItemStatus.CANCELLED);
        verify(premodManager).updatePremodItem(antiFraudItem);
    }

    @Test
    void processPremodAntiFraudWithProblemShop() {
        when(antiFraudScoringResult.getRules()).thenReturn(List.of(AntiFraudRule.NO_REGION_RULE));
        premodAntiFraudManager.processPremodAntiFraud();
        verify(antiFraudItem).setStatus(PremodItemStatus.NEW);
        verify(premodManager).updatePremodItem(antiFraudItem);
    }

    @Test
    void processPremodAntiFraudWithNonProblemShop() {
        when(antiFraudScoringResult.getRules()).thenReturn(Collections.emptyList());
        premodAntiFraudManager.processPremodAntiFraud();
        verify(antiFraudItem).setStatus(PremodItemStatus.PASS);
        verify(premodManager).updatePremodItem(antiFraudItem);
    }
}
