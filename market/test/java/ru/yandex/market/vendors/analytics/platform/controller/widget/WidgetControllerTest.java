package ru.yandex.market.vendors.analytics.platform.controller.widget;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.dashboard.WidgetType;
import ru.yandex.market.vendors.analytics.core.model.partner.PartnerType;
import ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Functional tests for {@link WidgetController}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "WidgetControllerTest.before.csv")
public class WidgetControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получить дерево виджетов, доступных пользователю в категории")
    void getWidgets() {
        // частичный список виджетов
        var response = getWidgetsResponse(125, 999);
        var expected = loadFromFile("WidgetControllerTest.getAvailableWidgets.response.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(
                expected,
                response
        );
        // полный список виджетов
        response = getWidgetsResponse(123, 999);
        expected = loadFromFile("groups.json");
        JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder(
                expected,
                response
        );
    }

    @Test
    @DisplayName("Получение всех доступных категорий для создания виджетов")
    void getCategoriesForNewWidget() {
        var response = getWidgetCategoriesResponse(123, "");
        var expected = loadFromFile("groupsWithAccess.json");
        JsonUnitUtils.assertJsonWithDatesEquals(expected, response);
    }

    @Test
    @DisplayName("Получение дефолтного виджета")
    void getDefaultWidget() {
        var response = getDefaultWidget(WidgetType.CATEGORY_MARKET_SHARE, 12, 123);
        var expected = "{\n"
                + "  \"type\": \"CATEGORY_MARKET_SHARE\",\n"
                + "  \"name\": \"Категория\",\n"
                + "  \"params\": {\n"
                + "    \"interval\": {\n"
                + "      \"startDate\": \"${json-unit.matches:today}-3r\",\n"
                + "      \"endDate\": \"${json-unit.matches:today}\"\n"
                + "    },\n"
                + "    \"timeDetailing\": \"MONTH\",\n"
                + "    \"categoryFilter\": {\n"
                + "      \"hid\": 39,\n"
                + "      \"priceSegments\": null\n"
                + "    },\n"
                + "    \"priceSegmentBounds\":null,\n"
                + "    \"geoFilters\": null,\n"
                + "    \"socdemFilters\": null,\n"
                + "    \"brands\": null,\n"
                + "    \"modelIds\": null,\n"
                + "    \"reportFilters\": null,\n"
                + "    \"visualization\": \"LINE\",\n"
                + "    \"measure\": \"MONEY\",\n"
                + "    \"domains\": null\n"
                + "  }\n"
                + "}";
        JsonUnitUtils.assertJsonWithDatesEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(after = "CreateWidgetV1.after.csv")
    @DisplayName("Добавление виджета на дашборд (v1)")
    void createWidgetV1() {
        String body = "{\n"
                + "  \"name\": \"Имя\",\n"
                + "  \"calculationInfos\": [\n"
                + "    {\n"
                + "      \"type\": \"CATEGORY_MARKET_SHARE\",\n"
                + "      \"params\": {\n"
                + "        \"hid\": 91491\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        FunctionalTestHelper.postForJson(createWidgetV1(12), body);
    }

    @Test
    @DisplayName("Добавление виджета с дубликатным названием на дашборд (v1)")
    void createWidgetV1BadName() {
        String body = getWidgetParamsBody();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.postForJson(createWidgetV1(12), body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expectedResponse = "{\n"
                + "  \"code\": \"DUPLICATE_NAME\",\n"
                + "  \"message\": \"Widget with name Продажи в категории already exists\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expectedResponse, exception.getResponseBodyAsString());

    }

    @Test
    @DbUnitDataSet(after = "CreateCompareWidget.after.csv")
    @DisplayName("Добавление виджета-сравнения на дашборд (v1)")
    void createCompareWidgetV1() {
        String body = "{\n"
                + "  \"name\": \"Имя\",\n"
                + "  \"calculationInfos\": [\n"
                + "    {\n"
                + "      \"type\": \"CATEGORY_MARKET_SHARE\",\n"
                + "      \"params\": {\n"
                + "        \"hid\": 91491\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"type\": \"CATEGORY_AVERAGE_PRICE\",\n"
                + "      \"params\": {\n"
                + "        \"hid\": 91491\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        FunctionalTestHelper.postForJson(createWidgetV1(12), body);
    }

    @Test
    @DbUnitDataSet(after = "CreateDefaultWidgets.after.csv")
    @DisplayName("Добавления дефолтного виджета на все дашборды пользователя")
    void createDefaultWidgets() {
        FunctionalTestHelper.postForJson(createDefaultWidgets(123, WidgetType.BRANDS_GROWTH), null);
    }

    @Test
    @DbUnitDataSet(after = "CreateDefaultWidgetsForVendor.after.csv")
    @DisplayName("Добавления дефолтного Категорийного виджета на Категорийных дашбордах всех пользователей Вендора")
    void createDefaultWidgetsForPartnerType() {
        FunctionalTestHelper.postForJson(
                createDefaultWidgetsForPartnerType(PartnerType.VENDOR, WidgetType.BRANDS_GROWTH, 1),
                null
        );
    }

    @Test
    @DbUnitDataSet(after = "CreateDefaultExistingWidgetsForVendor.after.csv")
    @DisplayName("Добавления существующего Категорийного виджета на Категорийных дашбордах всех пользователей Вендора")
    void createDefaultExistingWidgetForPartnerType() {
        FunctionalTestHelper.postForJson(createDefaultWidgetsForPartnerType(
                PartnerType.VENDOR, WidgetType.CATEGORY_MARKET_SHARE, 1),
                null
        );
    }

    @Test
    @DisplayName("Создание виджета на несуществующием дашборде")
    void createWidgetOnNonExistingDashboard() {
        String body = getWidgetParamsBody();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.postForJson(createWidgetV1(123), body)
        );

        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        String expectedResponse = "" +
                "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"Entity DASHBOARD not found by id: 123\",\n" +
                "   \"entityId\":123,\n" +
                "   \"entityType\":\"DASHBOARD\"\n" +
                "}";

        JsonAssert.assertJsonEquals(expectedResponse, exception.getResponseBodyAsString());
    }

    @Test
    @DbUnitDataSet(after = "ChangeAllWidgetsPosition.after.csv")
    @DisplayName("Изменение позиций всех виджетов на дашборде")
    void changeAllWidgetsPosition() {
        FunctionalTestHelper.putForJson(getChangeAllPositionsUrl(12, List.of(100L, 14L, 10L)), null);
    }

    @Test
    @DbUnitDataSet(after = "ChangeWidgetParams.after.csv")
    @DisplayName("Изменение параметров виджета")
    void changeWidgetParams() {
        String body = "{\n"
                + "  \"name\": \"Измененное название\",\n"
                + "  \"params\": {\"category\": 11}\n"
                + "}";
        FunctionalTestHelper.putForJson(getWidgetUrl(10), body);
    }

    @Test
    @DbUnitDataSet(after = "ChangeWidgetParamsV1.after.csv")
    @DisplayName("Изменение параметров виджета")
    void changeWidgetParamsV1() {
        String body = getWidgetParamsBody();
        FunctionalTestHelper.putForJson(getWidgetUrlV1(10), body);
    }

    @Test
    @DbUnitDataSet(after = "ChangeCompareWidgetParams.after.csv")
    @DisplayName("Изменение параметров виджета")
    void changeCompareWidgetParams() {
        String body = "{\n"
                + "  \"name\": \"Изменённое название\",\n"
                + "  \"calculationInfos\": [\n"
                + "    {\n"
                + "      \"type\": \"CATEGORY_MARKET_SHARE\",\n"
                + "      \"params\": {\n"
                + "        \"hid\": 91491\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"type\": \"CATEGORY_AVERAGE_PRICE\",\n"
                + "      \"params\": {\n"
                + "        \"hid\": 91491\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        FunctionalTestHelper.putForJson(getWidgetUrlV1(100), body);
    }

    @Test
    @DbUnitDataSet(after = "DeleteWidget.after.csv")
    @DisplayName("Удаление виджета")
    void deleteWidget() {
        FunctionalTestHelper.delete(getWidgetUrl(14));
    }

    @Test
    @DbUnitDataSet(after = "DeleteCompareWidget.after.csv")
    @DisplayName("Удаление виджета-сравнения")
    void deleteCompareWidget() {
        FunctionalTestHelper.delete(getWidgetUrl(100));
    }

    @Test
    @DisplayName("Попытка удаления дефолтного виджета")
    void deleteDefaultWidget() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.delete(getWidgetUrl(11))
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expectedResponse = "{\n"
                + "  \"code\": \"BAD_REQUEST\",\n"
                + "  \"message\": \"Unable to delete a default widget\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expectedResponse, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Получение дефолтного виджета на несуществующием дашборде")
    void getBadDefaultWidget() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getDefaultWidget(WidgetType.CATEGORY_MARKET_SHARE, 123, 123)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
        String expectedResponse = "" +
                "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"Entity DASHBOARD not found by id: 123\",\n" +
                "   \"entityId\":123,\n" +
                "   \"entityType\":\"DASHBOARD\"\n" +
                "}";
        JsonAssert.assertJsonEquals(expectedResponse, exception.getResponseBodyAsString());
    }

    private String getWidgetsUrl() {
        return baseUrl() + "/widgets";
    }

    private String getWidgetsResponse(long userId, long hid) {
        return FunctionalTestHelper.get(getWidgetsUrl() + "/groups?userId=" + userId + "&hid=" + hid).getBody();
    }

    private String getWidgetCategoriesResponse(long userId, String partName) {
        return FunctionalTestHelper.get(
                getWidgetsUrl() + "/categories?userId=" + userId + "&partName=" + partName
        ).getBody();
    }

    private String createWidgetV1(long dashboardId) {
        return getWidgetsUrl() + "/v1" + "?dashboardId=" + dashboardId;
    }

    private String createDefaultWidgets(long userId, WidgetType widgetType) {
        return getWidgetsUrl() + "/default?userId=" + userId + "&widgetType=" + widgetType;
    }

    private String createDefaultWidgetsForPartnerType(
            PartnerType partnerType,
            WidgetType widgetType,
            @Nullable Integer position
    ) {
        return getWidgetsUrl() + "/default/partner?partnerType=" + partnerType
                + "&widgetType=" + widgetType + "&position=" + position;
    }


    private String getWidgetUrlV1(long widgetId) {
        return UriComponentsBuilder.fromUriString(getWidgetsUrl())
                .pathSegment("v1", "{widgetId}")
                .buildAndExpand(widgetId)
                .toUriString();
    }

    private String getWidgetUrl(long widgetId) {
        return UriComponentsBuilder.fromUriString(getWidgetsUrl())
                .path("/{widgetId}")
                .buildAndExpand(widgetId)
                .toUriString();
    }

    private String getChangeAllPositionsUrl(long dashboardId, List<Long> widgetIds) {
        return getWidgetsUrl() + "/position?dashboardId=" + dashboardId
                + "&widgetIds=" + widgetIds.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String getDefaultWidget(WidgetType widgetType, long dashboardId, long userId) {
        String url = getWidgetsUrl() + "/default"
                + "?widgetType=" + widgetType
                + "&dashboardId=" + dashboardId
                + "&userId=" + userId;
        return FunctionalTestHelper.get(url).getBody();
    }

    private String getWidgetParamsBody() {
        return "{\n"
                + "  \"name\": \"Продажи в категории\",\n"
                + "  \"calculationInfos\": [\n"
                + "    {\n"
                + "      \"type\": \"CATEGORY_MARKET_SHARE\",\n"
                + "      \"params\": {\n"
                + "        \"hid\": 91491\n"
                + "      }\n"
                + "    }\n"
                + "  ]\n"
                + "}";
    }

}
