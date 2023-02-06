package ru.yandex.autotests.direct;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.excelstatistic.ReportFileFormat;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatGroupEnum;
import ru.yandex.autotests.direct.cmd.data.showcampstat.StatTypeEnum;
import ru.yandex.autotests.direct.cmd.data.stat.report.ShowStatRequest;
import ru.yandex.autotests.direct.steps.utils.Converters;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.directapi.model.User;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.direct.cmd.data.Logins.SUPER;
import static ru.yandex.autotests.direct.steps.XlsTestProperties.getXlsTestProperties;
import static ru.yandex.autotests.direct.steps.matchers.TableDifferMatcher.equalToTable;
import static ru.yandex.autotests.direct.steps.utils.ListUtils.joining;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.ADEPTH;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.AV_SUM;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.BOUNCE_RATIO;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.CLICKS;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.CTR;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.FP_CLICKS_AVG_POS;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.FP_SHOWS_AVG_POS;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.SHOWS;
import static ru.yandex.autotests.direct.web.data.statistic.ReportWizardColumns.SUM;
import static ru.yandex.autotests.direct.web.data.statistic.SearchQueriesSlices.ADGROUP;
import static ru.yandex.autotests.direct.web.data.statistic.SearchQueriesSlices.BANNER;
import static ru.yandex.autotests.direct.web.data.statistic.SearchQueriesSlices.CAMPAIGN;
import static ru.yandex.autotests.direct.web.data.statistic.SearchQueriesSlices.CAMPAIGN_TYPE;
import static ru.yandex.autotests.direct.web.data.statistic.SearchQueriesSlices.CONTEXTCOND_EXT;
import static ru.yandex.autotests.direct.web.data.statistic.SearchQueriesSlices.SEARCH_QUERY;

public class TableDifferTest {
    private static final String LOGIN = "smyinet-01";

    private List<List<String>> result1;
    private List<List<String>> result2;

    @Before
    public void setUp() {
        assumeThat("тест запускается локально", getXlsTestProperties().isLocalRun(), equalTo(true));

        System.setProperty("direct.stage", "TS");
        DirectTestRunProperties properties = DirectTestRunProperties.getInstance();
        DirectCmdSteps user = new DirectCmdSteps();
        user.authSteps().authenticate(User.get(SUPER));
        ShowStatRequest request = new ShowStatRequest()
                .withStatType(StatTypeEnum.SEARCH_QUERIES)
                .withFileFormat(ReportFileFormat.CSV)
                .withPageSize("100")
                .withWithNds("0")
                .withShowStat("1")
                .withStatPeriods("")
                .withColumns(joining(SHOWS, CLICKS, CTR, SUM, AV_SUM))
                .withColumnsPosition(joining(SHOWS, CLICKS, CTR, SUM, AV_SUM, FP_SHOWS_AVG_POS,
                        FP_CLICKS_AVG_POS, BOUNCE_RATIO, ADEPTH))
                .withGroupBy(joining(SEARCH_QUERY, CAMPAIGN, ADGROUP, BANNER, CONTEXTCOND_EXT))
                .withGroupByPositions(joining(SEARCH_QUERY, CAMPAIGN_TYPE, ADGROUP, BANNER, CONTEXTCOND_EXT))
                .withGroupByDate(StatGroupEnum.DAY.toString())
                .withDateFrom("2017-07-24")
                .withDateTo("2017-07-30")
                .withUlogin(LOGIN);

        result1 = Converters.tableFromCsv(user.excelStatisticSteps()
                .exportShowStatStatisticReport(request));

        result2 = Converters.tableFromCsv(user.excelStatisticSteps()
                .exportShowStatStatisticReport(request.withDateTo("2017-07-31")));

    }

    @Test
    public void tableDifferMatcherTest() {
        assertThat("результаты различаются", result1, not(equalToTable(result2)));
    }
}
