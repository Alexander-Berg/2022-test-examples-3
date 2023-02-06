package ru.yandex.market.abo.core.antifraud.yt;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.abo.core.antifraud.yt.model.AntiFraudScoringResult;
import ru.yandex.market.abo.core.yt.YtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.antifraud.yt.YtPremodAntiFraudManager.ANTI_FRAUD_PREMOD_TABLES_ROOT;
import static ru.yandex.market.abo.core.quality_monitoring.yt.idx.YtIdxMonitoringUtils.IDX_GENERATION_FORMATTER;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 26.02.2020
 */
class YtPremodAntiFraudManagerTest {

    private static final String LAST_GENERATION =
            LocalDateTime.now().minusHours(1L).format(IDX_GENERATION_FORMATTER);
    private static final String PREVIOUS_GENERATION =
            LocalDateTime.now().minusHours(6L).format(IDX_GENERATION_FORMATTER);

    @InjectMocks
    private YtPremodAntiFraudManager ytPremodAntiFraudManager;

    @Mock
    private YtService ytService;

    @Mock
    private AntiFraudScoringResult lastScoringResult;
    @Mock
    private AntiFraudScoringResult previousScoringResult;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(ytService.list(YPath.simple(ANTI_FRAUD_PREMOD_TABLES_ROOT))).thenReturn(antiFraudGenerations());
        when(ytService.readTableJson(generationTablePath(LAST_GENERATION), AntiFraudScoringResult.class))
                .thenReturn(List.of(lastScoringResult));
        when(ytService.readTableJson(generationTablePath(PREVIOUS_GENERATION), AntiFraudScoringResult.class))
                .thenReturn(List.of(previousScoringResult));
    }

    /**
     * Проверяем, что данные берутся только из последнего поколения.
     */
    @Test
    void loadAntiFraudResultsTest() {
        var scoringResults = ytPremodAntiFraudManager.loadAntiFraudResults();
        assertEquals(Set.of(lastScoringResult), new HashSet<>(scoringResults));
    }

    private static List<String> antiFraudGenerations() {
        return List.of(PREVIOUS_GENERATION, LAST_GENERATION);
    }

    private static YPath generationTablePath(String generation) {
        return YPath.simple(ANTI_FRAUD_PREMOD_TABLES_ROOT + "/" + generation);
    }
}
