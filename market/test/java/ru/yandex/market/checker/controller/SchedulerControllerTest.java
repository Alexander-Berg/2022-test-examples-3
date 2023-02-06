package ru.yandex.market.checker.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.common.util.StringUtils;
import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.TestUtils;
import ru.yandex.market.checker.api.model.CheckDto;
import ru.yandex.market.checker.api.model.CheckRequestBody;
import ru.yandex.market.checker.api.model.CheckTask;
import ru.yandex.market.checker.api.model.ImportDto;
import ru.yandex.market.checker.api.model.ImportRequestBody;
import ru.yandex.market.checker.api.model.ImportTask;
import ru.yandex.market.checker.api.model.PagerResponseInfo;
import ru.yandex.market.checker.api.model.RunType;
import ru.yandex.market.checker.api.model.SchedulerStatus;
import ru.yandex.market.checker.api.model.SchedulerStatusDto;
import ru.yandex.market.checker.api.model.SchedulerType;
import ru.yandex.market.checker.api.model.TaskDto;
import ru.yandex.market.checker.api.model.TaskStatus;
import ru.yandex.market.checker.matchers.SchedulerDtoMatchers;
import ru.yandex.market.checker.matchers.TaskDtoMatchers;
import ru.yandex.market.checker.st.client.STClient;
import ru.yandex.market.checker.yql.client.YqlClient;
import ru.yandex.market.checker.yql.model.OperationDto;
import ru.yandex.market.checker.yql.model.OperationStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.error.info.model.ErrorInfo;
import ru.yandex.startrek.client.model.Issue;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checker.TestUtils.parsePagerResponse;
import static ru.yandex.market.checker.matchers.ErrorInfoMatcher.hasCode;
import static ru.yandex.market.checker.matchers.ErrorInfoMatcher.hasMessage;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.post;

/**
 * Tests for {@link SchedulerController}
 */
public class SchedulerControllerTest extends FunctionalTest {
    @Autowired
    private YqlClient yqlClient;

    @Autowired
    private STClient stClient;

    @Test
    @DisplayName("Тест на создание сверки + добавление данных в аудит")
    @DbUnitDataSet(
            before = "createCheck.before.csv",
            after = "createCheck.after.csv"
    )
    void test_createCheck() {
        CheckRequestBody requestBody = new CheckRequestBody()
                .name("name")
                .description("description")
                .firstComponentId(1L)
                .secondComponentId(2L)
                .creationTicket("ticket")
                .entityId(1L)
                .yqlQuery("SELECT 1")
                .resultTable("result_table")
                .resultFolder("result_folder")
                .escalationLevelId(1L)
                .cronSchedule("0 30 11 * * ? *")
                .runType(
                        new RunType()
                                .timeWindowSize(1)
                                .timeWindowUnit(RunType.TimeWindowUnitEnum.DAYS));

        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/schedulers/checks", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на создание импорта + добавление данных в аудит")
    @DbUnitDataSet(
            before = "createImport.before.csv",
            after = "createImport.after.csv"
    )
    void test_createImport() {
        ImportRequestBody requestBody = new ImportRequestBody()
                .name("name")
                .description("description")
                .componentId(1L)
                .entityId(1L)
                .yqlQuery("SELECT 1")
                .cronSchedule("0 30 11 * * ? *")
                .escalationLevelId(1L)
                .creationTicket("ticket")
                .resultTable("result_table")
                .resultFolder("result_folder")
                .runType(
                        new RunType()
                                .timeWindowSize(1)
                                .timeWindowUnit(RunType.TimeWindowUnitEnum.DAYS));

        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/schedulers/imports", bodyEntity);
        assertOk(response);
    }

