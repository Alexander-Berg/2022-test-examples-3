package ru.yandex.autotests.direct.tests.campaign.searchqueries.csv;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.steps.TestFeatures;
import ru.yandex.autotests.direct.steps.utils.Converters;
import ru.yandex.autotests.direct.tests.campaign.searchqueries.SearchQueryReportTestBase;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Features(TestFeatures.SEARCH_QUERIES)
@Title("Тест выгрузки в csv статистики c ндс по поисковым запросам")
public class SearchQueriesWithNdsCSVTest extends SearchQueryReportTestBase {
    @Override
    protected Function<File, List<List<String>>> provideFileParser() {
        return Converters::tableFromCsv;
    }

    @Test
    @Title("тест статистики с ндс по поисковым запросам csv")
    public void searchQueriesWithNdsReportCSVTest() {
        super.test();
    }

    @Override
    protected ShowStatRequest getRequest() {
        return getRequestWithNds().withFileFormat(ReportFileFormat.CSV);
    }
}
