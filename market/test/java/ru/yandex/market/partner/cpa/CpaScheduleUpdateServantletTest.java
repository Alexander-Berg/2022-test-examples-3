package ru.yandex.market.partner.cpa;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CpaScheduleUpdateServantletTest extends FunctionalTest {

    private static final Set<String> IGNORE_RESPONSE_ATTRIBUTES = ImmutableSet.of("host", "executing-time");

    @Autowired
    private CheckouterAPI checkouterClient;

    @Test
    @DbUnitDataSet(before = "cpaScheduleUpdateServantletTest.before.csv",
            after = "testUpdateSchedule.after.csv")
    void testUpdateSchedule() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/cpaScheduleUpdate?id={campaignId}&schCount=2&schStartDay0=MONDAY&schEndDay0=FRIDAY\n" +
                        "&schStartTime0=10:00&schEndTime0=19:00&schStartDay1=SATURDAY&schEndDay1=SUNDAY\n" +
                        "&schStartTime1=11:30&schEndTime1=17:00", 10661L);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        System.out.println(response.getBody());
        MbiAsserts.assertXmlEquals(
                "<data servant=\"market-payment\" version=\"0\" actions=\"[cpaScheduleUpdate]\"><result>OK</result></data>",
                response.getBody(),
                IGNORE_RESPONSE_ATTRIBUTES
        );

    }

    @Test
    @DbUnitDataSet(before = "cpaSupplierScheduleUpdateServantletTest.before.csv",
            after = "testUpdateSupplierSchedule.after.csv")
    void testUpdateSupplierSchedule() {
        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/cpaScheduleUpdate?id={campaignId}&schCount=2&schStartDay0=MONDAY&schEndDay0=FRIDAY\n" +
                        "&schStartTime0=10:00&schEndTime0=19:00&schStartDay1=SATURDAY&schEndDay1=SUNDAY\n" +
                        "&schStartTime1=11:30&schEndTime1=17:00&timezone={timezoneName}", 10999L, "Asia/Aden");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        System.out.println(response.getBody());
        MbiAsserts.assertXmlEquals(
                "<data servant=\"market-payment\" version=\"0\" actions=\"[cpaScheduleUpdate]\"><result>OK</result></data>",
                response.getBody(),
                IGNORE_RESPONSE_ATTRIBUTES
        );

    }

    @Test
    @DbUnitDataSet(before = "cpaScheduleUpdateServantletTest.before.csv")
    void testErrorOnBadSchedule() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/cpaScheduleUpdate?id={campaignId}&schCount=2&schStartDay0=MONDAY&schEndDay0=FRIDAY\n" +
                        "&schStartTime0=10:00&schEndTime0=12:00&schStartDay1=SATURDAY&schEndDay1=SUNDAY\n" +
                        "&schStartTime1=11:30&schEndTime1=17:00", 10661L);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        MbiAsserts.assertXmlEquals(
                "<data servant=\"market-payment\" version=\"0\" actions=\"[cpaScheduleUpdate]\" ><errors><simple-error-info><message-code>insufficient-working-hours</message-code></simple-error-info></errors></data>",
                response.getBody(),
                IGNORE_RESPONSE_ATTRIBUTES
        );
    }
}
