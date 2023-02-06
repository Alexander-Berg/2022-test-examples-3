package ru.yandex.market.partner.mvc.controller.agency;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.agency.AgencyRewardXlsxReportTestUtil;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link AgencyRewardController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class AgencyRewardControllerTest extends FunctionalTest {

    private static final long UID_WITH_DATA = 1003L;
    private static final long UID_WITHOUT_DATA = 1004L;

    private static final Instant QUARTER_LAST_DAY_INSTANT = DateTimes.toInstant(2018, 6, 2, 12, 0, 0);
    private static final Instant NEXT_QUARTER_FIRST_DAY_INSTANT = DateTimes.toInstant(2018, 6, 3, 12, 0, 0);
    private static final Instant QUARTER_2019_4 = DateTimes.toInstant(2020, 1, 1, 0, 0, 0);

    @Autowired
    @Qualifier("agencyRewardCurrentInstant")
    private Supplier<Instant> agencyRewardCurrentInstant;


    @Test
    @DisplayName("Текущий квартал ищется по бину-сапплайеру. Конец квартала")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.current_quarter.with_total.csv"
    })
    void testCurrentQuarterProcessedWithSupplierLastDay() {
        when(agencyRewardCurrentInstant.get()).thenReturn(QUARTER_LAST_DAY_INSTANT);
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, null, null));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.current_quarter.last_day.json");
    }


    @Test
    @DisplayName("Текущий квартал ищется по бину-сапплайеру. Конец квартала")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.rangeTest.csv"
    })
    void rangeTest() {
        when(agencyRewardCurrentInstant.get()).thenReturn(QUARTER_LAST_DAY_INSTANT);
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, null, null));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.rangeTest.json");
    }

    @Test
    @DisplayName("Текущий квартал ищется по бину-сапплайеру. Начало квартала")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.current_quarter.with_total.csv"
    })
    void testCurrentQuarterProcessedWithSupplierFirstDay() {
        when(agencyRewardCurrentInstant.get()).thenReturn(NEXT_QUARTER_FIRST_DAY_INSTANT);
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, null, null));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.current_quarter.first_day" +
                ".json");
    }

    @Test
    @DisplayName("Для квартала не производился расчет. Квартал не найден. Текущий квартал")
    @DbUnitDataSet(before = "csv/AgencyRewardController.agency.before.csv")
    void testCurrentQuarterNotFound() {
        when(agencyRewardCurrentInstant.get()).thenReturn(QUARTER_2019_4);
        checkErrors(1003L, null, null, "json/AgencyRewardController.current_quarter.not_found.json");
    }

    @Test
    @DisplayName("Для квартала не производился расчет. Квартал не найден. Выбранный квартал")
    @DbUnitDataSet(before = "csv/AgencyRewardController.agency.before.csv")
    void testQuarterNotFound() {
        checkErrors(1003L, 2018, 2, "json/AgencyRewardController.quarter.not_found.json");
    }

    @Test
    @DisplayName("Кривой квартал")
    @DbUnitDataSet(before = "csv/AgencyRewardController.agency.before.csv")
    void testInvalidQuarter() {
        checkErrors(1003L, 2018, 22, "json/AgencyRewardController.quarter.invalid.json");
    }

    @Test
    @DisplayName("Кривой год")
    @DbUnitDataSet(before = "csv/AgencyRewardController.agency.before.csv")
    void testInvalidYear() {
        checkErrors(1003L, -3, 2, "json/AgencyRewardController.year.invalid.json");
    }

    @Test
    @DisplayName("Квартал есть. Но нет данных в нем для запрашиваемого агентства")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.reward.without_total.before.csv"
    })
    void testEmptyQuarter() {
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getRewardUrl(UID_WITHOUT_DATA, 2018, 1)));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(), "json/AgencyRewardController.reward" +
                ".not_found.json");
    }

    @Test
    @DisplayName("Получение данных для агентства. Нет итогового коэффициента")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.reward.without_total.before.csv"
    })
    void testGetRewardWithoutTotal() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, 2018, 1));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.reward.without_total.json");
    }

    @Test
    @DisplayName("Получение данных для агентства. Есть итоговый коэффициент. Первый конфиг")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.reward.with_total.before.csv"
    })
    void testGetRewardWithTotal() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, 2018, 1));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.reward.with_total.json");
    }

    @Test
    @DisplayName("Получение данных для агентства. Есть итоговый коэффициент. Второй конфиг")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.reward.with_total2.before.csv"
    })
    void testGetRewardWithTotal2() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, 2019, 2));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.reward.with_total2.json");
    }

    @Test
    @DisplayName("Получение данных для агентства. Есть итоговый коэффициент. Второй конфиг")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.reward.with_total3.before.csv"
    })
    void testGetRewardWithTotal3() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getRewardUrl(UID_WITH_DATA, 2020, 1));
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/AgencyRewardController.reward.with_total3.json");
    }

    @Test
    @DisplayName("Получение xlsx-отчета для агентства")
    @DbUnitDataSet(before = {
            "csv/AgencyRewardController.agency.before.csv",
            "csv/AgencyRewardController.reward.without_total.before.csv"
    })
    void testGetReport() throws Exception {
        final ResponseEntity<byte[]> result = FunctionalTestHelper.exchange(getRewardReportUrl(UID_WITH_DATA, 2018,
                1), null, HttpMethod.GET, byte[].class);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        final HttpHeaders headers = result.getHeaders();
        Assertions.assertEquals("reward-report-2018-Q1.xlsx", headers.getContentDisposition().getFilename());

        final byte[] body = result.getBody();
        Assertions.assertNotNull(body);

        final ByteArrayInputStream is = new ByteArrayInputStream(body);
        AgencyRewardXlsxReportTestUtil.checkXlsx(is, "report.csv", getClass());
    }

    private String getRewardUrl(final long uid, final Integer year, final Integer quarter) {
        return getUrl("/agency/reward", uid, year, quarter);
    }

    private String getRewardReportUrl(final long uid, final Integer year, final Integer quarter) {
        return getUrl("/agency/reward/report", uid, year, quarter);
    }

    private String getUrl(final String url, final long uid, final Integer year, final Integer quarter) {
        return baseUrl + url + "?_user_id=" + uid
                + (year != null ? "&year=" + year : "")
                + (quarter != null ? "&quarter=" + quarter : "")
                + "&new_arp=true";
    }

    private void checkErrors(final long uid, final Integer year, final Integer quarter, final String expectedError) {
        checkError(getRewardUrl(uid, year, quarter), expectedError);
        checkError(getRewardReportUrl(uid, year, quarter), expectedError);
    }

    private void checkError(final String url, final String expectedError) {
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url));
        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(), expectedError);
    }
}
