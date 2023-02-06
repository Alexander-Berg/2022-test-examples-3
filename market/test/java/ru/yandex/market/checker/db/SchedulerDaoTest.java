package ru.yandex.market.checker.db;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.api.model.CheckRequestBody;
import ru.yandex.market.checker.api.model.ImportRequestBody;
import ru.yandex.market.checker.api.model.RunType;
import ru.yandex.market.checker.api.model.SchedulerFilter;
import ru.yandex.market.checker.api.model.SchedulerStatus;
import ru.yandex.market.checker.api.model.SchedulerType;
import ru.yandex.market.checker.juggler.model.TaskResult;
import ru.yandex.market.checker.matchers.CheckMatchers;
import ru.yandex.market.checker.matchers.SchedulerMatchers;
import ru.yandex.market.checker.model.Check;
import ru.yandex.market.checker.model.EscalationLevelEnum;
import ru.yandex.market.checker.model.ImportInfo;
import ru.yandex.market.checker.model.Scheduler;
import ru.yandex.market.checker.model.SortType;
import ru.yandex.market.checker.st.TicketPlaceholders;
import ru.yandex.market.checker.utils.CheckerUtils;
import ru.yandex.market.checker.yql.model.OperationStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checker.st.TicketPlaceholders.ESCALATION_LEVEL;

