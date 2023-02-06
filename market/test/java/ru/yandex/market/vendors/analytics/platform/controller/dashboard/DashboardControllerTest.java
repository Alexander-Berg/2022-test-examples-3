package ru.yandex.market.vendors.analytics.platform.controller.dashboard;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.exception.badrequest.LockTimeoutException;
import ru.yandex.market.vendors.analytics.core.exception.badrequest.notfound.AnalyticsEntity;
import ru.yandex.market.vendors.analytics.core.model.common.language.Language;
import ru.yandex.market.vendors.analytics.core.service.dashboard.UserDashboardsLockService;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.mockito.Mockito.doThrow;

/**
 * Functional tests for {@link DashboardController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "UserDashboardControllerTest.before.csv")
@ClickhouseDbUnitDataSet(before = "UserDashboardControllerTestCh.before.csv")
class DashboardControllerTest extends FunctionalTest {

    private static final long UID = 123L;

    @SpyBean
    private UserDashboardsLockService userDashboardsLockService;

    @Test
    @DisplayName("Получение всех своих дашбордов")
    void getAllDashboards() {
        String response = getAllDashboards(UID);
        String expected = """
                {
                  "dashboards": [
                    {
                      "id": 13,
                      "name": "Продажи по брендам",
                      "type":"CATEGORY",
                      "params": {}
                    },
                    {
                      "id": 11,
                      "name": "Суммарные продажи",
                      "type":"CATEGORY",
                      "params": {}
                    },
                    {
                      "id": 12,
                      "name": "Суммарные продажи",
                      "type":"CATEGORY",
                      "params": {}
                    }
                  ],
                  "pagingInfo": {
                    "pageNumber": 0,
                    "pageSize": 50,
                    "totalPages": 1,
                    "totalElements": 3
                  }
                }""";

        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение всех своих дашбордов c пагинацией")
    void getAllDashboardsPagination() {
        String response = getAllDashboards(UID, null, 1, 2);
        String expected = """
                {
                  "dashboards": [
                    {
                      "id": 12,
                      "name": "Суммарные продажи",
                      "type":"CATEGORY",
                      "params": {}
                    }
                  ],
                  "pagingInfo": {
                    "pageNumber": 1,
                    "pageSize": 2,
                    "totalPages": 2,
                    "totalElements": 3
                  }
                }""";

        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение всех своих дашбордов по части названия")
    void getAllDashboardsByPartName() {
        String response = getAllDashboardsByPartName(UID, "СУмм");
        String expected = """
                {
                  "dashboards": [
                    {
                      "id": 11,
                      "name": "Суммарные продажи",
                      "type":"CATEGORY",
                      "params": {
                      }
                    },
                    {
                      "id": 12,
                      "name": "Суммарные продажи",
                      "type":"CATEGORY",
                      "params": {
                      }
                    }
                  ],
                  "pagingInfo": {
                    "pageNumber": 0,
                    "pageSize": 50,
                    "totalPages": 1,
                    "totalElements": 2
                  }
                }""";

        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение всех своих дашбордов по части названия с пагинацией")
    void getAllDashboardsByPartNamePagination() {
        String response = getAllDashboards(UID, "СУмм", 0, 1);
        String expected = """
                {
                  "dashboards": [
                    {
                      "id": 11,
                      "name": "Суммарные продажи",
                      "type":"CATEGORY",
                      "params": {
                      }
                    }
                  ],
                  "pagingInfo": {
                    "pageNumber": 0,
                    "pageSize": 1,
                    "totalPages": 2,
                    "totalElements": 2
                  }
                }""";

        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Создание или клонирование дашборда - существует свой дашборд")
    void getOrCloneDashboardTestExistsForSelfTest() {
        String body = """
                {
                  "uid": 100,
                  "dashboardId":15
                }""";
        String dashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/getOrClone")
                .toUriString();
        String response = FunctionalTestHelper.postForJson(dashboardsUrl, body);
        String expected = "{\"dashboardsId\":15}";
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Создание или клонирование дашборда - существует чужой дашборд")
    void getOrCloneDashboardExistsForOtherTest() {
        String body = """
                {
                  "uid": 101,
                  "dashboardId":15
                }""";
        String dashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/getOrClone")
                .toUriString();
        String response = FunctionalTestHelper.postForJson(dashboardsUrl, body);
        String expected = "{\"dashboardsId\":17}";
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Создание или клонирование дашборда - дашборда не существует")
    @DbUnitDataSet(after = "DashboardControllerGetOrCloneCreateNewTest.after.csv")
    void getOrCloneDashboardCreateNewTest() {
        String body = """
                {
                  "uid": 101,
                  "dashboardId":16
                }""";
        String dashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/getOrClone")
                .toUriString();
        String response = FunctionalTestHelper.postForJson(dashboardsUrl, body);
        String expected = "{\"dashboardsId\":1}";
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Создание дашборда по модели")
    @DbUnitDataSet(after = "UserDashboardControllerTestGetOrCreate.after.csv")
    void getOrCreateModelDashboardTest() {
        String body = """
                {
                  "uid": 100,
                  "model":14,
                  "hid":38
                }""";
        String dashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/forModel")
                .toUriString();
        String response = FunctionalTestHelper.postForJson(dashboardsUrl, body);
        String expected = "{\"dashboardsId\":1}";
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Создание дашборда по модели - проверка отсутствия дубликата при повторном вызове")
    @DbUnitDataSet(after = "UserDashboardControllerTestGetOrCreate.after.csv")
    void getOrCreateModelDashboardNoDuplicationTest() {
        String body = """
                {
                  "uid": 100,
                  "model":14,
                  "hid":38
                }""";
        String dashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/forModel")
                .toUriString();
        FunctionalTestHelper.postForJson(dashboardsUrl, body);
        String response = FunctionalTestHelper.postForJson(dashboardsUrl, body);
        String expected = "{\"dashboardsId\":1}";
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение всех своих дашбордов по списку категорий")
    void getDashboardsByHidsTest() {
        String body = """
                {
                  "hids": [
                    38
                  ]
                }""";
        String response = getDashboardsByHids(UID, body);
        String expected = """
                {
                  "dashboardsMapping": {
                    "38": {
                      "id": 11,
                      "name": "Суммарные продажи",
                      "type": "CATEGORY",
                      "params": {}
                    }
                  }
                }""";

        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Лок на получение дашбордов уже взят")
    void badRequestLockTimeout() {
        doThrow(new LockTimeoutException(AnalyticsEntity.DASHBOARD, UID))
                .when(userDashboardsLockService).lockUserDashboards(124L);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getAllDashboards(124L)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expected = """
                { \s
                   "code":"LOCK_TIMEOUT",
                   "message":"Lock timeout while taking lock on DASHBOARD by user 123"
                }""";
        JsonAssert.assertJsonEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Название дашборда не представлено в запросе")
    void badRequestEmptyName() {
        String body = """
                { \s
                   "params":{ \s

                   }
                }""";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> updateDashboard(11, body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expected = """
                { \s
                   "code":"BAD_REQUEST",
                   "message":"[name should be present]"
                }""";
        JsonAssert.assertJsonEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Параметры дашборда не представлены в запросе")
    void badRequestEmptyParams() {
        String body = "" +
                "{  " +
                "   \"name\": \"Dashboard the great\" " +
                "}";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> updateDashboard(11, body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
        String expected = """
                {
                   "code":"BAD_REQUEST",
                   "message":"[params should be present]"
                }""";
        JsonAssert.assertJsonEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Успешное обновление дашборда")
    @DbUnitDataSet(after = "updateDashboard.after.csv")
    void updateDashboard() {
        String body = """
                { \s
                   "name":"New name",
                   "params":{ \s
                      "geoIds":[ \s
                         1,
                         3,
                         5
                      ]
                   }
                }""";
        updateDashboard(11, body);
    }

    @Test
    @DisplayName("Удаление дашборда")
    @DbUnitDataSet(after = "deleteDashboard.after.csv")
    void deleteDashboard() {
        deleteDashboard(12);
    }


    @Test
    @DisplayName("Удаление всех дашбордов пользователя")
    @DbUnitDataSet(after = "deleteUserDashboards.after.csv")
    void deleteUserDashboards() {
        deleteUserDashboards(123);
    }

    @Test
    @DisplayName("Дашборд не найден")
    void dashboardNotFound() {
        HttpClientErrorException clientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getDashboard(-1L, Language.RU)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                clientErrorException.getStatusCode()
        );
        var expected = """
                { \s
                   "code":"ENTITY_NOT_FOUND",
                   "message":"Entity DASHBOARD not found by id: -1",
                   "entityId": -1,
                   "entityType":"DASHBOARD"
                }""";
        JsonAssert.assertJsonEquals(expected, clientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Получение дашборда без виджетов")
    void getDashboardEmpty() {
        var response = getDashboard(11L, Language.RU);
        var expected = loadFromFile("DashboardControllerTest.getDashboardEmpty.expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение дашборда с виджетами")
    void getDashboard() {
        var response = getDashboard(12, Language.RU);
        var expected = loadFromFile("DashboardControllerTest.getDashboard.expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение дашборда с виджетами на английском")
    void getDashboardEn() {
        var response = getDashboard(12, Language.EN);
        var expected = loadFromFile("DashboardControllerTest.getDashboardEn.expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Получение уровней доступа по дашбордам")
    void getDashboardAccessLevel() {
        String actualResponse = getDashboards(123, List.of(11L, 12L, 14L));
        String expectedResponse = """
                [
                  {
                    "market": false,
                    "external": true,
                    "multi": false,
                    "other": false,
                    "dashboardId": 11,
                    "userAccessLevels": {
                      "userLevel": "NONE",
                      "userShopLevel": "NONE",
                      "userVendorLevel": null,
                      "userPartnersLevels": [
                        {
                          "partnerType": "SHOP",
                          "accessLevel": "NONE"
                        }
                      ]
                    },
                    "partnerId": 1000000
                  },
                  {
                    "market": false,
                    "external": true,
                    "multi": false,
                    "other": false,
                    "dashboardId": 12,
                    "userAccessLevels": {
                      "userLevel": "FROM_5_TO_10",
                      "userShopLevel": "FROM_5_TO_10",
                      "userVendorLevel": null,
                      "userPartnersLevels": [
                        {
                          "partnerType": "SHOP",
                          "accessLevel": "FROM_5_TO_10"
                        }
                      ]
                    },
                    "partnerId": 1000000
                  }
                ]""";
        JsonAssert.assertJsonEquals(
                expectedResponse,
                actualResponse
        );
    }

    private String dashboardsUrl(long dashboardId, Language language) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("dashboards/{dashboardId}")
                .queryParam("language", language)
                .buildAndExpand(dashboardId)
                .toUriString();
    }

    private void updateDashboard(long dashboardId, String body) {
        var dashboardsUrl = dashboardsUrl(dashboardId, Language.RU);
        FunctionalTestHelper.putForJson(dashboardsUrl, body);
    }

    private void deleteDashboard(long dashboardId) {
        var dashboardsUrl = dashboardsUrl(dashboardId, Language.RU);
        FunctionalTestHelper.delete(dashboardsUrl);
    }

    private void deleteUserDashboards(long userId) {
        var userDashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/user/{userId}")
                .buildAndExpand(userId)
                .toUriString();
        FunctionalTestHelper.delete(userDashboardsUrl);
    }

    private String getAllDashboards(long userId) {
        return getAllDashboards(userId, null, null, null);
    }

    private String getAllDashboardsByPartName(long userId, String partName) {
        return getAllDashboards(userId, partName, null, null);
    }

    private String getAllDashboards(long userId, String partName, Integer pageNumber, Integer pageSize) {
        var dashboardsUrl = baseUrl() + "dashboards?userId=" + userId;
        dashboardsUrl = addQueryParam(dashboardsUrl, "partName", partName);
        dashboardsUrl = addQueryParam(dashboardsUrl, "pageNumber", pageNumber);
        dashboardsUrl = addQueryParam(dashboardsUrl, "pageSize", pageSize);
        return FunctionalTestHelper.get(dashboardsUrl).getBody();
    }

    private String getDashboardsByHids(long uid, String requestBody) {
        String dashboardsUrl = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/hids/")
                .queryParam("uid", uid)
                .toUriString();
        return FunctionalTestHelper.postForJson(dashboardsUrl, requestBody);
    }

    private static String addQueryParam(String url, String paramName, Object paramValue) {
        if (paramValue == null) {
            return url;
        }
        return url + "&" + paramName + "=" + paramValue;
    }

    private String getDashboard(long dashboardId, Language language) {
        var dashboardUrl = dashboardsUrl(dashboardId, language);
        return FunctionalTestHelper.get(dashboardUrl).getBody();
    }

    private String dashboardsUrl(long uid, Collection<Long> ids) {
        var idsParam = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/dashboards/accessLevels")
                .queryParam("userId", uid)
                .queryParam("id", idsParam)
                .toUriString();
    }

    private String getDashboards(long uid, Collection<Long> ids) {
        var url = dashboardsUrl(uid, ids);
        return FunctionalTestHelper.get(url).getBody();
    }
}
