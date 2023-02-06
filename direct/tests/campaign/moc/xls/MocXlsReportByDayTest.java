package ru.yandex.autotests.direct.tests.campaign.moc.xls;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.autotests.direct.steps.utils.Converters;
import ru.yandex.autotests.direct.tests.campaign.moc.MocReportTestBase;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Features(TestFeatures.REPORT_WIZARD_CAMPAIGN)
@Title("Тест выгрузки в xls МОК за день статистики")
public class MocXlsReportByDayTest extends MocReportTestBase {
    @Override
    protected ShowStatRequest getRequest() {
        return getGroupByDayRequest().withFileFormat(ReportFileFormat.XLS);
    }

    @Override
    protected Function<File, List<List<String>>> provideFileParser() {
        return Converters::tableFromFirstExcelSheet;
    }

    @Test
    @Title("Тест выгрузки в xls МОК за день статистики")
    public void mocXlsReportByDayTest() {
        super.test();
    }
}
