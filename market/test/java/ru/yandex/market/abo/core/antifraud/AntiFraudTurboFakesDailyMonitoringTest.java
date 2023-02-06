package ru.yandex.market.abo.core.antifraud;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.CoreCounter;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudRule;
import ru.yandex.market.abo.core.antifraud.service.AntiFraudFakesStTicketCreator;
import ru.yandex.market.abo.core.antifraud.yt.YtAntiFraudDailyManager;
import ru.yandex.market.abo.turbo.premod.db.TurboShopService;
import ru.yandex.market.abo.turbo.premod.db.model.TurboShop;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 16.09.2020
 */
class AntiFraudTurboFakesDailyMonitoringTest {

    private static final String PREVIOUS_YT_TABLE_NAME = "2020-09-15";
    private static final String CURRENT_YT_TABLE_NAME = "2020-09-16";

    private static final long SHOP_ID = 123L;
    private static final String HOST_URL = "turbo-test.ru";

    @InjectMocks
    private AntiFraudTurboFakesDailyMonitoring antiFraudTurboFakesDailyMonitoring;

    @Mock
    private TurboShopService turboShopService;
    @Mock
    private YtAntiFraudDailyManager ytAntiFraudDailyManager;
    @Mock
    private AntiFraudFakesStTicketCreator antiFraudFakesStTicketCreator;
    @Mock
    private ConfigurationService coreCounterService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(coreCounterService.getValue(CoreCounter.LAST_ANTI_FRAUD_TURBO_FAKES_TABLE.name())).thenReturn(PREVIOUS_YT_TABLE_NAME);

        when(ytAntiFraudDailyManager.loadLastYtTurboFakesTable()).thenReturn(CURRENT_YT_TABLE_NAME);
        when(ytAntiFraudDailyManager.loadTurboFakesResults(CURRENT_YT_TABLE_NAME))
                .thenReturn(Map.of(HOST_URL, List.of(AntiFraudRule.VACUUM_RULE)));

        var turboShop = mock(TurboShop.class);
        when(turboShop.getShopId()).thenReturn(SHOP_ID);
        when(turboShop.getOriginalDomain()).thenReturn(HOST_URL);
        when(turboShopService.load(HOST_URL)).thenReturn(turboShop);

        when(antiFraudFakesStTicketCreator.hasNoNewTickets(SHOP_ID)).thenReturn(true);
    }

    @Test
    void monitorTurboFakes__noNewYtTable() {
        when(ytAntiFraudDailyManager.loadLastYtTurboFakesTable()).thenReturn(PREVIOUS_YT_TABLE_NAME);

        antiFraudTurboFakesDailyMonitoring.monitor();

        verifyNoMoreInteractions(turboShopService, antiFraudFakesStTicketCreator);
        verify(ytAntiFraudDailyManager, never()).loadTurboFakesResults(anyString());
        verify(coreCounterService, never()).mergeValue(anyString(), anyString());
    }

    @Test
    void monitorTurboFakes__unknownTurboShop() {
        when(turboShopService.load(HOST_URL)).thenReturn(null);

        antiFraudTurboFakesDailyMonitoring.monitor();

        verifyNoMoreInteractions(antiFraudFakesStTicketCreator);
    }

    @Test
    void monitorTurboFakes__ticketAlreadyCreated() {
        when(antiFraudFakesStTicketCreator.hasNoNewTickets(SHOP_ID)).thenReturn(false);

        antiFraudTurboFakesDailyMonitoring.monitor();

        verify(antiFraudFakesStTicketCreator, never()).createStTicket(eq(SHOP_ID), any());
    }
}
