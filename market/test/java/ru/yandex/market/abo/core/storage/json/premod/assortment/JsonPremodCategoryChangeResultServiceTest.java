package ru.yandex.market.abo.core.storage.json.premod.assortment;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.CategoryToMonitor;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.CategoryToMonitorValue;
import ru.yandex.market.abo.core.quality_monitoring.startrek.newcategories.MonitoringScope;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.03.2020
 */
class JsonPremodCategoryChangeResultServiceTest extends EmptyTest {

    private static final long CATEGORY = 1L;
    private static final String CATEGORY_NAME = "foo";
    private static final long THRESHOLD = 10;
    private static final CategoryToMonitor CATEGORY_TO_MONITOR = new CategoryToMonitor(
            CATEGORY, CATEGORY_NAME, THRESHOLD, MonitoringScope.CATEGORY_CHANGE
    );
    private static final long OFFERS_COUNT = 15;

    private static final long TICKET_ID = 123L;

    @Autowired
    private JsonPremodCategoryChangeResultService jsonPremodCategoryChangeResultService;
    @Autowired
    private JsonPremodCategoryChangeResultRepo jsonPremodCategoryChangeResultRepo;

    @Test
    void saveAntiFraudRulesTest() {
        var categoriesToMonitorValues = List.of(new CategoryToMonitorValue(CATEGORY_TO_MONITOR, OFFERS_COUNT));
        jsonPremodCategoryChangeResultService.saveIfNotEmpty(TICKET_ID, categoriesToMonitorValues);
        flushAndClear();
        assertEquals(categoriesToMonitorValues, jsonPremodCategoryChangeResultService.loadCategoryChangeValues(TICKET_ID));
    }

    @Test
    void saveEmptyAntiFraudRulesTest() {
        jsonPremodCategoryChangeResultService.saveIfNotEmpty(TICKET_ID, Collections.emptyList());
        flushAndClear();
        assertEquals(Collections.emptyList(), jsonPremodCategoryChangeResultService.loadCategoryChangeValues(TICKET_ID));
        assertEquals(0, jsonPremodCategoryChangeResultRepo.count());
    }
}