    @ParameterizedTest(name = "[{index}] id = {0} and isSuccess = {1}")
    @MethodSource("getCheckTestData")
    @DisplayName("Тест на получение сверки")
    @DbUnitDataSet(before = "getCheck.before.csv")
    void test_getCheck(
            long id,
            boolean isSuccess
    ) {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result = () -> get(baseUrl() + "/schedulers/checks/" + id, bodyEntity);

        if (isSuccess) {
            ResponseEntity<String> response = result.get();
            assertOk(response);

            CheckDto checkDto = TestUtils.parseOneResult(response.getBody(), CheckDto.class);

            MatcherAssert.assertThat(checkDto, CoreMatchers.allOf(
                    SchedulerDtoMatchers.hasId(1L),
                    SchedulerDtoMatchers.hasName("name"),
                    SchedulerDtoMatchers.hasType(SchedulerType.CHECK),
                    SchedulerDtoMatchers.hasStatus(SchedulerStatus.TO_APPROVE)
            ));
        } else {
            HttpClientErrorException exception = Assertions.assertThrows(
                    HttpClientErrorException.class,
                    result::get
            );
            List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
            assertThat(errors, hasSize(1));
            assertThat(errors.get(0), allOf(
                    hasCode("ENTITY_NOT_FOUND"),
                    hasMessage("Check with id " + id + " is not found")
            ));
        }
    }

    @ParameterizedTest(name = "[{index}] id = {0} and isSuccess = {1}")
    @MethodSource("getCheckTestData")
    @DisplayName("Тест на получение сверки")
    @DbUnitDataSet(before = "getImport.before.csv")
    void test_getImport(
            long id,
            boolean isSuccess
    ) {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result = () -> get(baseUrl() + "/schedulers/imports/" + id, bodyEntity);

        if (isSuccess) {
            ResponseEntity<String> response = result.get();
            assertOk(response);

            ImportDto importDto = TestUtils.parseOneResult(response.getBody(), ImportDto.class);

            MatcherAssert.assertThat(importDto, CoreMatchers.allOf(
                    SchedulerDtoMatchers.hasId(1L),
                    SchedulerDtoMatchers.hasName("name"),
                    SchedulerDtoMatchers.hasType(SchedulerType.IMPORT),
                    SchedulerDtoMatchers.hasStatus(SchedulerStatus.APPROVED)
            ));
        } else {
            HttpClientErrorException exception = Assertions.assertThrows(
                    HttpClientErrorException.class,
                    result::get
            );
            List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
            assertThat(errors, hasSize(1));
            assertThat(errors.get(0), allOf(
                    hasCode("ENTITY_NOT_FOUND"),
                    hasMessage("ImportInfo with id " + id + " is not found")
            ));
        }
    }

    private static Stream<Arguments> getCheckTestData() {
        return Stream.of(
                Arguments.of(1, true),
                Arguments.of(2, false)
        );
    }

