package ru.yandex.market.vendors.analytics.platform.controller.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;

/**
 * @author fbokovikov
 */
@DbUnitDataSet(before = "ShopDashboardControllerTest.csv")
public class ShopDashboardControllerTest extends FunctionalTest {

    private static final long UID = 1L;

    @Test
    @DisplayName("Создание дашбордов, в случае, если их не было у пользователя")
    void createDashboardIfNotExists() {
        var actualResponse = getDashboards();
        var expectedResponse = loadFromFile("ShopDashboardControllerTest.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(expectedResponse, actualResponse);
    }

    private String getDashboards() {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards")
                .queryParam("userId", UID)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}
