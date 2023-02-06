package ru.yandex.market.admin.service.remote.asyncreport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.service.remote.RemoteAsyncReportUIService;
import ru.yandex.market.admin.ui.model.asyncreport.UIAsyncReport;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DbUnitDataSet(before = "RemoteAsyncReportUIServiceTest.before.csv")
public class RemoteAsyncReportUIServiceTest extends FunctionalTest {
    @Autowired
    private RemoteAsyncReportUIService remoteAsyncReportUIService;

    @Test
    @DisplayName("Запуск отчета по миграции падает, потому что canMigrate вернул false. Партнер не в ЕКате.")
    void testdryRunReport_canMigrateFalse_fail() {
        assertThat(assertThrows(
                RuntimeException.class,
                () -> remoteAsyncReportUIService.runBusinessChangeReport(12, 101))
        ).returns("Партнер (12) не в едином каталоге!", Throwable::getMessage);
    }

    // todo поправить тест. Тест проверяет, что партнер уже в екает, но валится ошибка, что он не в екате
    @Test
    @DisplayName("Запуск отчета по миграции в ЕКат падает, потому что canMigrate вернул false. Поставщик уже в ЕКате.")
    void testdryRunReport_canMigrateToUcatFalse_fail() {
        assertThat(assertThrows(
                RuntimeException.class,
                () -> remoteAsyncReportUIService.runBusinessChangeReport(13, 101))
        ).returns("Партнер (13) не в едином каталоге!", Throwable::getMessage);
    }

    @Test
    @DisplayName("Запуск отчета по миграции")
    void testRunDryRunReport() {
        //when
        UIAsyncReport report = remoteAsyncReportUIService.runBusinessChangeReport(21, 101);
        //then
        assertNotNull(report.getStringField(UIAsyncReport.ID));
        assertEquals(ReportState.PENDING.name(), report.getStringField(UIAsyncReport.STATUS));
    }

    @Test
    @DisplayName("Запуск отчета по миграции в екат")
    @DbUnitDataSet(after = "RemoteAsyncReportUIServiceTest.ucat.after.csv")
    void testRunDryRunReportForUcat() {
        //when
        UIAsyncReport report = remoteAsyncReportUIService.runBusinessChangeReport(11, 100);
        //then
        assertNotNull(report.getStringField(UIAsyncReport.ID));
        assertEquals(ReportState.PENDING.name(), report.getStringField(UIAsyncReport.STATUS));
    }

    @Test
    @DisplayName("Получение информации об отчете")
    void testGetReportInfo() {
        //when
        UIAsyncReport report = remoteAsyncReportUIService.getLastBusinessChangeReport(20);
        //then
        assertNotNull(report.getStringField(UIAsyncReport.ID));
        assertEquals("2", report.getStringField(UIAsyncReport.ID));
        assertEquals(ReportState.DONE.name(), report.getStringField(UIAsyncReport.STATUS));
        assertEquals("https://y.ru/report_2.xlsx", report.getStringField(UIAsyncReport.URL));
    }

    @Test
    @DisplayName("Получение информации об отчете. Заезд в екат свежее миграции")
    @DbUnitDataSet(before = "RemoteAsyncReportUIServiceTest.ucat.before.csv")
    void testGetReportInfoForUcat() {
        //when
        UIAsyncReport report = remoteAsyncReportUIService.getLastBusinessChangeReport(20);
        //then
        assertNotNull(report.getStringField(UIAsyncReport.ID));
        assertEquals("3", report.getStringField(UIAsyncReport.ID));
        assertEquals(ReportState.DONE.name(), report.getStringField(UIAsyncReport.STATUS));
        assertEquals("https://y.ru/report_2.xlsx", report.getStringField(UIAsyncReport.URL));
    }

    @Test
    @DisplayName("Запуск отчета по миграции. Такого бизнеса нет")
    void testRunReportBusinessIsAbsent() {
        assertThrows(NullPointerException.class, () ->
                remoteAsyncReportUIService.runBusinessChangeReport(30, 101)
        );
    }

    @Test
    @DisplayName("Запуск отчета по миграции падает, потому что целевой бизнес не существует")
    void testdryRunReportDstBusinessNotExists() {
        assertThat(assertThrows(
                RuntimeException.class,
                () -> remoteAsyncReportUIService.runBusinessChangeReport(21, 999))
        ).returns("Бизнес 999 отсутствует или удален!", Throwable::getMessage);
    }
}
