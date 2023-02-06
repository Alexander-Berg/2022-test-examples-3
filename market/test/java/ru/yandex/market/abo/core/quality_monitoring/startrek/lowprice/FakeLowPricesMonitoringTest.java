package ru.yandex.market.abo.core.quality_monitoring.startrek.lowprice;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.CoreCounter;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringSettings;
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.YtIdxMonitoringManager;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringIndexType;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringValue;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.shop.on.ShopOnService;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.exception.ExceptionalShopReason.IGNORE_LOW_PRICES;
import static ru.yandex.market.abo.core.startrek.model.StartrekTicketReason.FAKE_LOW_PRICES_SHOP;

/**
 * @author artemmz
 * @date 10.10.18.
 */
public class FakeLowPricesMonitoringTest {
    private static final long SHOP_ID = 890;
    private static final StartrekTicketReason ST_REASON = FAKE_LOW_PRICES_SHOP;
    private static final String LAST_IDX_GENERATION = "20191016_1345";

    @InjectMocks
    private FakeLowPricesMonitoring fakeLowPricesMonitoring;

    @Mock
    private ExceptionalShopsService exceptionalShopsService;
    @Mock
    private ShopOnService shopOnService;
    @Mock
    private MonitoringSettingsService monitoringSettingsService;
    @Mock
    private YtIdxMonitoringManager ytIdxMonitoringManager;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private StartrekTicketManager startrekTicketManager;
    @Mock
    private ConfigurationService coreCounterService;
    @Mock
    private MbiApiService mbiApiService;

    @Mock
    private MonitoringSettings settings;
    private final MonitoringValue monitoringValue = new MonitoringValue(SHOP_ID, 0, Collections.emptyList());

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fakeLowPricesMonitoring.setStartrekTicketReason(ST_REASON);

        when(monitoringSettingsService.getSettings(any())).thenReturn(settings);
        when(ytIdxMonitoringManager.loadLastMonitoringGeneration(MonitoringType.ANTI_FAKE, MonitoringIndexType.WHITE))
                .thenReturn(LAST_IDX_GENERATION);
        when(ytIdxMonitoringManager.loadAll(MonitoringType.ANTI_FAKE, MonitoringIndexType.WHITE, LAST_IDX_GENERATION))
                .thenReturn(List.of(monitoringValue));

        when(startrekTicketManager.hasNoNewTickets(eq(SHOP_ID), eq(ST_REASON), any())).thenReturn(true);
        when(mbiApiService.getOpenCutoffs(SHOP_ID)).thenReturn(Collections.emptyList());
    }

    @Test
    void createTicket() {
        fakeLowPricesMonitoring.monitor();
        verify(startrekTicketManager).createTicket(any());
        verify(coreCounterService).mergeValue(
                CoreCounter.LAST_IDX_MONITORING_PROCESSED_GENERATION.asPrefixTo(MonitoringType.ANTI_FAKE.name()),
                LAST_IDX_GENERATION
        );
    }

    @Test
    void exceptionalShop() {
        when(exceptionalShopsService.loadShops(IGNORE_LOW_PRICES)).thenReturn(Collections.singleton(SHOP_ID));
        fakeLowPricesMonitoring.monitor();
        verify(startrekTicketManager, never()).createTicket(any());
    }

    @Test
    void alreadyCreated() {
        when(startrekTicketManager.hasNoNewTickets(eq(SHOP_ID), eq(ST_REASON), any())).thenReturn(false);
        fakeLowPricesMonitoring.monitor();
        verify(startrekTicketManager, never()).createTicket(any());
    }

    @Test
    void alreadyProcessed() {
        when(coreCounterService.getValue(
                CoreCounter.LAST_IDX_MONITORING_PROCESSED_GENERATION.asPrefixTo(MonitoringType.ANTI_FAKE.name())
        )).thenReturn(LAST_IDX_GENERATION);
        fakeLowPricesMonitoring.monitor();
        verify(startrekTicketManager, never()).createTicket(any());
    }
}
