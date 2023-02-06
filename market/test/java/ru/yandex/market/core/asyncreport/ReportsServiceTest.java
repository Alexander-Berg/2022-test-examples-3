package ru.yandex.market.core.asyncreport;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.asyncreport.exception.ReportException;
import ru.yandex.market.core.asyncreport.model.ReportGenerationInfo;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.core.param.model.EntityName;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiCollectors;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasDescription;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasId;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasParamsItem;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasPartnerId;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasReportType;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasRequestCreatedAt;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasState;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasStateUpdatedAt;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasTouchedAt;
import static ru.yandex.market.core.asyncreport.ReportInfoMatchers.hasUrlToDownload;

/**
 * Тесты для {@link ReportsService}.
 */
@DbUnitDataSet(before = "AsyncReportsServiceTest.common.before.csv")
class ReportsServiceTest extends FunctionalTest {
    private static int reportIdGenerator = 1;
    private static final long PARTNER_ID_123 = 123L;
    private static final long PARTNER_ID_321 = 321L;
    private static final long PARTNER_ID_444 = 444L;
    private static final long PARTNER_ID_654 = 654;
    private static final int REPORTS_QUEUE_LIMIT = 10;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    private ReportsService<ReportsType> reportsService;

    private static Stream<Arguments> getReportUrlArgs() {
        return Stream.of(
                arguments(
                        "Отчёт еще не обработан. Процесс генерации не завершён и не должно быть url",
                        PARTNER_ID_123,
                        "1",
                        false,
                        null
                ),
                arguments(
                        "Отчёт в обработке. Процесс генерации не завершён и не должно быть url",
                        PARTNER_ID_123,
                        "2",
                        false,
                        null
                ),
                arguments(
                        "Отчёт готов. Процесс генерации завершён и должен быть url",
                        PARTNER_ID_123,
                        "3",
                        true,
                        "https://result-files.yandex-team.ru"
                ),
                arguments(
                        "При построении отчёта произошла ошибка. Процесс зафеёлился и не должно быть url",
                        PARTNER_ID_123,
                        "4",
                        false,
                        null
                ),
                arguments(
                        "Пустой отчет. Процесс генерации завершён и не должно быть url",
                        PARTNER_ID_123,
                        "5",
                        true,
                        null
                )
        );
    }

    @BeforeEach
    void init() {
        ReportsServiceSettings<ReportsType> reportTypesReportsServiceSettings =
                new ReportsServiceSettings.Builder<ReportsType>()
                        .setReportsQueueLimit(REPORTS_QUEUE_LIMIT)
                        .build();

        reportsService = new ReportsService<>(
                reportTypesReportsServiceSettings,
                new ReportsDao<>(jdbcTemplate, ReportsType.class),
                transactionTemplate,
                () -> Integer.toString(reportIdGenerator++),
                Clock.fixed(DateTimes.toInstantAtDefaultTz(2019, 8, 21), ZoneOffset.UTC),
                new DisabledAsyncReportService<>(jdbcTemplate),
                environmentService
        );
    }

