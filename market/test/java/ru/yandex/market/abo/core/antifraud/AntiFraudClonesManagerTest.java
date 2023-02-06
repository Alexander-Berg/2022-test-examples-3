package ru.yandex.market.abo.core.antifraud;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.abo.core.CoreCounter;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudCloneCheckResult;
import ru.yandex.market.abo.core.antifraud.service.AntiFraudCloneCheckResultService;
import ru.yandex.market.abo.core.yt.YtService;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.antifraud.AntiFraudClonesManager.CLONES_TABLES_PATH;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 10.03.2020
 */
class AntiFraudClonesManagerTest {

    private static final String LAST_ANTI_FRAUD_CLONES_GENERATION = "20200310_1256";
    private static final String PREVIOUS_ANTI_FRAUD_CLONES_GENERATION = "20200310_1014";

    private static final long SHOP_ID = 123L;
    private static final long CLONE_ID = 124L;
    private static final String CRYPTA_ID = "crypta/12345";

    @InjectMocks
    private AntiFraudClonesManager antiFraudClonesManager;

    @Mock
    private YtService ytService;
    @Mock
    private AntiFraudCloneCheckResultService antiFraudCloneCheckResultService;
    @Mock
    private ConfigurationService coreCounterService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(ytService.list(YPath.simple(CLONES_TABLES_PATH)))
                .thenReturn(List.of(LAST_ANTI_FRAUD_CLONES_GENERATION, PREVIOUS_ANTI_FRAUD_CLONES_GENERATION));
        when(coreCounterService.getValue(CoreCounter.LAST_ANTI_FRAUD_CLONES_GENERATION.name()))
                .thenReturn(PREVIOUS_ANTI_FRAUD_CLONES_GENERATION);
    }

    @Test
    void updateClonesResultsWhenNoNewGeneration() {
        when(coreCounterService.getValue(CoreCounter.LAST_ANTI_FRAUD_CLONES_GENERATION.name()))
                .thenReturn(LAST_ANTI_FRAUD_CLONES_GENERATION);
        antiFraudClonesManager.updateClonesResults();
        verify(ytService, never()).readTableJson(any(), eq(AntiFraudCloneCheckResult.class));
        verify(antiFraudCloneCheckResultService, never()).save(anyList());
    }

    @Test
    void updateClonesResultsWhenExistsNewGeneration() {
        var cloneCheckResult = createCloneCheckResult();
        when(ytService.readTableJson(any(), eq(AntiFraudCloneCheckResult.class)))
                .thenReturn(List.of(cloneCheckResult));
        antiFraudClonesManager.updateClonesResults();
        verify(ytService).readTableJson(eq(tablePath(LAST_ANTI_FRAUD_CLONES_GENERATION)), eq(AntiFraudCloneCheckResult.class));
        verify(antiFraudCloneCheckResultService).save(List.of(cloneCheckResult));
    }

    @Test
    void updateClonesResultsWhenCheckResultAlreadySaved() {
        var cloneCheckResult = createCloneCheckResult();
        when(ytService.readTableJson(any(), eq(AntiFraudCloneCheckResult.class)))
                .thenReturn(List.of(cloneCheckResult));
        when(antiFraudCloneCheckResultService.findAllCheckResultsForShops(Set.of(SHOP_ID)))
                .thenReturn(List.of(cloneCheckResult));
        antiFraudClonesManager.updateClonesResults();
        verify(antiFraudCloneCheckResultService, never()).save(anyList());
    }

    private static YPath tablePath(String generationName) {
        return YPath.simple(CLONES_TABLES_PATH + "/" + generationName);
    }

    private static AntiFraudCloneCheckResult createCloneCheckResult() {
        var cloneCheckResult = new AntiFraudCloneCheckResult();
        cloneCheckResult.setCompositeId(SHOP_ID, CLONE_ID);
        cloneCheckResult.setCryptaId(CRYPTA_ID);
        return cloneCheckResult;
    }
}
