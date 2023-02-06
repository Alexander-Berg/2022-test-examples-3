package ru.yandex.market.partner.mvc.controller.outlet;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link ShowOutletSummaryController}.
 *
 * @author serenitas
 */
public class ShowOutletSummaryControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Тест на получение агрегированных данных по результатам проверки аутлетов и их лицензий")
    @DbUnitDataSet(before = "ShowOutletSummaryControllerTest.before.csv")
    void testGetOutletSummary() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/showOutletSummary?id={campaignId}",
                1011L);
        Assert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyEquals(
                                "result", "[{" +
                                        "\"totalCount\":5," +
                                        "\"notModeratedCount\":2," +
                                        "\"inModerationCount\":0," +
                                        "\"goodCount\":2," +
                                        "\"badCount\":1," +
                                        "\"goodLicenseCount\":2," +
                                        "\"badLicenseCount\":1," +
                                        "\"licenseInModerationCount\":2" +
                                        "}]"
                        )
                )
        );
    }

    @Test
    @DisplayName("Тест на получение агрегированных данных по результатам проверки аутлетов, если у них нет лицензий")
    @DbUnitDataSet(before = "ShowOutletSummaryControllerTest.withoutLicense.before.csv")
    void testGetOutletSummaryWithoutLicenses() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/showOutletSummary?id={campaignId}",
                1011L);
        Assert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyEquals(
                                "result", "[{" +
                                        "\"totalCount\":5," +
                                        "\"notModeratedCount\":2," +
                                        "\"inModerationCount\":0," +
                                        "\"goodCount\":2," +
                                        "\"badCount\":1," +
                                        "\"goodLicenseCount\":0," +
                                        "\"badLicenseCount\":0," +
                                        "\"licenseInModerationCount\":0" +
                                        "}]"
                        )
                )
        );
    }

    @Test
    @DisplayName("Тест на получение агрегированных данных по результатам проверки аутлетов, если у магазина аутлетов " +
            "вовсе нет")
    @DbUnitDataSet(before = "ShowOutletSummaryControllerTest.before.csv")
    void testGetOutletSummaryWithoutOutlets() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/showOutletSummary?id={campaignId}",
                1012L);
        Assert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyEquals(
                                "result", "[{" +
                                        "\"totalCount\":0," +
                                        "\"notModeratedCount\":0," +
                                        "\"inModerationCount\":0," +
                                        "\"goodCount\":0," +
                                        "\"badCount\":0," +
                                        "\"goodLicenseCount\":0," +
                                        "\"badLicenseCount\":0," +
                                        "\"licenseInModerationCount\":0" +
                                        "}]"
                        )
                )
        );
    }
}
