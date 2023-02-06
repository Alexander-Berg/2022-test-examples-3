package ru.yandex.autotests.direct.tests.login.searchqueries.csv;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.autotests.direct.steps.utils.Converters;
import ru.yandex.autotests.direct.tests.login.searchqueries.SearchQueryLoginReportTestBase;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Features(TestFeatures.SEARCH_QUERIES)
@Title("Тест выгрузки в csv статистики по поисковым запросам (логин) с НДС")
public class SearchQueryLoginWithNdsCSVTest extends SearchQueryLoginReportTestBase {
    @Override
    protected Function<File, List<List<String>>> provideFileParser() {
        return Converters::tableFromCsv;
    }

    @Test
    @Title("тест статистики по поисковым запросам csv (логин) с НДС")
    public void searchQueryLoginReportWithNdsCSVTest() {
        super.test();
    }

    @Override
    protected ShowStatRequest getRequest() {
        return getRequestWithNds().withFileFormat(ReportFileFormat.CSV);
    }
}
