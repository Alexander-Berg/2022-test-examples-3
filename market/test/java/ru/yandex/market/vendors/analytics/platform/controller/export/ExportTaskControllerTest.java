package ru.yandex.market.vendors.analytics.platform.controller.export;

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
 * Functional tests for {@link ExportTaskController}.
 *
 * @author antipov93.
 */
public class ExportTaskControllerTest extends FunctionalTest {

    private static final String PATH_PREFIX = "/user/{userId}/export";

    @Test
    @DisplayName("Получение одной задачи")
    @DbUnitDataSet(before = "getExportTasks.before.csv")
    void getExportTask() {
        var expected = ""
                + "{\n"
                + "  \"taskId\": 2,\n"
                + "  \"entityType\": \"DASHBOARD\",\n"
                + "  \"dashboardId\": 2,\n"
                + "  \"userId\": 42,\n"
                + "  \"filename\": \"Ноготочки\",\n"
                + "  \"status\": \"SUCCESS\",\n"
                + "  \"creationTime\": \"2019-01-03T00:00:00\",\n"
                + "  \"finishTime\": null,\n"
                + "  \"resultLink\": null\n"
                + "}";
        var actual = getExportTask(42, 2);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Получение не своей задачи")
    @DbUnitDataSet(before = "getExportTasks.before.csv")
    void getExportTaskWrongUser() {
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getExportTask(44, 2)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                clientException.getStatusCode()
        );
        var expected = ""
                + "{\n"
                + "  \"code\": \"ENTITY_NOT_FOUND\",\n"
                + "  \"message\": \"Entity EXPORT_TASK not found by id: 2\",\n"
                + "  \"entityId\": 2,\n"
                + "  \"entityType\":\"EXPORT_TASK\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, clientException.getResponseBodyAsString());
    }


    @Test
    @DisplayName("Получение несуществующей задачи")
    @DbUnitDataSet(before = "getExportTasks.before.csv")
    void getExportTaskNotFound() {
        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getExportTask(42, 10000)
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                clientException.getStatusCode()
        );
        var expected = ""
                + "{\n"
                + "  \"code\": \"ENTITY_NOT_FOUND\",\n"
                + "  \"message\": \"Entity EXPORT_TASK not found by id: 10000\",\n"
                + "  \"entityId\": 10000,\n"
                + "  \"entityType\":\"EXPORT_TASK\"\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, clientException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Получение списка задач на экспорт дашбордов (целиком)")
    @DbUnitDataSet(before = "getExportTasks.before.csv")
    void getUserExportTasks() {
        var actual = getExportTasks(42, null, 0, 2);
        var expected = ""
                + "{\n"
                + "  \"exportTasks\": [\n"
                + "    {\n"
                + "      \"taskId\": 3,\n"
                + "      \"entityType\": \"DASHBOARD\",\n"
                + "      \"dashboardId\": 1,\n"
                + "      \"userId\": 42,\n"
                + "      \"filename\": \"Лак для ногтей\",\n"
                + "      \"status\": \"FAILED\",\n"
                + "      \"creationTime\": \"2019-01-04T00:00:00\",\n"
                + "      \"finishTime\": null,\n"
                + "      \"resultLink\": null\n"
                + "    },\n"
                + "    {\n"
                + "      \"taskId\": 2,\n"
                + "      \"entityType\": \"DASHBOARD\",\n"
                + "      \"dashboardId\": 2,\n"
                + "      \"userId\": 42,\n"
                + "      \"filename\": \"Ноготочки\",\n"
                + "      \"status\": \"SUCCESS\",\n"
                + "      \"creationTime\": \"2019-01-03T00:00:00\",\n"
                + "      \"finishTime\": null,\n"
                + "      \"resultLink\": null\n"
                + "    }\n"
                + "  ],\n"
                + "  \"pagingInfo\": {\n"
                + "    \"pageNumber\": 0,\n"
                + "    \"pageSize\": 2,\n"
                + "    \"totalPages\": 2,\n"
                + "    \"totalElements\": 3\n"
                + "  }\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Получение списка задач на экспорт для конкретного дашборда")
    @DbUnitDataSet(before = "getExportTasks.before.csv")
    void getUserExportTasksByDashboard() {
        var actual = getExportTasks(42, 1L, 0, 2);
        var expected = ""
                + "{\n"
                + "  \"exportTasks\": [\n"
                + "    {\n"
                + "      \"taskId\": 4,\n"
                + "      \"entityType\": \"WIDGET\",\n"
                + "      \"dashboardId\": 1,\n"
                + "      \"userId\": 42,\n"
                + "      \"filename\": \"Продажи в категории\",\n"
                + "      \"status\": \"IN_PROGRESS\",\n"
                + "      \"creationTime\": \"2019-01-08T00:00:00\",\n"
                + "      \"finishTime\": null,\n"
                + "      \"resultLink\": null\n"
                + "    },\n"
                + "    {\n"
                + "      \"taskId\": 3,\n"
                + "      \"entityType\": \"DASHBOARD\",\n"
                + "      \"dashboardId\": 1,\n"
                + "      \"userId\": 42,\n"
                + "      \"filename\": \"Лак для ногтей\",\n"
                + "      \"status\": \"FAILED\",\n"
                + "      \"creationTime\": \"2019-01-04T00:00:00\",\n"
                + "      \"finishTime\": null,\n"
                + "      \"resultLink\": null\n"
                + "    }\n"
                + "  ],\n"
                + "  \"pagingInfo\": {\n"
                + "    \"pageNumber\": 0,\n"
                + "    \"pageSize\": 2,\n"
                + "    \"totalPages\": 2,\n"
                + "    \"totalElements\": 3\n"
                + "  }\n"
                + "}";
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Создание задачи на экспорт данных")
    @DbUnitDataSet(before = "exportTaskCreation.before.csv", after = "exportTaskCreation.after.csv")
    void createExportTask() {
        var request = loadFromFile("ExportTaskControllerTest.createExportTask.request.json");
        createExportTaskRequest(142, request);
    }

    private String getExportTask(long userId, long taskId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path(PATH_PREFIX)
                .path("/{taskId}")
                .buildAndExpand(userId, taskId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String getExportTasks(long userId, Long dashboardId, int pageNumber, int pageSize) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path(PATH_PREFIX)
                .queryParam("dashboardId", dashboardId)
                .queryParam("pageNumber", pageNumber)
                .queryParam("pageSize", pageSize)
                .buildAndExpand(userId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }


    private String createExportTaskRequest(long userId, String body) {
        var dashboardsUrl = baseUrl() + "/user/" + userId + "/export";
        return FunctionalTestHelper.postForJson(dashboardsUrl, body);
    }
}
