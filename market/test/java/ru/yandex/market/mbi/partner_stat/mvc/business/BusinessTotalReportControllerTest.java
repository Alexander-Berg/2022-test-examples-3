package ru.yandex.market.mbi.partner_stat.mvc.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.config.ClickHouseTestConfig;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;

/**
 * Тесты для {@link BusinessTotalReportController}
 */
@DbUnitDataSet(
        dataSource = ClickHouseTestConfig.DATA_SOURCE,
        type = DataSetType.SINGLE_CSV,
        before = "stat.before.csv"
)
class BusinessTotalReportControllerTest extends ClickhouseFunctionalTest {
    private static final long BUSINESS_ID = 1;

    @DisplayName("Проверка получения таблицы тотального отчета понедельно")
    @Test
    void testGetTotalReportByWeeks() {
        String body = "" +
                "{" +
                "    \"filter\" : {\n" +
                "    \"detalization\" : \"WEEK\",\n" +
                "    \"dateTo\" : \"2022-02-08\"\n" +
                "    }\n" +
                "}";

        String url = getUrl(BUSINESS_ID);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(body,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "total/byWeeks.response.json");
    }

    @DisplayName("Проверка получения таблицы тотального отчета понедельно")
    @Test
    void testGetTotalReportByWeeksToday() {
        String body = "" +
                "{" +
                "    \"filter\" : {\n" +
                "    \"detalization\" : \"WEEK\"\n" +
                "    }\n" +
                "}";

        String url = getUrl(BUSINESS_ID);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(body,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "total/byWeeksToday.response.json");
    }

    @DisplayName("Проверка получения таблицы тотального отчета помесячно")
    @Test
    void testGetTotalReportByMonths() {
        String body = "" +
                "{" +
                "    \"filter\" : {\n" +
                "    \"partnerId\" : 774,\n" +
                "    \"detalization\" : \"MONTH\",\n" +
                "    \"dateTo\" : \"2022-01-30\"\n" +
                "    }\n" +
                "}";

        String url = getUrl(BUSINESS_ID);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(body,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "total/byMonths.response.json");
    }

    @DisplayName("Проверка получения пустого тотального отчета")
    @Test
    void testGetEmptyTotalReport() {
        String body = "" +
                "{" +
                "    \"filter\" : {\n" +
                "    \"partnerId\" : 774,\n" +
                "    \"detalization\" : \"MONTH\",\n" +
                "    \"dateTo\" : \"2000-07-01\"\n" +
                "    }\n" +
                "}";

        String url = getUrl(BUSINESS_ID);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(body,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "total/empty.response.json");
    }

    @DisplayName("Проверка получения таблицы тотального отчета для несуществующего бизнеса")
    @Test
    void testGetTotalReportBusinessNotFound() {
        String body = "" +
                "{" +
                "    \"filter\" : {\n" +
                "    \"detalization\" : \"MONTH\"\n" +
                "    }\n" +
                "}";

        String url = getUrl(3);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(body,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "total/businessNotFound.response.json");
    }

    @DisplayName("Проверка получения таблицы тотального отчета для несуществующего партнера")
    @Test
    void testGetTotalReportPartnerNotFound() {
        String body = "" +
                "{" +
                "    \"filter\" : {\n" +
                "    \"partnerId\" : 999,\n" +
                "    \"detalization\" : \"MONTH\"\n" +
                "    }\n" +
                "}";

        String url = getUrl(BUSINESS_ID);
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(body,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "total/partnerNotFound.response.json");
    }

    private String getUrl(long businessId) {
        return baseUrl() + "/businesses/" + businessId + "/total/report";
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
