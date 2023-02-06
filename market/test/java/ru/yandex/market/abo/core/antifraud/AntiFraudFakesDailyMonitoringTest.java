package ru.yandex.market.abo.core.antifraud;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.antifraud.model.AntiFraudRule;
import ru.yandex.market.abo.core.antifraud.service.AntiFraudFakesStTicketCreator;
import ru.yandex.market.abo.core.antifraud.yt.YtAntiFraudDailyManager;
import ru.yandex.market.abo.core.antifraud.yt.model.AntiFraudScoringResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 27.02.2020
 */
class AntiFraudFakesDailyMonitoringTest {

    @InjectMocks
    private AntiFraudFakesDailyMonitoring antiFraudFakesDailyMonitoring;
    @Mock
    private YtAntiFraudDailyManager ytAntiFraudDailyManager;
    @Mock
    private AntiFraudFakesStTicketCreator antiFraudFakesStTicketCreator;

    @Mock
    private AntiFraudScoringResult antiFraudScoringResult;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(ytAntiFraudDailyManager.loadFakesResults()).thenReturn(List.of(antiFraudScoringResult));

        when(antiFraudScoringResult.getRules()).thenReturn(List.of(AntiFraudRule.NO_REGION_RULE));

        when(antiFraudFakesStTicketCreator.hasNoNewTickets(antiFraudScoringResult)).thenReturn(true);
    }

    @Test
    void processFakesCheckWithoutProblemShops() {
        when(antiFraudScoringResult.getRules()).thenReturn(Collections.emptyList());
        antiFraudFakesDailyMonitoring.monitor();
        verify(antiFraudFakesStTicketCreator, never()).createStTicket(any());
    }

    @Test
    void processFakesCheckWhenNewTicketExists() {
        when(antiFraudFakesStTicketCreator.hasNoNewTickets(antiFraudScoringResult)).thenReturn(false);
        antiFraudFakesDailyMonitoring.monitor();
        verify(antiFraudFakesStTicketCreator, never()).createStTicket(any());
    }

    @Test
    void processFakesCheckWithProblemShops() {
        antiFraudFakesDailyMonitoring.monitor();
        verify(antiFraudFakesStTicketCreator).createStTicket(any());
    }
}
