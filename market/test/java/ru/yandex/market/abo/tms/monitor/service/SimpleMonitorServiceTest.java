package ru.yandex.market.abo.tms.monitor.service;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.monitor.SimpleMonitorService;
import ru.yandex.market.abo.core.monitor.model.MonitorQuery;
import ru.yandex.market.abo.core.monitor.model.MonitorResult;
import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author kukabara
 */
public class SimpleMonitorServiceTest extends EmptyTest {
    @Autowired
    SimpleMonitorService simpleMonitorService;

    @Test
    public void testRunMonitors() throws Exception {
        // Не прогоняем запросы в схемы tms, arbitrage_tms, arbitrage, тк в embedded базе эти таблицы не создаются
        List<MonitorQuery> monitorQueries = simpleMonitorService.getMonitorQueries().stream()
                .filter(m -> !m.getQuery().contains("tms."))
                .filter(m -> !m.getQuery().contains("arbitrage."))
                .collect(Collectors.toList());
        assertFalse(monitorQueries.isEmpty());
        simpleMonitorService.runQueriesAndSaveResult(monitorQueries);

        List<MonitorResult> monitorResult = simpleMonitorService.getMonitorResult();
        assertFalse(monitorResult.isEmpty());
    }
}
