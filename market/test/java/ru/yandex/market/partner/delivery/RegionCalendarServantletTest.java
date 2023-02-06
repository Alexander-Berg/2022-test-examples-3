package ru.yandex.market.partner.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты для {@link RegionCalendarServantlet}
 */
class RegionCalendarServantletTest extends FunctionalTest {

    @Test
    @DisplayName("Дни в ответе должны быть отсортированы")
    @DbUnitDataSet(before = "RegionCalendarServantletTest.testSortedDays.before.csv")
    void testSortedDays() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/regionCalendar?" +
                        "region_id={region_id}&begin_date={begin_date}&end_date={end_date}&day_types={day_types}&format=json",
                225, "2021-05-01", "2021-05-20", "calendar_holidays");
        JsonTestUtil.assertEquals(response, this.getClass(), "RegionCalendarServantletTest.testSortedDays.response.json");
    }
}
