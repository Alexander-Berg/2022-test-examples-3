package ru.yandex.market.abo.core.assortment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.common_inbox.CommonInboxService;
import ru.yandex.market.abo.core.cutoff.feature.FeatureStatusManager;
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.YtIdxMonitoringManager;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.YtIdxMonitoringUtils;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringIndexType;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringValue;
import ru.yandex.market.abo.core.storage.json.model.JsonEntityType;
import ru.yandex.market.abo.core.storage.json.premod.monitoring.JsonMonitoringResultService;
import ru.yandex.market.abo.core.supplier.SupplierOnIndexManager;
import ru.yandex.market.abo.cpa.lms.ExpressPartnerService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService;
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 19.08.2020
 */
class SupplierAssortmentMonitoringManagerTest extends EmptyTestWithTransactionTemplate {

    private static final String LAST_IDX_GENERATION = "20200820_1032";

    private static final long TICKET_ID = 34425423L;

    private static final long SHOP_ID = 123L;

    @InjectMocks
    private SupplierAssortmentMonitoringManager supplierAssortmentMonitoringManager;

    @Mock
    private YtIdxMonitoringManager ytIdxMonitoringManager;
    @Mock
    private JsonMonitoringResultService jsonMonitoringResultService;
    @Mock
    private RecheckTicketService recheckTicketService;
    @Mock
    private SupplierOnIndexManager supplierOnIndexManager;
    @Mock
    private FeatureStatusManager featureStatusManager;
    @Mock
    private ExpressPartnerService expressPartnerService;
    @Mock
    private CommonInboxService commonInboxService;

    @Mock
    private RecheckTicket recheckTicket;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(recheckTicketService.findAll(any())).thenReturn(List.of(recheckTicket));

        when(expressPartnerService.isExpressPartner(SHOP_ID)).thenReturn(false);

        when(recheckTicket.getId()).thenReturn(TICKET_ID);
        when(recheckTicket.getShopId()).thenReturn(SHOP_ID);
        when(recheckTicket.getCreationTime()).thenReturn(
                DateUtil.asDate(YtIdxMonitoringUtils.idxGenerationToLocalDateTime(LAST_IDX_GENERATION).minusMinutes(30))
        );

        when(ytIdxMonitoringManager.loadLastGenerationForAllMonitorings(any(), eq(MonitoringIndexType.BLUE)))
                .thenReturn(LAST_IDX_GENERATION);

        when(supplierOnIndexManager.loadSuppliersOnIndex(any(), anyString())).thenReturn(Set.of(SHOP_ID));
    }

    @Test
    void processSupplierAssortmentMonitoring__unprocessedTicketsNoExist() {
        when(recheckTicketService.findAll(any())).thenReturn(Collections.emptyList());

        supplierAssortmentMonitoringManager.processSupplierAssortmentMonitoring();

        verify(recheckTicketService, never()).update(any());
        verifyNoMoreInteractions(ytIdxMonitoringManager, jsonMonitoringResultService);
    }

    @ParameterizedTest(name = "enabledTicket_{index}")
    @MethodSource("monitoringResults")
    void processSupplierAssortmentMonitoring__enabledTicket(Map<Long, Map<MonitoringType, MonitoringValue>> monitoringResultsByShop) {
        when(recheckTicketService.findAll(any())).thenReturn(List.of(recheckTicket)).thenReturn(List.of());
        when(ytIdxMonitoringManager.loadMonitoringResultsByShop(any(), eq(MonitoringIndexType.BLUE), eq(LAST_IDX_GENERATION)))
                .thenReturn(monitoringResultsByShop);
        doNothing().when(commonInboxService).throwTicketFromInbox(any(), anyLong());

        supplierAssortmentMonitoringManager.processSupplierAssortmentMonitoring();

        verify(recheckTicket).setStatus(RecheckTicketStatus.OPEN);
        verify(recheckTicketService).update(recheckTicket);
        if (monitoringResultsByShop.containsKey(SHOP_ID)) {
            verify(jsonMonitoringResultService).saveIfNeeded(
                    TICKET_ID, JsonEntityType.SUPPLIER_ASSORTMENT_MONITORING_RESULT, monitoringResultsByShop.get(SHOP_ID)
            );
        }
        if (!monitoringResultsByShop.containsKey(SHOP_ID)) {
            verify(featureStatusManager).sendResult(any());
        }
    }

    private static Stream<Arguments> monitoringResults() {
        return Stream.of(
                Map.of(
                        SHOP_ID,
                        Map.of(MonitoringType.UNIQUE, new MonitoringValue(SHOP_ID, 12, List.of(123154L, 31541735L)))
                ), Map.of(
                        SHOP_ID + 1,
                        Map.of(MonitoringType.UNIQUE, new MonitoringValue(SHOP_ID + 1, 12, List.of(12314L, 3154135L)))
                )).map(Arguments::of);
    }
}
