package ru.yandex.autotests.direct.tests.login.mol.xls;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.autotests.direct.steps.utils.Converters;
import ru.yandex.autotests.direct.tests.login.mol.MolReportTestBase;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Features(TestFeatures.REPORT_WIZARD_LOGIN)
@Title("Тест выгрузки в xls МОЛ статистики по неделям")
public class MolXlsByWeekTest extends MolReportTestBase {

    @Override
    protected Function<File, List<List<String>>> provideFileParser() {
        return Converters::tableFromFirstExcelSheet;
    }

    @Test
    @Title("Тест выгрузки в xls МОЛ статистики по неделям")
    public void molXlsByWeekTest() {
        super.test();
    }

    @Override
    protected ShowStatRequest getRequest() {
        return getGroupByWeekRequest().withFileFormat(ReportFileFormat.XLS);
    }
}
