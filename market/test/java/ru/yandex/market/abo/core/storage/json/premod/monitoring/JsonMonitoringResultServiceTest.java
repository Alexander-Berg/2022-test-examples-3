package ru.yandex.market.abo.core.storage.json.premod.monitoring;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.quality_monitoring.startrek.model.MonitoringType;
import ru.yandex.market.abo.core.quality_monitoring.yt.idx.model.MonitoringValue;
import ru.yandex.market.abo.core.storage.json.model.JsonEntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 21.10.2019
 */
class JsonMonitoringResultServiceTest extends EmptyTest {
    private static final long TICKET_ID = 0;

    @Autowired
    private JsonMonitoringResultService jsonMonitoringResultService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void serializationTest() {
        var monitoringValue = Map.of(
                MonitoringType.ANTI_FAKE,
                new MonitoringValue(1, 1, 1, List.of(1L), List.of(
                        new MonitoringValue.MonitoringOffer(123L, 1535216L, "dSEGewgveSDGVAS", null, null, null))
                ),
                MonitoringType.UNIQUE, new MonitoringValue(2, 1000, List.of(1L, 2L, 3L))
        );
        jsonMonitoringResultService.saveIfNeeded(TICKET_ID, JsonEntityType.PREMOD_MONITORING_RESULT, monitoringValue);
        entityManager.flush();
        entityManager.clear();
        assertEquals(monitoringValue, jsonMonitoringResultService.load(TICKET_ID, JsonEntityType.PREMOD_MONITORING_RESULT));
    }

    @Test
    void emptyMapTest() {
        var monitoringValue = Collections.<MonitoringType, MonitoringValue>emptyMap();
        jsonMonitoringResultService.saveIfNeeded(TICKET_ID, JsonEntityType.PREMOD_MONITORING_RESULT, monitoringValue);
        entityManager.flush();
        entityManager.clear();
        assertTrue(jsonMonitoringResultService.load(TICKET_ID, JsonEntityType.PREMOD_MONITORING_RESULT).isEmpty());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM json_storage", Long.class));
    }
}
