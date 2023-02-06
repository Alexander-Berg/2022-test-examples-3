package ru.yandex.market.abo.core.quality_monitoring.startrek.counterfeit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.quality_monitoring.startrek.counterfeit.history.MonitoringCounterfeitHistory;
import ru.yandex.market.abo.core.quality_monitoring.startrek.counterfeit.history.MonitoringCounterfeitHistoryService;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringValue;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author zilzilok
 * @date 03.06.2021
 */
class MonitoringCounterfeitsServiceTest {

    private static final long SHOP_ID = 1L;
    private static final Long EXIST_RULE_ID = 3L;
    private static final Long NEW_RULE_ID = 5L;
    private List<MonitoringCounterfeitHistory> history;
    @InjectMocks
    private MonitoringCounterfeitsService monitoringCounterfeitsService;
    @Mock
    private MonitoringCounterfeitHistoryService monitoringCounterfeitHistoryService;

    private static List<MonitoringValue> buildMonitoringValues() {
        return Stream.of(EXIST_RULE_ID, NEW_RULE_ID)
                .map(ruleId -> new MonitoringValue(SHOP_ID, ruleId, ruleId)) // для удобства modelsCount == ruleId
                .collect(Collectors.toList());
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        history = new ArrayList<>() {{
            add(new MonitoringCounterfeitHistory(SHOP_ID, EXIST_RULE_ID));
        }};
        doAnswer(invocation -> {
            history.remove(0);
            return null;
        }).when(monitoringCounterfeitHistoryService).deleteAllByShopId(SHOP_ID);
        when(monitoringCounterfeitHistoryService.findAllByShopId(SHOP_ID)).thenReturn(history);
    }

    @Test
    void filterPremodShopMonitoringValues() {
        // Success premod
        monitoringCounterfeitHistoryService.deleteAllByShopId(SHOP_ID);

        List<MonitoringValue> filtered = monitoringCounterfeitsService.filterMonitoringValues(buildMonitoringValues());
        assertTrue(filtered.stream().anyMatch(v -> v.getModelsCount() == NEW_RULE_ID + EXIST_RULE_ID));
    }

    @Test
    void filterNotPremodShopMonitoringValues() {
        List<MonitoringValue> filtered = monitoringCounterfeitsService.filterMonitoringValues(buildMonitoringValues());
        assertTrue(filtered.stream().anyMatch(v -> v.getModelsCount() == NEW_RULE_ID));
    }
}
