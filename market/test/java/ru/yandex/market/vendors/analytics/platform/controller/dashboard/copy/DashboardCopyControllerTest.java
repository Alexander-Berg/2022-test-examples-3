package ru.yandex.market.vendors.analytics.platform.controller.dashboard.copy;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Functional tests for {@link DashboardCopyController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "DashboardCopyControllerTest.before.csv")
public class DashboardCopyControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Копируемый дашборд не найден")
    void dashboardNotFound() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> copy(100L, 11071991L)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
        var expected = ""
                + "{\n"
                + "   \"code\":\"ENTITY_NOT_FOUND\",\n"
                + "   \"message\":\"Entity DASHBOARD not found by id: 11071991\",\n"
                + "   \"entityId\":11071991,\n"
                + "   \"entityType\": \"DASHBOARD\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Успешное копирование дашборда")
    @DbUnitDataSet(after = "DashboardCopyControllerTest.copyDashboard.after.csv")
    void copyDashboard() {
        String actualResponse = copy(100L, 1000L);
        var expectedResponse = loadFromFile("DashboardCopyControllerTest.copyDashboard.response.json");
        JsonAssert.assertJsonEquals(expectedResponse, actualResponse);
    }

    private String copyUrl(long userId, long dashboardId) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/{dashboardId}/copy")
                .queryParam("userId", userId)
                .buildAndExpand(dashboardId)
                .toUriString();
    }

    private String copy(long userId, long dashboardId) {
        var copyUrl = copyUrl(userId, dashboardId);
        return FunctionalTestHelper.postForJson(copyUrl, null);
    }
}
