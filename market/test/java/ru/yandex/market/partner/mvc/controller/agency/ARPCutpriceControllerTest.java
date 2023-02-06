package ru.yandex.market.partner.mvc.controller.agency;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.agency.AgencyRewardXlsxReportTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты на {@link ARPCutpriceController}
 */
@DbUnitDataSet(before = "csv/ARPCutpriceData_agency_users.csv")
class ARPCutpriceControllerTest extends FunctionalTest {

    private static final int UID_NO_DATA = 1003;
    private static final long AGENCY_WITHOUT_CUTPRICE_OFFERS = 1005L;

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.csv")
    @DisplayName("Получение информации о программе для агентства, у которого есть магазины, размещающие уцененные " +
            "товары")
    void testGetProgramInfoForAgencyWithDatasourceOnlyWithCutpriceOffers() {
        ResponseEntity<String> response = getProgramInfo(1003L);
        JsonTestUtil.assertEquals(response, this.getClass(), "json/ARPCutprice.agency_without_program_ds.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.csv")
    @DisplayName("Получение информации о программе для агентства, у которого есть магазины, подподающие под условия " +
            "программы")
    void testGetProgramInfoForAgencyWithDatasourcesUnderProgram() {
        ResponseEntity<String> response = getProgramInfo(1004L);
        JsonTestUtil.assertEquals(response, this.getClass(), "json/ARPCutprice.agency_with_ds_under_program.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.csv")
    @DisplayName("Получение информации о программе для агентства, у которого нет магазинов, размещающих уцененные " +
            "товары")
    void testGetProgramInfoForAgencyWithoutDatasourcesWithCutpriceOffers() {
        ResponseEntity<String> response = getProgramInfo(AGENCY_WITHOUT_CUTPRICE_OFFERS);
        JsonTestUtil.assertEquals(response, this.getClass(), "json/ARPCutprice.agency_without_cutprice_ds.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutprice_no_data.csv")
    @DisplayName("Проверка вырожденного случая, когда данные отсутствуют")
    void testGetProgramInfoWhenNoDataAvailable() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> getProgramInfo(UID_NO_DATA));
        assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    private ResponseEntity<String> getProgramInfo(long uid) {
        return FunctionalTestHelper.get(baseUrl + "/agency/rewardPrograms/cutprice?_user_id={uid}", uid);
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutprice_no_data.csv")
    @DisplayName("Получение отчета: проверка вырожденного случая, когда данные отсутствуют")
    void testGetReportWhenNoDataAvailable() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> getReport(UID_NO_DATA));
        assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.csv")
    @DisplayName("Получение отчета: у агентства нет магазинов c уценкой, отчет должен быть пустым")
    void testGetReportNoDataForAgency() throws IOException {
        assertThatReportEquals(AGENCY_WITHOUT_CUTPRICE_OFFERS, "arp_cutprice_no_cutprice_offers.csv");
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.report.one_month_data.csv")
    @DisplayName("Получение отчета: у агентства нет магазинов c уценкой за прошлый месяц, отчет без секции за прошлый" +
            " " +
            "месяц")
    void testGetReportForAgencyWithoutDataForPreviousMonth() throws IOException {
        assertThatReportEquals(1003L, "arp_cutprice_one_month_report.csv");
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.report.three_month_data.csv")
    @DisplayName("Получение отчета: у агентства есть магазины c уценкой за текущий и прошлый месяц, отчет с двумя " +
            "секциями")
    void testGetReportForAgencyWithThreeMonthData() throws IOException {
        assertThatReportEquals(1003L, "arp_cutprice_three_month_report.csv");
    }

    @Test
    @DbUnitDataSet(before = "csv/ARPCutpriceData.csv")
    @DisplayName("Получение отчета: проверка имени файла")
    void testCheckReportFilename() {
        var responseEntity = getReport(AGENCY_WITHOUT_CUTPRICE_OFFERS);

        ContentDisposition contentDispositionHeader = responseEntity.getHeaders().getContentDisposition();
        assertThat(contentDispositionHeader.getFilename(), is("cutprice-reward-report-2019-11-20.xlsx"));
    }

    private void assertThatReportEquals(long uid, String csvFile) throws IOException {
        ResponseEntity<byte[]> response = getReport(uid);

        final byte[] body = response.getBody();
        assertThat(body, is(notNullValue()));

        final ByteArrayInputStream is = new ByteArrayInputStream(body);
        AgencyRewardXlsxReportTestUtil.checkXlsx(is, csvFile, getClass());
    }

    private ResponseEntity<byte[]> getReport(long uid) {
        return FunctionalTestHelper.exchange(baseUrl + "/agency/rewardPrograms/cutprice/report?_user_id={uid}", null,
                HttpMethod.GET, byte[].class, uid);
    }
}
