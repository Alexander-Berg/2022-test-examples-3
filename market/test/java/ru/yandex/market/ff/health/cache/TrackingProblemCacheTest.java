package ru.yandex.market.ff.health.cache;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.service.MonitoringEventService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static ru.yandex.market.ff.enums.TrackingProblemMonitoring.WRONG_STATUS_CODE_ERROR;

class TrackingProblemCacheTest extends MvcIntegrationTest {
    @Autowired
    private MonitoringEventService monitoringEventService;
    @Autowired
    private TrackingProblemCache trackingProblemCache;

    @BeforeEach
    void invalidateCache() {
        trackingProblemCache.invalidateCache();
    }
    @Test
    @DatabaseSetup("classpath:empty.xml")
    void shouldNotDuplicateErrorEntries() {
        var firstError = monitoringEventService.addErrorIfNotExists(
                WRONG_STATUS_CODE_ERROR.value(), 1, "Error");
        var secondError = monitoringEventService.addErrorIfNotExists(
                WRONG_STATUS_CODE_ERROR.value(), 1, "Error");
        var thirdError = monitoringEventService.addErrorIfNotExists(
                WRONG_STATUS_CODE_ERROR.value(), 2, "Error");
        assertEquals(firstError, secondError);
        assertNotEquals(firstError, thirdError);
    }

    @Test
    @DatabaseSetup("classpath:empty.xml")
    void testCaching() {
        //load cache
        assertEquals("0;ok", trackingProblemCache.checkMonitoring(WRONG_STATUS_CODE_ERROR));
        monitoringEventService.addErrorIfNotExists(WRONG_STATUS_CODE_ERROR.value(), 1, "Error");
        //still cached value
        assertEquals("0;ok", trackingProblemCache.checkMonitoring(WRONG_STATUS_CODE_ERROR));
        invalidateCache();
        assertEquals("2;Wrong status code received for entityId: 1, error: Error",
                trackingProblemCache.checkMonitoring(WRONG_STATUS_CODE_ERROR));
    }

}
