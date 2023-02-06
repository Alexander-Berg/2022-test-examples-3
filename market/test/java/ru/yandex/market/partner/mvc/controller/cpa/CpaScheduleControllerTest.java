package ru.yandex.market.partner.mvc.controller.cpa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CpaScheduleControllerTest extends FunctionalTest {
    @Autowired
    private CheckouterAPI checkouterClient;

    @Test
    @DbUnitDataSet(before = "cpa_schedule_update.before.csv", after = "cpa_schedule_update.after.csv")
    void testUpdateSchedule() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/schedule/cpa?id={campaignId}&sch_count=2&start_day0=MONDAY&end_day0=FRIDAY\n" +
                        "&start_time0=10:00&end_time0=19:00&start_day1=SATURDAY&end_day1=SUNDAY\n" +
                        "&start_time1=11:30&end_time1=17:00", 10661L);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    @DbUnitDataSet(before = "cpa_supplier_schedule_update.before.csv", after = "cpa_supplier_schedule_update.after.csv")
    void testUpdateSupplierSchedule() {
        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/schedule/cpa?id={campaignId}&sch_count=2&start_day0=MONDAY&end_day0=FRIDAY\n" +
                        "&start_time0=10:00&end_time0=19:00&start_day1=SATURDAY&end_day1=SUNDAY\n" +
                        "&start_time1=11:30&end_time1=17:00&timezone={timezoneName}", 10999L, "Asia/Aden");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    @DbUnitDataSet(before = "cpa_schedule_update.before.csv")
    void testForbiddenSchedule() {
        Exception ex = assertThrows(Exception.class, () -> FunctionalTestHelper.get(
                baseUrl + "/schedule/cpa?id={campaignId}&sch_count=2&start_day0=MONDAY&end_day0=FRIDAY\n" +
                        "&start_time0=10:00&end_time0=12:00&start_day1=SATURDAY&end_day1=SUNDAY\n" +
                        "&start_time1=11:30&end_time1=17:00", 10661L));
        Assertions.assertEquals("400 Bad Request", ex.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "cpa_schedule_update.before.csv")
    void testBadRequestSchedule() {
        Exception ex = assertThrows(Exception.class, () -> FunctionalTestHelper.get(
                baseUrl + "/schedule/cpa?id={campaignId}&sch_count=1&start_day1=SATURDAY&end_day1=SUNDAY\n" +
                        "&start_time1=11:30&end_time1=17:00", 10661L));
        Assertions.assertEquals("400 Bad Request", ex.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "cpa_schedule_update.before.csv", after = "cpa_schedule_update_24hours.after.csv")
    void testUpdateSchedule24hours() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/schedule/cpa?id={campaignId}&sch_count=1&start_day0=MONDAY&end_day0=FRIDAY\n" +
                        "&start_time0=00:00&end_time0=24:00", 10661L);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
