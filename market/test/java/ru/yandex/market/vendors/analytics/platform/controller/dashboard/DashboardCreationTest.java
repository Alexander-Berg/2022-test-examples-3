package ru.yandex.market.vendors.analytics.platform.controller.dashboard;

import java.time.LocalDate;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonTodayDateMatcher;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils.assertJsonWithDatesEquals;

/**
 * Functional tests for {@link DashboardController} creation.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "createDashboard.before.csv")
@ClickhouseDbUnitDataSet(before = "createDashboard.ch.before.csv")
public class DashboardCreationTest extends FunctionalTest {

    @Test
    @DisplayName("Создание категорийного дашборда")
    @DbUnitDataSet(after = "categoryDashboardCreation.after.csv")
    void categoryDashboardCreation() {
        String expected = loadFromFile("CategoryDashboardCreation.response.json");
        String body = "{\n"
                + "  \"category\": 31,\n"
                + "  \"name\": \"Продажи\",\n"
                + "  \"type\": \"CATEGORY\",\n"
                + "  \"uid\": 130\n"
                + "}";
        String actual = createDashboard(body);
        assertJsonWithDatesEquals(expected, actual);
    }

    @Test
    @DisplayName("Создание модельного дашборда")
    @DbUnitDataSet(after = "ModelDashboardCreation.after.csv")
    void modelDashboardCreation() {
        String expected = loadFromFile("ModelDashboardCreation.response.json");
        var today = LocalDate.now();
        var startDate = today.minusMonths(3).withDayOfMonth(1);
        String body = "{\n"
                + "  \"category\": 31,\n"
                + "  \"name\": \"Модели\",\n"
                + "  \"type\": \"MODEL\",\n"
                + "  \"model\": \"35\",\n"
                + "  \"uid\": 130\n"
                + "}";
        String actual = createDashboard(body);
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.withMatcher("today", new JsonTodayDateMatcher()));
    }

    @Test
    @DbUnitDataSet(after = "shopDashboardCreation.after.csv")
    @DisplayName("Создание категорийного дашборда для магазина")
    void shopDashboardCreation() {
        var requestBody = ""
                + "{\n"
                + "  \"category\": 91491,\n"
                + "  \"name\": \"Телефоны\",\n"
                + "  \"type\": \"CATEGORY\",\n"
                + "  \"uid\": 1000\n"
                + "}";
        String actual = createDashboard(requestBody);
        String expected = loadFromFile("ShopDashboardCreation.response.json");
        assertJsonWithDatesEquals(expected, actual);
    }

    private String createDashboard(String requestBody) {
        var dashboardsUrl = baseUrl() + "dashboards";
        return FunctionalTestHelper.postForJson(dashboardsUrl, requestBody);
    }
}