    @Test
    @DisplayName("Тест на получение сверки - страничный поиск")
    @DbUnitDataSet(before = "getScheduler.before.csv")
    void test_getSchedulers() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() + "/schedulers?page=0&pageSize=100&sortType=ASC&type=ALL", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), CheckDto.class);

        assertEquals(4, (long) pagerResponse.getTotalCount());
        assertThat(pagerResponse.getItems().stream()
                    .map(obj -> (CheckDto) obj).collect(Collectors.toList()).get(0), allOf(
                SchedulerDtoMatchers.hasId(1L),
                SchedulerDtoMatchers.hasName("check1"),
                SchedulerDtoMatchers.hasType(SchedulerType.CHECK),
                SchedulerDtoMatchers.hasStatus(SchedulerStatus.TO_APPROVE)
        ));
    }

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("getSchedulersSortByArguments")
    @DisplayName("Тест на сортировку при страничном поиске")
    @DbUnitDataSet(before = "getScheduler.before.csv")
    void test_getSchedulersSortBy(
            String sortBy,
            List<Long> expectedIds
    ) {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() +
                        "/schedulers?page=0&pageSize=100&sortType=ASC&type=ALL&sortBy=" + sortBy, bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), CheckDto.class);

        Assert.assertThat(
                pagerResponse.getItems().stream().map(obj -> (CheckDto) obj).map(CheckDto::getId).collect(Collectors.toList()),
                is(expectedIds));
    }

    static Stream<Arguments> getSchedulersSortByArguments() {
        return Stream.of(
                Arguments.of( // 1
                        "id",
                        List.of(1L, 2L, 3L, 4L)
                ),
                Arguments.of( // 2
                        "status",
                        List.of(2L, 4L, 3L, 1L)
                ),
                Arguments.of( // 3
                        "author",
                        List.of(1L, 2L, 4L, 3L)
                ),
                Arguments.of( // 4
                        "createdAt",
                        List.of(1L, 3L, 2L, 4L)
                ),
                Arguments.of( // 5
                        "lastSuccessfulRun",
                        List.of(4L, 2L, 3L, 1L)
                )
        );
    }

    @Test
    @DisplayName("Тест на обновление сверки")
    @DbUnitDataSet(
            before = "updateCheck.before.csv",
            after = "updateCheck.after.csv"
    )
    void test_updateCheck() {
        CheckRequestBody requestBody = new CheckRequestBody()
                .name("check2")
                .description("checkDesc2")
                .firstComponentId(1L)
                .secondComponentId(2L)
                .creationTicket("ticket")
                .entityId(1L)
                .yqlQuery("SELECT 2")
                .resultTable("result_table")
                .resultFolder("result_folder")
                .escalationLevelId(1L)
                .cronSchedule("0 30 11 * * ? *")
                .runType(
                        new RunType()
                                .timeWindowSize(2)
                                .timeWindowUnit(RunType.TimeWindowUnitEnum.DAYS));

        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/schedulers/checks/10", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на обновление импорта")
    @DbUnitDataSet(
            before = "updateImport.before.csv",
            after = "updateImport.after.csv"
    )
    void test_updateImport() {
        ImportRequestBody requestBody = new ImportRequestBody()
                .name("name2")
                .description("description2")
                .componentId(1L)
                .entityId(1L)
                .yqlQuery("SELECT 2")
                .cronSchedule("0 30 11 * * ? *")
                .escalationLevelId(1L)
                .creationTicket("ticket")
                .resultTable("result_table")
                .resultFolder("result_folder")
                .runType(
                        new RunType()
                                .timeWindowSize(2)
                                .timeWindowUnit(RunType.TimeWindowUnitEnum.DAYS));

        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/schedulers/imports/10", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на обновление статуса сверки c ошибкой")
    @DbUnitDataSet(
            before = "updateCheckState.before.csv",
            after = "updateCheckState.before.csv"
    )
    void test_updateCheckStateException() {
        SchedulerStatusDto requestBody = new SchedulerStatusDto().value(SchedulerStatus.ACTIVE);

        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        Supplier<ResponseEntity<String>> result = () -> post(baseUrl() + "/schedulers/checks/1/status", bodyEntity);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                result::get);

        List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("BAD_PARAM"),
                hasMessage("Can't transfer scheduler from status TO_APPROVE to status ACTIVE")
        ));
    }

    @Test
    @DisplayName("Тест на обновление статуса сверки")
    @DbUnitDataSet(
            before = "updateCheckState.before.csv",
            after = "updateCheckState.after.csv"
    )
    void test_updateCheckState() {
        SchedulerStatusDto requestBody = new SchedulerStatusDto().value(SchedulerStatus.APPROVED);
        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/schedulers/checks/1/status", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на обновление статуса импорта")
    @DbUnitDataSet(
            before = "updateImportState.before.csv",
            after = "updateImportState.after.csv"
    )
    void test_updateImportState() {
        SchedulerStatusDto requestBody = new SchedulerStatusDto().value(SchedulerStatus.APPROVED);
        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/schedulers/imports/1/status", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на обновление статуса импорта c ошибкой")
    @DbUnitDataSet(
            before = "updateImportState.before.csv",
            after = "updateImportState.before.csv"
    )
    void test_updateImportStateException() {
        SchedulerStatusDto requestBody = new SchedulerStatusDto().value(SchedulerStatus.ACTIVE);

        HttpEntity<String> bodyEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        Supplier<ResponseEntity<String>> result = () -> post(baseUrl() + "/schedulers/imports/1/status", bodyEntity);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                result::get);

        List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("BAD_PARAM"),
                hasMessage("Can't transfer scheduler from status TO_APPROVE to status ACTIVE")
        ));
    }

    @Test
    @DisplayName("Тест на получение запусков задач - страничный поиск")
    @DbUnitDataSet(before = "getSchedulerTasks.before.csv")
    void test_getSchedulerTasks() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() + "/schedulers/tasks?page=0&pageSize=100&sortType=asc&type=ALL", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), TaskDto.class);

        assertEquals(3, (long) pagerResponse.getTotalCount());
        assertThat(pagerResponse.getItems().stream()
                .map(obj -> (TaskDto) obj).collect(Collectors.toList()).get(0), allOf(
                TaskDtoMatchers.hasId(1L),
                TaskDtoMatchers.hasType(SchedulerType.CHECK),
                TaskDtoMatchers.hasSchedulerId(1L)
        ));

        result = () -> get(baseUrl() + "/schedulers/tasks?page=0&pageSize=100&sortType=asc&type=CHECK", bodyEntity);

        response = result.get();
        assertOk(response);

        pagerResponse = parsePagerResponse(response.getBody(), TaskDto.class);

        assertEquals(2, (long) pagerResponse.getTotalCount());
        assertThat(pagerResponse.getItems().stream()
                .map(obj -> (TaskDto) obj).collect(Collectors.toList()).get(0), allOf(
                TaskDtoMatchers.hasId(1L),
                TaskDtoMatchers.hasType(SchedulerType.CHECK),
                TaskDtoMatchers.hasSchedulerId(1L)
        ));

        result = () -> get(baseUrl() + "/schedulers/tasks?page=0&pageSize=100&sortType=asc&type=IMPORT", bodyEntity);

        response = result.get();
        assertOk(response);

        pagerResponse = parsePagerResponse(response.getBody(), TaskDto.class);

        assertEquals(1, (long) pagerResponse.getTotalCount());
        assertThat(pagerResponse.getItems().stream()
                .map(obj -> (TaskDto) obj).collect(Collectors.toList()).get(0), allOf(
                TaskDtoMatchers.hasId(2L),
                TaskDtoMatchers.hasType(SchedulerType.IMPORT),
                TaskDtoMatchers.hasSchedulerId(3L)
        ));

        result = () -> get(baseUrl() + "/schedulers/tasks?page=0&pageSize=100&sortType=asc&type=CHECK&dateFrom=2020-08-01", bodyEntity);
        response = result.get();
        assertOk(response);

        pagerResponse = parsePagerResponse(response.getBody(), TaskDto.class);

        assertEquals(1, (long) pagerResponse.getTotalCount());
        assertThat(pagerResponse.getItems().stream()
                .map(obj -> (TaskDto) obj).collect(Collectors.toList()).get(0), allOf(
                TaskDtoMatchers.hasId(1L),
                TaskDtoMatchers.hasType(SchedulerType.CHECK),
                TaskDtoMatchers.hasSchedulerId(1L)
        ));

        result = () -> get(baseUrl() + "/schedulers/tasks?page=0&pageSize=100&sortType=asc&type=CHECK&dateTo=2020-11-01", bodyEntity);

        response = result.get();
        assertOk(response);

        pagerResponse = parsePagerResponse(response.getBody(), TaskDto.class);

        assertEquals(1, (long) pagerResponse.getTotalCount());
        assertThat(pagerResponse.getItems().stream()
                .map(obj -> (TaskDto) obj).collect(Collectors.toList()).get(0), allOf(
                TaskDtoMatchers.hasId(3L),
                TaskDtoMatchers.hasType(SchedulerType.CHECK),
                TaskDtoMatchers.hasSchedulerId(1L)
        ));
    }

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("getSchedulerTasksSortByArguments")
    @DisplayName("Тест на сортировку при страничном поиске запусков задач")
    @DbUnitDataSet(before = "getSchedulerTasks.before.csv")
    void test_getSchedulerTasksSortBy(
            String sortBy,
            List<Long> expectedIds
    ) {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() +
                        "/schedulers/tasks?page=0&pageSize=100&sortType=ASC&filterType=ALL&sortBy=" + sortBy, bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), TaskDto.class);

        Assert.assertThat(
                pagerResponse.getItems().stream().map(obj -> (TaskDto) obj).map(TaskDto::getId).collect(Collectors.toList()),
                is(expectedIds));
    }

    static Stream<Arguments> getSchedulerTasksSortByArguments() {
        return Stream.of(
                Arguments.of( // 1
                        "id",
                        List.of(1L, 2L, 3L)
                ),
                Arguments.of( // 2
                        "status",
                        List.of(1L, 3L, 2L)
                ),
                Arguments.of( // 3
                        "startedAt",
                        List.of(1L, 2L, 3L)
                ),
                Arguments.of( // 4
                        "finishedAt",
                        List.of(2L, 1L, 3L)
                )
        );
    }

    @Test
    @DisplayName("Тест на получение запусков сверки - страничный поиск")
    @DbUnitDataSet(before = "getSchedulerTasks.before.csv")
    void test_getCheckTasks() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() + "/schedulers/checks/1/tasks?page=0&pageSize=100&sortType=asc", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        List<CheckTask> dtos = TestUtils.parseListResults(response.getBody(), CheckTask.class);

        assertThat(dtos, hasSize(2));
        assertThat(dtos.get(0), allOf(
                TaskDtoMatchers.hasId(1L),
                TaskDtoMatchers.hasType(SchedulerType.CHECK),
                TaskDtoMatchers.hasSchedulerId(1L),
                TaskDtoMatchers.hasStatus(TaskStatus.FINISHED),
                TaskDtoMatchers.hasTechnicalReport(StringUtils.EMPTY))
        );

        assertThat(dtos.get(0).getCheckDto(), allOf(
                SchedulerDtoMatchers.hasId(1L),
                SchedulerDtoMatchers.hasName("check1"),
                SchedulerDtoMatchers.hasType(SchedulerType.CHECK),
                SchedulerDtoMatchers.hasStatus(SchedulerStatus.TO_APPROVE)));
    }

    @Test
    @DisplayName("Тест на получение запусков импортов - страничный поиск")
    @DbUnitDataSet(before = "getSchedulerTasks.before.csv")
    void test_getImportTasks() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() + "/schedulers/imports/3/tasks?page=0&pageSize=100&sortType=asc", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        List<ImportTask> dtos = TestUtils.parseListResults(response.getBody(), ImportTask.class);

        assertThat(dtos, hasSize(1));

        assertThat(dtos.get(0), allOf(
                TaskDtoMatchers.hasId(2L),
                TaskDtoMatchers.hasType(SchedulerType.IMPORT),
                TaskDtoMatchers.hasSchedulerId(3L),
                TaskDtoMatchers.hasStatus(TaskStatus.RUNNING)));

        assertThat(dtos.get(0).getImportDto(), allOf(
                SchedulerDtoMatchers.hasId(3L),
                SchedulerDtoMatchers.hasType(SchedulerType.IMPORT),
                SchedulerDtoMatchers.hasStatus(SchedulerStatus.APPROVED)));
    }

    @Test
    @DisplayName("Тест на получение запуска сверки")
    @DbUnitDataSet(before = "getSchedulerTasks.before.csv")
    void test_getCheckTask() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() + "/schedulers/checks/1/tasks/1", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        CheckTask dto = TestUtils.parseOneResult(response.getBody(), CheckTask.class);

        assertThat(dto, allOf(
                TaskDtoMatchers.hasId(1L),
                TaskDtoMatchers.hasType(SchedulerType.CHECK),
                TaskDtoMatchers.hasSchedulerId(1L),
                TaskDtoMatchers.hasStatus(TaskStatus.FINISHED)
        ));

        assertThat(dto.getCheckDto(), allOf(
                SchedulerDtoMatchers.hasId(1L),
                SchedulerDtoMatchers.hasName("check1"),
                SchedulerDtoMatchers.hasType(SchedulerType.CHECK),
                SchedulerDtoMatchers.hasStatus(SchedulerStatus.TO_APPROVE)));
    }

    @Test
    @DisplayName("Тест на получение запуска импорта")
    @DbUnitDataSet(before = "getSchedulerTasks.before.csv")
    void test_getImportTask() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> get(baseUrl() + "/schedulers/imports/1/tasks/2", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);

        ImportTask dto = TestUtils.parseOneResult(response.getBody(), ImportTask.class);

        assertThat(dto, allOf(
                        TaskDtoMatchers.hasId(2L),
                        TaskDtoMatchers.hasType(SchedulerType.IMPORT),
                        TaskDtoMatchers.hasSchedulerId(3L),
                        TaskDtoMatchers.hasStatus(TaskStatus.RUNNING),
                        TaskDtoMatchers.hasResultTable("//some/folder/some_table/2020-10-01_2020-11-01"),
                        TaskDtoMatchers.hasFirstComponent("wms"),
                        TaskDtoMatchers.hasSecondComponent("axapta"),
                        TaskDtoMatchers.hasDateFrom(LocalDate.of(2020,10,1)),
                        TaskDtoMatchers.hasDateTo(LocalDate.of(2020,11,1)),
                        TaskDtoMatchers.hasFirstComponentTable("//folder/import1/null"),
                        TaskDtoMatchers.hasSecondComponentTable(null)
                )
        );

        assertThat(dto.getImportDto(), allOf(
                SchedulerDtoMatchers.hasId(3L),
                SchedulerDtoMatchers.hasType(SchedulerType.IMPORT),
                SchedulerDtoMatchers.hasStatus(SchedulerStatus.APPROVED)));
    }

    @Test
    @DisplayName("Тест запуска задания")
    @DbUnitDataSet(
            before = "runScheduler.before.csv",
            after = "runScheduler.after.csv")
    void test_runScheduler() {
        when(yqlClient.submitOperation(any()))
                .thenReturn(
                        Mono.just(OperationDto.builder()
                                .setId("1")
                                .setStatus(OperationStatus.PENDING)
                                .build()));

        when(yqlClient.getShareId(eq("1"))).thenReturn(Mono.just("erijvgh4gu2hg4"));

        when(stClient.createIssue(any(), any(), any(), any())).thenReturn(
                Mono.just(new Issue(null, null, "CHECKERTEST-1", null, 1, new EmptyMap<>(), null)));

        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());

        Supplier<ResponseEntity<String>> result =
                () -> post(baseUrl() + "/schedulers/1/run?dateFrom=2020-12-01&dateTo=2021-01-01", bodyEntity);

        ResponseEntity<String> response = result.get();
        assertOk(response);
    }

    @Test
    @DisplayName("Тест запуска задания c ошибкой")
    @DbUnitDataSet(before = "runScheduler.before.csv")
    void test_runSchedulerWithException() {
        HttpEntity<String> bodyEntity = new HttpEntity<>(jsonHeaders());
        Supplier<ResponseEntity<String>> result = () -> post(baseUrl() + "/schedulers/2/run", bodyEntity);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                result::get);

        List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("BAD_PARAM"),
                hasMessage("Can't start scheduler with status = TO_APPROVE")
        ));
    }
}