public class SchedulerDaoTest extends FunctionalTest {
    @Autowired
    private SchedulerDao schedulerDao;

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "pendingImports.before.csv")
    void test_pendingImports() {
        Set<Scheduler> pendingImportInfo = schedulerDao.getSchedulersToRun(SchedulerType.IMPORT);
        assertEquals(2, pendingImportInfo.size());
    }

    @Test
    @DbUnitDataSet(before = "pendingImports.before.csv")
    void test_pendingChecks() {
        Set<Scheduler> pendingChecks = schedulerDao.getSchedulersToRun(SchedulerType.CHECK);
        assertEquals(1, pendingChecks.size());
    }

    @Test
    @DbUnitDataSet(
            before = "createCheck.before.csv",
            after = "createCheck.after.csv")
    @DisplayName("Вставить сверку")
    void test_createCheck() {
        Long id = schedulerDao.createCheck(
                new CheckRequestBody()
                    .name("name")
                    .description("description")
                    .firstComponentId(1L)
                    .secondComponentId(2L)
                    .creationTicket("something/ticket")
                    .entityId(1L)
                    .yqlQuery("SELECT 1")
                    .resultTable("result_table")
                    .escalationLevelId(1L)
                    .cronSchedule("0 30 11 * * ? *")
                    .runType(
                        new RunType()
                            .timeWindowSize(1)
                            .timeWindowUnit(RunType.TimeWindowUnitEnum.DAYS)),
                clock.instant(),
                "ivan-ivanov",
                null);

        Check check = schedulerDao.getScheduler(id, SchedulerDao.CHECK_ROW_MAPPER).orElse(null);

        assertThat(check, allOf(
                SchedulerMatchers.hasId(1L),
                SchedulerMatchers.hasName("name"),
                SchedulerMatchers.hasTimeWindowSize(1),
                SchedulerMatchers.hasTimeWindowUnit(ChronoUnit.DAYS)
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "createImport.before.csv",
            after = "createImport.after.csv")
    @DisplayName("Вставить импорт")
    void test_createImport() {
        Long id = schedulerDao.createImport(
                new ImportRequestBody()
                        .name("name")
                        .description("description")
                        .componentId(1L)
                        .creationTicket("ticket")
                        .entityId(1L)
                        .yqlQuery("SELECT 1")
                        .escalationLevelId(1L)
                        .cronSchedule("0 30 11 * * ? *")
                        .runType(
                                new RunType()
                                        .timeWindowSize(1)
                                        .timeWindowUnit(RunType.TimeWindowUnitEnum.DAYS)),
                clock.instant(),
                "ivan-ivanov",
                null);

        ImportInfo imp = schedulerDao.getScheduler(id, SchedulerDao.IMPORT_ROW_MAPPER).orElse(null);

        assertThat(imp, allOf(
                SchedulerMatchers.hasId(1L),
                SchedulerMatchers.hasName("name"),
                SchedulerMatchers.hasStatus(SchedulerStatus.TO_APPROVE)
        ));
    }

    @Test
    @DbUnitDataSet(before = "getCheckInfo.before.csv")
    @DisplayName("Проверка получения информации об выполнении сверки")
    void test_getCheckInfo() {
        Map<TicketPlaceholders, String> stInfo = schedulerDao.getStInfo("1=").orElse(null);

        assertNotNull(stInfo);
        assertEquals(stInfo.get(ESCALATION_LEVEL), EscalationLevelEnum.CRIT.name());

        stInfo = schedulerDao.getStInfo("2=").orElse(null);

        assertNotNull(stInfo);
        assertEquals(stInfo.get(ESCALATION_LEVEL), "OTHER");
    }

    @Test
    @DbUnitDataSet(before = "getScheduler.before.csv")
    @DisplayName("Проверка получения импорта и сверки")
    void test_getScheduler() {
        Check check = schedulerDao.getScheduler(1L, SchedulerDao.CHECK_ROW_MAPPER).orElse(null);

        assertNotNull(check);
        assertThat(check, allOf(
                SchedulerMatchers.hasId(1L),
                SchedulerMatchers.hasName("check1"),
                SchedulerMatchers.hasStatus(SchedulerStatus.TO_APPROVE),
                CheckMatchers.hasCheckFields(List.of("count", "sum"))
        ));

        ImportInfo imp = schedulerDao.getScheduler(2L, SchedulerDao.IMPORT_ROW_MAPPER).orElse(null);

        assertNotNull(imp);
        assertThat(imp, allOf(
                SchedulerMatchers.hasId(2L),
                SchedulerMatchers.hasName("import2"),
                SchedulerMatchers.hasStatus(SchedulerStatus.ACTIVE)
        ));
    }

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("getSchedulersArguments")
    @DbUnitDataSet(before = "getScheduler.before.csv")
    @DisplayName("Проверка пейджированного поиска")
    void test_getSchedulers(
            SchedulerFilter filter,
            long expectedTotalCount,
            List<Long> expectedIds
    ) {
        List<Scheduler> schedulers = schedulerDao.getSchedulers(0, 10, "id", SortType.ASC,
                filter, SchedulerDao.SCHEDULER_ROW_MAPPER);
        Assert.assertThat(schedulers.stream().map(Scheduler::getId).collect(Collectors.toList()),
                is(expectedIds));

        assertEquals(schedulerDao.getSchedulerCount(filter), expectedTotalCount);
    }

    static Stream<Arguments> getSchedulersArguments() {
        return Stream.of(
                Arguments.of( // 1
                        new SchedulerFilter(),
                        4,
                        List.of(1L, 2L, 3L, 4L)
                ),
                Arguments.of( // 2
                        new SchedulerFilter().schedulerType(SchedulerType.IMPORT),
                        2,
                        List.of(2L, 4L)
                ),
                Arguments.of( // 3
                        new SchedulerFilter().name("check%"),
                        2,
                        List.of(1L, 3L)
                ),
                Arguments.of( // 4
                        new SchedulerFilter().author("ivan-ivanov"),
                        2,
                        List.of(1L, 4L)
                ),
                Arguments.of( // 5
                        new SchedulerFilter().createdAtLower(CheckerUtils.toOffsetDateTime(LocalDateTime.of(2020, 10, 2, 0, 0, 0))),
                        3,
                        List.of(2L, 3L, 4L)
                ),
                Arguments.of( // 6
                        new SchedulerFilter().createdAtUpper(CheckerUtils.toOffsetDateTime(LocalDateTime.of(2020, 10, 2, 0, 0, 0))),
                        1,
                        List.of(1L)
                ),
                Arguments.of( // 7
                        new SchedulerFilter().lastSuccessfulRunLower(CheckerUtils.toOffsetDateTime(LocalDateTime.of(2020, 10, 1, 11, 30, 0))),
                        2,
                        List.of(3L, 4L)
                ),
                Arguments.of( // 8
                        new SchedulerFilter().lastSuccessfulRunUpper(CheckerUtils.toOffsetDateTime(LocalDateTime.of(2020, 10, 1, 11, 30, 0))),
                        2,
                        List.of(1L, 2L)
                )
       );
    }

    @Test
    @DbUnitDataSet(
            before = "updateCheckState.before.csv",
            after = "updateCheckState.after.csv")
    @DisplayName("Проверка обновления статуса")
    void test_setStatus() {
        schedulerDao.setStatus(1L, SchedulerStatus.APPROVED);
    }

    @Test
    @DbUnitDataSet(before = "getLastTasks.before.csv")
    @DisplayName("Проверка получения результатов работы тасок")
    void test_getLastTasks() {
        Set<TaskResult> taskResults = schedulerDao.getLastTasks();
        Assert.assertThat(taskResults, hasSize(3));
        Assert.assertThat(taskResults, equalTo(TASK_RESULTS));
    }

    @ParameterizedTest(name = "[{index}] id = {0}, page = {1}, pageSize = {2}, sortType = {3}, expectedCount = {4}")
    @MethodSource("getSchedulerHistorySuccess")
    @DbUnitDataSet(
            before = "getHistory.before.csv",
            after = "getHistory.before.csv"
    )
    @DisplayName("Тест на получение истории сверок")
    void test_getSchedulerHistory(
            long id,
            Integer page,
            Integer pageSize,
            String sortType,
            long expectedCount
    ) {
        List<Scheduler> schedulersHistory = schedulerDao.getSchedulersHistory(
                id,
                page,
                pageSize,
                sortType,
                SchedulerDao.SCHEDULER_ROW_MAPPER);

        assertEquals(expectedCount, schedulersHistory.size());
    }

    private static Stream<Arguments> getSchedulerHistorySuccess() {
        return Stream.of(
                Arguments.of(3, 0, 50, "ASC", 3),
                Arguments.of(3, 0, 2, "ASC", 2),
                Arguments.of(3, 1, 2, "ASC", 1),
                Arguments.of(4, 0, 50, "ASC", 1),
                Arguments.of(5, 0, 50, "ASC", 0)
        );
    }

    private static final Set<TaskResult> TASK_RESULTS = Set.of(
            new TaskResult("one", OperationStatus.COMPLETED,"yql-one"),
            new TaskResult("two", OperationStatus.ABORTED,"yql-two"),
            new TaskResult("three", OperationStatus.RUNNING,"yql-three"));
}
