package ru.yandex.market.checker.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.api.model.CheckTask;
import ru.yandex.market.checker.api.model.SchedulerStatus;
import ru.yandex.market.checker.api.model.SchedulerType;
import ru.yandex.market.checker.db.TaskDao;
import ru.yandex.market.checker.matchers.TaskMatchers;
import ru.yandex.market.checker.model.ImportInfo;
import ru.yandex.market.checker.model.SortType;
import ru.yandex.market.checker.model.Task;
import ru.yandex.market.checker.st.client.STClient;
import ru.yandex.market.checker.yql.client.YqlClient;
import ru.yandex.market.checker.yql.model.OperationDto;
import ru.yandex.market.checker.yql.model.OperationStatus;
import ru.yandex.market.checker.yql.model.ProcessOperationDto;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.startrek.client.model.Issue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checker.utils.CheckerUtils.DEFAULT_ZONE_ID;

public class TaskServiceTest extends FunctionalTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private YqlClient yqlClient;

    @Autowired
    private STClient stClient;

    @Autowired
    private Clock clock;

    @Autowired
    private SchedulerService schedulerService;

    @Test
    @DbUnitDataSet(
            before = "runImports.before.csv",
            after = "runImports.after.csv")
    @DisplayName("Запуск сверок, проверка записей в БД.")
    void test_runImports() {
        ImportInfo info1 = new ImportInfo();
        info1.setId(1L);
        info1.setName("Import1");
        info1.setComponentId(1L);
        info1.setEntityId(1L);
        info1.setCreatedAt(clock.instant());
        info1.setYqlQuery("select a from table");
        info1.setStatus(SchedulerStatus.ACTIVE);
        info1.setSchedulerType(SchedulerType.IMPORT);
        info1.setTimeWindowSize(1);
        info1.setTimeWindowUnit(ChronoUnit.DAYS);
        info1.setCreationTicket("ticket");
        info1.setResultFolder("//some/folder");
        info1.setResultTable("some_table");

        ImportInfo info2 = new ImportInfo();
        info2.setId(2L);
        info2.setName("Import2");
        info2.setComponentId(2L);
        info2.setEntityId(1L);
        info2.setCreatedAt(clock.instant());
        info2.setYqlQuery("select b from table");
        info2.setStatus(SchedulerStatus.ACTIVE);
        info2.setSchedulerType(SchedulerType.IMPORT);
        info2.setTimeWindowSize(1);
        info2.setTimeWindowUnit(ChronoUnit.DAYS);
        info2.setCreationTicket("ticket");
        info2.setResultFolder("//some/other/folder");
        info2.setResultTable("some_other_table");

        when(yqlClient.submitOperation(any()))
                .thenReturn(
                        Mono.just(OperationDto.builder()
                                .setId("1")
                                .setStatus(OperationStatus.PENDING)
                                .build()))
                .thenReturn(
                        Mono.just(OperationDto.builder()
                                .setId("2")
                                .setStatus(OperationStatus.RUNNING)
                                .build()));

        when(yqlClient.getShareId(eq("1"))).thenReturn(Mono.just("erijvgh4gu2hg4"));
        when(yqlClient.getShareId(eq("2"))).thenReturn(Mono.just("th35hg3g2efverg"));

        taskService.upsertTasks(taskService.runSchedulers(new LinkedHashSet<>(List.of(info1, info2))));
    }

    @Test
    @DbUnitDataSet(before = "createTickets.before.csv")
    @DisplayName("Проверка создания тикета.")
    void test_createTickets() {
        when(stClient.createIssue(any(), any(), any(), any())).thenReturn(
                Mono.just(new Issue(null, null, "1", null, 1, new EmptyMap<>(), null)));

        taskService.createTickets(new HashSet<>(
                taskDao.getTasks(null, null, 0, 50, "id", SortType.ASC, SchedulerType.ALL, null)));

        String description = "Импорт name1 за 2020-01-01";
        String summary = "Компонента импорта: wms\n" +
                "Период импорта: начало (включительно) 2020-01-01 окончание (не включая) 2020-02-01\n" +
                "Таблицы исходных данных импортируемой системы:\n" +
                "https://yt.yandex-team.ru/hahn/navigation?path=first_component_table/2020-01-31\n" +
                "Таблица с результатами импорта:\n" +
                "https://yt.yandex-team.ru/hahn/navigation?path=result_table_folder/result_table/DAYS/2020-01-01\n" +
                "Результат выполнения импорта:\n" +
                "https://yql.yandex-team.ru/Operations/erijvgh4gu2hg4";
        verify(stClient).createIssue(any(), eq(description), any(), eq(summary));
    }

    @Test
    @DbUnitDataSet(before = "runImports.before.csv",
            after = "runImports_specific_date.after.csv")
    @DisplayName("Запуск за определенную дату.")
    void test_specificDateRun() {
        ImportInfo info1 = new ImportInfo();
        info1.setId(1L);
        info1.setName("Import1");
        info1.setComponentId(1L);
        info1.setEntityId(1L);
        info1.setCreatedAt(clock.instant());
        info1.setYqlQuery("select a from table");
        info1.setStatus(SchedulerStatus.ACTIVE);
        info1.setSchedulerType(SchedulerType.IMPORT);
        info1.setTimeWindowSize(1);
        info1.setTimeWindowUnit(ChronoUnit.DAYS);
        info1.setCreationTicket("ticket");
        info1.setResultFolder("//some/folder");
        info1.setFirstComponentTable("//home/table1/%(PREV_DATE_TO)");
        info1.setResultTable("some_table/%(DATE_FROM)_%(DATE_TO)");

        ImportInfo info2 = new ImportInfo();
        info2.setId(2L);
        info2.setName("Import2");
        info2.setComponentId(2L);
        info2.setEntityId(1L);
        info2.setCreatedAt(clock.instant());
        info2.setYqlQuery("select b from table");
        info2.setStatus(SchedulerStatus.ACTIVE);
        info2.setSchedulerType(SchedulerType.IMPORT);
        info2.setTimeWindowSize(1);
        info2.setTimeWindowUnit(ChronoUnit.DAYS);
        info2.setCreationTicket("ticket");
        info2.setResultFolder("//some/other/folder");
        info2.setFirstComponentTable("//home/table2/%(PREV_DATE_TO)");
        info2.setResultTable("some_other_table/%(DATE_FROM)_%(DATE_TO)");

        when(yqlClient.submitOperation(any()))
                .thenReturn(
                        Mono.just(OperationDto.builder()
                                .setId("1")
                                .setStatus(OperationStatus.PENDING)
                                .build()))
                .thenReturn(
                        Mono.just(OperationDto.builder()
                                .setId("2")
                                .setStatus(OperationStatus.RUNNING)
                                .build()));

        when(yqlClient.getShareId(eq("1"))).thenReturn(Mono.just("erijvgh4gu2hg4"));
        when(yqlClient.getShareId(eq("2"))).thenReturn(Mono.just("th35hg3g2efverg"));

        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 2, 1);

        Set<Task> specificDateTasks = taskService.runSchedulers(
                new LinkedHashSet<>(List.of(info1, info2)),
                from,
                to
        );

        assertThat(specificDateTasks, containsInAnyOrder(
                allOf(
                        TaskMatchers.hasDateFrom(from.atStartOfDay(DEFAULT_ZONE_ID).toInstant()),
                        TaskMatchers.hasDateTo(to.atStartOfDay(DEFAULT_ZONE_ID).toInstant())
                ),
                allOf(
                        TaskMatchers.hasDateFrom(from.atStartOfDay(DEFAULT_ZONE_ID).toInstant()),
                        TaskMatchers.hasDateTo(to.atStartOfDay(DEFAULT_ZONE_ID).toInstant())
                )
        ));

        taskService.upsertTasks(specificDateTasks);
    }

    @Test
    @DbUnitDataSet(before = "unfinished_tasks.before.csv")
    @DisplayName("Получаем невыполненные задачи.")
    void test_shouldReturnUnfinishedTasks() {
        Set<Task> notFinishedTasks = taskService.getNotFinishedTasks();
        assertThat(notFinishedTasks, hasSize(1));
    }

    @Test
    @DbUnitDataSet(before = "technical_report.before.csv")
    @DisplayName("Получаем поле из сервиса со ссылкой")
    void test_shouldGetTechnicalReport_whenResultTableExists() {
        CheckTask checkTask = taskService.getCheckTask(1L);
        assertEquals(checkTask.getTechnicalReport(), "https://excel-exporter.yt.yandex-team.ru/hahn/api/export?path=%2F%2Fhome%2Fmarket%2Fproduction%2Fmstat%2Fdictionaries%2Fmbi%2Fmbi_checker%2FFF_AX_3P_monthly_2020-02-15&output_format%5B%24value%5D=excel&dump_error_into_response=true");//"//some/folder/some_table/2020-01-01_2020-02-01");
    }

    @Test
    @DbUnitDataSet(before = "monthly_check_dates.before.csv")
    @DisplayName("Ежемесячная сверка с 1 по последнее число")
    void test_monthlyCheckDayFromTo() {
        when(yqlClient.submitOperation(any()))
                .thenReturn(
                        Mono.just(OperationDto.builder()
                                .setId("1")
                                .setStatus(OperationStatus.PENDING)
                                .build()));
        when(yqlClient.getShareId(anyString()))
                .thenReturn(Mono.just("12345"));
        taskService.runSchedulers(Set.of(schedulerService.getScheduler(1L)));
        verify(yqlClient, times(1)).submitOperation(
                eq(ProcessOperationDto.builder()
                        .setContent("DATE_FROM = 2020-09-01, DATE_TO = 2020-09-30")
                        .setType("SQLv1")
                        .setAction("RUN")
                        .build())
        );
    }
}