    @Test
    @DisplayName("Проверяет, что метод успешно принимает запрос на генерацию отчёта")
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.requestReportGeneration.before.csv",
            after = "AsyncReportsServiceTest.requestReportGeneration.after.csv"
    )
    void requestReportGeneration() {
        reportsService.requestReportGeneration(getBasicReportInfo());
        reportsService.requestReportGeneration(getExperimentalReportInfo());
    }

    @Test
    @DisplayName("Проверяет, что метод успешно отдаваёт список всех отчётов поставщика")
    @DbUnitDataSet(before = "AsyncReportsServiceTest.getReports.before.csv")
    void getReports() {
        List<ReportInfo<ReportsType>> results = reportsService.getReportInfos(PARTNER_ID_123,
                ReportsType.STOCKS_ON_WAREHOUSES);
        assertThat(
                results,
                contains(
                        allOf(
                                hasId("3"),
                                hasPartnerId(PARTNER_ID_123),
                                hasReportType(ReportsType.STOCKS_ON_WAREHOUSES),
                                hasParamsItem("from", "2019-02-15"),
                                hasParamsItem("to", "2019-04-20"),
                                hasRequestCreatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 24)),
                                hasState(ReportState.DONE),
                                hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 25)),
                                hasUrlToDownload("https://result-files.yandex-team.ru")
                        ),
                        allOf(
                                hasId("2"),
                                hasPartnerId(PARTNER_ID_123),
                                hasReportType(ReportsType.STOCKS_ON_WAREHOUSES),
                                hasParamsItem("from", "2019-01-13"),
                                hasParamsItem("to", "2019-01-14"),
                                hasRequestCreatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                                hasState(ReportState.PENDING),
                                hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21))
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Проверка получения url отчёта")
    @MethodSource("getReportUrlArgs")
    @DbUnitDataSet(before = "AsyncReportsServiceTest.isReportComplete_getReportUrl.before.csv")
    void getReportUrlAndIsReportComplete(
            String testDescription,
            long partnerId,
            String reportId,
            boolean isReportComplete,
            @Nullable String expectedUrlToDownload
    ) {
        assertThat(reportsService.isReportComplete(partnerId, reportId), equalTo(isReportComplete));
        ReportInfo<ReportsType> reportInfo = reportsService.getReportInfo(partnerId, reportId);
        assertThat(reportInfo.getUrlToDownload(), equalTo(expectedUrlToDownload));
    }

    @Test
    @DisplayName("Получить отчёт, который нужно сгенерировать")
    @DbUnitDataSet(before = "AsyncReportsServiceTest.receivePendingReport.before.csv")
    void receivePendingReport() {
        Set<ReportsType> reportTypes = Stream.of(ReportsType.DAILY_STOCKS, ReportsType.STOCKS_ON_WAREHOUSES,
                ReportsType.DSBS_ORDERS).collect(toSet());
        ReportInfo<ReportsType> dsbsResult = reportsService.fetchPendingReport(reportTypes)
                .orElse(null);

        ReportInfo<ReportsType> firstResult = reportsService.fetchPendingReport(reportTypes)
                .orElse(null);

        ReportInfo<ReportsType> secondResult = reportsService.fetchPendingReport(reportTypes)
                .orElse(null);

        ReportInfo<ReportsType> thirdResult = reportsService.fetchPendingReport(reportTypes)
                .orElse(null);

        assertThat(
                dsbsResult,
                allOf(
                        hasId("12"),
                        hasPartnerId(111),
                        hasReportType(ReportsType.DSBS_ORDERS),
                        hasParamsItem("from", "2019-02-15"),
                        hasParamsItem("to", "2019-04-20"),
                        hasRequestCreatedAt(DateTimes.toInstantAtDefaultTz(2019, 6, 1)),
                        hasState(ReportState.PROCESSING),
                        hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                        hasTouchedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21))
                )
        );

        assertThat(
                firstResult,
                allOf(
                        hasId("9"),
                        hasPartnerId(PARTNER_ID_654),
                        hasReportType(ReportsType.DAILY_STOCKS),
                        hasParamsItem("from", "2019-01-13"),
                        hasParamsItem("to", "2019-01-14"),
                        hasRequestCreatedAt(DateTimes.toInstantAtDefaultTz(2019, 7, 13)),
                        hasState(ReportState.PROCESSING),
                        hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                        hasTouchedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21))
                )
        );

        assertThat(
                secondResult,
                allOf(
                        hasId("4"),
                        hasPartnerId(PARTNER_ID_444),
                        hasReportType(ReportsType.STOCKS_ON_WAREHOUSES),
                        hasParamsItem("from", "2019-02-15"),
                        hasParamsItem("to", "2019-04-20"),
                        hasRequestCreatedAt(DateTimes.toInstantAtDefaultTz(2019, 7, 1)),
                        hasState(ReportState.PROCESSING),
                        hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                        hasTouchedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21))
                )
        );

        assertThat(
                thirdResult,
                allOf(
                        hasId("3"),
                        hasPartnerId(PARTNER_ID_123),
                        hasReportType(ReportsType.DAILY_STOCKS),
                        hasParamsItem("from", "2019-01-13"),
                        hasParamsItem("to", "2019-01-14"),
                        hasRequestCreatedAt(DateTimes.toInstantAtDefaultTz(2019, 7, 11)),
                        hasState(ReportState.PROCESSING),
                        hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                        hasTouchedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21))
                )
        );
    }

    @Test
    @Disabled("Починить, поломано быстрофиксом https://a.yandex-team.ru/review/2640737/details")
    @DisplayName("Обновить url для скачивания отчёта")
    @DbUnitDataSet(before = "AsyncReportsServiceTest.finishReportGeneration.before.csv")
    void finishReportGeneration() {
        testSavedUrlAndState("https://y.ru/report_1.xlsx", "1", PARTNER_ID_123);
        testSavedUrlAndState("https://y.ru/report_2.xlsx", "2", PARTNER_ID_444);

        // Отчёт с id = 2 уже в статусе ReportState.DONE после вызова предыдущей строчки
        Exception e = assertThrows(
                ReportException.class,
                () -> testSavedUrlAndState("https://y.ru/report_2.xlsx", "2", PARTNER_ID_444)
        );
        assertEquals(e.getMessage(), "The report with id: 2 have already been in the final state: DONE");
    }

    private void testSavedUrlAndState(String reportUrl, String reportId, long partnerId) {

        reportsService.updateReportState(
                reportId,
                ReportState.DONE,
                ReportGenerationInfo.builder().setUrlToDownload(reportUrl).build()
        );
        ReportInfo<ReportsType> reportInfo = reportsService.getReportInfos(partnerId, ReportsType.DAILY_STOCKS)
                .stream()
                .filter(r -> Objects.equals(r.getId(), reportId))
                .collect(MbiCollectors.atMostOneElementOrThrow((ignored) -> "Не может быть два отчёта с одинаковым id"))
                .orElse(null);

        assertThat(
                reportInfo,
                allOf(
                        hasState(ReportState.DONE),
                        hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                        hasUrlToDownload(reportUrl),
                        hasDescription(null)
                )
        );
    }

    @Test
    @Disabled("Починить, поломано быстрофиксом https://a.yandex-team.ru/review/2640737/details")
    @DisplayName("Обновить описание ошибки генерации отчёта")
    @DbUnitDataSet(before = "AsyncReportsServiceTest.failReportGeneration.before.csv")
    void failReportGeneration() {
        testSavedFailDescriptionAndState("Слишком большой отчёт", "1", PARTNER_ID_123);

        // Отчёт с id = 2 уже в статусе ReportState.DONE
        Exception e = assertThrows(
                ReportException.class,
                () -> testSavedFailDescriptionAndState("Строки не помещаются в excel", "2", PARTNER_ID_444)
        );
        assertEquals(e.getMessage(), "The report with id: 2 have already been in the final state: DONE");
    }

    private void testSavedFailDescriptionAndState(String description, String reportId, long partnerId) {
        reportsService.updateReportState(
                reportId,
                ReportState.FAILED,
                ReportGenerationInfo.builder().setDescription(description).build()
        );

        ReportInfo<ReportsType> reportInfo = reportsService.getReportInfos(partnerId, ReportsType.DAILY_STOCKS)
                .stream()
                .filter(r -> Objects.equals(r.getId(), reportId))
                .collect(MbiCollectors.atMostOneElementOrThrow((ignored) -> "Не может быть два отчёта с одинаковым id"))
                .orElse(null);

        assertThat(
                reportInfo,
                allOf(
                        hasState(ReportState.FAILED),
                        hasStateUpdatedAt(DateTimes.toInstantAtDefaultTz(2019, 8, 21)),
                        hasDescription(description),
                        hasUrlToDownload(null)
                )
        );
    }

    @Test
    @DisplayName("Удалить отчет")
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.deleteReport.before.csv",
            after = "AsyncReportsServiceTest.deleteReport.after.csv"
    )
    void deleteReport() {
        Assertions.assertDoesNotThrow(() -> reportsService.cancelReport(123, "1"));
    }

    @Test
    @DisplayName("Попытаться удалить несуществующий отчет")
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.deleteReport.before.csv",
            after = "AsyncReportsServiceTest.deleteReport.before.csv"
    )
    void deleteReportWrongId() {
        Assertions.assertThrows(ReportException.class, () -> reportsService.cancelReport(123, "999"));
    }

    @Test
    @DisplayName("Попытаться удалить отчет другого поставщика")
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.deleteReport.before.csv",
            after = "AsyncReportsServiceTest.deleteReport.before.csv"
    )
    void deleteReportWrongPartnerId() {
        Assertions.assertThrows(ReportException.class, () -> reportsService.cancelReport(444, "1"));
    }

    @DisplayName("Обновление промежуточного состояния PROCESSING отчета")
    @Test
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.touch.before.csv",
            after = "AsyncReportsServiceTest.touchProcessing.after.csv"
    )
    void touchProcessing() {
        reportsService.touch("1", 50, "{'my_custom_progress': '50/100'}");
        // Заодно проверим, что читается правильно
        ReportInfo<?> report = reportsService.getReportInfo(123, "1");
        assertThat(report, allOf(
                hasProperty("progress", equalTo(50)),
                hasProperty("extendedState", equalTo("{'my_custom_progress': '50/100'}")),
                hasProperty("touchedAt", equalTo(DateTimes.toInstantAtDefaultTz(2019, 8, 21)))
        ));
    }

    @DisplayName("Прогресс не должен выходить за пределы 0-100")
    @Test
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.touch.before.csv",
            after = "AsyncReportsServiceTest.touch.before.csv"
    )
    void touchWrongProgress() {
        assertThrows(ReportException.class, () ->
                reportsService.touch("1", -100, "{'my_custom_progress': '50/100'}")
        );
    }

    @DisplayName("Нельзя обновлять промежуточное состояние у незапущенного отчета")
    @Test
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.touch.before.csv",
            after = "AsyncReportsServiceTest.touch.before.csv"
    )
    void touchWrongState() {
        assertThrows(ReportException.class, () ->
                reportsService.touch("2", 50, "{'my_custom_progress': '50/100'}")
        );
    }

    @DisplayName("Нельзя обновлять промежуточного состояние у несуществующего отчета")
    @Test
    @DbUnitDataSet(
            before = "AsyncReportsServiceTest.touch.before.csv",
            after = "AsyncReportsServiceTest.touch.before.csv"
    )
    void touchNotFound() {
        assertThrows(ReportException.class, () ->
                reportsService.touch("3", 50, "{'my_custom_progress': '50/100'}")
        );
    }

    private ReportRequest<ReportsType> getBasicReportInfo() {
        return ReportRequest.<ReportsType>builder()
                .setEntityId(PARTNER_ID_123)
                .setReportType(ReportsType.DAILY_STOCKS)
                .setEntityName(ReportsType.DAILY_STOCKS.getEntityName())
                .setParams(ParamsUtils.convertParamsToMap("{\"from\":\"2019-01-13\",\"to\":\"2019-01-14\", " +
                        "\"partnerId\": 1234}"))
                .build();
    }

    private ReportRequest<ReportsType> getExperimentalReportInfo() {
        return ReportRequest.<ReportsType>builder()
                .setEntityId(PARTNER_ID_321)
                .setReportType(ReportsType.DAILY_STOCKS)
                .setEntityName(ReportsType.DAILY_STOCKS.getEntityName())
                .setParams(ParamsUtils.convertParamsToMap("{\"ora2pg_experimental_yt_gen\":true, \"partnerId\": 321}"))
                .build();
    }

    @Test
    @DisplayName("У каждого типа отчета должна быть указана сущность, с которой он работает")
    void testReportEntity() {
        for (ReportsType reportType : ReportsType.values()) {
            EntityName entityName = reportType.getEntityName();
            Assertions.assertNotNull(entityName, "ReportType without EntityName: " + reportType);
        }
    }
}
