package ru.yandex.market.vendors.analytics.platform.controller.widget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Functional tests for {@link WidgetMetaInfoController}.
 *
 * @author ogonek
 */
public class WidgetMetaInfoControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Интервал для драйверов роста с дневным скейлом")
    void growthDriversIntervalsDay() {
        var response = getGrowthDriversIntervals(TimeDetailing.DAY, "2019-02-05", "2019-06-17");
        var expected = "{\n"
                + "  \"basePeriod\": {\n"
                + "    \"startDate\": \"2018-09-25\",\n"
                + "    \"endDate\": \"2019-02-04\"\n"
                + "  },\n"
                + "  \"comparingPeriod\": {\n"
                + "    \"startDate\": \"2019-02-05\",\n"
                + "    \"endDate\": \"2019-06-17\"\n"
                + "  }\n"
                + "}";
        JsonTestUtil.assertEquals(
                expected,
                response
        );
    }

    @Test
    @DisplayName("Интервал для драйверов роста с квартальным скейлом")
    void growthDriversIntervalsQuarter() {
        var response = getGrowthDriversIntervals(TimeDetailing.QUARTER, "2019-02-05", "2019-06-17");
        var expected = "{\n"
                + "  \"basePeriod\": {\n"
                + "    \"startDate\": \"2018-07-01\",\n"
                + "    \"endDate\": \"2018-12-31\"\n"
                + "  },\n"
                + "  \"comparingPeriod\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-06-30\"\n"
                + "  }\n"
                + "}";
        JsonTestUtil.assertEquals(
                expected,
                response
        );
    }

    private String getWidgetsMetaInfoUrl() {
        return baseUrl() + "/widgets/info";
    }

    private String getGrowthDriversIntervals(TimeDetailing timeDetailing, String startDate, String endDate) {
        String url = getWidgetsMetaInfoUrl() + "/growthDrivers/intervals"
                + "?timeDetailing=" + timeDetailing
                + "&startDate=" + startDate
                + "&endDate=" + endDate;
        return FunctionalTestHelper.get(url).getBody();
    }

}
